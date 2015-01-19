(ns hipo
  (:require [hipo.compiler :as hc]))

(defmacro create
  "Create a DOM element from hiccup style representation."
  [h]
  (when h
    `(let [el# (hc/compile-create ~h)]
       (hipo/set-template! el# ~h)
       el#)))

(defmacro update!
  [el h]
  `(let [ph# (hipo/get-template ~el)]
     (hipo.interpreter/update! ~el ph# ~h)
     (hipo/set-template! ~el ~h)))
