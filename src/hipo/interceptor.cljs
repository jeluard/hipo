(ns hipo.interceptor)

(defprotocol Interceptor
  (-intercept [this t m]))

(deftype TimeInterceptor [s]
  Interceptor
  (-intercept [_ t _]
    (fn [f]
      (let [label (str s "-" (name t))]
        (.time js/console label)
        (f)
        (.timeEnd js/console label)))))

(deftype ProfileInterceptor [label]
  Interceptor
  (-intercept [_ t _]
    (fn [f]
      (if (= t :update)
        (do
          (.profile js/console label)
          (f)
          (.profileEnd js/console label))
        (f)))))

(deftype PerformanceInterceptor [label]
  Interceptor
  (-intercept [_ t _]
    (fn [f]
      (let [mark-begin (str label " begin " t)
            mark-end (str label " end " t)]
        (.mark js/performance mark-begin)
        (f)
        (.mark js/performance mark-end)
        (.measure js/performance (str label " " t) mark-begin mark-end)))))
