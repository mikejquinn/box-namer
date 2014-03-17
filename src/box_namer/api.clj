(ns box-namer.api
  (:use compojure.core
        ring.util.response
        [clojure.string :only [lower-case]]
        [ring.middleware.json :only [wrap-json-params wrap-json-response]])
  (:require [box-namer.naming :as naming]
            [box-namer.naming-utils :as naming-utils]))

(defn- invalid-request-error
  "Returns a structured error body which will be converted to JSON in the response."
  ([message] (invalid-request-error message 400))
  ([message status]
   {:status status
    :body {:message message}}))

(defn- missing-field-error
  [field]
  (invalid-request-error (format "Missing field name: '%s'" field)))

(defroutes api-routes
  (DELETE "/hostnames/:name" {:keys [params]}
   (let [hostname (:name params)]
     (if-let [[basename index] (naming-utils/split-name-components hostname)]
       (if (naming/deregister-name-for-basename (lower-case basename) index)
         {:status 204} ; Success, no content
         (invalid-request-error (format "'%s' has not been registered" hostname) 404))
       (invalid-request-error (format "'%s' is not a valid name" hostname)))))

  (POST "/hostnames" {:keys [params]}
    (if-let [basename (get params "basename")]
      (if (naming-utils/is-valid-name? basename)
        (let [index (naming/register-next-index-for-basename (lower-case basename))]
          (response {:name (format "%s%d" basename index)
                     :index index
                     :basename basename}))
        (invalid-request-error (format "'%s' is not a valid hostname" basename)))
      (missing-field-error "basename"))))

(def api
  (-> api-routes
      wrap-json-params
      wrap-json-response))
