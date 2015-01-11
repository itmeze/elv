 (ns elv.core
  (:import (java.util Calendar UUID)
           (java.io StringWriter PrintWriter ByteArrayOutputStream ByteArrayInputStream))
  (:require [hiccup.core :refer :all]
            [hiccup.page :refer :all]
            [clojure.java.io :refer [copy]]
            [elv.pages :refer :all]
            [elv.storage :refer :all]
            [elv.memory-storage :refer :all]
            [ring.middleware.params :refer [params-request wrap-params]]))


(defn- body-size [request]
  (or (:content-length request) (.available (:body request)) 0))

(defn- read-body [request]
  (if (> (body-size request) 4096)
    "...body exceeds 4096..."
    (let [out-stream (ByteArrayOutputStream.)
          neglect (copy (:body request) out-stream)
          b (.toByteArray out-stream)]
      {:body (ByteArrayInputStream. b) :content (slurp b)})))

(defn- build-ex
  [exception]
  (let [sw (StringWriter.)]
    (.printStackTrace exception (PrintWriter. sw))
    {:type (str (type exception)) :message (.getMessage exception) :stack-trace (str sw)}))

(defn- get-page
  [request]
  (Integer. (or ((:query-params request) "page") 1)))

(defn- get-size
  [request]
  (Integer. (or ((:query-params request) "size") 25)))

(defn- get-id
  [request]
  (and (:query-params request) ((:query-params request) "id")))

(defn log-type [storage uri request]
  (if (nil? (get-id request))
    :list
    :details))

(defmulti log-page log-type)

(defmethod log-page :list [storage err-uri request]
  (let [page-n (get-page request)
        size-n (get-size request)
        result (search storage page-n size-n nil nil)]
    {:status 200 :headers {"Content-Type" "text-html"} :body (log-page-list result page-n size-n err-uri)}))

(defmethod log-page :details [storage err-uri request]
  (let [id (str (get-id request))
        result (retrive storage id)]
    (if result
      {:status 200 :headers {"Content-Type" "text-html"} :body (log-page-details result err-uri)}
      {:status 404 :headers {"Content-Type" "text-html"} :body (error-not-found err-uri)})))

(defn uuid [] (str (UUID/randomUUID)))
(defn dt [] (.getTime (Calendar/getInstance)))

(defn is-local [request]
    (some #(= (:remote-addr request) %) ["127.0.0.1" "localhost" "::1" "0:0:0:0:0:0:0:1"]))

(defn only-local-middleware [handler]
  (fn [request]
    (if (is-local request)
      (handler request)
      {:status 401 })))

(defn- comp-name
  []
  (or (System/getenv "HOSTNAME") (System/getenv "COMPUTERNAME")))

(defn body-pre-read-middleware
  [handler]
  (fn [request]
    (let [b (read-body request)
          r (assoc request :body (:body b) :body-pre-read (:content b))]
      (handler r))))

(defn log-view-handler
  [storage path]
  (fn [request]
    (log-page storage path request)))

(defn wrap-exception
  [handler & { :keys [path log-page-handler storage]
                :or {
                  path "/log"
                  log-page-handler only-local-middleware
                  storage (->InMemoryStore (atom []))}}]
  (fn [request]
    (if (= path (:uri request))
      ((-> (log-view-handler storage path) (wrap-params) log-page-handler) request)
      (try
         (handler request)
         (catch Exception e
           (store storage (assoc request :id (uuid) :machine (comp-name) :datetime (dt) :exception (build-ex e)))
           (throw e))))))