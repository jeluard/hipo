(ns hipo.compiler
  (:require [clojure.string :as str]
            [cljs.analyzer.api :as ana]
            [hipo.element :as el]
            [hipo.hiccup :as hic]
            [hipo.interceptor :refer [intercept]]))

(defn compile-set-attribute!
  [t el n v]
  (if (hic/listener-name? n)
    `(.addEventListener ~el ~(hic/listener-name->event-name n) ~v)
    (cond
      ; class can only be as attribute for svg elements
      (= n "class")
      `(.setAttribute ~el ~n ~v)
      (el/input-property? t n)
      `(aset ~el ~n ~v)
      :else
      `(.setAttribute ~el ~n ~v))))

(defmacro compile-set-attribute!*
  "compile-time set attribute"
  [t el k v]
  {:pre [(keyword? k)]}
  (let [a (name k)]
    (if (hic/literal? v)
      (if v
        (compile-set-attribute! t el a v))
      (let [ve (gensym "v")]
        `(let [~ve ~v]
           (if ~ve
             ~(compile-set-attribute! t el a ve)))))))

(defn parse-keyword
  "return pair [tag class-str id] where tag is dom tag and attrs
   are key-value attribute pairs from css-style dom selector"
  [node-key]
  (let [node-str (name node-key)
        node-tag (second (re-find #"^([^.\#]+)[.\#]?" node-str))
        classes (map #(.substring ^String % 1) (re-seq #"\.[^.*]*" node-str))
        id (first (map #(.substring ^String % 1) (re-seq #"#[^.*]*" node-str)))]
    [(if (empty? node-tag) "div" node-tag)
     (if-not (empty? classes) (str/join " " classes))
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

(defmulti compile-append-form (fn [_ f] (form-name f)))

(defmethod compile-append-form "for"
  [el [_ bindings body]]
  `(doseq ~bindings (compile-append-child ~el ~body)))

(defmethod compile-append-form "if"
  [el [_ condition & body]]
  (if (= 1 (count body))
    `(if ~condition (compile-append-child ~el ~(first body)))
    `(if ~condition (compile-append-child ~el ~(first body))
                    (compile-append-child ~el ~(second body)))))

(defmethod compile-append-form "when"
  [el [_ condition & body]]
  (assert (= 1 (count body)) "Only a single form is supported with when")
  `(if ~condition (compile-append-child ~el ~(last body))))

(defmethod compile-append-form "list"
  [el [_ & body]]
  `(do ~@(for [o body] `(compile-append-child ~el ~o))))

(defmethod compile-append-form :default
  [el o]
  (if o
    `(let [o# ~o]
       (if o#
         (hipo.interpreter/append-to-parent ~el o#)))))

(defn text-compliant-hint?
  [data env]
  (if (seq? data)
    (if-let [f (first data)]
      (if (symbol? f)
        (let [t (:tag (ana/resolve env f))]
          (or (= t 'boolean)
              (= t 'string)
              (= t 'number)))))))

(defn text-content?
  [data env]
  (or (hic/literal? data)
      (-> data meta :text)
      (text-compliant-hint? data env)))

(defmacro compile-append-child
  [el data]
  (cond
    (text-content? data &env) `(.appendChild ~el (.createTextNode js/document ~data))
    (vector? data) `(.appendChild ~el (compile-create-vector ~data))
    :else (compile-append-form el data)))

(defn compile-class
  [literal-attrs class-keyword]
  (if class-keyword
    (if-let [literal-class (or (:class literal-attrs) (get literal-attrs "class"))]
      (if (string? literal-class)
        (str class-keyword " " literal-class)
        `(str ~(str class-keyword " ") ~literal-class))
      class-keyword)))

(defmacro compile-var-attrs
  [el var-attrs class]
  (let [k (gensym "k")
        v (gensym "v")]
    `(doseq [[~k ~v] ~var-attrs]
       (if ~v
         ~(if class
            `(let [cs# (if (= :class ~k) (str ~(str class " ") ~v) ~v)]
               (hipo.interpreter/set-attribute! ~el (name ~k) nil cs#))
            `(hipo.interpreter/set-attribute! ~el (name ~k) nil ~v))))))

(defmacro compile-create-vector
  [[node-key & rest]]
  (let [literal-attrs (if-let [f (first rest)] (if (map? f) f))
        var-attrs (if (and (not literal-attrs) (-> rest first meta :attrs))
                    (first rest))
        children (if (or literal-attrs var-attrs) (drop 1 rest) rest)
        [tag class id] (parse-keyword node-key)
        class (compile-class literal-attrs class)
        el (gensym "el")
        ns (el/tag->ns tag)]
    (cond
      (and id (or (contains? literal-attrs :id) (contains? literal-attrs "id")))
      `(throw (ex-info "Cannot define id multiple times" {}))

      (and (empty? rest) (= (name node-key) tag)) ; simple DOM element e.g. [:div]
      `(compile-create-element ~ns ~tag)

      :default
      `(let [~el (compile-create-element ~ns ~tag)]
         ~@(for [[k v] (merge literal-attrs (if id {:id id}) (if class {:class class}))]
             `(compile-set-attribute!* ~tag ~el ~k ~v))
         ~(if var-attrs
            (if id
              `(do
                 (if (or (contains? ~var-attrs :id) (contains? ~var-attrs "id"))
                   (throw (ex-info "Cannot define id multiple times" {}))
                   (compile-var-attrs ~el ~var-attrs ~class)))
              `(compile-var-attrs ~el ~var-attrs ~class)))
         ~@(if (seq children)
             (if (every? #(text-content? % &env) children)
               (if (= 1 (count children))
                 `[(set! (.-textContent ~el) ~@children)]
                 `[(set! (.-textContent ~el) (str ~@children))])
               (for [c (filter identity children)]
                 `(compile-append-child ~el ~c))))
         ~el))))

(defmacro compile-create
  [o]
  (cond
    (text-content? o &env) `(.createTextNode js/document ~o)
    (vector? o) `(compile-create-vector ~o)
    :else `(hipo.interpreter/create ~o)))

(defmacro compile-reconciliate
  [f o]
  `(let [h# (~f ~o)
         a# (atom h#)]
     (if-let [el# (compile-create h#)]
       [el#
        (fn [no# & [m#]]
          (let [int# (:interceptor m#)
                oh# @a#
                nh# (~f no#)]
            (intercept int# :reconciliate {:target el# :old-value oh# :new-value nh#}
              (do
                (hipo.interpreter/reconciliate! el# oh# nh# int#)
                (reset! a# nh#)))))])))