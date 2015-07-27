(ns hipo.compiler-test
  (:require [cljs.test :as test])
  (:require-macros [cljs.test :refer [deftest is testing]]
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
    (let [el (compile-create [:div {:attr {:hipo/key "value"}}] nil)]
      (is (= nil (.getAttribute el "attr")))))
  (testing "Node key must be a string or a keyword"
    (let [n :div]
      (is (thrown? js/Error (compile-create [n] nil)))))
  (testing "Attribute starting with on- are listeners"
    (let [el (compile-create [:div {:on-click #()}] nil)]
      (is (nil?(.-onclick el)))))
  (testing "Listeners can be provided as map"
    (let [el (compile-create [:div {:on-click {:name "click" :fn #()}}] nil)]
      (is (nil?(.-onclick el))))))

(deftest compile-form
  (let [e (compile-create [:ul (for [i (range 5)] [:li "content" ^:text (str i)])] {:force-compilation? true})]
    (is (= 5 (.. e -childNodes -length)))
    (is (= "content0" (.. e -firstChild -textContent))))
  (let [e (compile-create [:div (if true [:div])] {:force-compilation? true})]
    (is (= 1 (.. e -childNodes -length))))
  (let [e (compile-create [:div (if true nil [:div])] {:force-compilation? true})]
    (is (= 0 (.. e -childNodes -length))))
  (let [e (compile-create [:div (if false [:div])] {:force-compilation? true})]
    (is (= 0 (.. e -childNodes -length))))
  (let [e (compile-create [:div (if false [:div] nil)] {:force-compilation? true})]
    (is (= 0 (.. e -childNodes -length))))
  (let [e (compile-create [:div (when true [:div])] {:force-compilation? true})]
    (is (= 1 (.. e -childNodes -length))))
  (let [e (compile-create [:div (when false [:div])] {:force-compilation? true})]
    (is (= 0 (.. e -childNodes -length))))
  (let [e (compile-create [:div (list [:div "1"] [:div "2"])] {:force-compilation? true})]
    (is (= 2 (.. e -childNodes -length))))
  (is (thrown? js/Error (compile-create [:div (conj [] :div)] {:force-compilation? true}))))

(def my-str str)

(deftest hints
  (is (not (nil? (compile-create [:div (+ 1 2)] {:force-compilation? true}))))
  (is (not (nil? (compile-create [:div (not true)] {:force-compilation? true}))))
  (is (thrown? js/Error (compile-create [:div (my-str "content")] {:force-compilation? true})))
  (is (not (nil? (compile-create [:div ^:text (my-str "content")] {:force-compilation? true})))))
