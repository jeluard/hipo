(ns hipo
  (:require [hipo.compiler :as comp]))

(defmacro create
  "Create a DOM element from hiccup style representation."
  [o]
  (when o
    `(comp/compile-create ~o)))