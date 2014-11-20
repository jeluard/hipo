(ns hipo.template-test
  (:require [cemerick.cljs.test :as test])
  (:require-macros [cemerick.cljs.test :refer [deftest is]]
                   [hipo.macros :refer [create]]))

(deftest simple-template
  (is (= "B" (.-tagName (create [:b]))))
  (let [e (create [:span "some text"])]
    (is (= "SPAN" (.-tagName e)))
    (is (= "some text" (.-textContent e)))
    (is (= js/document.TEXT_NODE (-> e .-childNodes (aget 0) .-nodeType)))
    (is (zero? (-> e .-children .-length))))
  (let [e (create [:a {:href "http://somelink"} "anchor"])]
    (is (-> e .-tagName (= "A")))
    (is (= "anchor" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "href"))))
  (let [a (atom 0)
        next-id #(swap! a inc)
        e (create [:span {:attr (next-id)}])]
    (is (= "1" (.getAttribute e "attr"))))
  (let [e (create [:div#id {:class "class1 class2"}])]
    (is (= "class1 class2" (.-className e))))
  (let [e (create [:div#id.class1 {:class "class2 class3"}])]
    (is (= "class1 class2 class3" (.-className e))))
  (let [cs "class2 class3"
        e (create [:div (list [:div#id.class1 {:class cs}])])]
    (is (= "class1 class2 class3" (.-className (.-firstChild e)))))
  (let [e (create [:div.class1 ^:attrs (merge {:data-attr ""} {:class "class2 class3"})])]
    (is (= "class1 class2 class3" (.-className e))))
  (let [e (create [:div (interpose [:br] (repeat 3 "test"))])]
    (is (= 5 (.. e -childNodes -length)))
    (is (= "test" (.. e -firstChild -textContent))))
  (let [e (create [:div.class1 [:span#id1 "span1"] [:span#id2 "span2"]])]
    (is (= "span1span2" (.-textContent e)))
    (is (= "class1" (.-className e)))
    (is (= 2 (-> e .-childNodes .-length)))
    (is (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>"
           (.-innerHTML e)))
    (is (= "span1" (-> e .-childNodes (aget 0) .-innerHTML)))
    (is (= "span2" (-> e .-childNodes (aget 1) .-innerHTML))))
  (let [e (create[:div (for [x [1 2]] [:span {:id (str "id" x)} (str "span" x)])] )]
    (is (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>" (.-innerHTML e)))))

(deftest multiple-template
  (let [e (create [:div] [:div])]
    (is (= 2 (.. e -childNodes -length))))
  (let [e (create (list [:div] [:div]))]
    (is (= 2 (.. e -childNodes -length)))))

(deftest attrs-template
  (let [e (create [:a ^:attrs (merge {} {:href "http://somelink"}) "anchor"])]
    (is (-> e .-tagName (= "A")))
    (is (= "anchor" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "href")))))

(deftest nested-template
  ;; test html for example list form
  ;; note: if practice you can write the direct form (without the list) you should.
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end [:span.end "end"]
        e (create [:div#id1.class1 (list spans end)])]
    (is (-> e .-textContent (= "span0span1end")))
    (is (-> e .-className (= "class1")))
    (is (-> e .-childNodes .-length (= 3)))
    (is (-> e .-innerHTML
            (= "<span>span0</span><span>span1</span><span class=\"end\">end</span>")))
    (is (-> e .-childNodes (aget 0) .-innerHTML (= "span0")))
    (is (-> e .-childNodes (aget 1) .-innerHTML (= "span1")))
    (is (-> e .-childNodes (aget 2) .-innerHTML (= "end"))))

  ;; test equivalence of "direct inline" and list forms
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end   [:span.end "end"]
        e1 (create [:div.class1 (list spans end)])
        e2 (create [:div.class1 spans end])]
    (is (= (.-innerHTML e1) (.-innerHTML e2)))))

(deftest boolean-attribute
  (let [e1 (create [:option {:selected true} "some text"])
        e2 (create [:option {:selected false} "some text"])
        e3 (create [:option {:selected nil} "some text"])]
    (is (= "true" (.getAttribute e1 "selected")))
    (is (nil? (.getAttribute e2 "selected")))
    (is (nil? (.getAttribute e3 "selected")))))

(deftest custom-elements
  (is (exists? (.-registerElement js/document)))
  (.registerElement js/document "my-custom-div" #js {:prototype (js/Object.create (.-prototype js/HTMLDivElement) #js {:test #js {:get (fn[] "")}}) :extends "div"})
  (let [e (create [:div {:is "my-custom-div"} "content"])]
    (is (exists? (.-test e)))
    (is (-> e .-tagName (= "DIV")))
    (is (= "content" (.-textContent e)))
    (is (= "my-custom-div" (.getAttribute e "is"))))
  (let [e (create [:div {:is "my-non-existing-custom-div"} "content"])]
    (is (not (exists? (.-test e))))
    (is (-> e .-tagName (= "DIV")))
    (is (= "content" (.-textContent e)))
    (is (= "my-non-existing-custom-div" (.getAttribute e "is")))))

(deftest namespaces
  (is (= "http://www.w3.org/1999/xhtml" (.-namespaceURI (create [:p]))))
  (is (= "http://www.w3.org/2000/svg" (.-namespaceURI (create [:circle])))))
