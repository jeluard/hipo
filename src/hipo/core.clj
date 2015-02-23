(ns hipo.core
  (:require [hipo.compiler :as hc]
            [hipo.interceptor :refer [intercept]]))

(defmacro create
  "Create a DOM element from hiccup style representation."
  [h & [m]]
  (if h
    `(hc/compile-create ~h ~m)))

(defmacro create-for-update
  ([oh]
    `(if-let [el# (create ~oh)]
       (let [a# (atom ~oh)]
         [el#
          (fn [nh# & [m#]]
            (let [int# (:interceptor m#)
                  ahs# (hipo.interpreter/attribute-handlers m#)]
              (intercept int# :update {:target el#}
                (do
                  (hipo.interpreter/update! el# @a# nh# int# ahs#)
                  (reset! a# nh#)))))])))
  ([f oo]
    `(let [oo# ~oo
           f# ~f]
       (if-let [el# (create (f# oo#))]
         [el# (hc/compile-update el# f# oo#)]))))