(ns hipo.compiler-test
  (:require [cemerick.cljs.test :as test])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]
                   [hipo.compiler :refer [compile-create]]))

(deftest attrs
  (testing "Basic inline attributes"
    (let [el (compile-create [:div#id.class])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Basic attributes as static map"
    (let [el (compile-create [:div {:id "id" :class "class"}])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Mixed attributes 1"
    (let [el (compile-create [:div#id {:class "class"}])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Mixed attributes 2"
    (let [el (compile-create [:div.class {:id "id"}])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Mixed attributes 3"
    (let [el (compile-create [:div#id.class1 {:class "class2"}])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class1 class2"))))
  (testing "Complex inline attributes"
    (let [el (compile-create [:div#id.class1.class2])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class1 class2"))))
  (testing "Complex inline attributes 2"
    (let [el (compile-create [:div.class1.class2])]
      (is (= (.-className el) "class1 class2"))))
  (testing "Redundant id attributes 1"
    (is (thrown? js/Error (compile-create [:div#id {:id "id"}]))))
  (testing "Redundant id attributes 2"
    (is (thrown? js/Error (compile-create [:div#id ^:attrs (assoc nil :id "id")]))))
  (testing "Mixed attributes defined as function"
    (let [el (compile-create [:div#id.class1 ^:attrs (assoc nil :class "class2")])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class1 class2")))))
