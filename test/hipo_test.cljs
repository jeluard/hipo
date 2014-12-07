(ns hipo.hipo-test
  (:require [cemerick.cljs.test :as test]
            [hipo :as hipo :include-macros]
            [hipo.interpreter :refer [set-attribute!]])
  (:require-macros [cemerick.cljs.test :refer [deftest is]]))

(deftest simple
  (is (= "B" (.-tagName (hipo/create [:b]))))
  (let [e (hipo/create [:span "some text"])]
    (is (= "SPAN" (.-tagName e)))
    (is (= "some text" (.-textContent e)))
    (is (= js/document.TEXT_NODE (-> e .-childNodes (aget 0) .-nodeType)))
    (is (zero? (-> e .-children .-length))))
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

(deftest nested
  ;; test html for example list form
  ;; note: if practice you can write the direct form (without the list) you should.
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end [:span.end "end"]
        e (hipo/create [:div#id1.class1 (list spans end)])]
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
        e1 (hipo/create [:div.class1 (list spans end)])
        e2 (hipo/create [:div.class1 spans end])]
    (is (= (.-innerHTML e1) (.-innerHTML e2)))))

(defn my-button [s] [:button s])

(deftest function
  (let [e (hipo/create (my-button "label"))]
    (is (hipo/partially-compiled? e))
    (is (= "BUTTON" (.-tagName (.-firstChild e))))
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

(defn my-div [] [:div {:on-click (fn [])}])

(deftest listener
  (let [e (hipo/create [:div {:on-click (fn [])}])]
    (is (not (hipo/partially-compiled? e)))
    (is (nil? (.getAttribute e "on-click"))))
  (let [e (hipo/create (my-div))]
    (is (hipo/partially-compiled? e))
    (is (nil? (.getAttribute (.-firstChild e) "on-click")))))

(defn my-custom [] [:div {:test3 1 :test4 1}])

(defmethod set-attribute! "test4"
  [[el a v]]
  (.setAttribute el a (* 2 v)))

(deftest custom-attribute
  (let [e (hipo/create [:div {:test 1 :test2 1} (my-custom)])]
    (is (hipo/partially-compiled? e))
    (is (= "1" (.getAttribute e "test")))
    (is (= "2" (.getAttribute e "test2")))
    (is (= "1" (.getAttribute (.-firstChild e) "test3")))
    (is (= "2" (.getAttribute (.-firstChild e) "test4")))))

(deftest custom-elements
  (is (exists? (.-registerElement js/document)))
  (.registerElement js/document "my-custom-div" #js {:prototype (js/Object.create (.-prototype js/HTMLDivElement) #js {:test #js {:get (fn[] "")}}) :extends "div"})
  (let [e (hipo/create [:div {:is "my-custom-div"} "content"])]
    (is (exists? (.-test e)))
    (is (-> e .-tagName (= "DIV")))
    (is (= "content" (.-textContent e)))
    (is (= "my-custom-div" (.getAttribute e "is"))))
  (let [e (hipo/create [:div {:is "my-non-existing-custom-div"} "content"])]
    (is (not (exists? (.-test e))))
    (is (-> e .-tagName (= "DIV")))
    (is (= "content" (.-textContent e)))
    (is (= "my-non-existing-custom-div" (.getAttribute e "is")))))

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
