(ns hipo
  (:require [hipo.compiler :as hc]))

(defmacro create
  "Create a DOM element from hiccup style representation."
  [h]
  (when h
    `(let [el# (hc/compile-create ~h)]
       (hipo.interpreter/set-template! el# ~h)
       el#)))
