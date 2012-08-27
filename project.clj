(defproject saythanks "0.1.0-SNAPSHOT"
  :description "Say thanks to all the wonderful people who wish you on
               your birthday."
  :url "http://vedang.me/techlog"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-http "0.4.3"]
                 [clj-time "0.4.3"]
                 [org.clojure/data.json "0.1.3" :exclusions [org.clojure/clojure]]]
  :min-lein-version "2.0.0"
  :warn-on-reflection true
  :main saythanks.core)
