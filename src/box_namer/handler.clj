(ns box-namer.handler
  (:use compojure.core
        [ring.adapter.jetty :only [run-jetty]])
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
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (println "Shutting down...")
                               (persistence/shutdown-persistence)
                               (shutdown-agents)))))

(defn -main
  "Starts a Jetty webserver to serve our Ring app."
  []
  (init)
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (run-jetty app {:port port})))
