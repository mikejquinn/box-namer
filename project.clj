(defproject box-namer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :main box-namer.handler
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [ring/ring-json "0.3.0"]
                 [org.clojure/core.incubator "0.1.3"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [enlive "1.1.5"] ; HTML transformation
                 [watchtower "0.1.1"] ; Reload HTML templates during development
                 [log4j/log4j "1.2.16" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [org.clojure/tools.logging "0.2.6"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler box-namer.handler/app
         :init box-namer.handler/init
         :port 3000})
