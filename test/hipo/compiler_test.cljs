(ns hipo.compiler-test
  (:require [cemerick.cljs.test :as test])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [hipo.compiler :refer [compile-create]]))

(deftest attrs
  (testing "Basic inline attributes"
    (let [el (compile-create [:div#id.class] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Basic attributes as static map"
    (let [el (compile-create [:div {:id "id" :class "class"}] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Mixed attributes 1"
    (let [el (compile-create [:div#id {:class "class"}] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Mixed attributes 2"
    (let [el (compile-create [:div.class {:id "id"}] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Mixed attributes 3"
    (let [el (compile-create [:div#id.class1 {:class "class2"}] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class1 class2"))))
  (testing "Complex inline attributes"
    (let [el (compile-create [:div#id.class1.class2] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class1 class2"))))
  (testing "Complex inline attributes 2"
    (let [el (compile-create [:div.class1.class2] nil)]
      (is (= (.-className el) "class1 class2"))))
  (testing "Redundant id attributes 1"
    (is (thrown? js/Error (compile-create [:div#id {:id "id"}] nil))))
  (testing "Redundant id attributes 2"
    (is (thrown? js/Error (compile-create [:div#id ^:attrs (assoc nil :id "id")] nil))))
  (testing "Mixed attributes defined as function"
    (let [el (compile-create [:div#id.class1 ^:attrs (assoc nil :class "class2")] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class1 class2"))))
  (testing "Non literal attributes"
    (let [el (compile-create [:div {:attr {:key "value"}}] nil)]
      (is (= nil (.getAttribute el "attr")))))
  (testing "Node key must be a string or a keyword"
    (let [n :div]
      (is (thrown? js/Error (compile-create [n] nil))))))
