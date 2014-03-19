(ns box-namer.handler
  (:use compojure.core
        [ring.adapter.jetty :only [run-jetty]]
        [clojure.tools.logging :only [info]])
  (:require [compojure.handler :as handler]
            [box-namer.api :as api]
            [box-namer.naming :as naming]
            [box-namer.persistence :as persistence]))

(defroutes all-routes
  (context "/api/v1" [] api/api))

(def app
  (-> all-routes
      identity))

(defn init []
  ; Note that this shutdown hook will *not* be called when running via "lein run". This wouldn't affect
  ; us in production. See:
  ; http://stackoverflow.com/questions/10855559/shutdown-hook-doesnt-fire-when-running-with-lein-run
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (info "Shutting down...")
                               (persistence/shutdown-persistence)
                               (shutdown-agents)))))

(defn -main
  "Starts a Jetty webserver to serve our Ring app."
  []
  (init)
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (run-jetty app {:port port})))
