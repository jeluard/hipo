(ns hipo.macros
  (:require [clojure.string :as str]))

(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defn constant?
  [o]
  (or (string? o) (number? o) (true? o) (false? o) (keyword? o) (char? o)))

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
  (if (constant? v)
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

(defmacro create-element [namespace-uri tag is]
  (if namespace-uri
    (if is
      `(.createElementNS js/document ~namespace-uri ~tag ~is)
      `(.createElementNS js/document ~namespace-uri ~tag))
    (if is
      `(.createElement js/document ~tag ~is)
      `(.createElement js/document ~tag))))

(defmacro compile-compound [[node-key & rest]]
  (let [literal-attrs (when (map? (first rest)) (first rest))
        var-attrs (when (and (not literal-attrs) (-> rest first meta :attrs))
                    (first rest))
        children (drop (if (or literal-attrs var-attrs) 1 0) rest)
        [tag class-str id] (parse-keyword node-key)
        dom-sym (gensym "dom")
        element-ns (when (+svg-tags+ tag) +svg-ns+)
        is (:is literal-attrs)]
    `(let [~dom-sym (create-element ~element-ns ~(name tag) ~is)]
       ~@(when-not (empty? class-str)
           [`(set! (.-className ~dom-sym) ~class-str)])
       ~@(when id
           [`(set! (.-id ~dom-sym) ~id)])
       ~@(for [[k v] literal-attrs]
           `(compile-add-attr! ~dom-sym ~k ~v))
       ~@(when var-attrs
           [`(doseq [[k# v#] ~var-attrs]
               (when v# (.setAttribute ~dom-sym (name k#) v#)))])
       ~@(for [c children]
           `(.appendChild ~dom-sym (node ~c)))
       ~dom-sym)))

(defmacro node [data]
  (if (vector? data)
   `(compile-compound ~data)
   `(hipo.template/->node-like ~data)))

(defmacro deftemplate [name args & node-forms]
  `(defn ~name ~args
     ~(if (next node-forms)
        (let [doc-frag (gensym "frag")]
          `(let [~doc-frag (.createDocumentFragment js/document)]
             ~@(for [el node-forms]
                 `(.appendChild ~doc-frag (node ~el)))
             ~doc-frag))
        `(node ~(first node-forms)))))
