(ns hipo.attribute-test
  (:require #?(:clj [clojure.test :refer [deftest is]])
            #?(:cljs [cemerick.cljs.test :as test])
            [hipo.attribute :as attr])
  #?(:cljs (:require-macros [cemerick.cljs.test :refer [deftest is]])))

(deftest matches?
  (is (true? (attr/matches? "div" "div")))
  (is (true? (attr/matches? "div" #{"div"})))
  (is (false? (attr/matches? "div" nil)))
  (is (true? (attr/matches? nil "div"))))

(deftest handler
  (is (not (contains? (attr/handler {} nil "div" "class") :fn)))
  (is (contains? (attr/handler {:attribute-handlers [{:target {:tag "div" :attr "class"} :a ""}]} nil "div" "class") :a))
  (is (contains? (attr/handler {} "svg" nil "class") :fn))
  (is (contains? (attr/handler {} nil "input" "checked") :fn))
  (is (not (contains? (attr/handler {} nil "input" "name") :fn))))
