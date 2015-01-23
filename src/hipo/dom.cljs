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

(defn child-node
  [el i]
  {:pre [(element? el)
         (< i (.. el -childNodes -length))]}
  (.item (.-childNodes el) i))

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
  [:pre [(element? el)]]
  (set! (.-innerHTML el) ""))

(defn remove-trailing-children!
  [el n]
  [:pre [(element? el)]]
  (dotimes [_ n]
    (.removeChild el (.-lastChild el))))