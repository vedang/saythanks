(ns ^{:doc "Settings and other functions for Redis"
      :author "Vedang Manerikar <vedang@helpshift.com>"}
  saythanks.redis
  (:require [taoensso.carmine :refer [wcar]]))


(defonce ^{:doc "Server connection for Redis"} server-conn (atom nil))

(defn init-redis!
  "Initialize Redis connection"
  {:arglists '([{:keys [host port password timeout]}])}
  [redis-server-spec]
  (let [redis-server-conn {:pool {:max-active 8}
                           :spec redis-server-spec}]
    (reset! server-conn redis-server-conn)))


(defmacro redis
  [& [a1 & an :as args]]
  (if (symbol? a1)
    `(wcar (deref ~a1) ~@an)
    `(wcar (deref server-conn) ~@args)))
