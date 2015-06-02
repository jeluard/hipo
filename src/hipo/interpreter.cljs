(ns hipo.interpreter
  (:require [clojure.set :as set]
            [hipo.dom :as dom]
            [hipo.element :as el]
            [hipo.fast :as f]
            [hipo.hiccup :as hic])
  (:require-macros [hipo.interceptor :refer [intercept]]))

(defn set-attribute!
  ([el sok ov nv] (set-attribute! el sok ov nv nil))
  ([el sok ov nv int]
   (let [n (name sok)]
     (if (hic/listener-name? n)
       (if (or (nil? nv) (fn? nv))
         (intercept int (if nv :update-handler :remove-handler) (merge {:target el :name sok :old-value ov} (if nv {:new-value nv}))
           (do
             (if ov
               (.removeEventListener el (hic/listener-name->event-name n) ov))
             (if nv
               (.addEventListener el (hic/listener-name->event-name n) nv)))))
       (if (nil? nv)
         (intercept int :remove-attribute {:target el :name sok :old-value ov}
           (if (and (not (= n "class"))
                    (or (not (hic/literal? ov)) (el/input-property? (.-localName el) n)))
             (aset el n nil)
             (.removeAttribute el n)))
         (intercept int :update-attribute {:target el :name sok :old-value ov :new-value nv}
           (cond
             ; class can only be as attribute for svg elements
             (= n "class")
             (.setAttribute el n nv)
             (not (hic/literal? nv)) ; Set non-literal via property
             (aset el n nv)
             (el/input-property? (.-localName el) n)
             (aset el n nv)
             :else
             (.setAttribute el n nv))))))))

(declare create-child)

(defn append-child!
  [el o]
  (.appendChild el (create-child o)))

(defn append-children!
  [el v]
  {:pre [(vector? v)]}
  (loop [v (hic/flatten-children v)]
    (when-not (f/emptyv? v)
      (if-let [h (nth v 0)]
        (append-child! el h))
      (recur (rest v)))))

(defn create-vector
  [h]
  {:pre [(vector? h)]}
  (let [tag (hic/tag h)
        attrs (hic/attributes h)
        children (hic/children h)
        el (dom/create-element (el/tag->ns tag) tag)]
    (doseq [[sok v] attrs]
      (if v
        (set-attribute! el sok nil v)))
    (append-children! el children)
    el))

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
  (if (seq? o)
    (append-children! el (vec o))
    (append-child! el o)))

(defn create
  [o]
  {:pre [(not (nil? o))]}
  (mark-as-partially-compiled!
    (if (seq? o)
      (let [f (.createDocumentFragment js/document)]
        (append-children! f (vec o))
        f)
      (create-child o))))

; Reconciliate

(defn reconciliate-attributes!
  [el om nm int]
  (doseq [[sok nv] nm
          :let [ov (get om sok)]]
    (if-not (identical? ov nv)
      (if nv
        (set-attribute! el sok ov nv int)
        (set-attribute! el sok ov nil int))))
  (doseq [sok (set/difference (set (keys om)) (set (keys nm)))]
    (set-attribute! el sok (get om sok) nil int)))

(declare reconciliate!)

(defn- child-key [h] (:key (meta h)))
(defn- keyed-children->indexed-map [v] (into {} (for [ih (map-indexed (fn [idx itm] [idx itm]) v)] [(child-key (nth ih 1)) ih])))

(defn reconciliate-keyed-children!
  "Reconciliate a vector of children based on their associated key."
  [el och nch int]
  (let [om (keyed-children->indexed-map och)
        nm (keyed-children->indexed-map nch)
        ; TODO reduce set calculation
        cs (dom/children el (apply max (set/intersection (set (keys nm)) (set (keys om)))))]
    ; Iterate over new elements looking for matching (same key) in old vector
    ; TODO strategy is not optimale when removing first element. should remove first based on some threshold
    (doseq [[i [ii h]] nm]
      (if-let [[iii oh] (get om i)]
        (let [cel (nth cs iii)]
          ; existing node
          (if (identical? ii iii)
            ; node kept its position; reconciliate
            (reconciliate! cel oh h int)
            ; node changed location; if data is identical? move to new location; otherwise detach, reconciliate and insert at the right location
            (intercept int :move-at {:target el :value h :index ii}
              (if (identical? oh h)
                (dom/insert-child-at! el ii cel)
                (let [ncel (.removeChild el cel)]
                  (reconciliate! ncel oh h int)
                  (dom/insert-child-at! el ii ncel))))))
        ; new node; insert it at current index
        (intercept int :insert-at {:target el :value h :index ii}
          (dom/insert-child-at! el ii (create-child h)))))
    ; All now useless nodes have been pushed at the end; remove them
    (let [d (count (set/difference (set (keys om)) (set (keys nm))))]
      (if (pos? d)
        (intercept int :remove-trailing {:target el :count d}
          (dom/remove-trailing-children! el d))))))

(defn reconciliate-non-keyed-children!
  [el och nch int]
  (let [oc (count och)
        nc (count nch)
        d (- oc nc)]
    ; Remove now unused elements if (count och) > (count nch)
    (if (pos? d)
      (intercept int :remove-trailing {:target el :count d}
        (dom/remove-trailing-children! el d)))
    ; Assume children are always in the same order i.e. an element is identified by its position
    ; Reconciliate all existing node
    (dotimes [i (min oc nc)]
      (let [ov (nth och i)
            nv (nth nch i)]
        (if-not (identical? ov nv)
          ; Reconciliate value unless previously nil (insert) or newly nil (remove)
          (cond
            (nil? ov)
            (intercept int :insert-at {:target el :value nv :index i}
              (dom/insert-child-at! el i (create nv)))
            (nil? nv)
            (intercept int :remove-at {:target el :index i}
              (dom/remove-child-at! el i))
            :else
            (if-let [cel (dom/child-at el i)]
              (reconciliate! cel ov nv int))))))
    ; Create new elements if (count nch) > (count oh)
    (if (neg? d)
      (if (identical? -1 d)
        (if-let [h (nth nch oc)]
          (intercept int :append {:target el :value h}
            (append-child! el h)))
        (let [f (.createDocumentFragment js/document)
              cs (if (identical? 0 oc) nch (subvec nch oc))]
          ; An intermediary DocumentFragment is used to reduce the number of append to the attached node
          (intercept int :append {:target el :value cs}
            (append-children! f cs))
          (.appendChild el f))))))

(defn keyed-children? [v] (not (nil? (child-key (nth v 0)))))

(defn reconciliate-children!
  [el och nch int]
  (if (f/emptyv? nch)
    (if-not (f/emptyv? och)
      (intercept int :clear {:target el}
        (dom/clear! el)))
    (if (keyed-children? nch)
      (reconciliate-keyed-children! el och nch int)
      (reconciliate-non-keyed-children! el och nch int))))

(defn reconciliate-vector!
  [el oh nh int]
  {:pre [(vector? nh)]}
  (if (or (hic/literal? oh) (not (identical? (hic/tag nh) (hic/tag oh))))
    (let [nel (create nh)]
      (intercept int :replace {:target el :value nh}
        (dom/replace! el nel)))
    (let [om (hic/attributes oh)
          nm (hic/attributes nh)
          och (hic/children oh)
          nch (hic/children nh)]
      (if-not (identical? och nch)
        (intercept int :reconciliate-children {:target el :old-value och :new-value nch}
          (reconciliate-children! el (hic/flatten-children och) (hic/flatten-children nch) int)))
      (if-not (identical? om nm)
        (reconciliate-attributes! el om nm int)))))

(defn reconciliate!
  [el ph h int]
  {:pre [(or (vector? h) (hic/literal? h))]}
  (if (hic/literal? h) ; literal check is much more efficient than vector check
    (if-not (identical? ph h)
      (intercept int :replace {:target el :value h}
        (dom/replace-text! el (str h))))
    (reconciliate-vector! el ph h int)))
