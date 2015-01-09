(defproject hipo "0.5.0-SNAPSHOT"
  :description "ClojureScript DOM templating based on hiccup syntax."
  :url "https://github.com/jeluard/hipo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  ;:source-paths  ["src" "test"]
  :profiles
  {:dev
   {:dependencies [[org.clojure/clojurescript "0.0-2665"]]
    :plugins [[lein-cljsbuild "1.0.4"]
              [com.cemerick/clojurescript.test "0.3.3"]]}}
  :cljsbuild
  {:builds
   {:test {:source-paths ["src" "test"]
           :compiler {:output-to "target/unit-test.js"
                      :optimizations :whitespace
                      :pretty-print true}}}
   :test-commands {"unit" ["slimerjs" :runner "dev-resources/document-register-element-0.1.2.js" "target/unit-test.js"]}}
  :aliases {"clean-test" ["do" "clean," "cljsbuild" "test"]
            "clean-install" ["do" "clean," "install"]}
  :deploy-repositories {"clojars" {:sign-releases false :url "https://clojars.org/repo/"}}
  :min-lein-version "2.5.0")
