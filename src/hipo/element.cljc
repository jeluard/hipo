(ns hipo.element)

(def ^:private svg-ns "http://www.w3.org/2000/svg")
(def ^:private svg-tags #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defn tag->ns
  [s]
  (if (svg-tags s)
    svg-ns))

(def ^:private input-properties
  {"input" #{"value" "checked"}
   "option" #{"selected"}
   "select" #{"value" "selectIndex"}
   "textarea" #{"value"}})

(defn input-property?
  [t n]
  (if-let [s (get input-properties (name t))]
    (contains? s (name n))))