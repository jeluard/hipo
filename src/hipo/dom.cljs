(ns hipo.dom)

(defn create-element [namespace-uri tag is]
  (if namespace-uri
    (if is
      (.createElementNS js/document namespace-uri tag is)
      (.createElementNS js/document namespace-uri tag))
    (if is
      (.createElement js/document tag is)
      (.createElement js/document tag))))

(defn- element? [el] (when el (= 1 (.-nodeType el))))

(defn child-at
  [el i]
  {:pre [(element? el)]}
  (loop [c 0
         cel (.-firstChild el)]
    (if (or (nil? cel) (= i c))
      cel
      (recur (inc c) (.-nextSibling cel)))))

(defn children
  ([el] (children el 0))
  ([el i]
   {:pre [(element? el)
          (not (neg? i))]}
   (let [fel (.-firstChild el)]
     (when fel
       (loop [cel fel
              acc [cel]]
         (let [nel (.-nextSibling cel)]
           (if (and (not (zero? (- (count acc) (inc i)))) nel)
             (recur nel (conj acc nel))
             acc)))))))

(defn replace!
  [el nel]
  {:pre [(element? el) (element? nel)
         (not (nil? (.-parentElement el)))]}
  (.replaceChild (.-parentElement el) nel el))

(defn replace-text!
  [el s]
  [:pre [(element? el)]]
  (if (= 3 (.-nodeType el))
    (set! (.-textContent el) s)
    (replace! el (.createTextNode js/document s))))

(defn clear!
  [el]
  {:pre [(element? el)]}
  (set! (.-innerHTML el) ""))

(defn remove-trailing-children!
  [el n]
  {:pre [(element? el)]}
  (dotimes [_ n]
    (if (exists? (.-remove el))
      (.remove (.-lastChild el))
      (.removeChild el (.-lastChild el)))))

(defn insert-child-at!
  [el i nel]
  (.insertBefore el nel (child-at el i)))
