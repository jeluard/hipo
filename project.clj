(defproject hipo "0.5.1"
  :description "ClojureScript DOM templating based on hiccup syntax."
  :url "https://github.com/jeluard/hipo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :profiles
  {:dev
   {:dependencies [[org.clojure/clojurescript "0.0-3308"]
                   [cljsjs/document-register-element "0.4.3-0"]
                   [org.clojure/test.check "0.7.0"]]
    :plugins [[lein-cljsbuild "1.0.5"]
              [lein-doo "0.1.4-SNAPSHOT"]]}}
  :cljsbuild
  {:builds
   {:test {:source-paths ["src" "test"]
           :compiler {:output-to "target/unit-test.js"
                      :main 'hipo.runner
                      :optimizations :whitespace
                      :pretty-print true}}}}
  :aliases {"clean-test" ["do" "clean," "test," "doo" "phantom" "test" "once"]
            "clean-install" ["do" "clean," "install"]}
  :deploy-repositories {"clojars" {:sign-releases false :url "https://clojars.org/repo/"}}
  :min-lein-version "2.5.0")
