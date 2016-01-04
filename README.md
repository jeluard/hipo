# Hipo [![License](http://img.shields.io/badge/license-EPL-blue.svg?style=flat)](https://www.eclipse.org/legal/epl-v10.html) [![Build Status](http://img.shields.io/travis/jeluard/hipo.svg?style=flat)](http://travis-ci.org/#!/jeluard/hipo/builds)

[Usage](#usage) | [Extensibility](#extensibility) | [Performance](#performance)  | [Security](#security)

A ClojureScript DOM templating library based on [hiccup](https://github.com/weavejester/hiccup) syntax. Supports live DOM node reconciliation (à la [React](http://facebook.github.io/react/)).
`hipo` aims to be 100% compatible with `hiccup` syntax.

[![Clojars Project](http://clojars.org/hipo/latest-version.svg)](http://clojars.org/hipo).

`hipo` uses `Reader Conditionals`. Make sure your project depends on Clojure 1.7 and a recent ClojureScript version.

## Usage

### Creation

`hipo.core/create` converts an hiccup vector into a DOM node that can be directly inserted in a document.

Note that the hiccup syntax is extended to handle all properties whose name starts with **on-** as event listener registration.
Listeners can be provided as a function or as a map (`{:name "my-listener" :fn (fn [] (.log js/console 1))}`) in which case they will only be updated if the name is updated.

```clojure
(ns my-test
  (:require [hipo.core :as hipo]))

(let [el (hipo/create [:div#id.class [:span 1]])]
  (.appendChild js/document.body el)
  ; el is:
  ; <div id="id" class="class">
  ;   <span>1</span>
  ; </div>
  )
```

### Reconciliation

A DOM node can be reconciled to a new hiccup representation using `hipo.core/reconciliate!`.
Each time the reconciliation function is called the DOM element is modified so that it reflects the new hiccup element.
The reconciliation performs a diff of hiccup structure (DOM is not read) and tries to minimize DOM changes.

```clojure
(ns my-test
  (:require [hipo.core :as hipo]))

(let [el (hipo/create [:div#id.class [:span 1]])]
  (.appendChild js/document.body el)
  ; el is:
  ; <div id="id" class="class">
  ;   <span>1</span>
  ; </div>

  ; ... time passes
  (hipo/reconciliate! el [:div#id.class [:span 2]])

  ; el is now;
  ; <div id="id" class="class">
  ;   <span>2</span>
  ; </div>
  )
```

Children are assumed to keep their position across reconciliations. If children can be shuffled around while still keeping their identity the `hipo/key` metadata must be used.

```clojure
(ns my-test
  (:require [hipo.core :as hipo]))

(let [f (fn [s] [:ul (for [i s] ^{:hipo/key i} [:li i])])
      el (hipo/create (f (range 6)))]
  (.appendChild js/document.body el)
  ; ... time passes
  (hipo/reconciliate! el (f (reverse (range 6)))))
```

### Interceptor

Any DOM changes happening during the reconciliation can be intercepted / prevented via an `Interceptor` implementation. Interceptors are defined by providing a vector as `:interceptors` value in the option map.

An interceptor must implement the `-intercept` function that receives 3 arguments:

* a keyword type, either `:reconciliate`, `:append`, `:insert`, `:move`, `:remove`, `:replace`, `:clear`, `:remove-trailing`, `:update-attribute` or `:remove-attribute`.
* a map of relevant details
* a function encapsulating the change execution

It's the interceptor responsibility to call the provided function at most once to trigger the eventual change execution. If no interceptor skip the call the change is performed.

Beware that preventing some part of the reconciliation might lead to an inconsistent state.

```clojure
(ns …
  (:require [hipo.core :as hipo]
            [hipo.interceptor :refer [Interceptor]]))

(deftype PrintInterceptor []
  Interceptor
  (-intercept [_ t m]
    (if (= t :move)
      (println (:target m) "has been moved"))
    (f)))

(let [el (hipo/create [:div])]
  (.appendChild js/document.body el)
  ; ... time passes
  (hipo/reconciliate! el [:span] {:interceptors [(MyInterceptor.)]}))
```

Some [interceptors](https://github.com/jeluard/hipo/blob/master/src/hipo/interceptor.cljs) are bundled by default.

## Extensibility

### Attribute handling

Element attribute handling can be extending by providing a vector as `:attribute-handlers` value in the option map.
Attribute can be targeted by providing a combination of `:ns`, `:tag` and `:attr` (no value matches all candidates).


`:type` (`:prop` or `:attr`) defines if this attribute should be manipulated via attribute or property access.

```clojure
(hipo/create [:input {:checked true}]
             {:attribute-handlers [{:target {:tag "input" :attr #{"checked" "value"}} :type :prop}]})
```

Alternatively provide a custom function via `:fn` that will be responsible for dealing with this attribute value.

```clojure
(hipo/create [:span {:style {:background-color "blue"}]
             {:attribute-handlers [{:target {:attr "style"} :fn some-fn}]})
```

Some [handlers](https://github.com/jeluard/hipo/blob/master/src/hipo/attribute.cljc) are bundled by default.

### Namespaces

DOM elements are created assuming the default HTML namespace. Specific namespaces can be used by introducing a namespace when declaring DOM nodes / attributes. This namespace is then used as a key to lookup the full namespace URL. By default `svg` and `xlink` are supported.

```clojure
(hipo/create [:svg/svg
               [:svg/circle {:r "10"}]
               [:svg/use {:xlink/href "#id"}]])
```

```clojure
(hipo/create [:div
               [:some-ns/elem]]
             {:namespaces {"some-ns" "some://url"}})
```

### Element creation

A function can be passed to customize an element creation. This is useful when more efficient ways of creating a component are available.

```clojure
(ns my-ns)

(defn my-custom-fn
  [ns tag attrs]
  ...)

(hipo/create [:div ^:text (my-fn)] {:create-element-fn my-ns/my-custom-fn})
```

As it can be referenced at macro expansion time the function must be provided as a fully qualified symbol.

## Performance

### Creation

At compile-time JavaScript code is generated from the hiccup representation to minimize DOM node creation cost at the expense of code size.

`(hipo/create [:div.class {:on-click #(.log js/console "click)} [:span])` will be converted into the following ClojureScript:

```clojure
(let [el (. js/document createElement "div")]
  (.setAttribute "class" "class")
  (. el addEventListener "click" #(.log js/console "click"))
  (. el appendChild (. js/document createElement "span"))
  el)
```

Interpretation can be forced by providing `:force-interpretation? true` in the option map. Alternatively `:force-compilation? true` will make `create` fail if compilation is not complete.

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

### Type-Hinting

When you know the result of a function call will be converted to an HTML text node (as opposed to an HTML element) the `^:text` metadata can be used as a hint for the compiler to optimize the generated JavaScript code.

```clojure
(defn my-fn []
  (str "content"))

(hipo/create [:div ^:text (my-fn)])
```

## Security

`hipo` creates HTML element only based on the first element of hiccup vectors. No user-provided string will be used to create elements (no usage of `innerHTML` to set content).
It should be noted that attribute value are not filtered and will be set as-is. Some combination might trigger code evaluation (like `href="javascript:.."`) and should be treated accordingly.

## Credits

Initial code comes from the great [dommy](https://github.com/Prismatic/dommy) library which is now focused on DOM manipulation. The original dommy code is available as hipo [0.1.0](https://github.com/jeluard/hipo/tree/0.1.0).

## License

Copyright (C) 2013 Prismatic

Copyright (C) 2014 - 2015 Julien Eluard

Distributed under the Eclipse Public License, the same as Clojure.
