(ns hipo
  (:require [hipo.compiler :as hc]))

(defmacro create
  "Create a DOM element from hiccup style representation."
  [h]
  (if h
    `(let [el# (hc/compile-create ~h)]
       (hipo/set-template! el# ~h)
       el#)))

(defmacro intercept
  [int t m body]
  `(let [o# (if ~int (hipo.interpreter/-intercept ~int ~t ~m))]
     (when-not (false? o#)
       ~body
       (if (fn? o#)
         (o#)))))

(defmacro update!
  [el h & [m]]
  `(let [ph# (hipo/get-template ~el)
         int# (:interceptor ~m)]
     (intercept int# :update {:target ~el}
       (hipo.interpreter/update! ~el ph# ~h int#))
     (hipo/set-template! ~el ~h)))