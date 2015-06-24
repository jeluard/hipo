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
  (let [[el f] (hipo/create (fn [b] [:div (if b [:div "1"]) [:div "2"] (if b [:div "3"])]) true)]
    (is (= "123" (.-textContent el)))
    (f false)
    (is (= "2" (.-textContent el)))
    (f true)
    (is (= "123" (.-textContent el)))))

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

(deftest keyed
  (testing "Keep one element"
    (let [[el f] (hipo/create (fn [b] (if b [:div ^{:hipo/key "a"} [:div 1] ^{:hipo/key "b"} [:div 2]]
                                            [:div ^{:hipo/key "b"} [:div 2]])) true)]
      (is (= 2 (.-childElementCount el)))
      (is (= "1"  (.. el -firstElementChild -textContent)))
      (f false)
      (is (= 1 (.-childElementCount el)))
      (is (= "2" (.. el -firstElementChild -textContent)))
      (f true)
      (is (= 2 (.-childElementCount el)))
      (is (= "1"  (.. el -firstElementChild -textContent)))))
  (testing "Change attributes"
    (let [[el f] (hipo/create (fn [b] [:div ^{:hipo/key "1"} {:id (if b "1" "2")}]) true)]
      (is (= "1"  (.-id el)))
      (f false)
      (is (= "2"  (.-id el)))
      (f true)
      (is (= "1"  (.-id el))))))

(deftest static
  (let [[el f] (hipo/create (fn [b] (if b [:div [:span [:div]]]  [:div ^:hipo/static [:div [:div]]])) true)]
    (is (= "SPAN" (.-tagName (.-firstElementChild el))))
    (f false {:interceptors [(hipo.interceptor.StaticReconciliationInterceptor.)]})
    (is (= "SPAN" (.-tagName (.-firstElementChild el))))))

(deftest root-element
  (let [[_ f] (hipo/create (fn [b] (if b [:div] [:span])) true)]
    (is (thrown? js/Error (f false)))))