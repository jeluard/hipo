(ns hipo.attribute
  (:require [hipo.hiccup :as hic]))

(defn- property-name->js-property-name [n] (.replace n "-" "_"))

(defn set-property-value [el n v] (aset el (property-name->js-property-name n) v))

(def default-handler-fns
  {:prop {:fn #(set-property-value %1 %2 %4)}
   :attr {:fn #(if %4 (.setAttribute %1 %2 %4) (.removeAttribute %1 %2))}})

(def default-handlers
  [{:target {:ns "svg" :attr "class"} :type :attr}
   {:target {:tag "input" :attr #{"value" "checked"}} :type :prop}
   {:target {:tag "input" :attr "autofocus"} :fn #(when %4 (.focus %1) (.setAttribute %1 %2 %4))}
   {:target {:tag "option" :attr #{"selected"}} :type :prop}
   {:target {:tag "select" :attr #{"value" "selectIndex"}} :type :prop}
   {:target {:tag "textarea" :attr #{"value"}} :type :prop}])

(defn matches?
  [s expr]
  (if s
    (cond
      (set? expr) (contains? expr s)
      :else (= s expr))
    true))

(defn target-matches?
  [m ns tag attr]
  (and (matches? ns (:ns m))
       (matches? tag (:tag m))
       (matches? attr (:attr m))))

(defn handler
  [m ns tag attr]
  (let [v (concat (:attribute-handlers m) default-handlers)
        h (some #(let [t (:target %)] (if (target-matches? t ns tag attr) %)) v)]
    (if (contains? h :type)
      ((:type h) default-handler-fns)
      h)))

(defn default-set-value!
  [el attr ov nv]
  ; Set object via property access. Literal values are set via attribute access because associated properties might not exist.
  (if (or (hic/literal? ov) (hic/literal? nv))
    (if nv (.setAttribute el attr nv) (.removeAttribute el attr))
    (aset el attr (set-property-value el attr nv))))

(defn set-value!
  [el m ns tag attr ov nv]
  (let [h (handler m ns tag attr)
        f (or (:fn h) default-set-value!)]
    (f el attr ov nv)))