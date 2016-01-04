(ns hipo.hiccup
  (:require [clojure.string :as string]))

(def ^:private id-separator "#")
(def ^:private class-separator ".")

(def ^:private default-namespaces {"svg" "http://www.w3.org/2000/svg"
                                   "xlink" "http://www.w3.org/1999/xlink"})

(defn key->namespace
  [s m]
  (if s
    (or (get (:namespaces m) s) (get default-namespaces s))))

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
  (nth v 0))

(defn keyns
  [h]
  (namespace (node h)))

(defn tag
  [v]
  (parse-tag-name (name (node v))))

(defn attributes
  [v]
  (if v
    (let [n (name (node v))
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
          {:id id :class cs})))))

(defn children
  [v]
  (let [i (if (map? (nth v 1 nil)) 2 1)]
    (if (> (count v) i)
      (subvec v i))))

(defn flattened?
  [v]
  {:pre [(or (nil? v) (vector? v))]}
  (if (empty? v)
    true
    (let [c (dec (count v))]
      (loop [i 0]
        (let [o (nth v i)]
          (if (or (literal? o) (vector? o))
            (if (= c i)
              true
              (recur (inc i)))
            false))))))

(deftype Sentinel [])
(def ^:private sentinel (Sentinel.))

(defn conjs!
  [v s]
  (if (seq s)
    (recur (let [f (first s)] (if (or (literal? f) (vector? f)) (conj! v f) (conjs! v f))) (rest s))
    v))

(defn flatten-children
  [v]
  {:pre [(or (nil? v) (vector? v))]
   :post [(or (nil? v) (vector? v))]}
  (if (flattened? v)
    v
    (loop [acc (transient [])
           v v]
      (let [f (nth v 0 sentinel)]
        (if (identical? sentinel f)
          (persistent! acc)
          (recur
            (cond
              (seq? f) (conjs! acc f)
              (not (nil? f)) (conj! acc f)
              :else acc)
            (subvec v 1)))))))

(defn listener-name?
  [s]
  #?(:clj (.startsWith s "on-"))
  #?(:cljs (identical? 0 (.indexOf s "on-"))))

(defn listener-name->event-name
  [s]
  (if (listener-name? s)
    (subs s 3)))

(defn classes
  [s]
  (let [s (keep identity s)]
    (if-not (empty? s)
      (string/trim (string/join " " s)))))
