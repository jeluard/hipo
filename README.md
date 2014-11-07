# Hipo [![License](http://img.shields.io/badge/license-EPL-blue.svg?style=flat)](https://www.eclipse.org/legal/epl-v10.html) [![Build Status](http://img.shields.io/travis/jeluard/hipo.svg?style=flat)](http://travis-ci.org/#!/jeluard/hipo/builds) [![Dependency Status](https://www.versioneye.com/user/projects/545c247f287666dca9000049/badge.svg?style=flat)](https://www.versioneye.com/user/projects/545c247f287666dca9000049)

A ClojureScript DOM templating library based on [hiccup](https://github.com/weavejester/hiccup) syntax.

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

### Type-Hinting Template Macros

One caveat of using the compile-macro is that if you have a compound element (a vector element) and want to have a non-literal map as the attributes (the second element of the vector), then you need to use <code>^:attrs</code> meta-data so the compiler knows to process this symbol as a map of attributes in the runtime system. Here's an example:

```clojure
(node [:a ^:attrs (merge m1 m2)])
```

## Testing

For all pull requests, please ensure your tests pass (or add test cases) before submitting. 

    $ lein test

## Credits

Initial code comes from the great [dommy](https://github.com/Prismatic/dommy) library which is now focused on DOM manipulation.

## License

Copyright (C) 2013 Prismatic

Copyright (C) 2014 Julien Eluard

Distributed under the Eclipse Public License, the same as Clojure.
