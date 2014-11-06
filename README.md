A ClojureScript DOM templating library based on hiccup syntax.

## Usage

Add the following dependency to your `project.clj`:

```clojure
[hipo "0.1.0"]
```

### Templating

Templating syntax is based on [Hiccup](https://github.com/weavejester/hiccup/), a great HTML library for Clojure. Instead of returning a string of html, hipo's `node` macro returns a DOM node.

```clojure
(ns …
  (:require [hipo.core])
  (:use-macros
    [hipo.macros :only [node]]))

(node
  [:div#id.class1
    (for [r (range 2)]
      [:span.text (str "word" r)])]) ;; => [object HTMLElement]

;; Styles can be inlined as a map
(node
  [:span
    {:style
      {:color "#aaa"
       :text-decoration "line-through"}}])
```

The `deftemplate` macro is useful syntactic sugar for defining a function that returns a DOM node.

```clojure
(ns …
  (:require [hipo.core])
  (:use-macros
    [hipo.macros :only [node deftemplate]]))

(defn simple-template [cat]
  (node [:img {:src cat}]))

(deftemplate simple-template [cat]
  [:img {:src cat}])
```

Thanks to [@ibdknox](https://github.com/ibdknox/), you can define view logic for custom types by implementing the `PElement` protocol:

```clojure
(defrecord MyModel [data]
   hipo.template/PElement
   (-elem [this] (node [:p (str "My data " data)])))

(hipo/append! (sel1 :body) (MyModel. "is big"))
```

### Type-Hinting Template Macros

One caveat of using the compile-macro is that if you have a compound element (a vector element) and want to have a non-literal map as the attributes (the second element of the vector), then you need to use <code>^:attrs</code> meta-data so the compiler knows to process this symbol as a map of attributes in the runtime system. Here's an example:

```clojure
(node [:a ^:attrs (merge m1 m2)])
```

## Testing

For all pull requests, please ensure your tests pass (or add test cases) before submitting. 

    $ lein test

## License

Copyright (C) 2013 Prismatic
Copyright (C) 2014 Julien Eluard

Distributed under the Eclipse Public License, the same as Clojure.
