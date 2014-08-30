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
;; you need to give the read_stream and publish_actions permissions


;; Add as many messages as you want here, one will be
;; picked at random. remember to add a %s where you
;; want the name to go. this is not optional.
(def thank-you-msgs ["Thank you so much, %s! :-)"
                     "Thanks, %s! I had a blast!"
                     "Thank you for the wishes, %s :-)"])


;; Start reading at -main. The following are the important functions
(declare poll-for-posts! say-thank-you poll-poll-poll)


(def birthday-since-key "birthday.since")
(def unmatched-posts-key "unmatched.posts")
(def msg-count (count thank-you-msgs))
(def facebook-graph-api-url "https://graph.facebook.com/v2.1/")
(def redis-server {:host "127.0.0.1" :port 6379})
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


(defn update-since-token
  "Update the since token from given url"
  [paging-url]
  (when paging-url
    (when-let [token (first (re-seq #"since=[0-9]+" paging-url))]
      (let [new-since (second (clojure.string/split token #"="))]
        (redis (r/set birthday-since-key new-since))))))


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
    (http/post thanks-post-url)
    (http/post thanks-like-url)))


(defn birthday-matcher
  [post]
  (when (:message post)
    (re-seq happy-birthday-regex (:message post))))


(defn poll-for-posts!
  "Get posts from facebook. Update the since field so that we don't get
  them again."
  []
  (let [since (or (redis (r/get birthday-since-key))
                  (datetime->unix-timestamp (time/from-now (time/days -1))))
        url-args {:access_token access-token
                  :since since
                  :limit 1000}
        feed-url (str facebook-graph-api-url
                      "me/feed?"
                      (http/generate-query-string url-args))
        feed-res (http/get feed-url)
        feed-res (json/read-json (if (= (:status feed-res) 200)
                                   (:body feed-res)
                                   "{}"))
        feed-data (:data feed-res)]
    (println "Fetched posts since : " since)
    (update-since-token (get-in feed-res [:paging :previous]))
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
      ;;If type status, then log this post for manual intervention
      (when (and (= "status" (:type post))
                 (seq (:message post)))
        (redis (r/zadd unmatched-posts-key
                       (datetime->unix-timestamp)
                       (json/json-str
                        (select-keys post [:id :from :message]))))))))
