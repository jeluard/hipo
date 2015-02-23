(ns hipo.compiler
  (:require [clojure.string :as str]
            [cljs.analyzer.api :as ana]
            [hipo.interceptor :refer [intercept]]))

(def +svg-ns+ "http://www.w3.org/2000/svg")
(def +svg-tags+ #{"svg" "g" "rect" "circle" "clipPath" "path" "line" "polygon" "polyline" "text" "textPath"})

(defn literal?
  [o]
  (or (string? o) (number? o) (true? o) (false? o)))

(defmulti compile-set-attribute! (fn [_ a _] a))

(defmethod compile-set-attribute! :default
  [el a v]
  (cond
    (= a "id")
    `(set! (.-id ~el) ~v)
    (= 0 (.indexOf a "on-"))
    (let [e (.substring a 3)] `(.addEventListener ~el ~e ~v))
    :else
    `(.setAttribute ~el ~a ~v)))

(defmacro compile-set-attribute!*
  "compile-time set attribute"
  [el k v]
  {:pre [(keyword? k)]}
  (let [a (name k)]
    (if (literal? v)
      (if v
        (compile-set-attribute! el a v))
      (let [ve (gensym "v")]
        `(let [~ve ~v]
           (if ~ve
             ~(compile-set-attribute! el a ve)))))))

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
  [namespace-uri tag]
  (if namespace-uri
    `(.createElementNS js/document ~namespace-uri ~tag)
    `(.createElement js/document ~tag)))

(defn- form-name
  [form]
  (if (and (seq? form) (symbol? (first form)))
    (name (first form))))

(defmulti compile-append-form (fn [_ f _] (form-name f)))

(defmethod compile-append-form "for"
  [el [_ bindings body] ahs]
  `(doseq ~bindings (compile-append-child ~el ~body ~ahs)))

(defmethod compile-append-form "if"
  [el [_ condition & body] ahs]
  (if (= 1 (count body))
    `(if ~condition (compile-append-child ~el ~(first body) ~ahs))
    `(if ~condition (compile-append-child ~el ~(first body) ~ahs)
                    (compile-append-child ~el ~(second body) ~ahs))))

(defmethod compile-append-form "when"
  [el [_ condition & body] ahs]
  (assert (= 1 (count body)) "Only a single form is supported with when")
  `(if ~condition (compile-append-child ~el ~(last body) ~ahs)))

(defmethod compile-append-form "list"
  [el [_ & body] ahs]
  `(do ~@(for [o body] `(compile-append-child ~el ~o ~ahs))))

(defmethod compile-append-form :default
  [el o ahs]
  (when o
    `(let [o# ~o]
       (if o#
         (hipo.interpreter/append-to-parent ~el o# ~ahs)))))

(defn text-compliant-hint?
  [data env]
  (when (seq? data)
    (when-let [f (first data)]
      (when (symbol? f)
        (let [t (:tag (ana/resolve env f))]
          (or (= t 'boolean)
              (= t 'string)
              (= t 'number)))))))

(defn text-content?
  [data env]
  (or (literal? data)
      (-> data meta :text)
      (text-compliant-hint? data env)))

(defmacro compile-append-child
  [el data ahs]
  (cond
    (text-content? data &env) `(.appendChild ~el (.createTextNode js/document ~data))
    (vector? data) `(.appendChild ~el (compile-create-vector ~data ~ahs))
    :else (compile-append-form el data ahs)))

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
  [[node-key & rest] ahs]
  (let [literal-attrs (when-let [f (first rest)] (when (map? f) f))
        var-attrs (when (and (not literal-attrs) (-> rest first meta :attrs))
                    (first rest))
        children (if (or literal-attrs var-attrs) (drop 1 rest) rest)
        [tag class-keyword id-keyword] (parse-keyword node-key)
        class (compile-class literal-attrs class-keyword)
        el (gensym "el")
        element-ns (when (+svg-tags+ tag) +svg-ns+)]
    (if (and (nil? rest) (nil? id-keyword) (empty? class-keyword))
      `(compile-create-element ~element-ns ~tag)
    `(let [~el (compile-create-element ~element-ns ~tag)]
       ~(when id-keyword
          `(set! (.-id ~el) ~id-keyword))
       ~(when class
          `(set! (.-className ~el) ~class))
       ~@(for [[k v] (dissoc literal-attrs :class)]
           `(compile-set-attribute!* ~el ~k ~v))
       ~(when var-attrs
          (let [k (gensym "k")
                v (gensym "v")]
            `(doseq [[~k ~v] ~var-attrs]
               (when ~v
                 ~(if class
                    `(if (= :class ~k)
                       (set! (.-className ~el) (str ~(str class " ") ~v))
                       (hipo.interpreter/set-attribute! ~el (name ~k) nil ~v ~ahs))
                    `(hipo.interpreter/set-attribute! ~el (name ~k) nil ~v ~ahs))))))
       ~@(when (seq children)
          (if (every? #(text-content? % &env) children)
            `[(set! (.-textContent ~el) (str ~@children))]
            (for [c (filter identity children)]
              `(compile-append-child ~el ~c ~ahs))))
       ~el))))

(defmacro compile-create
  [o m]
  (cond
    (text-content? o &env) `(.createTextNode js/document ~o)
    (vector? o) `(compile-create-vector ~o (hipo.interpreter/attribute-handlers ~m))
    :else `(hipo.interpreter/create ~o (hipo.interpreter/attribute-handlers ~m))))

(defmacro compile-update
  [el f om]
  `(let [a# (atom ~om)]
     (fn [no# & [m#]]
       (let [int# (:interceptor m#)]
         (intercept int# :update {:target ~el}
           (do
             (hipo.interpreter/update! ~el (~f @a#) (~f no#) int# (hipo.interpreter/attribute-handlers m#))
             (reset! a# no#)))))))