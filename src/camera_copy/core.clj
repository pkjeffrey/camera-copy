(ns camera-copy.core
  (:require
   [camera-copy.camera-copy :as cc]
   [camera-copy.swing :as ui]
   [clojure.edn :as edn]
   [clojure.java.io :as io])
  (:import
   [java.awt Dimension GridBagLayout Insets]
   [javax.swing JButton JFrame JLabel JPanel JProgressBar SwingUtilities])
  (:gen-class))

(def config-file "camera-copy.edn")
(defonce config (atom {:source nil
                       :destination nil}))

(defn- read-config
  []
  (let [f (io/as-file config-file)]
    (when (.exists f)
      (reset! config (-> f slurp edn/read-string)))))

(defn- write-config
  []
  (spit config-file @config))

(defn- update-config-mru
  [existing mru]
  (->> existing
       (filter #(not= % mru))
       (cons mru)))

(defn- update-config
  [source-dir destination-dir]
  (swap! config update :source update-config-mru source-dir)
  (swap! config update :destination update-config-mru destination-dir))

(defn- get-source-text
  [dir]
  (str "Images to copy: " (count (cc/list-canon-raw dir))))

(defn- get-destination-text
  [dir]
  (str "Last used number: " (cc/get-max-number dir)))

(defn- get-selected-dir
  [combobox]
  (str (.getSelectedItem combobox)))

(defn- make-panel
  [source-dir source-text destination-dir destination-text copy-button progress]
  (doto (JPanel. (GridBagLayout.))
    (ui/grid-bag-layout
     :insets (Insets. 5 5 5 5)
     :gridx 0 :gridy 0 :anchor :LINE_END
     (JLabel. "Source:")
     :gridy 2
     (JLabel. "Destination:")
     :gridx 1 :gridy 0 :anchor :LINE_START :fill :HORIZONTAL :weightx 1
     source-dir
     :gridy 2
     destination-dir
     :gridy 1
     source-text
     :gridy 3
     destination-text
     :gridy 4 :anchor :LINE_END :fill :NONE
     copy-button
     :gridx 0 :gridy 5 :anchor :LINE_START :gridwidth 2
     progress)))

(defn app
  []
  (read-config)
  (let [source-dir       (ui/make-combobox (:source @config)) 
        source-text      (JLabel. (get-source-text (get-selected-dir source-dir)))
        destination-dir  (ui/make-combobox (:destination @config))
        destination-text (JLabel. (get-destination-text (get-selected-dir destination-dir)))
        copy-button      (JButton. "Copy Images")
        progress         (doto (JProgressBar.)
                           (.setPreferredSize (Dimension. 400 14)))
        panel            (make-panel source-dir source-text
                                     destination-dir destination-text
                                     copy-button progress)]
    (ui/on-action source-dir
                  (.setText source-text
                            (get-source-text (get-selected-dir source-dir))))
    (ui/on-action destination-dir
                  (.setText destination-text
                            (get-destination-text (get-selected-dir destination-dir))))
    (ui/on-action copy-button
                  (let [src-dir  (get-selected-dir source-dir)
                        dest-dir (get-selected-dir destination-dir)]
                    (update-config src-dir dest-dir)
                    (write-config)
                    (let [a (cc/copy src-dir dest-dir)]
                      (.setMaximum progress (:num-files @a))
                      (while (< (:num-done @a) (:num-files @a))
                        (.setValue progress (:num-done @a)))
                      (.setValue progress (:num-done @a)))))
    (doto (JFrame. "Camera Copy")
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      (.setContentPane panel)
      (.pack)
      (.setVisible true))))

(defn -main
  []
  (SwingUtilities/invokeLater app))
