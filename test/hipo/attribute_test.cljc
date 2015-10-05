(ns hipo.attribute-test
  (:require #?(:clj [clojure.test :refer [deftest is]])
            #?(:cljs [cljs.test :as test])
            [hipo.attribute :as attr])
  #?(:cljs (:require-macros [cljs.test :refer [deftest is]])))

(deftest matches?
  (is (true? (attr/matches? nil nil)))
  (is (true? (attr/matches? "div" "div")))
  (is (true? (attr/matches? #{"div"} "div")))
  (is (true? (attr/matches? nil "div")))
  (is (false? (attr/matches? "div" nil))))

(deftest target-matches?
  (is (true? (attr/target-matches? {:tag "div" :attr "style"} nil "div" "style")))
  (is (true? (attr/target-matches? {:attr "style"} nil "div" "style"))))

(deftest handler
  (is (nil? (attr/handler {} nil "div" "class")))
  (is (not (contains? (attr/handler {} nil "div" "class") :fn)))
  (is (contains? (attr/handler {:attribute-handlers [{:target {:tag "div" :attr "class"} :a ""}]} nil "div" "class") :a))
  (is (contains? (attr/handler {:attribute-handlers [{:target {:attr "class"} :a ""}]} nil "div" "class") :a))
  (is (contains? (attr/handler {} "svg" nil "class") :fn))
  (is (contains? (attr/handler {} nil "input" "checked") :fn))
  (is (not (contains? (attr/handler {} nil "input" "name") :fn))))