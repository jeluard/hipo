(ns hipo.template
  (:require [clojure.string :as str]))

(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defn literal?
  [o]
  (or (string? o) (number? o) (true? o) (false? o) (nil? o)))

(defn- ^boolean class-match?
  "does class-name string have class starting at index idx.
   only will be used when Element::classList doesn't exist"
  [class-name class idx]
  (and
    ;; start
    (or (zero? idx) (identical? \space (.charAt class-name (dec idx))))
    ;; stop
    (let [total-len (.-length class-name)
          stop (+ idx (.-length class))]
      (when (<= stop total-len)
        (or (identical? stop total-len)
            (identical? \space (.charAt class-name stop)))))))

(defn- class-index
  "Finds the index of class in a space-delimited class-name
   only will be used when Element::classList doesn't exist"
  [class-name class]
  (loop [start-from 0]
    (let [i (.indexOf class-name class start-from)]
      (when (>= i 0)
        (if (class-match? class-name class i)
        i
        (recur (+ i (.-length class))))))))

(defn add-class!
  "add class to element"
  ([elem classes]
   (let [classes (-> classes name str/trim)]
     (when (seq classes)
       (if-let [class-list (.-classList elem)]
         (doseq [class (.split classes #"\s+")]
           (.add class-list class))
         (doseq [class (.split classes #"\s+")]
           (let [class-name (.-className elem)]
             (when-not (class-index class-name class)
               (set! (.-className elem)
                     (if (identical? class-name "")
                       class
                       (str class-name " " class))))))))
     elem)))

(defn next-css-index
  "index of css character (#,.) in base-element. bottleneck"
  [s start-idx]
  (let [id-idx (.indexOf s "#" start-idx)
        class-idx (.indexOf s "." start-idx)
        idx (.min js/Math id-idx class-idx)]
    (if (< idx 0)
      (.max js/Math id-idx class-idx)
      idx)))

(defn- create-element [namespace-uri tag is]
  (if namespace-uri
    (if is
      (.createElementNS js/document namespace-uri tag is)
      (.createElementNS js/document namespace-uri tag))
    (if is
      (.createElement js/document tag is)
      (.createElement js/document tag))))

(defn base-element
  "dom element from css-style keyword like :a.class1 or :span#my-span.class"
  ([node-key] (base-element node-key nil))
  ([node-key is]
   (let [node-str (name node-key)
         base-idx (next-css-index node-str 0)
         tag (cond
               (> base-idx 0) (.substring node-str 0 base-idx)
               (zero? base-idx) "div"
               :else node-str)
         el (create-element (when (+svg-tags+ tag) +svg-ns+)

                              tag is)]
     (when (>= base-idx 0)
       (loop [str (.substring node-str base-idx)]
         (let [next-idx (next-css-index str 1)
               frag (if (>= next-idx 0)
                      (.substring str 0 next-idx)
                      str)]
           (case (.charAt frag 0)
             \. (add-class! el (.substring frag 1))
             \# (.setAttribute el "id" (.substring frag 1)))
           (when (>= next-idx 0)
             (recur (.substring str next-idx))))))
     el)))

(defn create-vector
  "element with either attrs or nested children [:div [:span \"Hello\"]]"
  [[tag-name maybe-attrs & children]]
  (let [attrs (when (map? maybe-attrs)
                maybe-attrs)
        children  (if attrs children (cons maybe-attrs children))
        el (base-element tag-name (:is attrs))]
    (doseq [[k v] attrs]
      (if (= :class k)
        (add-class! el v)
        (when v (.setAttribute el (name k) v))))
    (when children
      (create-children el children))
    el))

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
