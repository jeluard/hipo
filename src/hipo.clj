(ns hipo
  (:require [hipo.compiler :as hc]))

(defmacro create
  "Create a DOM element from hiccup style representation."
  [h]
  (if h
    `(hc/compile-create ~h)))

(defmacro intercept
  [int t m body]
  `(let [o# (if ~int (hipo.interceptor/-intercept ~int ~t ~m))]
     (when-not (false? o#)
       ~body
       (if (fn? o#)
         (o#)))))

(defmacro create-for-update
  ([oh]
    `(if-let [el# (create ~oh)]
       (let [a# (atom ~oh)]
         [el#
          (fn [nh# & [m#]]
            (let [int# (:interceptor m#)]
              (intercept int# :update {:target el#}
                (do
                  (hipo.interpreter/update! el# @a# nh# int#)
                  (reset! a# nh#)))))])))
  ([f oo]
    `(if-let [el# (create (~f ~oo))]
       (let [a# (atom ~oo)]
         [el#
          (fn [no# & [m#]]
            (let [int# (:interceptor m#)]
              (intercept int# :update {:target el#}
                (do
                  (hipo.interpreter/update! el# (~f @a#) (~f no#) int#)
                  (reset! a# no#)))))]))))
