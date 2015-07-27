(ns hipo.core
  (:require [hipo.compiler :as hc]
            [hipo.interceptor :refer [intercept]]))

(defmacro create
  "Create a DOM element from hiccup style vector. Second argument is an optional map of options.
   An instance of `HTMLElement`is returned.

   Options:

   * force-compilation? fail create if DOM construction can't be fully compiled
   * force-interpretation? bypass the DOM construction compilation
   * create-element-fn
   * namespaces
   * attribute-handlers
   "
  [h & [m]]
  ; Must be a macro or compilation won't be used as compiler does not walk symbol currently
  (let [v (gensym "v")]
   `(let [~v ~h
          el# ~(if (:force-interpretation? m) `(hipo.interpreter/create ~v ~m) `(hc/compile-create ~v ~m))]
      (hipo.core/set-hiccup! el# ~v)
      el#)))
