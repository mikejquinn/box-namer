(ns box-namer.file-utils
  (:import java.io.File java.io.FileOutputStream java.io.PrintWriter
           java.io.OutputStreamWriter java.io.BufferedWriter))

(defn get-writable-directory-at-path [path]
  (let [dir (new File path)]
    (cond
      (not (.exists dir)) (throw (Exception. (format "The path %s does not exist." path)))
      (not (.isDirectory dir)) (throw (Exception. (format "The path %s is not a directory." path)))
      (not (.canWrite dir)) (throw (Exception. (format "The path %s is not writable." path)))
      :else dir)))

(defn write-file-atomically
  [directory filename write-fn]
  (let [tmp-file (File/createTempFile filename ".tmp" directory)
        file-out-stream (new FileOutputStream tmp-file)
        out-stream-writer (new OutputStreamWriter file-out-stream)
        buffered-writer (new BufferedWriter out-stream-writer)
        print-writer (new PrintWriter buffered-writer)
        out-file (new File directory filename)]
    (write-fn print-writer)
    (.flush print-writer)
    (.force (.getChannel file-out-stream) true)
    (.sync (.getFD file-out-stream))
    (.close print-writer)
    (.renameTo tmp-file out-file)))
