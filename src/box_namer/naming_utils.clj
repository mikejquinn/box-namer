(ns box-namer.naming-utils)

(def ^:private name-with-index-regex #"([0-9]*)$")

; This is a basic case-insensitive hostname regex as found here:
; http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
(def ^:private name-regex
  (re-pattern "(?i)^(([a-z0-9]|[a-z0-9][a-z0-9\\-]*[a-z0-9])\\.)*([a-z0-9]|[a-z0-9][a-z0-9\\-]*[a-z0-9])$"))

(defn split-name-components
  "Splits a name into its basename and index. Returns a tuple for destructuring.
  Ex: (split-name-components 'api13') => ['api' 13]"
  [boxname]
  (let [[match index] (re-find name-with-index-regex boxname)]
    (if (not (= "" index))
      [(subs boxname 0 (- (count boxname) (count index))) (Integer/parseInt index)]
      nil)))

(defn is-valid-name? [boxname]
  (not (nil? (re-find name-regex boxname))))
