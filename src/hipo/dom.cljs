(ns hipo.dom)

(defn create-element [namespace-uri tag]
  (if namespace-uri
    (.createElementNS js/document namespace-uri tag)
    (.createElement js/document tag)))

(defn- node? [o] (instance? js/Node o))
(defn- element? [el] (if el (identical? 1 (.-nodeType el))))
(defn- text-element? [el] (if el (identical? 3 (.-nodeType el))))

(defn child
  [el i]
  {:pre [(node? el) (not (neg? i))]}
  (aget (.-childNodes el) i))

(defn children
  ([el] (children el 0))
  ([el i]
   {:pre [(node? el)
          (not (neg? i))]}
   (let [fel (.-firstChild el)]
     (if fel
       (loop [cel fel
              acc [cel]]
         (let [nel (.-nextSibling cel)]
           (if (and (not (zero? (- (count acc) (inc i)))) nel)
             (recur nel (conj acc nel))
             acc)))))))

(defn replace!
  [el nel]
  {:pre [(node? el) (node? nel)
         (not (nil? (.-parentElement el)))]}
  (.replaceChild (.-parentElement el) nel el))

(defn replace-text!
  [el s]
  {:pre [(node? el) (string? s)]}
  (if (text-element? el)
    (set! (.-textContent el) s)
    (replace! el (.createTextNode js/document s))))

(defn clear!
  [el]
  {:pre [(element? el)]}
  (set! (.-innerHTML el) ""))

(defn remove-trailing-children!
  [el n]
  {:pre [(node? el) (not (neg? n))]}
  (dotimes [_ n]
    (.removeChild el (.-lastChild el))))

(defn insert-child!
  [el i nel]
  {:pre [(node? el) (not (neg? i)) (node? nel)]}
  (.insertBefore el nel (child el i)))

(defn remove-child!
  [el i]
  {:pre [(node? el) (not (neg? i))]}
  (.removeChild el (child el i)))
