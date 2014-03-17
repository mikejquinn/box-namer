(defproject box-namer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :main box-namer.handler
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [ring/ring-json "0.3.0"]
                 [org.clojure/core.incubator "0.1.3"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler box-namer.handler/app})
