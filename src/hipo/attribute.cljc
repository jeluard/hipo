(ns hipo.attribute
  (:require [hipo.hiccup :as hic]))

#?(:cljs
(def style-handler {:target {:attr "style"} :fn #(doseq [[k v] %4] (aset %1 "style" (name k) v))}))

(defn- property-name->js-property-name [n] (.replace n "-" "_"))

(defn set-property-value [el k v] (aset el (property-name->js-property-name (name k)) v))

(defn set-attribute!
  [el k v m]
  (if-let [nns (if (keyword? k) (hic/key->namespace (namespace k) m))]
    (.setAttributeNS el nns (name k) v)
    (.setAttribute el (name k) v)))

(defn remove-attribute!
  [el k m]
  (if-let [nns (if (keyword? k) (hic/key->namespace (namespace k) m))]
    (.removeAttributeNS el nns (name k))
    (.removeAttribute el (name k))))

(def default-handler-fns
  {:prop {:fn #(set-property-value %1 %2 %4)}
   :attr {:fn #(if %4 (set-attribute! %1 %2 %4 %5) (remove-attribute! %1 %2 %5))}})

(def default-handlers
  [{:target {:ns "svg" :attr "class"} :type :attr}
   {:target {:tag "input" :attr #{"value" "checked"}} :type :prop}
   {:target {:tag "input" :attr "autofocus"} :fn #(when %4 (.focus %1) (.setAttribute %1 %2 %4))}
   {:target {:tag "option" :attr #{"selected"}} :type :prop}
   {:target {:tag "select" :attr #{"value" "selectIndex"}} :type :prop}
   {:target {:tag "textarea" :attr #{"value"}} :type :prop}])

(defn matches?
  [expr s]
  (if expr
    (cond
      (set? expr) (contains? expr s)
      :else (= s expr))
    true))

(defn target-matches?
  [m ns tag attr]
  (and (matches? (:ns m) ns)
       (matches? (:tag m) tag)
       (matches? (:attr m) attr)))

(defn handler
  [m ns tag attr]
  (let [v (concat (:attribute-handlers m) default-handlers)
        h (some #(let [t (:target %)] (if (target-matches? t ns tag (name attr)) %)) v)]
    (if (contains? h :type)
      ((:type h) default-handler-fns)
      h)))

(defn default-set-value!
  [el attr ov nv m]
  ; Set object via property access. Literal values are set via attribute access because associated properties might not exist.
  (if (or (hic/literal? ov) (hic/literal? nv))
    (if nv (set-attribute! el attr nv m) (remove-attribute! el attr m))
    (aset el attr (set-property-value el attr nv))))

(defn set-value!
  [el m ns tag attr ov nv]
  (let [h (handler m ns tag attr)
        f (or (:fn h) default-set-value!)]
    (f el attr ov nv m)))
