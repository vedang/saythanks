(ns saythanks.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [clj-time.core :as time]
            [clojure.data.json :as json]
            [clojure.string :as cs]
            [taoensso.carmine :as r]
            [saythanks.redis :refer [init-redis! redis]]
            [saythanks.access :refer [access-token]])
  (:import [org.joda.time DateTime]))


;; IMP: create the file src/saythanks/access.clj
;; and add the following to it
;; (ns saythanks.access)
;; (def access-token "<your access token>")
;; Add your access token here.
;; Get it from https://developers.facebook.com/tools/explorer
;; you need to give the user_posts and publish_actions permissions


;; Add as many messages as you want here, one will be
;; picked at random. remember to add a %s where you
;; want the name to go. this is not optional.
(def thank-you-msgs ["Thank you so much, %s! :-)"
                     "Thanks, %s! I had a blast!"
                     "Thank you for the wishes, %s :-)"])


;; Start reading at -main. The following are the important functions
(declare poll-for-posts! say-thank-you poll-poll-poll)


(def birthday-since-key "birthday.since")
(def birthday-until-key "birthday.until")
(def unmatched-posts-key "unmatched.posts")
(def interesting-posts-key "interesting.posts")
(def msg-count (count thank-you-msgs))
(def facebook-graph-api-url "https://graph.facebook.com/v2.4/")
(def redis-server {:host "192.168.33.20" :port 6379})
;; Regex contributed by Kiran Kulkarni (@kirankulkarni)
(def happy-birthday-regex #"(?i)h?a+p+y(?:\s|.)*b?(?:irth|'| )?d+ay")


(defn -main
  "Poll FB for new posts on your wall. Thank kind folk."
  []
  (println "Start polling facebook for relevant posts")
  (if access-token
    (do (init-redis! redis-server)
        (poll-poll-poll))
    (println "You've forgotten to set your access-token in the code.")))


(defn poll-poll-poll
  "Keep the poor fellow polling."
  []
  (while true
    (let [timeout (+ (rand-int (- 300000 30000)) 30000)]
      (say-thank-you (poll-for-posts!))
      (println "Sleeping for " timeout " seconds.")
      (Thread/sleep timeout))))


(defn datetime->unix-timestamp
  "Converts the given datetime to unix-timestamp(seconds)"
  ([] (datetime->unix-timestamp (time/now)))
  ([^DateTime datetime]
     (str (long (/ (.getMillis datetime) 1000)))))


(defn update-time-tokens
  "Update the tokens from given url"
  [paging-url]
  (when paging-url
    (let [since-token (first (re-seq #"since=[0-9]+" paging-url))
          new-since (when since-token
                      (second (clojure.string/split since-token #"=")))
          until-token (first (re-seq #"until=[0-9]+" paging-url))
          new-until (when until-token
                      (second (clojure.string/split until-token #"=")))]
      (redis (r/set birthday-since-key new-since)
             (r/set birthday-until-key new-until)))))


(defn- log-interesting-wish
  "This person said something more than 'Happy Birthday, x'. Consider
  thanking him/her manually."
  [post]
  (redis (r/zadd interesting-posts-key
                 (datetime->unix-timestamp)
                 (json/json-str post))))


(defn thank-you-person
  "Thank the person individually.
  Like their post."
  [post]
  (let [thankee (:from post)
        thankyou-str (format (thank-you-msgs (rand-int msg-count))
                             (first (clojure.string/split (:name thankee)
                                                          #" ")))
        thanks-post-url (str facebook-graph-api-url
                             (:id post)
                             "/comments?"
                             (http/generate-query-string
                              {:access_token access-token
                               :message thankyou-str}))
        thanks-like-url (str facebook-graph-api-url
                             (:id post)
                             "/likes?"
                             (http/generate-query-string
                              {:access_token access-token}))]
    (println (format "%s said: %s. I said: %s"
                     (:name thankee)
                     (:message post)
                     thankyou-str))
    (when (> (count (cs/split (cs/replace (:message post)
                                          happy-birthday-regex
                                          "")
                              #"\s"))
             5)
      (println "Logging this person's post as interesting.")
      (log-interesting-wish post))
    ;; (http/post thanks-post-url)
    ;; (http/post thanks-like-url)
    ))


(defn birthday-matcher
  [post]
  (when (and (= (:type post) "status")
             (seq (:message post)))
    (re-seq happy-birthday-regex (:message post))))


(defn poll-for-posts!
  "Get posts from facebook. Update the since and until fields to used
  time-based pagination of the feed."
  []
  (let [since-time (or (redis (r/get birthday-since-key))
                       (datetime->unix-timestamp (time/from-now (time/days -3))))
        until-time (or (redis (r/get birthday-until-key))
                       (datetime->unix-timestamp (time/now)))
        url-args {:access_token access-token
                  :since since-time
                  :until until-time
                  :limit 10 ;; Max limit for v2.4 for the Facebook Graph API
                  :fields "id,message,from,type,properties,status_type,story"}
        feed-url (str facebook-graph-api-url
                      "me/feed?"
                      (http/generate-query-string url-args))
        feed-res (http/get feed-url)
        feed-res (json/read-json (if (= (:status feed-res) 200)
                                   (:body feed-res)
                                   "{}"))
        feed-data (:data feed-res)]
    (println "Fetched posts since : " since-time " and until: " until-time)
    (update-time-tokens (get-in feed-res [:paging :next]))
    (println "Data count = " (count feed-data))
    feed-data))


(defn say-thank-you
  "Say thank you to all the nice folk."
  [posts]
  (println "posts matching birthday regex = "
           (count (filter birthday-matcher posts)))
  (doseq [post posts]
    (if (birthday-matcher post)
      (thank-you-person post)
      ;;Log this post for manual intervention
      (redis (r/zadd unmatched-posts-key
                     (datetime->unix-timestamp)
                     (json/json-str post))))))
