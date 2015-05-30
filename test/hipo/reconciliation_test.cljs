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

(deftest nils
  (let [[el f] (hipo/create (fn [b] [:div (if b [:div "content"])]) true)]
    (is (= "content" (.-textContent el)))
    (f false)
    (is (= "" (.-textContent el)))
    (f true)
    (is (= "content" (.-textContent el)))))

(deftest children-as-text
  (let [[el f] (hipo/create (fn [b] [:div (if b [:div "content"] "")]) true)]
    (is (= "content" (.-textContent el)))
    (f false)
    (is (= "" (.-textContent el)))
    (f true)
    (is (= "content" (.-textContent el)))))

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

(deftest list-single-append
  (testing "List single append"
    (let [[el f] (hipo/create (fn [m] [:ul (for [i (:items m)] [:li i])]) {:items [1]})]
      (let [children (.-childNodes el)]
        (is (= 1 (.-length children)))
        (let [c (.item children 0)]
          (is (= "1" (.-textContent c)))))
      (f {:items [1 2]})
      (let [children (.-childNodes el)]
        (is (= 2 (.-length children)))
        (let [c0 (.item children 0)
              c1 (.item children 1)]
          (is (= "1" (.-textContent c0)))
          (is (= "2" (.-textContent c1)))))
      (f {:items [1 2 3]})
      (let [children (.-childNodes el)]
        (is (= 3 (.-length children)))
        (let [c0 (.item children 0)
              c1 (.item children 1)
              c2 (.item children 2)]
          (is (= "1" (.-textContent c0)))
          (is (= "2" (.-textContent c1)))
          (is (= "3" (.-textContent c2)))))))
  (testing "List single prepend"
    (let [[el f] (hipo/create (fn [m] [:ul (for [i (:items m)] [:li i])]) {:items [1]})]
      (let [children (.-childNodes el)]
        (is (= 1 (.-length children)))
        (let [c (.item children 0)]
          (is (= "1" (.-textContent c)))))
      (f {:items [2 1]})
      (let [children (.-childNodes el)]
        (is (= 2 (.-length children)))
        (let [c0 (.item children 0)
              c1 (.item children 1)]
          (is (= "2" (.-textContent c0)))
          (is (= "1" (.-textContent c1)))))
      (f {:items [3 2 1]})
      (let [children (.-childNodes el)]
        (is (= 3 (.-length children)))
        (let [c0 (.item children 0)
              c1 (.item children 1)
              c2 (.item children 2)]
          (is (= "3" (.-textContent c0)))
          (is (= "2" (.-textContent c1)))
          (is (= "1" (.-textContent c2))))))))

(deftest list-multi-append
  (testing "List multi append"
    (let [[el f] (hipo/create (fn [m] [:ul (for [i (:items m)] [:li i])]) {:items [1]})]
      (let [children (.-childNodes el)]
        (is (= 1 (.-length children)))
        (let [c (.item children 0)]
          (is (= "1" (.-textContent c)))))
      (f {:items [1 2 3]})
      (let [children (.-childNodes el)]
        (is (= 3 (.-length children)))
        (let [c0 (.item children 0)
              c1 (.item children 1)
              c2 (.item children 2)]
          (is (= "1" (.-textContent c0)))
          (is (= "2" (.-textContent c1)))
          (is (= "3" (.-textContent c2)))))))
  (testing "List multi prepend"
    (let [[el f] (hipo/create (fn [m] [:ul (for [i (:items m)] [:li i])]) {:items [1]})]
      (let [children (.-childNodes el)]
        (is (= 1 (.-length children)))
        (let [c (.item children 0)]
          (is (= "1" (.-textContent c)))))
      (f {:items [3 2 1]})
      (let [children (.-childNodes el)]
        (is (= 3 (.-length children)))
        (let [c0 (.item children 0)
              c1 (.item children 1)
              c2 (.item children 2)]
          (is (= "3" (.-textContent c0)))
          (is (= "2" (.-textContent c1)))
          (is (= "1" (.-textContent c2))))))))