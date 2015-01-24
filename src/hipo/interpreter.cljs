(ns hipo.interpreter
  (:require [clojure.set :as set]
            [hipo.dom :as dom]
            [hipo.hiccup :as hic]))

(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(def ^:private id-separator "#")
(def ^:private class-separator ".")

(defn- listener-name? [s] (= 0 (.indexOf s "on-")))
(defn- listener-name->event-name [s] (.substring s 3))

(defmulti set-attribute! (fn [_ a _ _] a))

(defmethod set-attribute! :default
  [el a ov nv]
  (if (listener-name? a)
    (when-not ov (.addEventListener el (listener-name->event-name a) nv)) ; Only one listeners added: when previous value was nil
    (.setAttribute el a nv)))

(defmulti remove-attribute! (fn [_ a _] a))

(defmethod remove-attribute! :default
  [el a ov]
  (if (listener-name? a)
    (.removeEventListener el (listener-name->event-name a) ov)
    (.removeAttribute el a)))

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
        node (name node-key)
        tag (hic/parse-tag-name node)
        id (hic/parse-id node)
        class-str (hic/parse-classes node)
        class-str (if-let [c (:class literal-attrs)] (if class-str (str class-str " " c) c) class-str)
        element-ns (when (+svg-tags+ tag) +svg-ns+)
        is (:is literal-attrs)]
    (let [el (dom/create-element element-ns tag is)]
      (when (seq class-str)
        (set! (.-className el) class-str))
      (when id
        (set! (.-id el) id))
      (doseq [[k v] (dissoc literal-attrs :class :is)]
        (when v
          (set-attribute! el (name k) nil v)))
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
    (hic/literal? o) (.createTextNode js/document o)
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

; Update

(defn update-attributes!
  [el om nm]
  (doseq [[nk nv] nm
          :let [ov (nk om) n (name nk)]]
    (when-not (= ov nv)
      (if nv
        (set-attribute! el n ov nv)
        (remove-attribute! el n ov))))
  (doseq [k (set/difference (set (keys om)) (set (keys nm)))]
    (remove-attribute! el (name k) (k om))))

(declare update!)

(defn update-keyed-children!
  [el och nch]
  )

(defn update-non-keyed-children!
  [el och nch]
  (let [oc (count och)
        nc (count nch)
        d (- oc nc)]
    ; Assume children are always in the same order i.e. an element is identified by its position
    ; Update all existing node up to (min (count och) (count nch))
    (dotimes [i (min oc nc)]
      (let [ov (nth och i)
            nv (nth nch i)]
        (update! (dom/child-node el i) ov nv)))
    ; Create new elements if (count nch) > (count oh)
    (when (neg? d)
      (if (= -1 d)
        (append-children! el (peek nch))
        (let [f (.createDocumentFragment js/document)]
          ; An intermediary DocumentFragment is used to reduce the number of append to the attached node
          (append-children! f (apply list (if (= 0 oc) nch (subvec nch oc))))
          (.appendChild el f))))
    ; Remove now unused elements if (count och) > (count nch)
    (when (pos? d)
      (dom/remove-trailing-children! el d))))

(defn keyed-children?
  [v]
  (contains? (meta (v 0)) :key))

(defn update-children!
  [el och nch]
  (if (empty? nch)
    (when (seq och) ; No children but used to have some
      (dom/clear! el))
    (if (keyed-children? nch)
      (update-keyed-children! el och nch)
      (update-non-keyed-children! el och nch))))

(defn update-vector!
  [el oh nh]
  {:pre [(vector? oh) (vector? nh)]}
  (if-not (= (hic/parse-tag-name (name (nh 0))) (hic/parse-tag-name (name (oh 0))))
    (dom/replace! el (create oh))
    (let [om (hic/attributes oh)
          nm (hic/attributes nh)
          och (hic/children oh)
          nch (hic/children nh)]
      (when-not (= och nch)
        (update-children! el (hic/flatten-children och) (hic/flatten-children nch)))
      (when-not (= om nm)
        (update-attributes! el om nm)))))

(defn update!
  [el ph h]
  (when-not (= ph h)
    (cond
      (hic/literal? h) (dom/replace-text! el h)
      (vector? h) (update-vector! el ph h))))