(ns box-namer.file-utils
  (:import java.io.File java.io.FileOutputStream java.io.PrintWriter
           java.io.OutputStreamWriter java.io.BufferedWriter
           java.io.FilenameFilter))

(defn get-writable-directory-at-path [path]
  (let [dir (new File path)]
    (cond
      (not (.exists dir)) (throw (Exception. (format "The path %s does not exist." path)))
      (not (.isDirectory dir)) (throw (Exception. (format "The path %s is not a directory." path)))
      (not (.canWrite dir)) (throw (Exception. (format "The path %s is not writable." path)))
      :else dir)))

(defn delete-file-in-directory
  [directory filename]
  (let [file (new File directory filename)]
    (.delete file)))

(defn find-files-with-extension-in-directory
  "Returns an array of all files in a directory with a given extension."
  [directory extension]
  (let [file-filter (proxy [FilenameFilter] []
                      (accept [directory file-name]
                        (.endsWith file-name extension)))]
    (.listFiles directory file-filter)))

(defn strip-file-extension
  "Strips the extension from a filename. This doesn't handle all the edge cases, but is fine for our
  purposes."
  [filename]
  (.substring filename 0 (.lastIndexOf filename ".")))

(defn write-file-atomically
  "Writes a file to disk using the tmp file + rewrite strategy supported by most filesystems these days."
  [directory filename write-fn]
  (let [tmp-file (File/createTempFile filename ".tmp" directory)
        ; Using a FileOutputStream as opposed to one of the nicer abstractions so that we can get access
        ; to the Channel. See below.
        file-out-stream (new FileOutputStream tmp-file)
        out-stream-writer (new OutputStreamWriter file-out-stream)
        buffered-writer (new BufferedWriter out-stream-writer)
        print-writer (new PrintWriter buffered-writer)
        out-file (new File directory filename)]
    (write-fn print-writer)
    (.flush print-writer)
    ; Force the OS to actually write the flushed data to the physical device.
    ; See: http://stackoverflow.com/questions/730521/really-force-file-sync-flush-in-java
    (.force (.getChannel file-out-stream) true)
    (.sync (.getFD file-out-stream))
    (.close print-writer)
    (.renameTo tmp-file out-file)))
