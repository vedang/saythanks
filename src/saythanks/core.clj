(ns saythanks.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [clj-time.core :as time]
            [clojure.data.json :as json])
  (:import [org.joda.time DateTime]))


(def settings-map {;; your access token goes here. replace the nil with a
                   ;; string
                   ;; example: "asdskjdgkhg"
                   :access-token "AAACEdEose0cBAAZCaKVMuf0vfKdKxq32Fbl274XNSbGQ1PIDvZAiPj0t9m46mSZBifR7gOPJNMXL4WMGPC8i4iC2SJh4kDUwxIoNwsLvQZDZD"
                   :happy-birthday-regex #"[Hh]appy.*"
                   ;; name will go where the %s is
                   :thank-you-msg "Thank you so much, %s! :-)"
                   :facebook-graph-api-url "https://graph.facebook.com/"})


(defn datetime->unix-timestamp
  "Converts the given datetime to unix-timestamp(seconds)"
  ([] (datetime->unix-timestamp (time/now)))
  ([^DateTime datetime]
     (str (long (/ (.getMillis datetime) 1000)))))


(def ^:dynamic *since* (datetime->unix-timestamp (time/from-now (time/days -1))))


;; Start reading at -main. The following two are the important functions
(declare poll-for-posts! say-thank-you)


(defn -main
  "Poll FB for new posts on your wall. Thank kind folk."
  [& args]
  (println "Start polling facebook for relevant posts")
  (if (:access-token settings-map)
    (-> (poll-for-posts!)
        (say-thank-you))
    (println "You've forgotten to set your access-token in the code.")))


(defn update-since-token
  "Update the since token from given url"
  [paging-url]
  (when-let [token (first (re-seq #"since=[0-9]+" paging-url))]
    (let [new-since (second (clojure.string/split token #"="))]
      (alter-var-root #'*since* (constantly new-since)))))


(defn poll-for-posts!
  "Get posts from facebook. Update the since field so that we don't get
  them again."
  []
  (let [access-token (:access-token settings-map)
        url-args {:access_token access-token
                  :since *since*
                  :limit 1000}
        feed-url (str (:facebook-graph-api-url settings-map)
                      "me/feed?"
                      (http/generate-query-string url-args))
        feed-res (http/get feed-url)
        feed-res (json/read-json (if (= (:status feed-res) 200)
                                   (:body feed-res)
                                   "{}"))
        feed-data (:data feed-res)]
    (println "Fetched posts since : " *since*)
    (update-since-token (get-in feed-res [:paging :previous]))
    (println "Data count = " (count feed-data))
    feed-data))


(defn thank-you-person
  "Thank the person individually.
  Like their post if it's bigger than 5 words :-D"
  [post]
  (let [thankee (:from post)
        thankyou-str (format (:thank-you-msg settings-map)
                             (first (clojure.string/split (:name thankee)
                                                          #" ")))]
    (println thankyou-str)))


(defn birthday-matcher
  [post]
  (when (:message post)
    (re-seq (:happy-birthday-regex settings-map)
            (:message post))))


(defn say-thank-you
  "Say thank you to all the nice folk."
  [posts]
  (let [filtered-posts (filter birthday-matcher posts)]
    (println "posts matching birthday regex = " (count filtered-posts))
    (dorun (map thank-you-person filtered-posts))))
