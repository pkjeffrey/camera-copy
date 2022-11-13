(ns camera-copy.camera-copy
  (:import
   [java.nio.file CopyOption Files LinkOption Path Paths StandardCopyOption]
   [java.nio.file.attribute FileAttribute]
   [java.time LocalDateTime ZoneId]))

(def copy-options (into-array CopyOption [StandardCopyOption/COPY_ATTRIBUTES
                                          LinkOption/NOFOLLOW_LINKS]))
(def no-follow-links (into-array [LinkOption/NOFOLLOW_LINKS]))
(def re-canon-raw #"(.*_)(\d{4})(\.CR[2|3])$")

(defn- get-path
  [parent & more]
  (Paths/get parent (into-array String more)))

(defn- get-prefix-extension
  "Returns a vector of the prefix and extension of a Canon Raw file."
  [filename]
  (let [[_ prefix _ extension] (re-find re-canon-raw filename)]
    [prefix extension]))

(defn- get-number
  "Returns the number of a Canon Raw file."
  [filename]
  (let [[_ _ number _] (re-find re-canon-raw filename)]
    (Integer/parseInt number)))

(defn- canon-raw-file?
  "Tests whether a file is a Canon Raw file."
  [filename]
  (let [[_ extension] (get-prefix-extension filename)]
    (some? extension)))

(defn- directory?
  "Tests whether a file is a directory."
  [path]
  (Files/isDirectory path no-follow-links))

(defn- file?
  "Tests whether a file is a regular file with opaque content."
  [path]
  (Files/isRegularFile path no-follow-links))

(defn- writable?
  "Tests whether a file is writable."
  [path]
  (Files/isWritable path))

(defn- created-date
  "Returns the file created date."
  [path]
  (-> path
      (Files/getAttribute "creationTime" no-follow-links)
      .toInstant
      (LocalDateTime/ofInstant (ZoneId/systemDefault))
      .toLocalDate))

(defmulti list-canon-raw
  "Returns a sequence Canon Raw filenames in the directory."
  class)
(defmethod list-canon-raw Path 
  [path]
  (->> (-> path .toFile .list)
       (filter canon-raw-file?)))
(defmethod list-canon-raw String
  [dir]
  (list-canon-raw (get-path dir)))

(defmulti get-max-number
  "Returns the max image number in the subdirectories."
  class)
(defmethod get-max-number Path
  [path]
  (if-let [max-dir  (->> (-> path .toFile .list)
                         (map #(get-path (.toString path) %))
                         (filter directory?)
                         sort
                         last)]
    (if-let [max-file (->> (-> max-dir .toFile .list)
                           (filter canon-raw-file?)
                           sort
                           last)]
      (get-number max-file)
      0)
    0))
(defmethod get-max-number String
  [dir]
  (get-max-number (get-path dir)))

(defn- copy-file [[src dest-num] dest-dir]
  (let [[pre ext] (get-prefix-extension (.toString (.getFileName src)))
        dest-date (.toString (created-date src))
        dest-dir  (get-path dest-dir dest-date)
        dest      (.resolve dest-dir (format "%s%04d%s" pre dest-num ext))]
    (when-not (directory? dest-dir)
      (Files/createDirectory dest-dir (make-array FileAttribute 0)))
    (Files/copy src dest copy-options)))

(defn- copy-start [{:as state :keys [src-file-renumber dest-dir]}]
  (if (seq src-file-renumber)
    (do (send *agent* copy-start)
        (copy-file (first src-file-renumber) dest-dir)
        (-> state
            (update :src-file-renumber rest)
            (update :num-done inc)))
    state))

(defn copy [src-dir dest-dir]
  (let [src-files         (list-canon-raw src-dir)
        src-count         (count src-files)
        last-num          (get-max-number dest-dir)
        src-file-renumber (mapv (fn [f n] [(get-path src-dir f) (+ (inc last-num) n)])
                                src-files (range))]
    (when (< 0 src-count)
      (let [copy-agent (agent {:src-file-renumber src-file-renumber
                               :dest-dir          dest-dir
                               :num-files         src-count
                               :num-done          0})]
        (send copy-agent copy-start)))))