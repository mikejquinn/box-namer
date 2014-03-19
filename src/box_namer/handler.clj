(ns box-namer.handler
  (:gen-class)
  (:use compojure.core
        net.cgrand.enlive-html
        [ring.adapter.jetty :only [run-jetty]]
        [clojure.tools.logging :only [info]])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [watchtower.core :as watcher]
            [box-namer.api :as api]
            [box-namer.naming :as naming]
            [box-namer.persistence :as persistence]))

(deftemplate layout-template "layout.html"
  [body]
  [:#content] (content body))

(defsnippet index-boxname-info "index.html" [:#index :li.boxname-info]
  [basename name-count]
  [:.boxname-info] (set-attr :data-boxname basename)
  [:.boxname] (content basename)
  [:.count] (content (str name-count)))

(defsnippet index-page "index.html" [:#index]
  [names-with-counts]
  [:li.boxname-info] (clone-for [[basename name-count] names-with-counts]
                       (substitute (index-boxname-info basename name-count))))

(defroutes all-routes
  (GET "/" []
    (let [boxnames (naming/get-current-names)
          names-with-counts (for [[basename indicies] boxnames] [basename (count indicies)])]
      (layout-template (index-page names-with-counts))))

  (route/resources "/")
  (context "/api/v1" [] api/api))

(def app
  (-> all-routes
      identity))

(defn init []
  (persistence/start-persistence naming/name-buckets)

  ; Reload resources folder whenever they change in development
  (when-not (= (System/getenv "RING_ENV") "production")
    (watcher/watcher ["resources"]
                     (watcher/rate 50) ;; poll every 50ms
                     (watcher/file-filter (watcher/extensions :html))
                     (watcher/on-change (fn [files] (require 'box-namer.handler :reload)))))
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
