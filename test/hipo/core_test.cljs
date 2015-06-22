(ns hipo.core-test
  (:require [cemerick.cljs.test :as test]
            [hipo.core :as hipo]
            [hipo.interceptor :refer [Interceptor]]
            cljsjs.document-register-element)
  (:require-macros [cemerick.cljs.test :refer [deftest is]]))

(deftest simple
  (is (= "B" (.-tagName (hipo/create-static [:b]))))
  (let [e (hipo/create-static [:span "some text"])]
    (is (= "SPAN" (.-tagName e)))
    (is (= "some text" (.-textContent e)))
    (is (= js/document.TEXT_NODE (-> e .-childNodes (aget 0) .-nodeType)))
    (is (zero? (-> e .-children .-length))))
  (let [e (hipo/create-static [:script {:src "http://somelink"}])]
    (is (= "" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "src"))))
  (let [e (hipo/create-static [:a {:href "http://somelink"} "anchor"])]
    (is (-> e .-tagName (= "A")))
    (is (= "anchor" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "href"))))
  (let [a (atom 0)
        next-id #(swap! a inc)
        e (hipo/create-static [:span {:attr (next-id)}])]
    (is (= "1" (.getAttribute e "attr"))))
  (let [e (hipo/create-static [:div#id {:class "class1 class2"}])]
    (is (= "class1 class2" (.-className e))))
  (let [e (hipo/create-static [:div#id.class1 {:class "class2 class3"}])]
    (is (= "class1 class2 class3" (.-className e))))
  (let [cs "class1 class2"
        e (hipo/create-static [:div ^:attrs (merge {} {:class cs})])]
    (is (= "class1 class2" (.-className e))))
  (let [cs "class2 class3"
        e (hipo/create-static [:div (list [:div#id.class1 {:class cs}])])]
    (is (= "class1 class2 class3" (.-className (.-firstChild e)))))
  (let [e (hipo/create-static [:div.class1 ^:attrs (merge {:data-attr ""} {:class "class2 class3"})])]
    (is (= "class1 class2 class3" (.-className e))))
  (let [e (hipo/create-static [:div (interpose [:br] (repeat 3 "test"))])]
    (is (= 5 (.. e -childNodes -length)))
    (is (= "test" (.. e -firstChild -textContent))))
  (let [e (hipo/create-static [:div.class1 [:span#id1 "span1"] [:span#id2 "span2"]])]
    (is (= "span1span2" (.-textContent e)))
    (is (= "class1" (.-className e)))
    (is (= 2 (-> e .-childNodes .-length)))
    (is (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>"
           (.-innerHTML e)))
    (is (= "span1" (-> e .-childNodes (aget 0) .-innerHTML)))
    (is (= "span2" (-> e .-childNodes (aget 1) .-innerHTML))))
  (let [e (hipo/create-static [:div (for [x [1 2]] [:span {:id (str "id" x)} (str "span" x)])] )]
    (is (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>" (.-innerHTML e)))))

(deftest attrs
  (let [e (hipo/create-static [:a ^:attrs (merge {} {:href "http://somelink"}) "anchor"])]
    (is (-> e .-tagName (= "A")))
    (is (= "anchor" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "href")))))

(defn my-div-with-nested-list [] [:div [:div] (list [:div "a"] [:div "b"] [:div "c"])])

(deftest nested
  ;; test html for example list form
  ;; note: if practice you can write the direct form (without the list) you should.
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end [:span.end "end"]
        e (hipo/create-static [:div#id1.class1 (list spans end)])]
    (is (-> e .-textContent (= "span0span1end")))
    (is (-> e .-className (= "class1")))
    (is (-> e .-childNodes .-length (= 3)))
    (is (= "<span>span0</span><span>span1</span><span class=\"end\">end</span>" (.-innerHTML e)))
    (is (-> e .-childNodes (aget 0) .-innerHTML (= "span0")))
    (is (-> e .-childNodes (aget 1) .-innerHTML (= "span1")))
    (is (-> e .-childNodes (aget 2) .-innerHTML (= "end"))))
  ;; test equivalence of "direct inline" and list forms
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end   [:span.end "end"]
        e1 (hipo/create-static [:div.class1 (list spans end)])
        e2 (hipo/create-static [:div.class1 spans end])]
    (is (= (.-innerHTML e1) (.-innerHTML e2))))
  (let [e (hipo/create-static (my-div-with-nested-list))]
    (is (hipo/partially-compiled? e))
    (is (= 4 (.. e -childNodes -length)))
    (is (= "abc" (.-textContent e)))))

(defn my-button [s] [:button s])

(deftest function
  (let [e (hipo/create-static (my-button "label"))]
    (is (hipo/partially-compiled? e))
    (is (= "BUTTON" (.-tagName e)))
    (is (= "label" (.-textContent e))))
  (let [e (hipo/create-static [:div (my-button "label") (my-button "label")])]
    (is (hipo/partially-compiled? e))
    (is (= "BUTTON" (.-tagName (.-firstChild e))))
    (is (= "label" (.-textContent (.-firstChild e))))))

(deftest boolean-attribute
  (let [e1 (hipo/create-static [:div {:attr true} "some text"])
        e2 (hipo/create-static [:div {:attr false} "some text"])
        e3 (hipo/create-static [:div {:attr nil} "some text"])]
    (is (= "true" (.getAttribute e1 "attr")))
    (is (nil? (.getAttribute e2 "attr")))
    (is (nil? (.getAttribute e3 "attr")))))

(deftest camel-case-attribute
  (let [el (hipo/create-static [:input {:defaultValue "default"}])]
    (is (= "default" (.getAttribute el "defaultValue")))))

(defn my-div [] [:div {:on-dragend (fn [])}])

(deftest listener
  (let [e (hipo/create-static [:div {:on-drag (fn [])}])]
    (is (not (hipo/partially-compiled? e)))
    (is (nil? (.getAttribute e "on-drag"))))
  (let [e (hipo/create-static (my-div))]
    (is (hipo/partially-compiled? e))
    (is (nil? (.getAttribute e "on-dragend")))))

(defn my-nil [] [:div nil "content" nil])

(deftest nil-children
  (let [e (hipo/create-static [:div nil "content" nil])]
    (is (not (hipo/partially-compiled? e)))
    (is (= "content" (.-textContent e))))
  (let [e (hipo/create-static (my-nil))]
    (is (hipo/partially-compiled? e))
    (is (= "content" (.-textContent e)))))

(deftest custom-elements
  (is (exists? (.-registerElement js/document)))
  (.registerElement js/document "my-custom" #js {:prototype (js/Object.create (.-prototype js/HTMLDivElement) #js {:test #js {:get (fn[] "")}})})
  (let [e (hipo/create-static [:my-custom "content"])]
    (is (exists? (.-test e)))
    (is (-> e .-tagName (= "MY-CUSTOM")))
    (is (= "content" (.-textContent e))))
  (let [e (hipo/create-static [:my-non-existing-custom "content"])]
    (is (not (exists? (.-test e))))
    (is (-> e .-tagName (= "MY-NON-EXISTING-CUSTOM")))
    (is (= "content" (.-textContent e)))))

(deftest namespaces
  (is (= "http://www.w3.org/1999/xhtml" (.-namespaceURI (hipo/create-static [:p]))))
  (is (= "http://www.w3.org/2000/svg" (.-namespaceURI (hipo/create-static [:circle])))))

(deftest partially-compiled
  (is (false? (hipo/partially-compiled? (hipo/create-static [:div]))))
  (is (true? (hipo/partially-compiled? (hipo/create-static [:div] {:force-interpretation? true}))))
  (is (true? (hipo/partially-compiled? (hipo/create-static [:div (conj [] :div)])))))

(deftest compile-form
  (let [e (hipo/create-static [:ul (for [i (range 5)] [:li "content" ^:text (str i)])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 5 (.. e -childNodes -length)))
    (is (= "content0" (.. e -firstChild -textContent))))
  (let [e (hipo/create-static [:div (if true [:div])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 1 (.. e -childNodes -length))))
  (let [e (hipo/create-static [:div (if true nil [:div])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 0 (.. e -childNodes -length))))
  (let [e (hipo/create-static [:div (if false [:div])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 0 (.. e -childNodes -length))))
  (let [e (hipo/create-static [:div (if false [:div] nil)])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 0 (.. e -childNodes -length))))
  (let [e (hipo/create-static [:div (when true [:div])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 1 (.. e -childNodes -length))))
  (let [e (hipo/create-static [:div (when false [:div])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 0 (.. e -childNodes -length))))
  (let [e (hipo/create-static [:div (list [:div "1"] [:div "2"])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 2 (.. e -childNodes -length)))))

(def my-str str)

(deftest hints
  (let [e (hipo/create-static [:div (+ 1 2)])]
    (is (false? (hipo/partially-compiled? e))))
  (let [e (hipo/create-static [:div (not true)])]
    (is (false? (hipo/partially-compiled? e))))
  (let [e (hipo/create-static [:div (my-str "content")])]
    (is (true? (hipo/partially-compiled? e))))
  (let [e (hipo/create-static [:div ^:text (my-str "content")])]
    (is (false? (hipo/partially-compiled? e)))))

(deftest update-simple
  (let [[el f] (hipo/create (fn [m] [:div {:id (:id m)} (:content m)]) {:id "id1" :content "a"})]
    (f {:id "id2" :content "b"})

    (is (= "b" (.-textContent el)))
    (is (= "id2" (.-id el)))))

(if (exists? js/MutationObserver)
  (deftest update-nested
    (let [c1 [:div {:class "class1" :attr1 "1"} [:span "content1"] [:span]]
          c2 [:div {:attr1 nil :attr2 nil} [:span]]
          c3 [:div]
          c4 [:div {:class "class2" :attr2 "2"} [:span] [:div "content2"]]
          [el f] (hipo/create (fn [c] c) c1)
          o (js/MutationObserver. identity)]
      (.observe o el #js {:attributes true :childList true :characterData true :subtree true})

      (is "div" (.-localName el))
      (is (= 2 (.-childElementCount el)))

      (f c1)

      (is (= 0 (count (array-seq (.takeRecords o)))))

      (f c2)

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

      (f c3)

      (is (= 0 (.-childElementCount el)))

      (let [v (array-seq (.takeRecords o))]
        (is (= 1 (count v)))
        (is (= "childList" (.-type (first v)))))

      (f c4)

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
        [el f] (hipo/create (fn [b] [:div (if b {:on-click #(swap! a inc)})]) true)]
    (fire-click-event el)
    (f false)
    (fire-click-event el)

    (is (= 1 @a))
    (f true)
    (fire-click-event el)
    (is (= 2 @a))))

(deftest update-listener-as-map
  (let [a (atom 0)
        [el f] (hipo/create (fn [m] [:div ^:attrs (if m {:on-click {:name "click" :fn #(when-let [f (:fn m)] (swap! a (fn [evt] (f evt))))}})]) {:fn #(inc %)})]
    (fire-click-event el)
    (is (= 1 @a))
    (f)
    (fire-click-event el)

    (is (= 1 @a))
    (f {:fn #(dec %)})
    (fire-click-event el)
    (is (= 0 @a))))

(deftest update-keyed
  (let [[el f] (hipo/create (fn [r] [:ul (for [i r] ^{:key i} [:li {:class i} i])]) (range 6))]
    (f (reverse (range 6)))

    (is (= 6 (.. el -childNodes -length)))
    (is (= "5" (.. el -firstChild -textContent)))
    (is (= "5" (.. el -firstChild -className)))
    (is (= "4" (.. el -firstChild -nextSibling -textContent)))
    (is (= "3" (.. el -firstChild -nextSibling -nextSibling -textContent)))
    (is (= "2" (.. el -firstChild -nextSibling -nextSibling -nextSibling -textContent)))
    (is (= "1" (.. el -firstChild -nextSibling -nextSibling -nextSibling -nextSibling -textContent)))
    (is (= "0" (.. el -lastChild -textContent)))))

(deftest update-keyed-sparse
  (let [[el f] (hipo/create (fn [r] [:ul (for [i r] ^{:key i} [:li {:class i} i])]) (range 6))]
    (f (cons 7 (filter odd? (reverse (range 6)))))

    (is (= 4 (.. el -childNodes -length)))
    (is (= "7" (.. el -firstChild -textContent)))
    (is (= "7" (.. el -firstChild -className)))
    (is (= "5" (.. el -firstChild -nextSibling -textContent)))
    (is (= "3" (.. el -firstChild -nextSibling -nextSibling -textContent)))
    (is (= "1" (.. el -firstChild -nextSibling -nextSibling -nextSibling -textContent)))))

(deftest update-state
  (let [m1 {:children (range 6)}
        m2 {:children (cons 7 (filter odd? (reverse (range 6))))}
        f (fn [m]
            [:ul (for [i (:children m)]
                   ^{:key i} [:li {:class i} i])])
        [el uf] (hipo/create f m1)]
    (uf m2)

    (is (= 4 (.. el -childNodes -length)))
    (is (= "7" (.. el -firstChild -textContent)))
    (is (= "7" (.. el -firstChild -className)))
    (is (= "5" (.. el -firstChild -nextSibling -textContent)))
    (is (= "3" (.. el -firstChild -nextSibling -nextSibling -textContent)))
    (is (= "1" (.. el -firstChild -nextSibling -nextSibling -nextSibling -textContent)))))

(deftype FunctionInterceptor []
  Interceptor
  (-intercept [_ t _]
    ; let update be performed but reject all others
    (fn [f] (if (= :update t) (f)))))

(deftype BooleanInterceptor [b]
  Interceptor
  (-intercept [_ t o]
    b))

(deftest interceptor
  (let [[el f] (hipo/create (fn [m] [:div {:class (:value m)}]) {:value 1})]
    (f {:value 2}
       {:interceptor (BooleanInterceptor. false)})
    (is (= "1" (.-className el)))

    (f {:value 3}
       {:interceptor (BooleanInterceptor. true)})
    (is (= "3" (.-className el)))

    (f {:value 4}
       {:interceptor (FunctionInterceptor.)})
    (is (= "3" (.-className el)))))
