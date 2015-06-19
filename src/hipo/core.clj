(ns hipo.core
  (:require [hipo.compiler :as hc]
            [hipo.interceptor :refer [intercept]]))

(defmacro create-static
  "Create a DOM element from hiccup style representation provided as a vector. Second argument is an optional map of options."
  [h & [m]]
  (if h
    `(hc/compile-create ~h ~m)))

(defmacro create
  "Create a DOM element from hiccup style representation provided as a couple function / initial values. Third argument is an optional map of options that is propagated to the reconciliate function.

   The first argument must be a function of one argument returning an hiccup vector and the second argument a payload that will be provided to the function described previously.
   Invocation will return a vector composed of a DOM element and a function that can be used to reconciliate the DOM element based on a new payload.
   This function accept as second argument an optional map of options."
  [f o & [m]]
  `(hc/compile-reconciliate ~f ~o ~m))
