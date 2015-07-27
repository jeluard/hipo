(ns hipo.core
  (:require [hipo.interceptor]
            [hipo.interpreter :as hi])
  (:require-macros hipo.core))

(def ^:private hiccup-property "hipo_hiccup")

(defn get-hiccup [el] (aget el hiccup-property))

(defn set-hiccup!
  [el h]
  (aset el hiccup-property h))

(defn reconciliate!
  "Reconciliate an existing DOM element to match an hiccup style vector.
   Reconciliation works by diffing the hiccup used to create the DOM element with a new hiccup. Element created with `hipo.core/create` can be reconcilied without providing the previous hiccup.
   Last argument is an optional map of options.

   Options:

   * force-compilation? fail create if DOM construction can't be fully compiled
   * force-interpretation? bypass the DOM construction compilation
   * create-element-fn
   * namespaces
   * attribute-handlers
   * interceptors
  "
  ([el nh] (reconciliate! el nh {}))
  ([el nh m] (reconciliate! el (get-hiccup el) nh m))
  ([el oh nh m]
    (assert (not (nil? oh)) "Previous hiccup can't be nil")
    (set-hiccup! el nh)
    (hi/reconciliate! el oh nh m)))
