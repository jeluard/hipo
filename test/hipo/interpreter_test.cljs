(ns hipo.interpreter-test
  (:require [cljs.test :as test]
            [hipo.interpreter :as hi])
  (:require-macros [cljs.test :refer [deftest is testing]]))

(deftest attrs
  (testing "Basic inline attributes"
    (let [el (hi/create [:div#id.class] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Basic attributes as static map"
    (let [el (hi/create [:div {:id "id" :class "class"}] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Mixed attributes 1"
    (let [el (hi/create [:div#id {:class "class"}] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Mixed attributes 2"
    (let [el (hi/create [:div.class {:id "id"}] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class"))))
  (testing "Mixed attributes 3"
    (let [el (hi/create [:div#id.class1 {:class "class2"}] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class1 class2"))))
  (testing "Complex inline attributes"
    (let [el (hi/create [:div#id.class1.class2] nil)]
      (is (= (.-id el) "id"))
      (is (= (.-className el) "class1 class2"))))
  (testing "Complex inline attributes 2"
    (let [el (hi/create [:div.class1.class2] nil)]
      (is (= (.-className el) "class1 class2"))))
  (testing "Redundant id attributes"
    (is (thrown? js/Error (hi/create [:div#id {:id "id"}] nil))))
  (testing "Non literal attributes"
    (let [el (hi/create [:div {:attr {:key "value"}}] nil)]
      (is (= nil (.getAttribute el "attr")))))
  (testing "Attribute starting with on- are listeners"
    (let [el (hi/create [:div {:on-click #()}] nil)]
      (is (nil?(.-onclick el)))))
  (testing "Listeners can be provided as map"
    (let [el (hi/create [:div {:on-click {:name "click" :fn #()}}] nil)]
      (is (nil?(.-onclick el))))))
