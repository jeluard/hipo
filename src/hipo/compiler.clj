(ns hipo.compiler
  (:require [clojure.string :as str]
            [cljs.analyzer :as ana]))

(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defn literal?
  [o]
  (or (string? o) (number? o) (true? o) (false? o)))

(defmacro set-attr!
  [el k v]
  (cond
    (identical? k :id)
    `(set! (.-id ~el) ~v)
    :else
    `(.setAttribute ~el ~(name k) ~v)))

(defmacro compile-set-attr!
  "compile-time set attribute"
  [el k v]
  (assert (keyword? k))
  (if (literal? v)
    (if v
      `(set-attr! ~el ~k ~v))
    `(let [v# ~v]
       (if v#
         (set-attr! ~el ~k v#)))))

(defn parse-keyword
  "return pair [tag class-str id] where tag is dom tag and attrs
   are key-value attribute pairs from css-style dom selector"
  [node-key]
  (let [node-str (name node-key)
        node-tag (second (re-find #"^([^.\#]+)[.\#]?" node-str))
        classes (map #(.substring ^String % 1) (re-seq #"\.[^.*]*" node-str))
        id (first (map #(.substring ^String % 1) (re-seq #"#[^.*]*" node-str)))]
    [(if (empty? node-tag) "div" node-tag)
     (when-not (empty? classes) (str/join " " classes))
     id]))

(defmacro compile-create-element
  [namespace-uri tag is]
  (if namespace-uri
    (if is
      `(.createElementNS js/document ~namespace-uri ~tag ~is)
      `(.createElementNS js/document ~namespace-uri ~tag))
    (if is
      `(.createElement js/document ~tag ~is)
      `(.createElement js/document ~tag))))

(defn- form-name
  [form]
  (if (and (seq? form) (symbol? (first form)))
    (name (first form))))

(defmulti compile-form
  #(form-name (second %)))

(defmethod compile-form "for"
  [[el [_ bindings body]]]
  `(doseq ~bindings (compile-create-child ~el ~body)))

(defmethod compile-form "if"
  [[el [_ condition & body]]]
  (if (= 1 (count body))
    `(if ~condition (compile-create-child ~el ~(first body)))
    `(if ~condition (compile-create-child ~el ~(first body))
                    (compile-create-child ~el ~(second body)))))

(defmethod compile-form "when"
  [[el [_ condition & body]]]
  (assert (= 1 (count body)) "Only a single form is supported with when")
  `(if ~condition (compile-create-child ~el ~(last body))))

(defmethod compile-form "list"
  [[el [_ & body]]]
  `(do ~@(for [o body] `(compile-create-child ~el ~o))))

(defmethod compile-form :default
  [[el data]]
  `(do
     (hipo.interpreter/mark-as-partially-compiled! ~el)
     (hipo.interpreter/create-children ~el ~data)))

(defn text-compliant-hint?
  [data env]
  (when (seq? data)
    (when-let [f (first data)]
      (when (symbol? f)
        (let [t (:tag (ana/resolve-var env f))]
          (or (= t 'boolean)
              (= t 'string)
              (= t 'number)))))))

(defn text-content?
  [data env]
  (or (literal? data)
      (-> data meta :text)
      (text-compliant-hint? data env)))

(defmacro compile-create-child
  [el data]
  (cond
    (nil? data) nil
    (text-content? data &env) `(.appendChild ~el (.createTextNode js/document ~data))
    (vector? data) `(.appendChild ~el (compile-create-vector ~data))
    :else (compile-form [el data])))

(defn compile-class
  [literal-attrs class-keyword]
  (let [literal-class (:class literal-attrs)]
    (if class-keyword
      (cond
        (nil? literal-class) class-keyword
        (string? literal-class) (str class-keyword " " literal-class)
        :else `(str ~(str class-keyword " ") ~literal-class))
      literal-class)))

(defmacro compile-create-vector
  [[node-key & rest]]
  (let [literal-attrs (when-let [f (first rest)] (when (map? f) f))
        var-attrs (when (and (not literal-attrs) (-> rest first meta :attrs))
                    (first rest))
        children (if (or literal-attrs var-attrs) (drop 1 rest) rest)
        [tag class-keyword id-keyword] (parse-keyword node-key)
        class (compile-class literal-attrs class-keyword)
        el (gensym "el")
        element-ns (when (+svg-tags+ tag) +svg-ns+)
        is (:is literal-attrs)]
    (if (and (nil? rest) (nil? id-keyword) (empty? class-keyword))
      `(compile-create-element ~element-ns ~tag ~is)
    `(let [~el (compile-create-element ~element-ns ~tag ~is)]
       ~(when id-keyword
          `(set! (.-id ~el) ~id-keyword))
       ~(when class
          `(set! (.-className ~el) ~class))
       ~@(for [[k v] (dissoc literal-attrs :class)]
           `(compile-set-attr! ~el ~k ~v))
       ~(when var-attrs
          (let [k (gensym "k")
                v (gensym "v")]
            `(doseq [[~k ~v] ~var-attrs]
               (when ~v
                 ~(if class
                    `(if (= :class ~k)
                       (.setAttribute ~el "class" (str ~(str class " ") ~v))
                       (.setAttribute ~el (name ~k) ~v))
                    `(.setAttribute ~el (name ~k) ~v))))))
       ~@(when (seq children)
          (if (every? #(text-content? % &env) children)
            `[(set! (.-textContent ~el) (str ~@children))]
            (for [c children]
              `(compile-create-child ~el ~c))))
       ~el))))