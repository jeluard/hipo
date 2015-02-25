(ns hipo.interpreter
  (:require [clojure.set :as set]
            [hipo.dom :as dom]
            [hipo.fast :as f]
            [hipo.hiccup :as hic])
  (:require-macros [hipo.interceptor :refer [intercept]]))

(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defn- listener-name? [s] (identical? 0 (.indexOf s "on-")))
(defn- listener-name->event-name [s] (.substring s 3))

(defn set-attribute!
  [el n ov nv]
  (if (listener-name? n)
    (do
      (if ov
        (.removeEventListener el (listener-name->event-name n) ov))
      (if nv
        (.addEventListener el (listener-name->event-name n) nv)))
    (if nv
      (.setAttribute el n nv)
      (.removeAttribute el n))))

(declare create-child)

(defn append-child!
  [el o]
  (.appendChild el (create-child o)))

(defn append-children!
  [el o]
  (if (seq? o)
    (loop [s o]
      (when-not (empty? s)
        (if-let [h (first s)]
          (append-children! el h))
        (recur (rest s))))
    (append-child! el o)))

(defn create-vector
  [[node-key & rest]]
  (let [literal-attrs (when-let [f (first rest)] (when (map? f) f))
        children (if literal-attrs (drop 1 rest) rest)
        node (name node-key)
        tag (hic/parse-tag-name node)
        id (hic/parse-id node)
        class-str (hic/parse-classes node)
        class-str (if-let [c (:class literal-attrs)] (if class-str (str class-str " " c) (str c)) class-str)
        element-ns (when (+svg-tags+ tag) +svg-ns+)]
    (let [el (dom/create-element element-ns tag)]
      (if class-str
        (set! (.-className el) class-str))
      (if id
        (set! (.-id el) id))
      (doseq [[k v] (dissoc literal-attrs :class)]
        (if v
          (set-attribute! el (name k) nil v)))
      (if (seq children)
        (append-children! el children))
      el)))

(defn mark-as-partially-compiled!
  [el]
  (if-let [pel (.-parentElement el)]
    (recur pel)
    (do (aset el "hipo-partially-compiled" true) el)))

(defn create-child
  [o]
  {:pre [(or (hic/literal? o) (vector? o))]}
  (if (hic/literal? o) ; literal check is much more efficient than vector check
    (.createTextNode js/document o)
    (create-vector o)))

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
  [el om nm int]
  (doseq [[nk nv] nm
          :let [ov (nk om) n (name nk)]]
    (if-not (identical? ov nv)
      (if nv
        (intercept int :update-attribute {:target el :name n :value nv}
          (set-attribute! el n ov nv))
        (intercept int :remove-attribute {:target el :name n}
          (set-attribute! el n ov nil)))))
  (doseq [k (set/difference (set (keys om)) (set (keys nm)))
          :let [n (name k) ov (k om)]]
    (intercept int :remove-attribute {:target el :name n}
      (set-attribute! el n ov nil))))

(declare update!)

(defn- child-key [h] (:key (meta h)))
(defn keyed-children->map [v] (into {} (for [h v] [(child-key h) h])))
(defn keyed-children->indexed-map [v] (into {} (for [ih (map-indexed (fn [idx itm] [idx itm]) v)] [(child-key (nth ih 1)) ih])))

(defn update-keyed-children!
  [el och nch int]
  (let [om (keyed-children->map och)
        nm (keyed-children->indexed-map nch)
        cs (dom/children el (apply max (set/intersection (set (keys nm)) (set (keys om)))))]
    (doseq [[i [ii h]] nm]
      (if-let [oh (get om i)]
        ; existing node; if data is identical? move to new location; otherwise detach, update and insert at the right location
        (intercept int :move-at {:target el :value h :index ii}
          (if (identical? oh h)
            (dom/insert-child-at! el ii (nth cs i))
            (let [ncel (.removeChild el (nth cs i))]
              (update! ncel oh h int)
              (dom/insert-child-at! el ii ncel)))); TODO improve perf by relying on (cs ii)? index should be updated based on new insertions
        ; new node
        (let [nel (create-child h)]
          (intercept int :insert-at {:target el :value nel :index ii}
            (dom/insert-child-at! el ii nel)))))
    (let [d (count (set/difference (set (keys om)) (set (keys nm))))]
      (intercept int :remove-trailing {:target el :value d}
        (dom/remove-trailing-children! el d)))))

(defn update-non-keyed-children!
  [el och nch int]
  (let [oc (count och)
        nc (count nch)
        d (- oc nc)]
    ; Remove now unused elements if (count och) > (count nch)
    (if (pos? d)
      (intercept int :remove-trailing {:target el :value d}
        (dom/remove-trailing-children! el d)))
    ; Assume children are always in the same order i.e. an element is identified by its position
    ; Update all existing node
    (dotimes [i (min oc nc)]
      (let [ov (nth och i)
            nv (nth nch i)]
        (if-not (identical? ov nv)
          (if-let [cel (dom/child-at el i)]
            (update! cel ov nv int)))))
    ; Create new elements if (count nch) > (count oh)
    (if (neg? d)
      (if (identical? -1 d)
        (let [nel (peek nch)]
          (intercept int :append {:target el :value nel}
            (append-child! el nel)))
        (let [f (.createDocumentFragment js/document)
              cs (apply list (if (identical? 0 oc) nch (subvec nch oc)))]
          ; An intermediary DocumentFragment is used to reduce the number of append to the attached node
          (intercept int :append {:target el :value cs}
            (append-children! f cs))
          (.appendChild el f))))))

(defn keyed-children? [v] (not (nil? (child-key (nth v 0)))))

(defn update-children!
  [el och nch int]
  (if (f/emptyv? nch)
    (intercept int :clear {:target el}
      (dom/clear! el))
    (if (keyed-children? nch)
      (update-keyed-children! el och nch int)
      (update-non-keyed-children! el och nch int))))

(defn update-vector!
  [el oh nh int]
  {:pre [(vector? oh) (vector? nh)]}
  (if-not (identical? (hic/parse-tag-name (name (nth nh 0))) (hic/parse-tag-name (name (nth oh 0))))
    (let [nel (create nh)]
      (intercept int :replace {:target el :value nel}
        (dom/replace! el nel)))
    (let [om (hic/attributes oh)
          nm (hic/attributes nh)
          och (hic/children oh)
          nch (hic/children nh)]
      (if-not (identical? och nch)
        (intercept int :update-children {:target el}
          (update-children! el (hic/flatten-children och) (hic/flatten-children nch) int)))
      (if-not (identical? om nm)
        (update-attributes! el om nm int)))))

(defn update!
  [el ph h int]
  {:pre [(or (vector? h) (hic/literal? h))]}
  (if (hic/literal? h) ; literal check is much more efficient than vector check
    (if-not (identical? ph h)
      (intercept int :replace {:target el :value h}
        (dom/replace-text! el h)))
    (update-vector! el ph h int)))