# Hipo [![License](http://img.shields.io/badge/license-EPL-blue.svg?style=flat)](https://www.eclipse.org/legal/epl-v10.html) [![Build Status](http://img.shields.io/travis/jeluard/hipo.svg?style=flat)](http://travis-ci.org/#!/jeluard/hipo/builds) [![Dependency Status](https://www.versioneye.com/user/projects/545c247f287666dca9000049/badge.svg?style=flat)](https://www.versioneye.com/user/projects/545c247f287666dca9000049)

[Usage](#usage) | [Extensibility](#extensibility) | [Performance](#performance)

A ClojureScript DOM templating library based on [hiccup](https://github.com/weavejester/hiccup) syntax. Supports live DOM node reconciliation (à la [React](http://facebook.github.io/react/)).
`hipo` aims to be 100% compatible with `hiccup` syntax.

[![Clojars Project](http://clojars.org/hipo/latest-version.svg)](http://clojars.org/hipo).

## Usage

### Creation

`create-static` converts an hiccup vector into a DOM node that can be directly inserted in a document.

```clojure
(ns …
  (:require [hipo.core :as hipo]))

(let [el (hipo/create-static [:div#id.class {:on-click #(.log js/console "click")} [:span]])]
  (.appendChild js/document.body el))
```

Note that the hiccup syntax is extended to handle all properties whose name starts with **on-** as event listener registration.
Listeners can be provided as a function or as a map (`{:name "my-listener" :fn (fn [] (.log js/console 1))}`) in which case they will only be updated if the name is updated.

### Reconciliation

`create` accepts a function returning an hiccup vector and a payload. It then returns a vector of the DOM element and a reconciliation function accepting another payload as argument.
Each time the reconciliation function is called the DOM element is modified so that it reflects the new hiccup element.
The reconciliation performs a diff of hiccup structure (DOM is not read) and tries to minimize DOM changes.

```clojure
(let [[el f] (hipo/create (fn [m] [:div#id.class [:span (:some-key m)]])
                          {:some-key "1"})]
  (.appendChild js/document.body el)
  ; el is:
  ; <div id="id" class="class">
  ;   <span>1</span>
  ; </div>

  ; ... time passes
  (f {:some-key "2"})

  ; el is now;
  ; <div id="id" class="class">
  ;   <span>2</span>
  ; </div>
  )
```

Children are assumed to keep their position across reconciliations. If children can be shuffled around while still keeping their identity the `key` metadata must be used.

```clojure
(let [[el f] (hipo/create
               (fn [m]
                 [:ul (for [i (:children m)]
                   ^{:key i} [:li {:class i} i])])
               {:children (range 6)})]
  (.appendChild js/document.body el)
  ; ... time passes
  (f {:children (reverse (range 6))}))
```

### Interceptor

Any DOM changes happening during the reconciliation can be intercepted / prevented via an `Interceptor` implementation.

An interceptor must implement the `-intercept` function that receives 2 arguments:

* a keyword type, either `:reconciliate`, `:append`, `:insert`, `:move`, `:remove`, `:replace`, `:clear`, `:remove-trailing`, `:update-attribute` or `:remove-attribute`.
* a map of relevant details

When called this function can return either:

* false, then associated DOM manipulation is skipped
* a function that receives as only argument the function performing this specific DOM reconciliation

Beware that preventing some part of the reconciliation might lead to an inconsistent state.

```clojure
(ns …
  (:require [hipo.core :as hipo]
            [hipo.interceptor :refer [Interceptor]]))

(deftype PrintInterceptor []
  Interceptor
  (-intercept [_ t m]
    (case t
      :update-attribute false ; cancel all update-attribute
      :move (fn [f] (f) (println (:target m) "has been moved"))
      true))

(let [[el f] (hipo/create
               (fn [m]
                 [:ul (for [i (:children m)]
                   ^{:key i} [:li {:class i} i])])
               {:children (range 6)})]
  (.appendChild js/document.body el)
  ; ... time passes
  (f {:children (reverse (range 6))}
     {:interceptor (MyInterceptor.)}))
```

Some [interceptors](https://github.com/jeluard/hipo/blob/master/src/hipo/interceptor.cljs) are bundled by default.

## Performance

### Creation

At compile-time JavaScript code is generated from the hiccup representation to minimize DOM node creation cost at the expense of code size.

`(create-static [:div.class {:on-click #(.log js/console "click)} [:span])` will be converted into the following ClojureScript:

```clojure
(let [el (. js/document createElement "div")]
  (.setAttribute "class" "class")
  (. el addEventListener "click" #(.log js/console "click"))
  (. el appendChild (. js/document createElement "span"))
  el)
```

Attributes defined via a function (as opposed to literal maps) must be annotated with `^:attrs`. This allows for simpler generated code as a function in second place can denote either attributes or a child node.

```clojure
(ns …
  (:require [hipo.core :as hipo]))

(hipo/create [:div ^:attrs (merge {:class "class"} {:id "id"}) (fn [] [:span])])
```

When the hiccup representation can't be fully compiled the remaining hiccup elements are interpreted at runtime. This might happen when functions or parameters are used.
Once in interpreted mode any nested child will not be compiled even if it is a valid candidate for compilation.

```clojure
(defn children []
  (let [data ...] ; some data accessed at runtime
    (case (:type data)
      1 [:div "content"]
      2 [:ul (for [o (:value data)]
          [:li (str "content-" o)])])))

(hipo/create [:div (children)]) ; anything returned by children will be interpreted at runtime
```

`partially-compiled?` allows to check if some hiccup vector has been partially compiled or not.

```clojure
(ns …
  (:require [hipo.core :as hipo]))

(let [el (hipo/create [:div#id.class "content"])]
  (hipo/partially-compiled? el)) ; => false
```

### Type-Hinting

When you know the result of a function call will be converted to an HTML text node (as opposed to an HTML element) the `^:text` metadata can be used as a hint for the compiler to optimise the generated JavaScript code.

```clojure
(defn my-fn []
  (str "content"))

(hipo/create [:div ^:text (my-fn)])
```

## Extensibility

A function can be passed to customize an element creation. This is useful when more efficient ways of creating a component are available.

```clojure
(ns my-ns)

(defn my-custom-fn
  [ns tag attrs]
  ...)

(hipo/create-static [:div ^:text (my-fn)] {:create-element-fn my-ns/my-custom-fn})
```

As it can be referenced at macro expansion time the function must be provided as a fully qualified symbol.

## Credits

Initial code comes from the great [dommy](https://github.com/Prismatic/dommy) library which is now focused on DOM manipulation. The original dommy code is available as hipo [0.1.0](https://github.com/jeluard/hipo/tree/0.1.0).

## License

Copyright (C) 2013 Prismatic

Copyright (C) 2014 - 2015 Julien Eluard

Distributed under the Eclipse Public License, the same as Clojure.
