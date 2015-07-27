(ns hipo.interceptor
  (:require-macros hipo.interceptor))

(defprotocol Interceptor
  (-intercept [this t m f]))

(defn call
  [f v t m]
  (let [i (first v)]
    (-intercept i t m #(let [s (rest v)]
                        (if (seq s)
                          (call f s t m)
                          (f))))))

(deftype LogInterceptor [b]
  Interceptor
  (-intercept [_ t m f]
    (if (or (not b) (not= :reconciliate t))
      (.log js/console (name t) " " (clj->js m)))
    (f)))

(deftype TimeInterceptor [s]
  Interceptor
  (-intercept [_ t _ f]
    (let [label (str s "-" (name t))]
      (.time js/console label)
      (f)
      (.timeEnd js/console label))))

(deftype ProfileInterceptor [label]
  Interceptor
  (-intercept [_ t _ f]
    (when (= t :reconciliate)
      (.profile js/console label)
      (f)
      (.profileEnd js/console label))
    (f)))

(deftype PerformanceInterceptor [label]
  Interceptor
  ; http://w3c.github.io/user-timing/
  (-intercept [_ t _ f]
    (let [mark-begin (str label " begin " t)
          mark-end (str label " end " t)]
      (.mark js/performance mark-begin)
      (f)
      (.mark js/performance mark-end)
      (.measure js/performance (str label " " t) mark-begin mark-end))))

(deftype StateInterceptor [a]
  Interceptor
  (-intercept [_ t o f]
    (swap! a #(cons %2 %1) {:type t :value o})
    (f)))

(deftype StaticReconciliationInterceptor []
  Interceptor
  (-intercept [_ t o f]
    (if (= :reconciliate t)
      (if-not (contains? (meta (:new-value o)) :hipo/static)
        (f))
      (f))))

(deftype IdentityReconciliationInterceptor []
  Interceptor
  (-intercept [_ t o f]
    (if (= :reconciliate t)
      (if-not (identical? (:old-value o) (:new-value o))
        (f))
      (f))))
