(ns hipo.macros
  (:require [clojure.string :as str]))

(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defn literal?
  [o]
  (or (string? o) (number? o) (true? o) (false? o) (nil? o)))

(defmacro set-attr!
  [el k v]
  (cond
    (identical? k :id)
    `(set! (.-id ~el) ~v)
    :else
    `(.setAttribute ~el ~(name k) ~v)))

(defmacro compile-set-attr!
  "compile-time add attribute"
  [el k v]
  (assert (keyword? k))
  (if (literal? v)
    (if v
      `(set-attr! ~el ~k ~v))
    `(let [v# ~v]
       (if v#
         (set-attr! ~el ~k v#)))))

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

(defmacro compile-create-element
  [namespace-uri tag is]
  (if namespace-uri
    (if is
      `(.createElementNS js/document ~namespace-uri ~tag ~is)
      `(.createElementNS js/document ~namespace-uri ~tag))
    (if is
      `(.createElement js/document ~tag ~is)
      `(.createElement js/document ~tag))))

(defmacro compile-create-child
  [el data]
  (cond
    (literal? data) `(.appendChild ~el (.createTextNode js/document ~data))
    (vector? data) `(.appendChild ~el (compile-create-vector ~data))
    :else `(hipo.template/create-children ~el ~data)))

(defmacro compile-create-vector
  [[node-key & rest]]
  (let [literal-attrs (when-let [f (first rest)] (when (map? f) f))
        var-attrs (when (and (not literal-attrs) (-> rest first meta :attrs))
                    (first rest))
        children (if (or literal-attrs var-attrs) (drop 1 rest) rest)
        [tag class-str id] (parse-keyword node-key)
        class-str (if-let [c (:class literal-attrs)] (if-not (str/blank? class-str) (str class-str " " c) c) class-str)
        el (gensym "dom")
        element-ns (when (+svg-tags+ tag) +svg-ns+)
        is (:is literal-attrs)]
    `(let [~el (compile-create-element ~element-ns ~tag ~is)]
       ~@(when-not (empty? class-str)
           [`(set! (.-className ~el) ~class-str)])
       ~@(when id
           [`(set! (.-id ~el) ~id)])
       ~@(for [[k v] (dissoc literal-attrs :class)]
           `(compile-set-attr! ~el ~k ~v))
       ~@(when var-attrs
           [`(doseq [[k# v#] ~var-attrs]
               (when v# (.setAttribute ~el (name k#) v#)))])
       ~@(for [c children]
           `(compile-create-child ~el ~c))
       ~el)))

(defmacro create
  [& data]
  (if (and (= 1 (count data)) (vector? (first data)))
    `(compile-create-vector ~(first data))
    (let [f (gensym "f")]
      `(let [~f (.createDocumentFragment js/document)]
         ~@(for [o data]
           `(compile-create-child ~f ~o))
         ~f))))