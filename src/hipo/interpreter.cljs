(ns hipo.interpreter
  (:require [clojure.string :as str]))

(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defn literal?
  [o]
  (or (string? o) (number? o) (true? o) (false? o)))

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
     (when-not (empty? classes) (str/join " " classes))
     id]))

(defmulti set-attribute! #(second %))

(defmethod set-attribute! :default
  [[el a v]]
  (if (= 0 (.indexOf a "on-"))
    (let [e (.substring a 3)]
      (.addEventListener el e v))
    (.setAttribute el a v)))

(declare create-child)

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
          (set-attribute! [el (name k) v])))
      (doseq [c (filter identity children)]
        (.appendChild el (create-child c)))
      el)))

(defn mark-as-partially-compiled!
  [el]
  (loop [el el]
    (if-let [pel (.-parentElement el)]
      (recur pel)
      (aset el "hipo-partially-compiled" true))))

(defn create-child
  [o]
  (cond
    (literal? o) (.createTextNode js/document o)
    (vector? o) (create-vector o)
    :else
    (throw (str "Don't know how to make node from: " (pr-str o)))))

(defn create-with-parent
  [el o]
  (when o
    (mark-as-partially-compiled! el)
    (if (seq? o)
      (doseq [c (filter identity o)]
        (.appendChild el (create-child c)))
      (.appendChild el (create-child o)))))

(defn create
  [o]
  (when o
    (if (seq? o)
      (let [f (.createDocumentFragment js/document)]
        (mark-as-partially-compiled! f)
        (doseq [c (filter identity o)]
          (.appendChild f (create-child c)))
        f)
      (let [el (create-child o)]
        (mark-as-partially-compiled! el)
        el))))