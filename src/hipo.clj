(ns hipo
  (:require [hipo.compiler :as comp]))

(defmacro create
  [& data]
  (if (and (= 1 (count data)) (vector? (first data)))
    `(comp/compile-create-vector ~(first data))
    (let [f (gensym "f")]
      `(let [~f (.createDocumentFragment js/document)]
         ~@(for [o data]
             `(comp/compile-create-child ~f ~o))
         ~f))))