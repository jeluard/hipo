(ns hipo.reconciliation-test
  (:require [cemerick.cljs.test :as test]
            [hipo.core :as hipo])
  (:require-macros [cemerick.cljs.test :refer [deftest is testing]]))

(deftest html
  (testing "Basic element"
    (let [hf (fn [m] [:div {:class (:class m) :title (:title m)}])
          [el f] (hipo/create (hf {:class "class" :title "title1"}))]
      (is (= (.-title el) "title1"))
      (is (= (.-className el) "class"))
      (f (hf {:title "title2"}))
      (is (= (.-title el) "title2"))
      (is (= (.-className el) "")))))

(deftest nils
  (let [hf (fn [b] [:div (if b [:div "1"]) [:div "2"] (if b [:div "3"])])
        [el f] (hipo/create (hf true))]
    (is (= "123" (.-textContent el)))
    (f (hf false))
    (is (= "2" (.-textContent el)))
    (f (hf true))
    (is (= "123" (.-textContent el)))))

(deftest children-as-text
  (let [hf (fn [b] [:div (if b [:div "content"] "")])
        [el f] (hipo/create (hf true))]
    (is (= "content" (.-textContent el)))
    (f (hf false))
    (is (= "" (.-textContent el)))
    (f (hf true))
    (is (= "content" (.-textContent el)))))

(deftest inputs
  (let [hf (fn [m] [:input {:checked (:checked? m) :type "checkbox"}])
        [el f] (hipo/create (hf {:checked? true}))]
    (is (nil? (.getAttribute el "checked")))
    (is (= (.-checked el) true))
    (f (hf {:checked? false}))
    (is (nil? (.getAttribute el "checked")))
    (is (= (.-checked el) false))))

(deftest svg
  (testing "Basic element"
   (let [hf (fn [m] [:svg {:class (:class m)}])
         [el f] (hipo/create (hf {:class "class1"}))]
     (is (= (.getAttribute el "class") "class1"))
     (f (hf {:class "class2"}))
     (is (= (.getAttribute el "class") "class2")))))

(deftest list-single-append
  (testing "List single append"
    (let [hf (fn [m] [:ul (for [i (:items m)] [:li i])])
          [el f] (hipo/create (hf {:items [1]}))]
      (let [children (.-childNodes el)]
        (is (= 1 (.-length children)))
        (let [c (.item children 0)]
          (is (= "1" (.-textContent c)))))
      (f (hf {:items [1 2]}))
      (let [children (.-childNodes el)]
        (is (= 2 (.-length children)))
        (let [c0 (.item children 0)
              c1 (.item children 1)]
          (is (= "1" (.-textContent c0)))
          (is (= "2" (.-textContent c1)))))
      (f (hf {:items [1 2 3]}))
      (let [children (.-childNodes el)]
        (is (= 3 (.-length children)))
        (let [c0 (.item children 0)
              c1 (.item children 1)
              c2 (.item children 2)]
          (is (= "1" (.-textContent c0)))
          (is (= "2" (.-textContent c1)))
          (is (= "3" (.-textContent c2)))))))
  (testing "List single prepend"
    (let [hf (fn [m] [:ul (for [i (:items m)] [:li i])])
          [el f] (hipo/create (hf {:items [1]}))]
      (let [children (.-childNodes el)]
        (is (= 1 (.-length children)))
        (let [c (.item children 0)]
          (is (= "1" (.-textContent c)))))
      (f (hf {:items [2 1]}))
      (let [children (.-childNodes el)]
        (is (= 2 (.-length children)))
        (let [c0 (.item children 0)
              c1 (.item children 1)]
          (is (= "2" (.-textContent c0)))
          (is (= "1" (.-textContent c1)))))
      (f (hf {:items [3 2 1]}))
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
    (let [hf (fn [m] [:ul (for [i (:items m)] [:li i])])
          [el f] (hipo/create (hf {:items [1]}))]
      (let [children (.-childNodes el)]
        (is (= 1 (.-length children)))
        (let [c (.item children 0)]
          (is (= "1" (.-textContent c)))))
      (f (hf {:items [1 2 3]}))
      (let [children (.-childNodes el)]
        (is (= 3 (.-length children)))
        (let [c0 (.item children 0)
              c1 (.item children 1)
              c2 (.item children 2)]
          (is (= "1" (.-textContent c0)))
          (is (= "2" (.-textContent c1)))
          (is (= "3" (.-textContent c2)))))))
  (testing "List multi prepend"
    (let [hf (fn [m] [:ul (for [i (:items m)] [:li i])])
          [el f] (hipo/create (hf {:items [1]}))]
      (let [children (.-childNodes el)]
        (is (= 1 (.-length children)))
        (let [c (.item children 0)]
          (is (= "1" (.-textContent c)))))
      (f (hf {:items [3 2 1]}))
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
    (let [hf (fn [b] (if b [:div ^{:hipo/key "a"} [:div 1] ^{:hipo/key "b"} [:div 2]]
                           [:div ^{:hipo/key "b"} [:div 2]]))
          [el f] (hipo/create (hf true))]
      (is (= 2 (.-childElementCount el)))
      (is (= "1"  (.. el -firstElementChild -textContent)))
      (f (hf false))
      (is (= 1 (.-childElementCount el)))
      (is (= "2" (.. el -firstElementChild -textContent)))
      (f (hf true))
      (is (= 2 (.-childElementCount el)))
      (is (= "1"  (.. el -firstElementChild -textContent)))))
  (testing "Change attributes"
    (let [hf (fn [b] [:div ^{:hipo/key "1"} {:id (if b "1" "2")}])
          [el f] (hipo/create (hf true))]
      (is (= "1"  (.-id el)))
      (f (hf false))
      (is (= "2"  (.-id el)))
      (f (hf true))
      (is (= "1"  (.-id el))))))

(deftest static-reconciliation
  (let [hf (fn [b] (if b [:div [:span [:div]]]  [:div ^:hipo/static [:div [:div]]]))
        [el f] (hipo/create (hf true))]
    (is (= "SPAN" (.-tagName (.-firstElementChild el))))
    (f (hf false) {:interceptors [(hipo.interceptor.StaticReconciliationInterceptor.)]})
    (is (= "SPAN" (.-tagName (.-firstElementChild el))))))

(deftest identity-reconciliation
  (let [a (atom nil)
        h [:div [:div [:div]]]
        [_ f] (hipo/create h)]
    (f h {:interceptors [(hipo.interceptor.StateInterceptor. a)]})
    (is (= 6 (count @a)))
    (reset! a nil)
    (f h {:interceptors [(hipo.interceptor.IdentityReconciliationInterceptor.) (hipo.interceptor.StateInterceptor. a)]})
    (is (zero? (count @a)))))

(deftest root-element
  (let [hf (fn [b] (if b [:div] [:span]))
        [_ f] (hipo/create (hf true))]
    (is (thrown? js/Error (f (hf false))))))