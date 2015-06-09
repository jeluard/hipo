(ns hipo.hiccup-test
  (:require #?(:clj [clojure.test :refer [deftest is]])
            #?(:cljs [cemerick.cljs.test :as test])
            [hipo.hiccup :as hi])
  #?(:cljs (:require-macros [cemerick.cljs.test :refer [deftest is]])))

(deftest parse-tag-name
  (is (= "div" (hi/parse-tag-name "div")))
  (is (= "div" (hi/parse-tag-name "div#id")))
  (is (= "div" (hi/parse-tag-name "div.class1.class2")))
  (is (= "div" (hi/parse-tag-name "div#id.class1.class2"))))

(deftest parse-id
  (is (= nil (hi/parse-id "div")))
  (is (= "id" (hi/parse-id "div#id")))
  (is (= nil (hi/parse-id"div.class1.class2")))
  (is (= "id" (hi/parse-id "div#id.class1.class2"))))

(deftest parse-classes
  (is (= nil (hi/parse-classes "div")))
  (is (= nil (hi/parse-classes "div#id")))
  (is (= "class1 class2 class3" (hi/parse-classes "div.class1.class2.class3")))
  (is (= "class1 class2 class3" (hi/parse-classes "div#id.class1.class2.class3"))))

(deftest children
  (is (= nil (hi/children [:div])))
  (is (= nil (hi/children [:div {}])))
  (is (= [[:span]] (hi/children [:div [:span]])))
  (is (= [[:span]] (hi/children [:div {} [:span]]))))

(deftest flattened?
  (is (true? (hi/flattened? nil)))
  (is (true? (hi/flattened? [])))
  (is (true? (hi/flattened? ["content"])))
  (is (true? (hi/flattened? [nil "content" nil])))
  (is (true? (hi/flattened? [[:div] [:div]])))
  (is (false? (hi/flattened? [(list [:div])]))))

(deftest flatten-children
  (is (= ["content"] (hi/flatten-children ["content"])))
  (is (= [[:div]] (hi/flatten-children [[:div]])))
  (is (= [[:div] "content"] (hi/flatten-children [[:div] "content"])))
  (is (= [nil "content" nil] (hi/flatten-children [nil "content" nil])))
  (is (= [[:div] [:span]] (hi/flatten-children [[:div] '([:span])])))
  (is (= [[:div]] (hi/flatten-children [[:div] '()]))))

(deftest listener-name
  (is (true? (hi/listener-name? "on-listener")))
  (is (false? (hi/listener-name? "listener"))))

(deftest listener-name
  (is (= "listener" (hi/listener-name->event-name "on-listener"))))