(ns hipo.core-test
  (:require [cemerick.cljs.test :as test]
            [hipo.core :as hipo]
            [hipo.interceptor :refer [Interceptor]]
            cljsjs.document-register-element)
  (:require-macros [cemerick.cljs.test :refer [deftest is]]))

(deftest simple
  (is (= "B" (.-tagName (hipo/create [:b]))))
  (let [e (hipo/create [:span "some text"])]
    (is (= "SPAN" (.-tagName e)))
    (is (= "some text" (.-textContent e)))
    (is (= js/document.TEXT_NODE (-> e .-childNodes (aget 0) .-nodeType)))
    (is (zero? (-> e .-children .-length))))
  (let [e (hipo/create [:script {:src "http://somelink"}])]
    (is (= "" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "src"))))
  (let [e (hipo/create [:a {:href "http://somelink"} "anchor"])]
    (is (-> e .-tagName (= "A")))
    (is (= "anchor" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "href"))))
  (let [a (atom 0)
        next-id #(swap! a inc)
        e (hipo/create [:span {:attr (next-id)}])]
    (is (= "1" (.getAttribute e "attr"))))
  (let [e (hipo/create [:div#id {:class "class1 class2"}])]
    (is (= "class1 class2" (.-className e))))
  (let [e (hipo/create [:div#id.class1 {:class "class2 class3"}])]
    (is (= "class1 class2 class3" (.-className e))))
  (let [cs "class1 class2"
        e (hipo/create [:div ^:attrs (merge {} {:class cs})])]
    (is (= "class1 class2" (.-className e))))
  (let [cs "class2 class3"
        e (hipo/create [:div (list [:div#id.class1 {:class cs}])])]
    (is (= "class1 class2 class3" (.-className (.-firstChild e)))))
  (let [e (hipo/create [:div.class1 ^:attrs (merge {:data-attr ""} {:class "class2 class3"})])]
    (is (= "class1 class2 class3" (.-className e))))
  (let [e (hipo/create [:div (interpose [:br] (repeat 3 "test"))])]
    (is (= 5 (.. e -childNodes -length)))
    (is (= "test" (.. e -firstChild -textContent))))
  (let [e (hipo/create [:div.class1 [:span#id1 "span1"] [:span#id2 "span2"]])]
    (is (= "span1span2" (.-textContent e)))
    (is (= "class1" (.-className e)))
    (is (= 2 (-> e .-childNodes .-length)))
    (is (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>"
           (.-innerHTML e)))
    (is (= "span1" (-> e .-childNodes (aget 0) .-innerHTML)))
    (is (= "span2" (-> e .-childNodes (aget 1) .-innerHTML))))
  (let [e (hipo/create[:div (for [x [1 2]] [:span {:id (str "id" x)} (str "span" x)])] )]
    (is (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>" (.-innerHTML e)))))

(deftest attrs
  (let [e (hipo/create [:a ^:attrs (merge {} {:href "http://somelink"}) "anchor"])]
    (is (-> e .-tagName (= "A")))
    (is (= "anchor" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "href")))))

(defn my-div-with-nested-list [] [:div [:div] (list [:div "a"] [:div "b"] [:div "c"])])

(deftest nested
  ;; test html for example list form
  ;; note: if practice you can write the direct form (without the list) you should.
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end [:span.end "end"]
        e (hipo/create [:div#id1.class1 (list spans end)])]
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
        e1 (hipo/create [:div.class1 (list spans end)])
        e2 (hipo/create [:div.class1 spans end])]
    (is (= (.-innerHTML e1) (.-innerHTML e2))))
  (let [e (hipo/create (my-div-with-nested-list))]
    (is (hipo/partially-compiled? e))
    (is (= 4 (.. e -childNodes -length)))
    (is (= "abc" (.-textContent e)))))

(defn my-button [s] [:button s])

(deftest function
  (let [e (hipo/create (my-button "label"))]
    (is (hipo/partially-compiled? e))
    (is (= "BUTTON" (.-tagName e)))
    (is (= "label" (.-textContent e))))
  (let [e (hipo/create [:div (my-button "label") (my-button "label")])]
    (is (hipo/partially-compiled? e))
    (is (= "BUTTON" (.-tagName (.-firstChild e))))
    (is (= "label" (.-textContent (.-firstChild e))))))

(deftest boolean-attribute
  (let [e1 (hipo/create [:option {:selected true} "some text"])
        e2 (hipo/create [:option {:selected false} "some text"])
        e3 (hipo/create [:option {:selected nil} "some text"])]
    (is (= "true" (.getAttribute e1 "selected")))
    (is (nil? (.getAttribute e2 "selected")))
    (is (nil? (.getAttribute e3 "selected")))))

(defn my-div [] [:div {:on-dragend (fn [])}])

(deftest listener
  (let [e (hipo/create [:div {:on-drag (fn [])}])]
    (is (not (hipo/partially-compiled? e)))
    (is (nil? (.getAttribute e "on-drag"))))
  (let [e (hipo/create (my-div))]
    (is (hipo/partially-compiled? e))
    (is (nil? (.getAttribute e "on-dragend")))))

(defn my-nil [] [:div nil "content" nil])

(deftest nil-children
  (let [e (hipo/create [:div nil "content" nil])]
    (is (not (hipo/partially-compiled? e)))
    (is (= "content" (.-textContent e))))
  (let [e (hipo/create (my-nil))]
    (is (hipo/partially-compiled? e))
    (is (= "content" (.-textContent e)))))

(deftest custom-elements
  (is (exists? (.-registerElement js/document)))
  (.registerElement js/document "my-custom" #js {:prototype (js/Object.create (.-prototype js/HTMLDivElement) #js {:test #js {:get (fn[] "")}})})
  (let [e (hipo/create [:my-custom "content"])]
    (is (exists? (.-test e)))
    (is (-> e .-tagName (= "MY-CUSTOM")))
    (is (= "content" (.-textContent e))))
  (let [e (hipo/create [:my-non-existing-custom "content"])]
    (is (not (exists? (.-test e))))
    (is (-> e .-tagName (= "MY-NON-EXISTING-CUSTOM")))
    (is (= "content" (.-textContent e)))))

(deftest namespaces
  (is (= "http://www.w3.org/1999/xhtml" (.-namespaceURI (hipo/create [:p]))))
  (is (= "http://www.w3.org/2000/svg" (.-namespaceURI (hipo/create [:circle])))))

(deftest partially-compiled
  (is (false? (hipo/partially-compiled? (hipo/create [:div]))))
  (is (true? (hipo/partially-compiled? (hipo/create [:div (conj [] :div)])))))

(deftest compile-form
  (let [e (hipo/create [:ul (for [i (range 5)] [:li "content" ^:text (str i)])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 5 (.. e -childNodes -length)))
    (is (= "content0" (.. e -firstChild -textContent))))
  (let [e (hipo/create [:div (if true [:div])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 1 (.. e -childNodes -length))))
  (let [e (hipo/create [:div (if true nil [:div])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 0 (.. e -childNodes -length))))
  (let [e (hipo/create [:div (if false [:div])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 0 (.. e -childNodes -length))))
  (let [e (hipo/create [:div (if false [:div] nil)])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 0 (.. e -childNodes -length))))
  (let [e (hipo/create [:div (when true [:div])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 1 (.. e -childNodes -length))))
  (let [e (hipo/create [:div (when false [:div])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 0 (.. e -childNodes -length))))
  (let [e (hipo/create [:div (list [:div "1"] [:div "2"])])]
    (is (false? (hipo/partially-compiled? e)))
    (is (= 2 (.. e -childNodes -length)))))

(def my-str str)

(deftest hints
  (let [e (hipo/create [:div (+ 1 2)])]
    (is (false? (hipo/partially-compiled? e))))
  (let [e (hipo/create [:div (not true)])]
    (is (false? (hipo/partially-compiled? e))))
  (let [e (hipo/create [:div (my-str "content")])]
    (is (true? (hipo/partially-compiled? e))))
  (let [e (hipo/create [:div ^:text (my-str "content")])]
    (is (false? (hipo/partially-compiled? e)))))

(deftest update-simple
  (let [h1 [:div#id1 "a"]
        h2 [:div#id2 "b"]
        [el f] (hipo/create-for-update h1)]
    (f h2)

    (is (= "b" (.-textContent el)))
    (is (= "id2" (.-id el)))))

(if (exists? js/MutationObserver)
  (deftest update-nested
    (let [h1 [:div {:class "class1" :attr1 "1"} [:span "content1"] [:span]]
          h2 [:div {:attr1 nil :attr2 nil} [:span]]
          h3 [:div]
          h4 [:div {:class "class2" :attr2 "2"} [:span] [:div "content2"]]
          [el f] (hipo/create-for-update h1)
          o (js/MutationObserver. identity)]
      (.observe o el #js {:attributes true :childList true :characterData: true :subtree true})

      (is "div" (.-localName el))
      (is (= 2 (.-childElementCount el)))

      (f h1)

      (is (= 0 (count (array-seq (.takeRecords o)))))

      (f h2)

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

      (f h3)

      (is (= 0 (.-childElementCount el)))

      (let [v (array-seq (.takeRecords o))]
        (is (= 1 (count v)))
        (is (= "childList" (.-type (first v)))))

      (f h4)

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
        h1 [:div {:on-click #(swap! a inc)}]
        h2 [:div]
        [el f] (hipo/create-for-update h1)]

    (fire-click-event el)

    (f h2)

    (fire-click-event el)

    (is (= 1 @a))))

(deftest update-keyed
  (let [h1 [:ul (for [i (range 6)]
                  ^{:key i} [:li i])]
        h2 [:ul (for [i (reverse (range 6))]
                  ^{:key i} [:li {:class i} i])]
        [el f] (hipo/create-for-update h1)]
    (f h2)

    (is (= 6 (.. el -childNodes -length)))
    (is (= "5" (.. el -firstChild -textContent)))
    (is (= "5" (.. el -firstChild -className)))
    (is (= "4" (.. el -firstChild -nextSibling -textContent)))
    (is (= "3" (.. el -firstChild -nextSibling -nextSibling -textContent)))
    (is (= "2" (.. el -firstChild -nextSibling -nextSibling -nextSibling -textContent)))
    (is (= "1" (.. el -firstChild -nextSibling -nextSibling -nextSibling -nextSibling -textContent)))
    (is (= "0" (.. el -lastChild -textContent)))))

(deftest update-keyed-sparse
  (let [h1 [:ul (for [i (range 6)]
                  ^{:key i} [:li i])]
        h2 [:ul (for [i (cons 7 (filter odd? (reverse (range 6))))]
                  ^{:key i} [:li {:class i} i])]
        [el f] (hipo/create-for-update h1)]
    (f h2)

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
        [el uf] (hipo/create-for-update f m1)]
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
  (let [[el f] (hipo/create-for-update [:div {:class "1"} [:div]])]
    (f [:div {:class "2"} [:span] [:span]]
       {:interceptor (BooleanInterceptor. false)})
    (is (= "1" (.-className el)))

    (f [:div {:class "3"} [:span] [:span]]
       {:interceptor (BooleanInterceptor. true)})
    (is (= "3" (.-className el)))

    (f [:div {:class "4"} [:span] [:span]]
       {:interceptor (FunctionInterceptor.)})
    (is (= "3" (.-className el)))))
