(ns box-namer.persistence
  (:use [clojure.java.io :only [reader]]
        [clojure.tools.logging :only [info debug trace]])
  (:require [box-namer.file-utils :as file-utils])
  (:import java.util.concurrent.LinkedBlockingQueue))

; The frequency at which we check for dirty buckets are enqueue them to be written to disk
(def ^:private flush-frequency 1000)

; Dirty buckets are placed into this queue to be written to disk
(def ^:private write-queue (new LinkedBlockingQueue))

; This token is added to the queue when we want the writing thread to exit
(def ^:private shutdown-token "SHUTDOWN QUEUE")

; While true, a background thread will move dirty buckets to the write queue
(def ^:private writing-data? (atom false))

; A set of buckets which need to be persisted
(def ^:private dirty-bucket-names (atom #{}))

; The extension for the stored boxname files
(def ^:private file-extension ".names")

; A writable directory for storing the boxname files
(def ^:private db-directory
  (file-utils/get-writable-directory-at-path (or (System/getenv "BOX_NAMER_DB_DIR") "./db")))

; Stores the thread responsible for flushing data in the background so that we can join it on shutdown
(def ^:private flushing-thread (atom nil))

(defn- queue-sequence
  "Returns a lazily evaluated sequence of items from the blocking write queue so that we can operate
  on the items in the queue in an idiomatic way. The sequence will end when it reaches the shutdown
  token."
  []
  (lazy-seq
    (let [bucket-name (.take write-queue)]
      (if (= bucket-name shutdown-token)
        nil
        (cons bucket-name (queue-sequence))))))

(defn- read-set-from-file-at-path
  "Reads a clojure set of integers from a file."
  [path]
  (with-open [rdr (reader path)]
    (->> (map (fn [i] (Integer/parseInt i)) (line-seq rdr))
         (reduce #(conj %1 %2) #{}))))

(defn- write-set-at-path
  "Atomically writes each integer in a set to a file."
  [directory filename s]
  (debug "Writing file" filename)
  (letfn [(write-set [print-writer]
            (doseq [item s]
              (.println print-writer item)))]
    (file-utils/write-file-atomically directory filename write-set)))

(defn process-write-queue
  "Spawns a background thread to process each item in the write queue."
  [name-buckets-atom]
  (future
    (doseq [bucket-name (queue-sequence)]
      (if-let [bucket (get @name-buckets-atom bucket-name)]
        (write-set-at-path db-directory (format "%s%s" bucket-name file-extension) bucket)
        ; Delete the file if the bucket is now empty so it isn't picked up if we reload
        (file-utils/delete-file-in-directory db-directory bucket-name)))))

(defn- build-flush-thread-action
  [name-buckets-atom]
  (fn []
    (let [write-queue-future (process-write-queue name-buckets-atom)]
      (while @writing-data?
        (trace "Data flush thread sleeping")
        (Thread/sleep flush-frequency)
        ; Every few seconds, we enqueue all of the dirty name buckets to be flushed to disk.
        ; This will protect us from writing the same file many times immediatly after a burst
        ; of naming changes.
        (locking dirty-bucket-names
          (doseq [bucket-name @dirty-bucket-names]
            (debug "Queueing bucket" bucket-name "to be written to disk")
            (.put write-queue bucket-name))
          (reset! dirty-bucket-names #{})))
      ; The consumer thread for the blocking queue will exit once it receives this token
      (debug "Writing shutdown token")
      (.put write-queue shutdown-token)
      @write-queue-future)))

(defn start-persistence
  "Starts background threads for flushing all changes to disk."
  [name-buckets-atom]
  (reset! writing-data? true)
  (reset! flushing-thread (Thread. (build-flush-thread-action name-buckets-atom)))
  (.start @flushing-thread))

(defn shutdown-persistence
  "Shuts down the background threads responsible for flushing the DB to disk. Blocks until threads are
  finished."
  []
  (reset! writing-data? false)
  (.join @flushing-thread))

(defn mark-bucket-as-dirty
  "Marks a bucket name as changed so that the changes are eventually flushed to disk."
  [bucket-name]
  (locking dirty-bucket-names
    ; This adds the bucket name to a set. Since we're using a set, we can call this method many times
    ; in rapid succession, but the bucket will only be written once on the next flush periodic.
    (reset! dirty-bucket-names (conj @dirty-bucket-names bucket-name))))

(defn load-stored-names
  "Loads all boxname sets from files in the database directory. Returns a map of basenames to
  sets of used indicies (e.g. basename => #{1 2 4 6 20}"
  []
  (info "Loading database names")
  (reduce (fn [name-map file]
            (let [bucket-name (file-utils/strip-file-extension (.getName file))]
              (debug "Reading names for" bucket-name)
              (assoc name-map bucket-name (read-set-from-file-at-path (.getAbsolutePath file)))))
          {}
          (file-utils/find-files-with-extension-in-directory db-directory file-extension)))
