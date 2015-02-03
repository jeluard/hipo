(ns hipo.interceptor
  (:require [hipo.interpreter :refer [Interceptor]]))

(deftype TimeInterceptor [s]
  Interceptor
  (-intercept [_ t _]
    (let [label (str s "-" (name t))]
      (.time js/console label)
      (fn [] (.timeEnd js/console label)))))

(deftype ProfileInterceptor [label]
  Interceptor
  (-intercept [_ t _]
    (if (= t :update)
      (.profile js/console label)
      (fn [] (.profileEnd js/console label)))))

(deftype PerformanceInterceptor [label]
  Interceptor
  (-intercept [_ t _]
    (let [mark-begin (str label " begin " t)
          mark-end (str label " end " t)]
      (.mark js/performance mark-begin)
      (fn []
        (.mark js/performance mark-end)
        (.measure js/performance (str label " " t) mark-begin mark-end)))))
