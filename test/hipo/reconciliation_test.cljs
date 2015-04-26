(ns hipo.reconciliation-test
  (:require [cemerick.cljs.test :as test]
            [hipo.core :as hipo])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]))

(deftest html
  (testing "Basic element"
    (let [[el f] (hipo/create (fn [m] [:div {:class (:class m) :title (:title m)}]) {:class "class" :title "title1"})]
      (is (= (.-title el) "title1"))
      (is (= (.-className el) "class"))
      (f {:title "title2"})
      (is (= (.-title el) "title2"))
      (is (= (.-className el) "")))))

(deftest inputs
  (let [[el f] (hipo/create (fn [m] [:input {:checked (:checked? m) :type "checkbox"}]) {:checked? true})]
    (is (nil? (.getAttribute el "checked")))
    (is (= (.-checked el) true))
    (f {:checked? false})
    (is (nil? (.getAttribute el "checked")))
    (is (= (.-checked el) false))))

(deftest svg
  (testing "Basic element"
   (let [[el f] (hipo/create (fn [m] [:svg {:class (:class m)}]) {:class "class1"})]
     (is (= (.getAttribute el "class") "class1"))
     (f {:class "class2"})
     (is (= (.getAttribute el "class") "class2")))))