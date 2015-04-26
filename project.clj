(defproject hipo "0.5.0-SNAPSHOT"
  :description "ClojureScript DOM templating based on hiccup syntax."
  :url "https://github.com/jeluard/hipo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-beta2"]]
  :profiles
  {:dev
   {:dependencies [[org.clojure/clojurescript "0.0-3211"]
                   [cljsjs/document-register-element "0.2.1-0"]
                   [org.clojure/test.check "0.7.0"]]
    :plugins [[lein-cljsbuild "1.0.5"]
              [com.cemerick/clojurescript.test "0.3.3"]]}}
  :cljsbuild
  {:builds
   {:test {:source-paths ["src" "test"]
           :compiler {:output-to "target/unit-test.js"
                      :optimizations :whitespace
                      :pretty-print true}}}
   :test-commands {"slimerjs" ["slimerjs" :runner "target/unit-test.js"]
                   "phantomjs" ["phantomjs" :runner "target/unit-test.js"]}}
  :aliases {"clean-test" ["do" "clean," "test," "cljsbuild" "test"]
            "clean-install" ["do" "clean," "install"]}
  :deploy-repositories {"clojars" {:sign-releases false :url "https://clojars.org/repo/"}}
  :min-lein-version "2.5.0")
