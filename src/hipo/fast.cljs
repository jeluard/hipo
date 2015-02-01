(ns hipo.fast)

(defn emptyv?
  [v]
  ; Prevent creation of ISeqable when vector is not empty
  (zero? (count v)))