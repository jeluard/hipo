(ns hipo.interceptor)

(defmacro intercept
  [int t m & body]
  `(let [b# (fn [] ~@body)]
     (if-not ~int
       (b#)
       (let [o# (hipo.interceptor/-intercept ~int ~t ~m)]
         (if-not (false? o#)
           (if (fn? o#)
             (o# b#)
             (b#)))))))