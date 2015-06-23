(ns hipo.element)

(def ^:private input-properties
  {"input" #{"value" "checked"}
   "option" #{"selected"}
   "select" #{"value" "selectIndex"}
   "textarea" #{"value"}})

(defn input-property?
  [t n]
  (if-let [s (get input-properties (name t))]
    (contains? s (name n))))