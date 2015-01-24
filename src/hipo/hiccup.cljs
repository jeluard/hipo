(ns hipo.hiccup)

(def ^:private id-separator "#")
(def ^:private class-separator ".")

(defn parse-tag-name
  [s]
  (let [i (.indexOf s id-separator)]
    (if (pos? i)
      (.substr s 0 i)
      (let [j (.indexOf s class-separator)]
        (if (pos? j)
          (.substr s 0 j)
          s)))))

(defn parse-id
  [s]
  (let [i (.indexOf s id-separator)]
    (when (pos? i)
      (let [j (.indexOf s class-separator)]
        (if (pos? j)
          (.substr s (inc i) (- j i 1))
          (.substr s (inc i)))))))

(defn parse-classes
  [s]
  (let [i (.indexOf s class-separator)]
    (when (pos? i)
      (.replace (.substr s (inc i)) class-separator " "))))

(defn literal?
  [o]
  (or (string? o) (number? o) (true? o) (false? o)))

(defn attributes
  [v]
  (when-let [m (nth v 1 nil)]
    (when (map? m)
      m)))

(defn children
  [v]
  (if (map? (nth v 1 nil))
    (subvec v 2)
    (subvec v 1)))

(defn flatten-children
  [v]
  {:pre [(vector? v)]
   :post [(vector? v)]}
  (if (every? #(or (literal? %) (vector? %)) v)
    v
    (loop [acc [] [elt & others] v]
      (if (nil? elt)
        acc
        (recur
          (if (seq? elt)
            (apply conj acc elt)
            (conj acc elt))
          others)))))