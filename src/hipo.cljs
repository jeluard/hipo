(ns hipo
  (:require [hipo.interpreter :as hi]))

(defn get-template [el] (aget el "hipo_template"))
(defn set-template! [el h] (aset el "hipo_template" h))

(defn partially-compiled?
  [el]
  "Returns true if an element created by `create` is partially compiled."
  (boolean (aget el "hipo-partially-compiled")))