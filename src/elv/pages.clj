(ns elv.pages
  (:require [hiccup.core :refer :all]
            [hiccup.page :refer :all]
            [cheshire.core :refer :all]
            [cheshire.generate :refer [add-encoder encode-str remove-encoder]]
            [clojure.java.io :refer [copy]]
            [clojure.string :as string])
  (:import (java.io ByteArrayInputStream)))

(add-encoder ByteArrayInputStream
             (fn [c jsonGenerator]
               (.writeString jsonGenerator (slurp c))))
(add-encoder java.lang.Object encode-str)

(defn th [tags]
  (map (fn [tag] [:th tag]) tags))

(defn table-with-errors [errors uri]
  (html
    [:table.table.table-striped
     [:tr (th ["Machine" "Method" "Uri"  "Remote address" "Exception" "Message" "Date" ""])]
     (map (fn [e] [:tr [:td (:machine e)] [:td (:request-method e)] [:td (:uri e)] [:td (:remote-addr e)] [:td (-> e :exception :type)] [:td (-> e :exception :message)] [:td (:datetime e)] [:td [:a {:href (str uri "?id=" (:id e))} "Details"]]]) errors)
     ]))

(defn log-link [uri page size]
  (str uri "?page=" page "&size=" size))

(defn navigation
  [page size has-next log-uri]
  (html
    [:nav
     [:ul.pager
      (if (> page 1)
      [:li.previous [:a {:href (log-link log-uri (dec page) size)} [:span {:aria-hidden "true"} "&larr;"] " Previous"]])
      (if has-next
      [:li.next [:a {:href (log-link log-uri (inc page) size)} [:span {:aria-hidden "true"} "&rarr;"] " Next"]])
     ]
    ]))

(defn log-page-list [errors page size err-uri]
  (html5
  [:html {:lang "en"}
   [:head
    [:title "Error log for app"]
    (include-css "//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css")]
   [:body
    [:div.container-fluid
     [:div.row
      [:h1 "Error Logs"]
      (table-with-errors errors err-uri)
      (navigation page size (= (count errors) size) err-uri)
     ]
    ]
   ]
  ]))

(defn error-not-found [err-uri]
  (html5
    [:html {:lang "en"}
     [:head
      [:title "Error log for app"]
      (include-css "//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css")]
     [:body
      [:div.container-fluid
       [:div.row
        [:h3 [:a {:href err-uri} "Back to error logs"]]
        [:div.alert.alert-warning "Provided error was not found"]
        ]
       ]
      ]
     ]))

(defn log-page-details [error err-uri]
  (html5
    [:html {:lang "en"}
     [:head
      [:title "Error log for app"]
      (include-css "//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css")]
     [:body
      [:div.container-fluid
       [:div.row
        [:h3 [:a {:href err-uri} "Back to error logs"]]
        [:h1 (str "Details for error: " (:id error))]
        [:h2 "Exception"]
        [:div.alert.alert-danger (-> error :exception :type) ":" (-> error :exception :message)]
        [:h2 "Stacktrace:"]
        [:div.alert.alert-warning
         [:textarea {:style "width:100%;height:500px;border:none;background:transparent" :disabled "disabled"} (-> error :exception :stack-trace) ]]
        [:h2 "Full details:"]
        [:div [:textarea {:style "width:100%;height:500px;border:none;background:transparent" :disabled "disabled"}  (generate-string error {:pretty true}) ]]
        ]
       ]
      (include-js "//ajax.googleapis.com/ajax/libs/jquery/2.1.3/jquery.min.js")
      [:script {:type "text/javascript"}
       "$('textarea').each(function(){ var el = $(this); el.height(el[0].scrollHeight) })"
       ]
      ]
     ])) 
