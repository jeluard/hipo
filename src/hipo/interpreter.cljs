(ns hipo.interpreter
  (:require [clojure.string :as str]))

(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defn literal?
  [o]
  (or (string? o) (number? o) (true? o) (false? o) (nil? o)))

(defn- create-element [namespace-uri tag is]
  (if namespace-uri
    (if is
      (.createElementNS js/document namespace-uri tag is)
      (.createElementNS js/document namespace-uri tag))
    (if is
      (.createElement js/document tag is)
      (.createElement js/document tag))))

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

(declare create-children)

(defn create-vector
  [[node-key & rest]]
  (let [literal-attrs (when-let [f (first rest)] (when (map? f) f))
        children (if literal-attrs (drop 1 rest) rest)
        [tag class-str id] (parse-keyword node-key)
        class-str (if-let [c (:class literal-attrs)] (str class-str " " c) class-str)
        element-ns (when (+svg-tags+ tag) +svg-ns+)
        is (:is literal-attrs)]
    (let [el (create-element element-ns tag is)]
      (when-not (empty? class-str)
        (set! (.-className el) class-str))
      (when id
        (set! (.-id el) id))
      (doseq [[k v] (dissoc literal-attrs :class :is)]
        (when v
          (.setAttribute el (name k) v)))
      (when children
        (create-children el children))
      el)))

(defn create-children
  [el data]
  (cond
    (literal? data) (.appendChild el (.createTextNode js/document data))
    (vector? data) (.appendChild el (create-vector data))
    (seq? data)
    (doseq [o data]
      (create-children el o))
    :else
    (throw (str "Don't know how to make node from: " (pr-str data)))))
