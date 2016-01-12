(defproject hipo "0.5.3-SNAPSHOT"
  :description "ClojureScript DOM templating based on hiccup syntax."
  :url "https://github.com/jeluard/hipo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :profiles
  {:dev
   {:dependencies [[org.clojure/clojurescript "1.7.170"]
                   [cljsjs/document-register-element "0.5.3-1"]]
    :plugins [[lein-cljsbuild "1.1.2"]
              [lein-doo "0.1.6"]]}}
  :cljsbuild
  {:builds
   {:test {:source-paths ["src" "test"]
           :compiler {:output-to "target/unit-test.js"
                      :main 'hipo.runner
                      :optimizations :none}}}}
  :aliases {"clean-test" ["do" "clean," "test," "doo" "phantom" "test" "once"]
            "clean-install" ["do" "clean," "install"]}
  :deploy-repositories {"clojars" {:sign-releases false :url "https://clojars.org/repo/"}}
  :min-lein-version "2.5.0")
