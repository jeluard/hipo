(ns hipo.core
  (:require [hipo.compiler :as hc]
            [hipo.interceptor :refer [intercept]]))

(defmacro create
  "Create a DOM element from hiccup style representation.

   When 1 argument is provided it must be an hiccup vector. Invocation will return a DOM element.
   When 2 arguments are provided the first argument must be a function of one argument returning an hiccup vector and the second argument a payload that will be provided to the function described previously.
   Invocation will return a vector composed of a DOM element and an update function that can be used to reconciliate the DOM element based on a new payload."
  ([h]
   (if h
     `(hc/compile-create ~h)))
  ([f oo]
   `(let [f# ~f
          oh# (f# ~oo)]
      (if-let [el# (create oh#)]
        [el# (hc/compile-update el# f# oh#)]))))
