(ns hipo.template-test
  (:require [cemerick.cljs.test :as test]
            [hipo.template :as template])
  (:require-macros [cemerick.cljs.test :refer [deftest is]]
                   [hipo.macros :refer [node]]))

(deftest simple-template
  (is (= "B" (-> [:b] template/node .-tagName)))
  (is (= "some text" (-> "some text" template/node .-textContent)))
  ;; unfortunately to satisfy the macro gods, you need to
  ;; duplicate the vector literal to test compiled and runtime template
  (let [e1 (template/node [:span "some text"])
        e2 (node [:span "some text"])]
    (doseq [e [e1 e2]]
      (is (= "SPAN" (.-tagName e)))
      (is (= "some text" (.-textContent e)))
      (is (= js/document.TEXT_NODE (-> e .-childNodes (aget 0) .-nodeType)))
      (is (zero? (-> e .-children .-length)))))
  (let [e1 (template/node [:a {:href "http://somelink"} "anchor"])
        e2 (node
            [:a {:href "http://somelink"} "anchor"])]
    (doseq [e [e1 e2]] (is (-> e .-tagName (= "A")))
           (is (= "anchor" (.-textContent e)))
           (is (= "http://somelink" (.getAttribute e "href")))))
  (let [a (atom 0)
        next-id #(swap! a inc)
        e (node [:span {:attr (next-id)}])]
    (is (= "1" (.getAttribute e "attr"))))
  (let [e1 (template/node [:div#id {:class "class1 class2"}])
        e2 (node [:div#id {:class "class1 class2"}])]
    (doseq [e [e1 e2]]
      (is (= "class1 class2" (.-className e)))))
  (let [e (template/compound-element [:div (interpose [:br] (repeat 3 "test"))])]
    (is (-> e .-outerHTML (= "<div>test<br>test<br>test</div>"))))
  (let [e1 (template/compound-element [:div.class1 [:span#id1 "span1"] [:span#id2 "span2"]])
        e2 (node [:div.class1 [:span#id1 "span1"] [:span#id2 "span2"]])]
    (doseq [e [e1 e2]]
      (is (= "span1span2" (.-textContent e)))
      (is (= "class1" (.-className e)))
      (is (= 2 (-> e .-childNodes .-length)))
      (is (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>"
             (.-innerHTML e)))
      (is (= "span1" (-> e .-childNodes (aget 0) .-innerHTML)))
      (is (= "span2" (-> e .-childNodes (aget 1) .-innerHTML)))))
  (is (= "<span id=\"id1\">span1</span><span id=\"id2\">span2</span>"
         (-> [:div (for [x [1 2]] [:span {:id (str "id" x)} (str "span" x)])]
             template/node
             .-innerHTML))))

(deftest attrs-template-test
  (let [e (node [:a ^:attrs (merge {} {:href "http://somelink"}) "anchor"])]
    (is (-> e .-tagName (= "A")))
    (is (= "anchor" (.-textContent e)))
    (is (= "http://somelink" (.getAttribute e "href")))))

(deftest nested-template-test
  ;; test html for example list form
  ;; note: if practice you can write the direct form (without the list) you should.
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end [:span.end "end"]
        h   [:div#id1.class1 (list spans end)]
        e1 (template/compound-element h)
        e2 (template/node             h)]
    (doseq [e [e1 e2]]
      (is (-> e .-textContent (= "span0span1end")))
      (is (-> e .-className (= "class1")))
      (is (-> e .-childNodes .-length (= 3)))
      (is (-> e .-innerHTML
              (= "<span>span0</span><span>span1</span><span class=\"end\">end</span>")))
      (is (-> e .-childNodes (aget 0) .-innerHTML (= "span0")))
      (is (-> e .-childNodes (aget 1) .-innerHTML (= "span1")))
      (is (-> e .-childNodes (aget 2) .-innerHTML (= "end")))))

  ;; test equivalence of "direct inline" and list forms
  (let [spans (for [i (range 2)] [:span (str "span" i)])
        end   [:span.end "end"]
        h1    [:div.class1 (list spans end)]
        h2    [:div.class1 spans end]
        e11 (template/compound-element h1)
        e12 (template/node             h1)
        e21 (template/compound-element h2)
        e22 (template/node             h2)]
    (doseq [[e1 e2] [[e11 e12]
                     [e12 e21]
                     [e21 e22]
                     [e22 e11]]]
      (is (= (.-innerHTML e1) (.-innerHTML e2))))))

(deftest boolean-attribute
  (let [e1 (template/node [:option {:selected true} "some text"])
        e1c (node [:option {:selected true} "some text"])
        e2 (template/node [:option {:selected false} "some text"])
        e2c (node [:option {:selected false} "some text"])
        e3 (template/node [:option {:selected nil} "some text"])
        e3c (node [:option {:selected nil} "some text"])]
    (doseq [e [e1 e1c]] (is (-> e (.getAttribute "selected") (= "true"))))
    (doseq [e [e2 e2c]] (is (-> e (.getAttribute "selected") (nil?))))
    (doseq [e [e3 e3c]] (is (-> e (.getAttribute "selected") (nil?))))))

(deftest custom-elements-test
  (is (exists? (.-registerElement js/document)))
  (.registerElement js/document "my-custom-div" #js {:prototype (js/Object.create (.-prototype js/HTMLDivElement) #js {:test #js {:get (fn[] "")}}) :extends "div"})
  (let [e1 (node [:div {:is "my-custom-div"} "content"])
        e2 (template/node [:div {:is "my-custom-div"} "content"])]
    (doseq [e [e1 e2]] (is (exists? (.-test e))))
    (doseq [e [e1 e2]] (is (-> e .-tagName (= "DIV"))))
    (doseq [e [e1 e2]] (is (= "content" (.-textContent e))))
    (doseq [e [e1 e2]] (is (= "my-custom-div" (.getAttribute e "is"))))))

(deftest namespaces
  (is (= "http://www.w3.org/1999/xhtml" (.-namespaceURI (node [:p]))))
  (is (= "http://www.w3.org/2000/svg" (.-namespaceURI (node [:circle])))))
