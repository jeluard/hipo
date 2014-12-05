(ns hipo
  (:require [hipo.interpreter :as hi]))

(defn partially-compiled?
  [el]
  "Returns true if an element created by `create` is partially compiled."
  (boolean (aget el "hipo-partially-compiled")))
