(ns camera-copy.swing
  (:import
   [javax.swing JComboBox]))

(defmacro on-action
  [component & body]
  `(. ~component addActionListener
      (proxy [java.awt.event.ActionListener] []
        (actionPerformed [e#] ~@body))))

(defmacro set-grid!
  [constraints field value]
  `(set! (. ~constraints ~(symbol (name field)))
         ~(if (keyword? value)
            `(. java.awt.GridBagConstraints ~(symbol (name value)))
            value)))

(defmacro grid-bag-layout
  [container & body]
  (let [c (gensym "c")
        cntr (gensym "cntr")]
    `(let [~c (java.awt.GridBagConstraints.)
           ~cntr ~container]
       ~@(loop [result '()
                body body]
           (if (empty? body)
             (reverse result)
             (let [expr (first body)]
               (if (keyword? expr)
                 (recur (cons `(set-grid! ~c ~expr ~(second body)) result)
                        (next (next body)))
                 (recur (cons `(.add ~cntr ~expr ~c) result)
                        (next body)))))))))

(defn make-combobox
  [items]
  (if items
    (doto (JComboBox. (into-array items))
      (.setSelectedIndex 0)
      (.setEditable true))
    (doto (JComboBox.)
      (.setEditable true))))