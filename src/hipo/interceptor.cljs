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

#_
(deftype LoggingInterceptor [label]
  Interceptor
  (-intercept [_ t _]
    (.profile js/console (str label "-" (name t)))
    (fn [] (.profileEnd js/console))))