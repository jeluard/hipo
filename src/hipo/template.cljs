(ns hipo.template
  (:require [clojure.string :as str]))

(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defprotocol PElement
  (-elem [this] "return the element representation of this"))

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

(defn base-element
  "dom element from css-style keyword like :a.class1 or :span#my-span.class"
  [node-key]
  (let [node-str (name node-key)
        base-idx (next-css-index node-str 0)
        tag (cond
             (> base-idx 0) (.substring node-str 0 base-idx)
             (zero? base-idx) "div"
             :else node-str)
        node (if (+svg-tags+ tag)
               (.createElementNS js/document +svg-ns+ tag)
               (.createElement js/document tag))]
    (when (>= base-idx 0)
      (loop [str (.substring node-str base-idx)]
        (let [next-idx (next-css-index str 1)
              frag (if (>= next-idx 0)
                     (.substring str 0 next-idx)
                     str)]
          (case (.charAt frag 0)
            \. (add-class! node (.substring frag 1))
            \# (.setAttribute node "id" (.substring frag 1)))
          (when (>= next-idx 0)
            (recur (.substring str next-idx))))))
    node))

(defn throw-unable-to-make-node [node-data]
  (throw (str "Don't know how to make node from: " (pr-str node-data))))

(defn ->document-fragment
  "take data and return a document fragment"
  ([data]
     (->document-fragment (.createDocumentFragment js/document) data))
  ([result-frag data]
     (cond
      (satisfies? PElement data)
      (do (.appendChild result-frag (-elem data))
          result-frag)

      (seq? data)
      (do (doseq [child data] (->document-fragment result-frag child))
          result-frag)

      (nil? data)
      result-frag

      :else
      (throw-unable-to-make-node data))))

(defn ->node-like
  "take data and return DOM node if it satisfies PElement and tries to
   make a document fragment otherwise"
  [data]
  (if (satisfies? PElement data)
    (-elem data)
    (->document-fragment data)))

(defn compound-element
  "element with either attrs or nested children [:div [:span \"Hello\"]]"
  [[tag-name maybe-attrs & children]]
  (let [n (base-element tag-name)
        attrs (when (and (map? maybe-attrs)
                         (not (satisfies? PElement maybe-attrs)))
                maybe-attrs)
        children  (if attrs children (cons maybe-attrs children))]
    (doseq [[k v] attrs]
      (if (= :class k)
        (add-class! n v)
        (when v (.setAttribute n (name k) v))))
    (.appendChild n (->node-like children))
    n))

(extend-protocol PElement
  js/Element
  (-elem [this] this)

  js/Comment
  (-elem [this] this)

  js/Text
  (-elem [this] this)

  PersistentVector
  (-elem [this] (compound-element this))

  number
  (-elem [this] (.createTextNode js/document (str this)))

  string
  (-elem [this]
    (if (keyword? this)
      (base-element this)
      (.createTextNode js/document (str this)))))

;; extend additional prototypes, which might not be available on all
;; versions of IE or phantom

(when (exists? js/HTMLElement)
  (extend-protocol PElement
    js/HTMLElement
    (-elem [this] this)))

(when (exists? js/DocumentFragment)
  (extend-protocol PElement
    js/DocumentFragment
    (-elem [this] this)))

(when (exists? js/Document)
  (extend-protocol PElement
    js/Document
    (-elem [this] this)))

(when (exists? js/HTMLDocument)
  (extend-protocol PElement
    js/HTMLDocument
    (-elem [this] this)))

(when (exists? js/SVGElement)
  (extend-protocol PElement
    js/SVGElement
    (-elem [this] this)))

(when (exists? js/Window)
  (extend-protocol PElement
    js/Window
    (-elem [this] this)))

(defn node [data]
  (if (satisfies? PElement data)
    (-elem data)
    (throw-unable-to-make-node data)))
