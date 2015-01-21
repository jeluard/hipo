(ns hipo.dom)

(defn create-element [namespace-uri tag is]
  (if namespace-uri
    (if is
      (.createElementNS js/document namespace-uri tag is)
      (.createElementNS js/document namespace-uri tag))
    (if is
      (.createElement js/document tag is)
      (.createElement js/document tag))))

(defn child-node
  [el i]
  (.item (.-childNodes el) i))

(defn replace!
  [el nel]
  (.replaceChild (.-parentElement el) nel el))

(defn replace-text!
  [el s]
  (if (= 3 (.-nodeType el))
    (set! (.-textContent el) s)
    (replace! el (.createTextNode js/document s))))

(defn clear!
  [el]
  (set! (.-innerHTML el) ""))

(defn remove-trailing-children!
  [el n]
  (dotimes [_ n]
    (.removeChild el (.-lastChild el))))