(ns hipo.interpreter-test
  (:require [cemerick.cljs.test :as test]
            [hipo.interpreter :as hi])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]))

(deftest attrs
  (testing "Basic inline attributes"
    (let [el (hi/create [:div#id.class])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Basic attributes as static map"
    (let [el (hi/create [:div {:id "id" :class "class"}])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Mixed attributes 1"
    (let [el (hi/create [:div#id {:class "class"}])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Mixed attributes 2"
    (let [el (hi/create [:div.class {:id "id"}])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Mixed attributes 3"
    (let [el (hi/create [:div#id.class1 {:class "class2"}])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class1 class2"))))
  (testing "Complex inline attributes"
    (let [el (hi/create [:div#id.class1.class2])]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class1 class2"))))
  (testing "Complex inline attributes 2"
    (let [el (hi/create [:div.class1.class2])]
      (is (= (.-className el) "class1 class2"))))
  (testing "Redundant id attributes"
    (is (thrown? js/Error (hi/create [:div#id {:id "id"}])))))
