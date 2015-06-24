(ns hipo.interceptor)

(defmacro intercept
  [v t m & body]
  `(let [b# (fn [] ~@body)
         v# ~v]
     (if (or (not v#) (empty? v#))
       (b#)
       (hipo.interceptor/call b# v# ~t ~m))))