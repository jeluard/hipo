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

(def ^:private id-separator "#")
(def ^:private class-separator ".")

(defn parse-keyword
  [s]
  (let [i (.indexOf s class-separator)]
    (if (pos? i)
      (let [n (.substr s 0 i)
            c (.replace (.substr s (inc i)) class-separator " ")]
        (let [i (.indexOf n id-separator)]
          (if (pos? i)
            (let [v (.split n id-separator)]
              [(aget v 0) (aget v 1) c])
            [n nil c])))
      (let [i (.indexOf s id-separator)]
        (if (pos? i)
          (let [v (.split s id-separator)]
            [(aget v 0) (aget v 1)])
          [s])))))

(defmulti set-attribute! #(second %))

(defmethod set-attribute! :default
  [[el a v]]
  (if (= 0 (.indexOf a "on-"))
    (let [e (.substring a 3)]
      (.addEventListener el e v))
    (.setAttribute el a v)))

(declare create-child)

(defn append-children!
  [el o]
  (if (seq? o)
    (doseq [c (filter identity o)]
      (append-children! el c))
    (.appendChild el (create-child o))))

(defn create-vector
  [[node-key & rest]]
  (let [literal-attrs (when-let [f (first rest)] (when (map? f) f))
        children (if literal-attrs (drop 1 rest) rest)
        [tag id class-str] (parse-keyword (name node-key))
        class-str (if-let [c (:class literal-attrs)] (if class-str (str class-str " " c) c) class-str)
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
      (when children
        (append-children! el children))
      el)))

(defn mark-as-partially-compiled!
  [el]
  (if-let [pel (.-parentElement el)]
    (recur pel)
    (do (aset el "hipo-partially-compiled" true) el)))

(defn create-child
  [o]
  (cond
    (literal? o) (.createTextNode js/document o)
    (vector? o) (create-vector o)
    :else
    (throw (str "Don't know how to make node from: " (pr-str o)))))

(defn append-to-parent
  [el o]
  {:pre [(not (nil? o))]}
  (mark-as-partially-compiled! el)
  (append-children! el o))

(defn create
  [o]
  {:pre [(not (nil? o))]}
  (mark-as-partially-compiled!
    (if (seq? o)
      (let [f (.createDocumentFragment js/document)]
        (append-children! f o)
        f)
      (create-child o))))
