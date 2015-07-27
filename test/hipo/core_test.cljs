(ns hipo.core-test
  (:require [cemerick.cljs.test :as test]
            [hipo.core :as hipo]
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
  (let [e (hipo/create [:div (for [x [1 2]] [:span {:id (str "id" x)} (str "span" x)])] )]
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
    (is (= 4 (.. e -childNodes -length)))
    (is (= "abc" (.-textContent e)))))

(defn my-button [s] [:button s])

(deftest function
  (let [e (hipo/create (my-button "label"))]
    (is (= "BUTTON" (.-tagName e)))
    (is (= "label" (.-textContent e))))
  (let [e (hipo/create [:div (my-button "label") (my-button "label")])]
    (is (= "BUTTON" (.-tagName (.-firstChild e))))
    (is (= "label" (.-textContent (.-firstChild e))))))

(deftest boolean-attribute
  (let [e1 (hipo/create [:div {:attr true} "some text"])
        e2 (hipo/create [:div {:attr false} "some text"])
        e3 (hipo/create [:div {:attr nil} "some text"])]
    (is (= "true" (.getAttribute e1 "attr")))
    (is (nil? (.getAttribute e2 "attr")))
    (is (nil? (.getAttribute e3 "attr")))))

(deftest camel-case-attribute
  (let [el (hipo/create [:input {:defaultValue "default"}])]
    (is (= "default" (.getAttribute el "defaultValue")))))

(defn my-div [] [:div {:on-dragend (fn [])}])

(deftest listener
  (let [e (hipo/create [:div {:on-drag (fn [])}])]
    (is (nil? (.getAttribute e "on-drag"))))
  (let [e (hipo/create (my-div))]
    (is (nil? (.getAttribute e "on-dragend")))))

(defn my-nil [] [:div nil "content" nil])
(defn my-when [b o] (when b o))

(deftest nil-children
  (let [e (hipo/create [:div nil "content" nil])]
    (is (= "content" (.-textContent e))))
  (let [e (hipo/create [:div (my-when false "prefix") "content"])]
    (is (= "content" (.-textContent e))))
  (let [e (hipo/create (my-nil))]
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
  (is (= "http://www.w3.org/2000/svg" (.-namespaceURI (hipo/create [:svg/circle]))))
  (is (= "http://www.w3.org/2000/svg" (.-namespaceURI (hipo/create [:svg/circle] {:force-interpretation? true})))))
