(ns hipo.dom)

(defn create-element [namespace-uri tag is]
  (if namespace-uri
    (if is
      (.createElementNS js/document namespace-uri tag is)
      (.createElementNS js/document namespace-uri tag))
    (if is
      (.createElement js/document tag is)
      (.createElement js/document tag))))

(defn cached-child-nodes
  [el]
  (if-let [chs (aget el "hipo_chs")]
    chs
    (let [chs (.-childNodes el)]
      (aset el "hipo_chs" chs)
      chs)))

(defn child-node
  [el i]
  (.item (cached-child-nodes el) i))

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