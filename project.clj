(defproject saythanks "0.3.0"
  :description "Say thanks to all the wonderful people who wish you on
               your birthday."
  :url "http://vedang.me/techlog"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http "2.0.0"]
                 [clj-time "0.11.0"]
                 [com.taoensso/carmine "2.11.0"]
                 [org.clojure/data.json "0.2.6"
                  :exclusions [org.clojure/clojure]]]
  :min-lein-version "2.5.2"
  :global-vars {*warn-on-reflection* true}
  :aot [saythanks.core]
  :main saythanks.core)
