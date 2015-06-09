(ns hipo.hiccup
  (:require [clojure.string :as string]
            [hipo.fast :as f]))

(def ^:private id-separator "#")
(def ^:private class-separator ".")

(defn parse-tag-name
  [s]
  (let [i (.indexOf s id-separator)]
    (if (pos? i)
      (subs s 0 i)
      (let [j (.indexOf s class-separator)]
        (if (pos? j)
          (subs s 0 j)
          s)))))

(defn parse-id
  [s]
  (let [i (.indexOf s id-separator)]
    (if (pos? i)
      (let [j (.indexOf s class-separator)]
        (if (pos? j)
          (subs s (inc i) j)
          (subs s (inc i)))))))

(defn parse-classes
  [s]
  (let [i (.indexOf s class-separator)]
    (if (pos? i); First locate the class part
      (let [cs (subs s (inc i))]
        (loop [s cs] ; Then convert from 'a.b.c' to 'a b c'
          (let [i (.indexOf s class-separator)]
            (if (pos? i)
              ; Replace with string in a loop is more efficient than replace with global regex
              (recur (.replace s class-separator " "))
              s)))))))

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
  (let [i (if (map? (nth v 1 nil)) 2 1)]
    (if (> (count v) i)
      (subvec v i))))

(defn flattened?
  [v]
  {:pre [(or (nil? v) (vector? v))]}
  (if (f/emptyv? v)
    true
    (let [c (dec (count v))]
      (loop [i 0]
        (let [o (nth v i)]
          (if (or (nil? o) (literal? o) (vector? o))
            (if (= c i)
              true
              (recur (inc i)))
            false))))))

(defn flatten-children
  [v]
  {:pre [(or (nil? v) (vector? v))]
   :post [(or (nil? v) (vector? v))]}
  (if (flattened? v)
    v
    (loop [acc (transient [])
           v v]
      (let [f (if (= 0 (count v)) nil (nth v 0))]
        (if (nil? f)
          (persistent! acc)
          (recur
            (if (seq? f)
              (f/conjs! acc f)
              (conj! acc f))
            (subvec v 1)))))))

(defn listener-name?
  [s]
  #?(:clj (.startsWith s "on-"))
  #?(:cljs (identical? 0 (.indexOf s "on-"))))

(defn listener-name->event-name
  [s]
  {:pre [(listener-name? s)]}
  (subs s 3))

(defn classes
  [s]
  (let [s (filter identity s)]
    (if-not (empty? s)
      (string/trim (string/join " " s)))))