(ns hipo.reconciliation-test
  (:require [cljs.test :as test]
            [hipo.core :as hipo]
            [hipo.interceptor :refer [Interceptor]])
  (:require-macros [cljs.test :refer [deftest is testing]]))

(deftest html
  (testing "Basic element"
    (let [hf (fn [m] [:div {:class (:class m) :title (:title m)}])
          el (hipo/create (hf {:class "class" :title "title1"}))]
      (is (= (.-title el) "title1"))
      (is (= (.-className el) "class"))
      (hipo/reconciliate! el (hf {:title "title2"}))
      (is (= (.-title el) "title2"))
      (is (= (.-className el) "")))))

#_
(deftest nill
  (let [el (.createElement js/document "div")]
    (hipo/reconciliate! el [:div {:id "id"}])))

(deftest nils
  (let [hf (fn [b] [:div (if b [:div "1"]) [:div "2"] (if b [:div "3"])])
        el (hipo/create (hf true))]
    (is (= "123" (.-textContent el)))
    (hipo/reconciliate! el (hf false))
    (is (= "2" (.-textContent el)))
    (hipo/reconciliate! el (hf true))
    (is (= "123" (.-textContent el)))))

(deftest children-as-text
  (let [hf (fn [b] [:div (if b [:div "content"] "")])
        el (hipo/create (hf true))]
    (is (= "content" (.-textContent el)))
    (hipo/reconciliate! el (hf false))
    (is (= "" (.-textContent el)))
    (hipo/reconciliate! el (hf true))
    (is (= "content" (.-textContent el)))))

(deftest inputs
  (let [hf (fn [m] [:input {:checked (:checked? m) :type "checkbox"}])
        el (hipo/create (hf {:checked? true}))]
    (is (nil? (.getAttribute el "checked")))
    (is (= (.-checked el) true))
    (hipo/reconciliate! el (hf {:checked? false}))
    (is (nil? (.getAttribute el "checked")))
    (is (= (.-checked el) false))))

(deftest svg
  (testing "Basic element"
   (let [hf (fn [m] [:svg {:class (:class m)}])
         el (hipo/create (hf {:class "class1"}))]
     (is (= (.getAttribute el "class") "class1"))
     (hipo/reconciliate! el (hf {:class "class2"}))
     (is (= (.getAttribute el "class") "class2")))))

(deftest list-single-append
  (testing "List single append"
    (let [hf (fn [m] [:ul (for [i (:items m)] [:li i])])
          el (hipo/create (hf {:items [1]}))]
      (let [children (.-childNodes el)]
        (is (= 1 (.-length children)))
        (let [c (.item children 0)]
          (is (= "1" (.-textContent c)))))
      (hipo/reconciliate! el (hf {:items [1 2]}))
      (let [children (.-childNodes el)]
        (is (= 2 (.-length children)))
        (let [c0 (.item children 0)
              c1 (.item children 1)]
          (is (= "1" (.-textContent c0)))
          (is (= "2" (.-textContent c1)))))
      (hipo/reconciliate! el (hf {:items [1 2 3]}))
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
          el (hipo/create (hf {:items [1]}))]
      (let [children (.-childNodes el)]
        (is (= 1 (.-length children)))
        (let [c (.item children 0)]
          (is (= "1" (.-textContent c)))))
      (hipo/reconciliate! el (hf {:items [2 1]}))
      (let [children (.-childNodes el)]
        (is (= 2 (.-length children)))
        (let [c0 (.item children 0)
              c1 (.item children 1)]
          (is (= "2" (.-textContent c0)))
          (is (= "1" (.-textContent c1)))))
      (hipo/reconciliate! el (hf {:items [3 2 1]}))
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
          el (hipo/create (hf {:items [1]}))]
      (let [children (.-childNodes el)]
        (is (= 1 (.-length children)))
        (let [c (.item children 0)]
          (is (= "1" (.-textContent c)))))
      (hipo/reconciliate! el (hf {:items [1 2 3]}))
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
          el (hipo/create (hf {:items [1]}))]
      (let [children (.-childNodes el)]
        (is (= 1 (.-length children)))
        (let [c (.item children 0)]
          (is (= "1" (.-textContent c)))))
      (hipo/reconciliate! el (hf {:items [3 2 1]}))
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
          el (hipo/create (hf true))]
      (is (= 2 (.-childElementCount el)))
      (is (= "1"  (.. el -firstElementChild -textContent)))
      (hipo/reconciliate! el (hf false))
      (is (= 1 (.-childElementCount el)))
      (is (= "2" (.. el -firstElementChild -textContent)))
      (hipo/reconciliate! el (hf true))
      (is (= 2 (.-childElementCount el)))
      (is (= "1"  (.. el -firstElementChild -textContent)))))
  (testing "Change attributes"
    (let [hf (fn [b] [:div ^{:hipo/key "1"} {:id (if b "1" "2")}])
          el (hipo/create (hf true))]
      (is (= "1"  (.-id el)))
      (hipo/reconciliate! el (hf false))
      (is (= "2"  (.-id el)))
      (hipo/reconciliate! el (hf true))
      (is (= "1"  (.-id el))))))

(deftest static-reconciliation
  (let [hf (fn [b] (if b [:div [:span [:div]]]  [:div ^:hipo/static [:div [:div]]]))
        el (hipo/create (hf true))]
    (is (= "SPAN" (.-tagName (.-firstElementChild el))))
    (hipo/reconciliate! el (hf false) {:interceptors [(hipo.interceptor.StaticReconciliationInterceptor.)]})
    (is (= "SPAN" (.-tagName (.-firstElementChild el))))))

(deftest identity-reconciliation
  (let [a (atom nil)
        h [:div [:div [:div]]]
        el (hipo/create h)]
    (hipo/reconciliate! el h {:interceptors [(hipo.interceptor.StateInterceptor. a)]})
    (is (= 6 (count @a)))
    (reset! a nil)
    (hipo/reconciliate! el h {:interceptors [(hipo.interceptor.IdentityReconciliationInterceptor.) (hipo.interceptor.StateInterceptor. a)]})
    (is (zero? (count @a)))))

(deftest root-element
  (let [hf (fn [b] (if b [:div] [:span]))
        el (hipo/create (hf true))]
    (is (thrown? js/Error (hipo/reconciliate! el (hf false))))))

(deftest update-simple
  (let [hf (fn [m] [:div {:id (:id m)} (:content m)])
        el (hipo/create (hf {:id "id1" :content "a"}))]
    (hipo/reconciliate! el (hf {:id "id2" :content "b"}))

    (is (= "b" (.-textContent el)))
    (is (= "id2" (.-id el)))))

(if (exists? js/MutationObserver)
  (deftest update-nested
    (let [c1 [:div {:class "class1" :attr1 "1"} [:span "content1"] [:span]]
          c2 [:div {:attr1 nil :attr2 nil} [:span]]
          c3 [:div]
          c4 [:div {:class "class2" :attr2 "2"} [:span] [:div "content2"]]
          el (hipo/create c1)
          o (js/MutationObserver. identity)]
      (.observe o el #js {:attributes true :childList true :characterData true :subtree true})

      (is "div" (.-localName el))
      (is (= 2 (.-childElementCount el)))

      (hipo/reconciliate! el c1)

      (is (= 0 (count (array-seq (.takeRecords o)))))

      (hipo/reconciliate! el c2)

      (is (not (.hasAttribute el "class")))
      (is (not (.hasAttribute el "attr1")))
      (is (not (.hasAttribute el "attr2")))
      (is (= 1 (.-childElementCount el)))

      (let [v (array-seq (.takeRecords o))]
        (is (= 4 (count v)))
        (is (= "childList" (.-type (first v))))
        (is (= "childList" (.-type (second v))))
        (is (= "attributes" (.-type (nth v 2))))
        (is (= "attr1" (.-attributeName (nth v 2))))
        (is (= "attributes" (.-type (nth v 3))))
        (is (= "class" (.-attributeName (nth v 3)))))

      (hipo/reconciliate! el c3)

      (is (= 0 (.-childElementCount el)))

      (let [v (array-seq (.takeRecords o))]
        (is (= 1 (count v)))
        (is (= "childList" (.-type (first v)))))

      (hipo/reconciliate! el c4)

      (is "div" (.-localName el))
      (is (= "class2" (.getAttribute el "class")))
      (is (not (.hasAttribute el "attr1")))
      (is (= "2" (.getAttribute el "attr2")))
      (is (= 2 (.-childElementCount el)))
      (let [c (.-firstChild el)]
        (is (= "span" (.-localName c))))
      (let [c (.. el -firstChild -nextElementSibling)]
        (is (= "div" (.-localName c)))
        (is (= "content2" (.-textContent c))))

      (let [v (array-seq (.takeRecords o))]
        (is (= 3 (count v)))
        (is (= "childList" (.-type (first v))))
        (is (= "attributes" (.-type (second v))))
        (is (= "class" (.-attributeName (second v))))
        (is (= "attributes" (.-type (nth v 2))))
        (is (= "attr2" (.-attributeName (nth v 2)))))

      (.disconnect o))))

(defn fire-click-event
  [el]
  (let [ev (.createEvent js/document "HTMLEvents")]
    (.initEvent ev "click" true true)
    (.dispatchEvent el ev)))

(deftest update-listener
  (let [a (atom 0)
        hf (fn [b] [:div (if b {:on-click #(swap! a inc)})])
        el (hipo/create (hf true))]
    (fire-click-event el)
    (hipo/reconciliate! el (hf false))
    (fire-click-event el)

    (is (= 1 @a))
    (hipo/reconciliate! el (hf true))
    (fire-click-event el)
    (is (= 2 @a))))

(deftest update-listener-as-map
  (let [a (atom 0)
        hf (fn [m] [:div ^:attrs (if m {:on-click {:name "click" :fn #(when-let [f (:fn m)] (swap! a (fn [evt] (f evt))))}})])
        el (hipo/create (hf {:fn #(inc %)}))]
    (fire-click-event el)
    (is (= 1 @a))
    (hipo/reconciliate! el (hf nil))
    (fire-click-event el)

    (is (= 1 @a))
    (hipo/reconciliate! el (hf {:fn #(dec %)}))
    (fire-click-event el)
    (is (= 0 @a))))

(deftest update-keyed
  (let [hf (fn [r] [:ul (for [i r] ^{:hipo/key i} [:li {:class i} i])])
        el (hipo/create (hf (range 6)))]
    (hipo/reconciliate! el (hf (reverse (range 6))))

    (is (= 6 (.. el -childNodes -length)))
    (is (= "5" (.. el -firstChild -textContent)))
    (is (= "5" (.. el -firstChild -className)))
    (is (= "4" (.. el -firstChild -nextSibling -textContent)))
    (is (= "3" (.. el -firstChild -nextSibling -nextSibling -textContent)))
    (is (= "2" (.. el -firstChild -nextSibling -nextSibling -nextSibling -textContent)))
    (is (= "1" (.. el -firstChild -nextSibling -nextSibling -nextSibling -nextSibling -textContent)))
    (is (= "0" (.. el -lastChild -textContent)))))

(deftest update-keyed-sparse
  (let [hf (fn [r] [:ul (for [i r] ^{:hipo/key i} [:li {:class i} i])])
        el (hipo/create (hf (range 6)))]
    (hipo/reconciliate! el (hf (cons 7 (filter odd? (reverse (range 6))))))

    (is (= 4 (.. el -childNodes -length)))
    (is (= "7" (.. el -firstChild -textContent)))
    (is (= "7" (.. el -firstChild -className)))
    (is (= "5" (.. el -firstChild -nextSibling -textContent)))
    (is (= "3" (.. el -firstChild -nextSibling -nextSibling -textContent)))
    (is (= "1" (.. el -firstChild -nextSibling -nextSibling -nextSibling -textContent)))))

(deftest update-state
  (let [m1 {:children (range 6)}
        m2 {:children (cons 7 (filter odd? (reverse (range 6))))}
        hf (fn [m]
             [:ul (for [i (:children m)]
                    ^{:hipo/key i} [:li {:class i} i])])
        el (hipo/create (hf m1))]
    (hipo/reconciliate! el (hf m2) m2)

    (is (= 4 (.. el -childNodes -length)))
    (is (= "7" (.. el -firstChild -textContent)))
    (is (= "7" (.. el -firstChild -className)))
    (is (= "5" (.. el -firstChild -nextSibling -textContent)))
    (is (= "3" (.. el -firstChild -nextSibling -nextSibling -textContent)))
    (is (= "1" (.. el -firstChild -nextSibling -nextSibling -nextSibling -textContent)))))

(deftype MyInterceptor []
  Interceptor
  (-intercept [_ t _ f]
    ; let update be performed but reject all others
    (if (= :update t)
      (f))))

(deftype MyOtherInterceptor []
  Interceptor
  (-intercept [_ _ _ f]
    ; let update be performed but reject all others
    (f)))

(deftest interceptor
  (let [hf (fn [m] [:div {:class (:value m)}])
        el (hipo/create (hf {:value 1}))]
    (hipo/reconciliate! el (hf {:value 2}) {:interceptors [(MyOtherInterceptor.)]})
    (is (= "2" (.-className el)))
    (hipo/reconciliate! el (hf {:value 3}) {:interceptors [(MyInterceptor.)]})
    (is (= "2" (.-className el)))
    (hipo/reconciliate! el (hf {:value 3}) {:interceptors [(MyInterceptor.) (MyOtherInterceptor.)]})
    (is (= "2" (.-className el)))
    (hipo/reconciliate! el (hf {:value 4}) {:interceptors [(MyOtherInterceptor.) (MyInterceptor.)]})
    (is (= "2" (.-className el)))))
