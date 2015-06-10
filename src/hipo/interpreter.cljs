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
           (if ov
             (.removeEventListener el (hic/listener-name->event-name n) ov))
           (if nv
             (.addEventListener el (hic/listener-name->event-name n) nv))))
       (if (nil? nv)
         (if ov
           (intercept int :remove-attribute {:target el :name sok :old-value ov}
              (if (and (not (identical? n "class"))
                       (or (not (hic/literal? ov)) (el/input-property? (.-localName el) n)))
                (aset el n nil)
                (.removeAttribute el n))))
         (if-not (= ov nv)
           (intercept int :update-attribute {:target el :name sok :old-value ov :new-value nv}
             (cond
               ; class can only be as attribute for svg elements
               (identical? n "class")
               (.setAttribute el n nv)
               (not (hic/literal? nv)) ; Set non-literal via property
               (aset el n nv)
               (el/input-property? (.-localName el) n)
               (aset el n nv)
               :else
               (.setAttribute el n nv)))))))))

(declare create-child)

(defn append-children!
  [el v m]
  {:pre [(vector? v)]}
  (loop [v (hic/flatten-children v)]
    (when-not (f/emptyv? v)
      (if-let [h (nth v 0)]
        (.appendChild el (create-child h m)))
      (recur (rest v)))))

(defn default-create-element
  [ns tag attrs]
  (let [el (dom/create-element ns tag)]
    (doseq [[sok v] attrs]
      (if v
        (set-attribute! el sok nil v)))
    el))

(defn create-element
  [ns tag attrs m]
  (if-let [f (:create-element-fn m)]
    (f ns tag attrs)
    (default-create-element ns tag attrs)))

(defn create-vector
  [h m]
  {:pre [(vector? h)]}
  (let [tag (hic/tag h)
        attrs (hic/attributes h)
        children (hic/children h)
        el (create-element (el/tag->ns tag) tag attrs m)]
    (if children
      (append-children! el children m))
    el))

(defn mark-as-partially-compiled!
  [el]
  (if-let [pel (.-parentElement el)]
    (recur pel)
    (do (aset el "hipo-partially-compiled" true) el)))

(defn create-child
  [o m]
  {:pre [(or (hic/literal? o) (vector? o))]}
  (if (hic/literal? o) ; literal check is much more efficient than vector check
    (.createTextNode js/document o)
    (create-vector o m)))

(defn append-to-parent
  [el o m]
  {:pre [(not (nil? o))]}
  (mark-as-partially-compiled! el)
  (if (seq? o)
    (append-children! el (vec o) m)
    (.appendChild el (create-child o m))))

(defn create
  [o m]
  {:pre [(not (nil? o))]}
  (mark-as-partially-compiled!
    (create-child o m)))

; Reconciliate

(defn reconciliate-attributes!
  [el om nm int]
  (doseq [[sok nv] nm
          :let [ov (get om sok)]]
    (set-attribute! el sok ov nv int))
  (doseq [sok (set/difference (set (keys om)) (set (keys nm)))]
    (set-attribute! el sok (get om sok) nil int)))

(declare reconciliate!)

(defn- child-key [h] (:key (meta h)))
(defn- keyed-children->indexed-map [v] (into {} (for [ih (map-indexed (fn [idx itm] [idx itm]) v)] [(child-key (nth ih 1)) ih])))

(defn reconciliate-keyed-children!
  "Reconciliate a vector of children based on their associated key."
  [el och nch {:keys [interceptor] :as m}]
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
            (reconciliate! cel oh h m)
            ; node changed location; detach, reconciliate and insert at the right location
            (intercept interceptor :move {:target el :value h :index ii}
              (let [ncel (.removeChild el cel)]
                (reconciliate! ncel oh h m)
                (dom/insert-child! el ii ncel)))))
        ; new node; insert it at current index
        (intercept interceptor :insert {:target el :value h :index ii}
          (dom/insert-child! el ii (create-child h m)))))
    ; All now useless nodes have been pushed at the end; remove them
    (let [d (count (set/difference (set (keys om)) (set (keys nm))))]
      (if (pos? d)
        (intercept interceptor :remove-trailing {:target el :count d}
          (dom/remove-trailing-children! el d))))))

(defn reconciliate-non-keyed-children!
  [el och nch {:keys [interceptor] :as m}]
  (let [oc (count och)
        nc (count nch)
        d (- oc nc)]
    ; Remove now unused elements if (count och) > (count nch)
    (if (pos? d)
      (intercept interceptor :remove-trailing {:target el :count d}
        (dom/remove-trailing-children! el d)))
    ; Assume children are always in the same order i.e. an element is identified by its position
    ; Reconciliate all existing node
    (dotimes [i (min oc nc)]
      (let [ov (nth och i)
            nv (nth nch i)]
        (if-not (and (nil? ov) (nil? nv))
          ; Reconciliate value unless previously nil (insert) or newly nil (remove)
          (cond
            (nil? ov)
            (intercept interceptor :insert {:target el :value nv :index i}
              (dom/insert-child! el i (create-child nv m)))
            (nil? nv)
            (intercept interceptor :remove {:target el :index i}
              (dom/remove-child! el i))
            :else
            (if-let [cel (dom/child el i)]
              (reconciliate! cel ov nv m))))))
    ; Create new elements if (count nch) > (count oh)
    (if (neg? d)
      (if (identical? -1 d)
        (if-let [h (nth nch oc)]
          (intercept interceptor :append {:target el :value h}
            (.appendChild el (create-child h m))))
        (let [f (.createDocumentFragment js/document)
              cs (if (identical? 0 oc) nch (subvec nch oc))]
          ; An intermediary DocumentFragment is used to reduce the number of append to the attached node
          (intercept interceptor :append {:target el :value cs}
            (append-children! f cs m))
          (.appendChild el f))))))

(defn keyed-children? [v] (not (nil? (child-key (nth v 0)))))

(defn reconciliate-children!
  [el och nch {:keys [interceptor] :as m}]
  (if (f/emptyv? nch)
    (if-not (f/emptyv? och)
      (intercept interceptor :clear {:target el}
        (dom/clear! el)))
    (if (keyed-children? nch)
      (reconciliate-keyed-children! el och nch m)
      (reconciliate-non-keyed-children! el och nch m))))

(defn reconciliate-vector!
  [el oh nh {:keys [interceptor] :as m}]
  {:pre [(vector? nh)]}
  (if (or (hic/literal? oh) (not (identical? (hic/tag nh) (hic/tag oh))))
    (let [nel (create-child nh m)]
      (intercept interceptor :replace {:target el :value nh}
        (dom/replace! el nel)))
    (let [om (hic/attributes oh)
          nm (hic/attributes nh)
          och (hic/children oh)
          nch (hic/children nh)]
      (intercept interceptor :reconciliate {:target el :old-value och :new-value nch}
        (reconciliate-children! el (hic/flatten-children och) (hic/flatten-children nch) m))
      (reconciliate-attributes! el om nm interceptor))))

(defn reconciliate!
  [el oh nh {:keys [interceptor] :as m}]
  {:pre [(or (vector? nh) (hic/literal? nh))
         (or (nil? m) (map? m))]}
  (if (hic/literal? nh) ; literal check is much more efficient than vector check
    (if-not (identical? oh nh)
      (intercept interceptor :replace {:target el :value nh}
        (dom/replace-text! el (str nh))))
    (reconciliate-vector! el oh nh m)))
