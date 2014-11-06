(defproject hipo "0.1.0-SNAPSHOT"
  :description "ClojureScript DOM templating based on hiccup syntax."
  :url "https://github.com/jeluard/hipo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :source-paths  ["src" "test"]
  :profiles
  {:dev
   {:dependencies [[org.clojure/clojurescript "0.0-2371"]]
    :plugins [[lein-cljsbuild "1.0.3"]
              [com.cemerick/clojurescript.test "0.3.1"]]}}
  :cljsbuild
  {:builds
   {:test {:source-paths ["src" "test"]
           :compiler {:output-to "target/unit-test.js"
                      :optimizations :whitespace
                      :pretty-print true}}}
   :test-commands {"unit" ["phantomjs" :runner "target/unit-test.js"]}}
  :aliases {"test" ["do" "clean" ["cljsbuild" "test"]]}
  :min-lein-version "2.5.0")
