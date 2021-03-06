(ns hipo.compiler
  (:require [clojure.string :as str]
            [cljs.analyzer.api :as ana]
            [hipo.hiccup :as hic]))

(defn compile-set-attribute!
  [el m ns tag k v]
  (if-let [en (hic/listener-name->event-name (name k))]
    (let [f (or (:fn v) v)]
      `(do (.addEventListener ~el ~en ~f)
           (aset ~el ~(str "hipo_listener_" en) ~f)))
    `(hipo.attribute/set-value! ~el ~m ~ns ~tag ~k nil ~v)))

(defmacro compile-set-attribute!*
  "compile-time set attribute"
  [el m ns tag k v]
  {:pre [(keyword? k)]}
  (if (hic/literal? v)
    (if v
      (compile-set-attribute! el m ns tag k v))
    (let [ve (gensym "v")]
      `(let [~ve ~v]
         (if ~ve
           ~(compile-set-attribute! el m ns tag k ve))))))

(defn parse-keyword
  "return pair [tag class-str id] where tag is dom tag and attrs
   are key-value attribute pairs from css-style dom selector"
  [node-key]
  (let [node-str (name node-key)
        node-tag (second (re-find #"^([^.\#]+)[.\#]?" node-str))
        classes (map #(.substring ^String % 1) (re-seq #"\.[^.*]*" node-str))
        id (first (map #(.substring ^String % 1) (re-seq #"#[^.*]*" node-str)))]
    [(namespace node-key)
     (if (empty? node-tag) "div" node-tag)
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

(defmulti compile-append-form (fn [_ f _] (form-name f)))

(defmethod compile-append-form "for"
  [el [_ bindings body] m]
  `(doseq ~bindings (compile-append-child ~el ~body ~m)))

(defmethod compile-append-form "if"
  [el [_ condition & body] m]
  (if (= 1 (count body))
    `(if ~condition (compile-append-child ~el ~(first body) ~m))
    `(if ~condition (compile-append-child ~el ~(first body)~m )
                    (compile-append-child ~el ~(second body) ~m))))

(defmethod compile-append-form "when"
  [el [_ condition & body] m]
  (assert (= 1 (count body)) "Only a single form is supported with when")
  `(if ~condition (compile-append-child ~el ~(last body) ~m)))

(defmethod compile-append-form "list"
  [el [_ & body] m]
  `(do ~@(for [o body] `(compile-append-child ~el ~o ~m))))

(defmethod compile-append-form :default
  [el o m]
  (if o
    (if (:force-compilation? m)
      `(throw (ex-info "Failed to compile" {:value ~o}))
      `(hipo.interpreter/append-to-parent ~el ~o ~m))))

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
  [el data m]
  (cond
    (text-content? data &env) `(.appendChild ~el (.createTextNode js/document ~data))
    (vector? data) `(.appendChild ~el (compile-create-vector ~data ~m))
    :else (compile-append-form el data m)))

(defn compile-class
  [literal-attrs class-keyword]
  (if class-keyword
    (if-let [literal-class (or (:class literal-attrs) (get literal-attrs "class"))]
      (if (string? literal-class)
        (str class-keyword " " literal-class)
        `(str ~(str class-keyword " ") ~literal-class))
      class-keyword)))

(defmacro compile-var-attrs
  [el ns tag var-attrs class m]
  (let [k (gensym "k")
        v (gensym "v")]
    `(doseq [[~k ~v] ~var-attrs]
       (if ~v
         ~(if class
            `(let [cs# (if (= :class ~k) (str ~(str class " ") ~v) ~v)]
               (hipo.interpreter/set-attribute! ~el ~ns ~tag (name ~k) nil cs# ~m))
            `(hipo.interpreter/set-attribute! ~el ~ns ~tag (name ~k) nil ~v ~m))))))

(defn compile-children
  [el children m env]
  (if (seq children)
    (if (every? #(text-content? % env) children)
      (if (= 1 (count children))
        `[(set! (.-textContent ~el) ~@children)]
        `[(set! (.-textContent ~el) (str ~@children))])
      (for [c (keep identity children)]
        `(compile-append-child ~el ~c ~m)))))

(defmacro compile-create-vector
  [[node-key & rest] m]
  (let [literal-attrs (if-let [f (first rest)] (if (map? f) f))
        var-attrs (if (and (not literal-attrs) (-> rest first meta :attrs))
                    (first rest))
        children (if (or literal-attrs var-attrs) (drop 1 rest) rest)
        [nns tag class id] (parse-keyword node-key)
        ns (hic/key->namespace nns m)
        class (compile-class literal-attrs class)
        el (gensym "el")]
    (cond
      (not (keyword? node-key))
      `(throw (ex-info "Node key must be a keyword" {}))

      (and id (or (contains? literal-attrs :id) (contains? literal-attrs "id")))
      `(throw (ex-info "Cannot define id multiple times" {}))

      (and (empty? rest) (= (name node-key) tag)) ; simple DOM element e.g. [:div]
      `(compile-create-element ~ns ~tag)

      :default
      (if-let [f (:create-element-fn m)]
        (do
          (assert (symbol? f) "Value for :create-element-fn must be a symbol")
          `(let [~el (~f ~ns ~tag (merge ~@literal-attrs ~@var-attrs ~@(if id `{:id ~id}) ~@(if class `{:class ~class})))]
             ~@(compile-children el children m &env)
             ~el))
        `(let [~el (compile-create-element ~ns ~tag)]
           ~@(for [[k v] (merge literal-attrs (if id {:id id}) (if class {:class class}))]
               `(compile-set-attribute!* ~el ~m ~tag ~ns ~k ~v))
           ~@(if var-attrs
               (if id
                 `[(do
                     (if (or (contains? ~var-attrs :id) (contains? ~var-attrs "id"))
                       (throw (ex-info "Cannot define id multiple times" {}))
                       (compile-var-attrs ~el ~ns ~tag ~var-attrs ~class ~m)))]
                 `[(compile-var-attrs ~el ~ns ~tag ~var-attrs ~class ~m)]))
           ~@(compile-children el children m &env)
           ~el)))))

(defmacro compile-create
  [o m]
  (cond
    (text-content? o &env) `(.createTextNode js/document ~o)
    (vector? o) `(compile-create-vector ~o ~m)
    :else (if (:force-compilation? m) `(throw (ex-info "Failed to compile" {:value ~o})) `(hipo.interpreter/create ~o ~m))))
