(ns hipo.hiccup
  (:require [hipo.fast :as f]))

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
    (if (pos? i)
      (let [j (.indexOf s class-separator)]
        (if (pos? j)
          (.substr s (inc i) (- j i 1))
          (.substr s (inc i)))))))

(defn parse-classes
  [s]
  (let [i (.indexOf s class-separator)]
    (if (pos? i)
      (.replace (.substr s (inc i)) class-separator " "))))

(defn literal?
  [o]
  (or (string? o) (number? o) (true? o) (false? o)))

(defn node
  [v]
  (name (nth v 0)))

(defn tag
  [v]
  (parse-tag-name (node v)))

(defn attributes
  [v]
  (let [n (node v)
        id (parse-id n)
        cs (parse-classes n)
        m? (nth v 1 nil)]
    (if (map? m?)
      (if (and id (contains? m? :id))
        (throw (ex-info "Cannot define id multiple times" {}))
        (if (or id cs)
          (merge m? (if id {:id id}) (if cs {:class (if-let [c (:class m?)] (if cs (str cs " " c) (str c)) cs)}))
          m?))
      (if (or id cs)
        {:id id :class cs}))))

(defn children
  [v]
  (if (map? (nth v 1 nil))
    (subvec v 2)
    (subvec v 1)))

(defn flattened?
  [v]
  {:pre [(or (nil? v) (vector? v))]}
  (if (f/emptyv? v)
    true
    (let [c (dec (count v))]
      (loop [i 0]
        (let [o (nth v i)]
          (if (or (nil? o) (literal? o) (vector? o))
            (if (identical? c i)
              true
              (recur (inc i)))
            false))))))

(defn flatten-children
  [v]
  {:pre [(vector? v)]
   :post [(vector? v)]}
  (if (flattened? v)
    v
    (loop [acc (transient [])
           v v]
      (let [f (if (identical? 0 (count v)) nil (nth v 0))]
        (if (nil? f)
          (persistent! acc)
          (recur
            (if (seq? f)
              (f/conjs! acc f)
              (conj! acc f))
            (subvec v 1)))))))