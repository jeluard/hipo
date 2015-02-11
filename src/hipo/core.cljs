(ns hipo.core
  (:require [hipo.interceptor]
            [hipo.interpreter])
  (:require-macros hipo.core))

(defn partially-compiled?
  [el]
  "Returns true if an element created by `create` is partially compiled."
  (boolean (aget el "hipo-partially-compiled")))