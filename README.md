# Hipo [![License](http://img.shields.io/badge/license-EPL-blue.svg?style=flat)](https://www.eclipse.org/legal/epl-v10.html) [![Build Status](http://img.shields.io/travis/jeluard/hipo.svg?style=flat)](http://travis-ci.org/#!/jeluard/hipo/builds) [![Dependency Status](https://www.versioneye.com/user/projects/545c247f287666dca9000049/badge.svg?style=flat)](https://www.versioneye.com/user/projects/545c247f287666dca9000049)

[Usage](#usage) | [Extensibility](#extensibility) | [Performance](#performance)

A ClojureScript DOM templating library based on [hiccup](https://github.com/weavejester/hiccup) syntax. Supports live DOM node reconciliation (à la [React](http://facebook.github.io/react/)).

Hipo is available in clojars as `[hipo "0.3.0"]`.

## Usage

### Creation

`create` converts an hiccup vector into a DOM node that can be directly inserted in a document.

```clojure
(ns …
  (:require [hipo :as hipo]))

(let [el (hipo/create [:div#id.class {:on-click #(.log js/console "click")} [:span]])]
  (.appendChild js/document.body el))
```

Note that the hiccup syntax is extended to handle all properties whose name starts with **on-** as event listener registration.

### Reconciliation

`create-for-update` extends `create` by also returning a function that performs DOM reconciliation based on a new hiccup representation.

`create-for-update` can be called in 2 different ways:

As a 1 arity function: the argument is then expected to be a valid hiccup representation. It then returns a reconciliation function accepting another hiccup vector.

As a 2 arity function: the first argument is then expected to be a function and the second a payload. The hiccup representation is the result of calling the function with the payload as unique argument.
It then returns a reconciliation function accepting another payload as argument.

This second variant is more convenient as components usually keep their general shape across time.

```clojure
(let [[el f] (hipo/create-for-update [:div#id.class [:span "1"]])]
  (.appendChild js/document.body el)
  ; ... time passes
  (f [:div#id.class [:span "2"]]))

; or template style if the element shape stays similar

(let [[el f] (hipo/create-for-update (fn [m] [:div#id.class [:span (:some-key m)]]) {:some-key "1"})]
  (.appendChild js/document.body el)
  ; ... time passes
  (f {:some-key "2"}))
```

Children are assumed to keep their position across reconciliations. If children can be shuffled around while still keeping their identity the `key` metadata must be used.

```clojure
(let [[el f] (hipo/create-for-update
               [:ul (for [i (:children m)]
                 ^{:key i} [:li {:class i} i])]
               {:children (range 6)})]
  (.appendChild js/document.body el)
  ; ... time passes
  (f {:children (reverse (range 6))}))
```

### Interceptor

Any DOM changes happening during the reconciliation can be intercepted / prevented via an `Interceptor` implementation.

An interceptor must implement the `-intercept` function that receives 2 arguments:

* a keyword type, either `:update`,`:update-children`, `:append`, `:insert-at`, `:move-at`, `:replace`, `:clear`, `:remove-trailing`, `:update-attribute` or `:remove-attribute`.
* a map of relevant details

When called this function can return either:

* false, then associated DOM manipulation is skipped
* a function that will be called after the manipulation

```clojure
(ns …
  (:require [hipo :as hipo]
            [hipo.interceptor :refer [Interceptor]]))

(deftype PrintInterceptor []
  Interceptor
  (-intercept [_ t m]
    (println t m)
    (case t
      :update-attribute false ; cancel all update-attribute
      :move-at (fn [] (println (:target m) "has been moved"))
      true))

(let [[el f] (hipo/create-for-update
               [:ul (for [i (:children m)]
                 ^{:key i} [:li {:class i} i])]
               {:children (range 6)})]
  (.appendChild js/document.body el)
  ; ... time passes
  (f {:children (reverse (range 6))}
     {:interceptor (MyInterceptor.)}))
```

## Extensibility

### Attribute handling hook

How attributes are handled can be customized both at compilation time and runtime.

```clojure
; this is a clj file
(ns my-custom-methods
  (:require [hipo.compiler :refer [compile-set-attribute!]]))

(defmethod compile-set-attribute! "test"
  [[el a v]]
  `(.setAttribute ~el ~a ~(* 2 v)))
```

```clojure
; this is a cljs file
(ns ..
  (:require [hipo.interpreter :refer [set-attribute!]]
            [my-custom-methods]))

(defmethod set-attribute! "test"
  [[el a v]]
  (.setAttribute el a (* 2 v)))

(create [:div {:test 1}]) ; will set "test" attribute to 2
```

This might be useful to handle special events not supported natively (e.g. **on-tap**) or to implement more efficient methods of setting attributes (e.g. via native attributes).

### Form compilation hook

Some forms can be optimised to increase compilation level.

Default hooks are shipped for `for`, `if`, `when` and `list`. Complex hiccup vectors are then fully compiled:

```clojure
(defn [s]
  (hipo/create
    [:div
      (when s [:h2 s])
      [:ul
        (for [i (range 50)]
          [:li (str "content-" i)])]]))
```

## Performance

### Creation

At compile-time JavaScript code is generated from the hiccup representation to minimize DOM node creation cost at the expense of code size.

The previous hiccup representation would be converted into the following ClojureScript:

```clojure
(let [el (. js/document createElement "div")]
  (set! (. el -id) "id")
  (set! (. el -className) "class")
  (. el addEventListener "click" #(.log js/console "click"))
  (. el appendChild (. js/document createElement "span"))
  el)
```

itself compiled into the following JavaScript:

```javascript
var el = document.createElement("div");
el.id="id";
el.className="class";
el.addEventListener("click", function() {console.log("click")});
el.appendChild(document.createElement("span"));
```

Attributes defined via a function (as opposed to literal maps) must be annotated with `^:attrs`. This allows for simpler generated code as a function in second place can denote either attributes or a child node.

```clojure
(ns …
  (:require [hipo :as hipo]))

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
  (:require [hipo :as hipo]))

(let [el (hipo/create [:div#id.class "content"])]
  (hipo/partially-compiled? el)) ; => false
```

### Reconciliation

A diff / patch style approach is used

### Type-Hinting

When you know the result of a function call will be converted to an HTML text node (as opposed to an HTML element) the `^:text` metadata can be used as a hint for the compiler to optimise the generated JavaScript code.

```clojure
(defn my-fn []
  (str "content"))

(hipo/create [:div ^:text (my-fn)])
```

## Credits

Initial code comes from the great [dommy](https://github.com/Prismatic/dommy) library which is now focused on DOM manipulation. The original dommy code is available as hipo [0.1.0](https://github.com/jeluard/hipo/tree/0.1.0).

## License

Copyright (C) 2013 Prismatic

Copyright (C) 2014 - 2015 Julien Eluard

Distributed under the Eclipse Public License, the same as Clojure.
