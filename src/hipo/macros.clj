(ns hipo.macros
  (:require [clojure.string :as str]))

(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defmacro compile-add-attr!
  "compile-time add attribute"
  [d k v]
  (assert (keyword? k))
  `(when ~v
     ~(if (identical? k :class)
       `(set! (.-className ~d) (.trim (str (.-className ~d) " " ~v)))
       `(.setAttribute ~d ~(name k) ~v))))

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
        element-ns (when (+svg-tags+ tag) +svg-ns+)]
    `(let [~dom-sym (create-element ~element-ns ~(name tag) (or (:is ~literal-attrs) (:is ~var-attrs)))]
       ~@(when-not (empty? class-str)
           [`(set! (.-className ~dom-sym) ~class-str)])
       ~@(when id
           [`(.setAttribute ~dom-sym "id" ~id)])
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
