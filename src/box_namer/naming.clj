(ns box-namer.naming
  (:use [clojure.core.incubator :only [dissoc-in]]
        [clojure.string :only [trim]]
        [clojure.java.io :only [reader writer]])
  (:require [overtone.at-at :as at-at]
            [box-namer.file-utils :as file-utils]))

; A map of "basename" => #{set of registered indicies"}
; We use an atom to manage concurrent access.
(def ^:private name-buckets (atom {}))

; An array of buckets which need to be flushed to the filesystem
(def ^:private dirty-buckets (atom []))

; The frequency at which dirty buckets are checked and flushed to disk
(def ^:private flush-frequency 5000)

; A thread pool used to flush buckets to disk in the background
(def ^:private at-at-pool (at-at/mk-pool))

(def ^:private db-directory
  (file-utils/get-writable-directory-at-path (or (System/getenv "BOX_NAMER_DB_DIR") "./db")))

(defn read-set-from-file-at-path
  [path]
  (with-open [rdr (reader path)]
    (->> (map (fn [i] (Integer/parseInt i)) (line-seq rdr))
         (reduce #(conj %1 %2) #{}))))

(defn write-set-at-path
  [directory filename s]
  (letfn [(write-set [print-writer]
            (doseq [item s]
              (.println print-writer item)))]
    (file-utils/write-file-atomically directory filename write-set)))

(defn- flush-buckets-to-disk
  [directory])

(defn- mark-bucket-as-dirty
  [bucket]
  (swap! dirty-buckets (fn [buckets] (conj buckets bucket))))

(defn- find-lowest-missing-integer
  "Returns the lowest positive integer within a set.
  EX: (find-lowest-missing-integer #{3 1 2 5 6}) => 4"
  [bucket]
  (loop [i 1]
    (if (contains? bucket i)
      (recur (+ i 1))
      i)))

(defn deregister-name-for-basename
  "Deregisters a previously registered index for a particular basename.  If no more indicies are
  registered for a particular basename, that basename is removed from the global map.
  Returns true if the name was found, false otherwise."
  [basename index]
  ; Unfortunately must use a mutable variable because the swap! function only returns the newly swapped value.
  (let [found-name (atom false)]
    (swap! name-buckets (fn [buckets]
                          (if-let [bucket (get buckets basename)]
                            (if (contains? bucket index)
                              (do
                                (reset! found-name true)
                                (let [new-bucket (disj bucket index)]
                                  (if (empty? new-bucket)
                                    (dissoc-in buckets [basename])
                                    (assoc-in buckets [basename] new-bucket))))
                            buckets))))
    @found-name))

(defn register-next-index-for-basename
  "Given a name 'base', returns the next available full name (e.g. base4, base16, etc.)"
  [basename]
  (let [next-int (atom 1)]
    ; 'swap!' performs a compare-and-set to the name-buckets atom. The current value of the atom is passed
    ; to the anonymous function. The value of the atom is swapped if and only if another thread has
    ; not modified the atom in the time it took for the anon function to complete. If it was modified
    ; by another thread, the function is called again - therefore the anonymous function must be
    ; referentially transparent and free of side effects.
    (swap! name-buckets (fn [buckets]
                          (if-let [bucket (get buckets basename)]
                            (do
                              (swap! next-int (fn [i] (find-lowest-missing-integer bucket)))
                              (assoc-in buckets [basename] (conj bucket @next-int)))
                            (assoc-in buckets [basename] #{1}))))
    @next-int))
