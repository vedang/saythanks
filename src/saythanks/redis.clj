(ns ^{:doc "Settings and other functions for Redis"
      :author "Vedang Manerikar <vedang@helpshift.com>"}
  saythanks.redis
  (:require [carmine.core :as r]))


(defonce ^{:doc "The global Redis pool."} pool nil)
(defonce ^{:doc "Server spec for Redis connection"} server-spec nil)

(defn init-redis!
  "Initialize Redis connection"
  {:arglists '([{:keys [host port password timeout]}])}
  [server-spec]
  (let [redis-pool (r/make-conn-pool :max-active 8)
        redis-server-spec (apply r/make-conn-spec
                                 (apply concat server-spec))]
    (alter-var-root #'pool (constantly redis-pool))
    (alter-var-root #'server-spec (constantly redis-server-spec))))


(defmacro redis
  "Basically like (partial with-conn pool spec-server1).
  Taken from Carmine documentation:
  https://github.com/ptaoussanis/carmine"
  [& body]
  `(r/with-conn pool server-spec ~@body))
