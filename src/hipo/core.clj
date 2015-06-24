(ns hipo.core
  (:require [hipo.compiler :as hc]
            [hipo.interceptor :refer [intercept]]))

(defmacro create
  "Create a DOM element from hiccup style representation provided as a vector. Third argument is an optional map of options that is propagated to the reconciliate function.
   A vector of [`element` `reconciliation-fn`] is returned, with:

   * `element` a DOM node
   * `reconciliation-fn` a function that reconciliate the live `element` based on a new hiccup vector passed as argument. Takes an optional map of options merged with the one provided in the `create` call

   Options:

   * force-compilation? fail create if DOM construction can't be fully compiled
   * force-interpretation? bypass the DOM construction compilation
   * create-element-fn
   * namespaces
   * attribute-handlers
   * interceptors
   "
  [h & [m]]
  ; Must be a macro or compilation won't be used as compiler does not walk symbol currently
  `(let [h# ~h
         a# (volatile! h#)
         m# ~m]
     (if-let [el# (hc/compile-create h# m#)]
       [el#
        (fn [nh# & [mm#]]
          (let [m# (merge m# mm#)
                oh# @a#]
            (hipo.interpreter/reconciliate! el# oh# nh# m#)
            (vreset! a# nh#)))])))
