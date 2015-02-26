(ns hipo.core
  (:require [hipo.compiler :as hc]
            [hipo.interceptor :refer [intercept]]))

(defmacro create
  "Create a DOM element from hiccup style representation."
  [h]
  (if h
    `(hc/compile-create ~h)))

(defmacro create-for-update
  "Create a DOM element and associated reconciliation function. Returned as an vector `[el f]."
  ([oh]
    `(hipo.interpreter/create-for-update ~oh))
  ([f oo]
    `(let [oo# ~oo
           f# ~f]
       (if-let [el# (create (f# oo#))]
         [el# (hc/compile-update el# f# oo#)]))))