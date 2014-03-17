(ns box-namer.handler
  (:use compojure.core
        [ring.adapter.jetty :only [run-jetty]])
  (:require [compojure.handler :as handler]
            [box-namer.api :as api]
            [box-namer.naming :as naming]))

(defroutes all-routes
  (GET "/hello" []
       {:status 200
        :header {"Content-Type" "text/plain"}
        :body "Hello"})

  (context "/api/v1" [] api/api))

(def app
  (-> all-routes
      identity))

(def init[])

(defn -main
  "Starts a Jetty webserver to serve our Ring app."
  []
  (init)
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (run-jetty app {:port port})))
