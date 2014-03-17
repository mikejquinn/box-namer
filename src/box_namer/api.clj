(ns box-namer.api
  (:use compojure.core))

(defroutes api-routes
  (GET "/" []
       {:status 200
        :header {"Content-Type" "text/plain"}
        :body "Hello api"}))
