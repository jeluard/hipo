(ns hipo.fast)

(defn emptyv?
  [v]
  ; Prevent creation of ISeqable when vector is not empty
  (zero? (count v)))

(defn conjs!
  [v s]
  (if (seq s)
    (recur (conj! v (first s)) (rest s))
    v))