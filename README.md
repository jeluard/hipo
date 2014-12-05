# Hipo [![License](http://img.shields.io/badge/license-EPL-blue.svg?style=flat)](https://www.eclipse.org/legal/epl-v10.html) [![Build Status](http://img.shields.io/travis/jeluard/hipo.svg?style=flat)](http://travis-ci.org/#!/jeluard/hipo/builds) [![Dependency Status](https://www.versioneye.com/user/projects/545c247f287666dca9000049/badge.svg?style=flat)](https://www.versioneye.com/user/projects/545c247f287666dca9000049)

A ClojureScript DOM templating library based on [hiccup](https://github.com/weavejester/hiccup) syntax.

Hipo is available in clojars as `[hipo "0.2.0"]`.

## Usage

`create` macro converts an hiccup vector into a DOM node that can be directly inserted in a document.

```clojure
(ns …
  (:require [hipo :as hipo :include-macros true]))

(let [el (hipo/create [:div#id.class [:span]])]
  (.appendChild js/document.body el))
```

At compile-time JavaScript code is generated from the hiccup representation to minimize DOM node creation cost at the expense of code size.

The previous hiccup representation would be converted into the following ClojureScript:

```clojure
(let [el (. js/document createElement "div")]
  (set! (. el -id) "id")
  (set! (. el -className) "class")
  (. el appendChild (. js/document createElement "span"))
  el)
```

itself compiled into the following JavaScript:

```javascript
var el = document.createElement("div");
el.id="id";
el.className="class";
el.appendChild(document.createElement("span"));
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
  (:require [hipo :as hipo :include-macros true]))

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

## Credits

Initial code comes from the great [dommy](https://github.com/Prismatic/dommy) library which is now focused on DOM manipulation. The original dommy code is available as hipo [0.1.0](https://github.com/jeluard/hipo/tree/0.1.0).

## License

Copyright (C) 2013 Prismatic

Copyright (C) 2014 Julien Eluard

Distributed under the Eclipse Public License, the same as Clojure.
