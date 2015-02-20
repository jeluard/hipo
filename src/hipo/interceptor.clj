(ns hipo.interceptor)

(defmacro intercept
  [int t m body]
  `(if-not ~int
     ~body
     (let [o# (hipo.interceptor/-intercept ~int ~t ~m)]
       (if-not (false? o#)
         (if (fn? o#)
           (o# (fn [] ~body))
           ~body)))))