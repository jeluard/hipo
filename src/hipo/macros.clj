(ns hipo.macros
  (:require [clojure.string :as str]))

(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defn literal?
  [o]
  (or (string? o) (number? o) (true? o) (false? o) (keyword? o) (symbol? o) (char? o) (nil? o)))

(defmacro add-attr!
  [el k v]
  (cond
    (identical? k :id)
    `(set! (.-id ~el) ~v)
    (identical? k :class)
    `(set! (.-className ~el) (.trim (str (.-className ~el) " " ~v)))
    :else
    `(.setAttribute ~el ~(name k) ~v)))

(defmacro compile-add-attr!
  "compile-time add attribute"
  [el k v]
  (assert (keyword? k))
  (if (literal? v)
    (if v
      `(add-attr! ~el ~k ~v))
    `(let [v# ~v]
       (if v#
         (add-attr! ~el ~k v#)))))

(defn parse-keyword
  "return pair [tag class-str id] where tag is dom tag and attrs
   are key-value attribute pairs from css-style dom selector"
  [node-key]
  (let [node-str (name node-key)
        node-tag (second (re-find #"^([^.\#]+)[.\#]?" node-str))
        classes (map #(.substring ^String % 1) (re-seq #"\.[^.*]*" node-str))
        id (first (map #(.substring ^String % 1) (re-seq #"#[^.*]*" node-str)))]
    [(if (empty? node-tag) "div" node-tag)
     (str/join " " classes)
     id]))

(defmacro create-element
  [namespace-uri tag is]
  (if namespace-uri
    (if is
      `(.createElementNS js/document ~namespace-uri ~tag ~is)
      `(.createElementNS js/document ~namespace-uri ~tag))
    (if is
      `(.createElement js/document ~tag ~is)
      `(.createElement js/document ~tag))))

(defmacro compile-child
  [el data]
  (cond
    (literal? data) `(.appendChild ~el (.createTextNode js/document ~data))
    :else `(.appendChild ~el (node ~data))))

(defmacro compile-vector
  [[node-key & rest]]
  (let [literal-attrs (when (map? (first rest)) (first rest))
        var-attrs (when (and (not literal-attrs) (-> rest first meta :attrs))
                    (first rest))
        children (drop (if (or literal-attrs var-attrs) 1 0) rest)
        [tag class-str id] (parse-keyword node-key)
        el (gensym "dom")
        element-ns (when (+svg-tags+ tag) +svg-ns+)
        is (:is literal-attrs)]
    `(let [~el (create-element ~element-ns ~(name tag) ~is)]
       ~@(when-not (empty? class-str)
           [`(set! (.-className ~el) ~class-str)])
       ~@(when id
           [`(set! (.-id ~el) ~id)])
       ~@(for [[k v] literal-attrs]
           `(compile-add-attr! ~el ~k ~v))
       ~@(when var-attrs
           [`(doseq [[k# v#] ~var-attrs]
               (when v# (.setAttribute ~el (name k#) v#)))])
       ~@(for [c children]
           `(compile-child ~el ~c))
       ~el)))

(defmacro node
  [data]
  (if (vector? data)
   `(compile-vector ~data)
   `(hipo.template/->node-like ~data)))
