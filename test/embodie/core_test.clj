(ns embodie.core-test
  (:require [clj-http.client :as http]
            [clojure.test :refer [deftest is testing]]
            [embodie.core :refer [fetch fetch-resource]]
            [embodie.oembed
             :refer
             [find-oembed-provider
              init-oembed-providers
              sanitize-url
              with-oembed-providers]]))

(def providers [{"provider_name" "Defunkt",
                 "provider_url" "http://*.defunkt.pl/",
                 "endpoints" [{"schemes" ["https://*.defunkt.com.pl/api/*" "http://defunkt.pl/api/*"]
                               "url" "http://www.defunkt.pl/api/oembed"
                               "discovery" true}]}
                {"provider_name" "Quiz.biz"
                 "provider_url" "http://www.quiz.biz/"
                 "endpoints" [{"schemes" ["http://quiz.biz/quizz.html"]
                               "url" "http://www.quiz.biz/api/oembed"}]}])

(def oembed-link "http://www.defunkt.pl/oembed?format=json&url=https%3A%2F%2Fwww.defunkt.pl%2Fwatch%3Fv%3DXmNRZ-JJGBw")

(def html-title "Lorem ipsum")

(def html-resource `({:tag :html,
                      :attrs {:lang "pl"}
                      :content
                      ({:tag :head
                        :content ({:tag :title, :content "Lorem ipsum"}
                                  {:tag :meta, :attrs {:name "title"
                                                       :content ~html-title}}
                                  {:tag :link, :attrs {:rel "alternate"
                                                       :type "application/json+oembed"
                                                       :href ~oembed-link
                                                       :title "Lorem ipsum"}}
                                  {:tag :meta, :attrs {:property "og:site_name"
                                                       :content "Defunkt"}}
                                  {:tag :meta, :attrs {:property "og:title"
                                                       :content "Lorem ipsum"}}
                                  {:tag :meta, :attrs {:property "og:type"
                                                       :content "video"}}
                                  {:tag :meta, :attrs {:property "og:video:width"
                                                       :content "1280"}}
                                  {:tag :meta, :attrs {:property "og:video:height"
                                                       :content "720"}})}
                       {:tag :body
                        :content ({:tag :img
                                   :attrs {:width "60"
                                           :src "../images/cljs-logo-120b.png"
                                           :class "clj-logo"}})})}))

(deftest normalize-url
  (is (= "defunkt.com.pl" (sanitize-url "http://*.defunkt.com.pl/")))
  (is (= "defunkt.pl" (sanitize-url "https://defunkt.pl/watch?v=XmNRZ-JJGBw")))
  (is (= "defunkt.pl" (sanitize-url "http://www.Defunkt.PL/"))))

(deftest oembed-provider
  (with-oembed-providers (init-oembed-providers providers)

    (testing "locating oembed provider based on wildcarded url"
      (let [provider (find-oembed-provider "http://defunkt.pl")]
        (is (= "Defunkt" (:name provider)))
        (is (= "http://www.defunkt.pl/api/oembed" (:url provider)))))

    (testing "locating oembed provider based on non-wildcarded url"
      (let [provider (find-oembed-provider "http://quiz.biz/quizz.html")]
        (is (= "Quiz.biz" (:name provider)))
        (is (= "http://www.quiz.biz/api/oembed" (:url provider)))))

    (let [mock (atom "")]
      (with-redefs [fetch-resource (fn [url] html-resource)
                    http/get (fn [url headers] (reset! mock url))]
        (testing "discovery enabled"
          (fetch "http://defunkt.pl/watch?v=XmNRZ-JJGBw" [:oembed])
          (is (= oembed-link @mock)))

        (testing "discovery disabled"
          (fetch "http://quiz.biz/quizz.html" [:oembed])
          (is (= "http://www.quiz.biz/api/oembed?url=http%3A%2F%2Fquiz.biz%2Fquizz.html" @mock)))))))

(deftest open-graph-provider
  (with-oembed-providers (init-oembed-providers providers)
    (with-redefs [fetch-resource (fn [url] html-resource)]
      (testing "open-graph meta tags"
        (let [result (fetch "http://defunkt.pl/watch?v=XmNRZ-JJGBw" [:open-graph])]
          (is (= "video" (get-in result [:open-graph "og:type"])))
          (is (= "Lorem ipsum" (get-in result [:open-graph "og:title"]))))))))

(deftest html-provider
  (with-oembed-providers (init-oembed-providers providers)
    (with-redefs [fetch-resource (fn [url] html-resource)]
      (let [result (:html (fetch "http://defunkt.pl/watch?v=XmNRZ-JJGBw" [:html]))]
        (is (= html-title (:title result)))
        (is (not (empty? (:images result))))))))
