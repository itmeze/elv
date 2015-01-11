(ns elv.core-test
  (:import (java.io ByteArrayInputStream))
  (:require [clojure.test :refer :all]
            [elv.core :refer :all]
            [elv.storage :refer :all]
            [elv.memory-storage :refer :all]
            [ring.mock.request :as mock]))

(def ok-result
  {:status 200 :body "ok" })

(defn- return-ok [request]
  ok-result)

(defn- throw-exception-middleware
  [handler ex]
  (fn [request]
    (throw ex)))

(defn request-with-exception
  ([ex storage]
    ((-> return-ok (throw-exception-middleware ex) (wrap-exception :storage storage)) (mock/request :get "/test")))
  ([ex]
    (request-with-exception ex (->InMemoryStore (atom [])))))

(defmacro swallow [& body]
  `(try ~@body (catch Exception e#)))

(deftest non-affecting-behaviour
  (testing "Error middleware should not affect other middlewares"
    (is (= ( (wrap-exception return-ok) (mock/request :get "/test")))
        ok-result)))

(deftest body-pre-reading
  (testing "pre-read of body should store it under :body-pre-read key"
    (let [result ((-> identity body-pre-read-middleware) (mock/request :post "/test-body" { "test" "body-content"}))]
      (is (instance? ByteArrayInputStream (:body result)))
      (is (= "test=body-content" (:body-pre-read result))))))

(deftest logs-exception
  (testing "Re-throws exception"
    (is (thrown-with-msg? Exception #"tex"
      (request-with-exception (Exception. "tex")))))
  (testing "exception gets logged"
    (let [storage (->InMemoryStore (atom []))]
      (swallow (request-with-exception (Exception. "tex1") storage))
      (is (= 1 (count (search storage 1 10 nil nil))))
      (is (= "/test" (-> (search storage 1 10 nil nil) first :uri)))))
  (testing "multiple gets logged"
    (let [storage (->InMemoryStore (atom []))]
      (swallow (request-with-exception (Exception. "tex1") storage))
      (swallow (request-with-exception (Exception. "tex2") storage))
      (swallow (request-with-exception (Exception. "tex3") storage))
      (is (= 3 (count (search storage 1 10 nil nil)))))))

(deftest listing-errors
  (testing "should show logged exception on page"
    (let [storage (->InMemoryStore (atom []))]
      (swallow (request-with-exception (Exception. "tex1") storage))
      (let [response ((wrap-exception return-ok :storage storage ) (mock/request :get "/log"))]
        (is (re-find #"tex1" (:body response))))))
  (testing "multiple exceptions as well"
    (let [storage (->InMemoryStore (atom []))]
      (swallow (request-with-exception (Exception. "tex1") storage))
      (swallow (request-with-exception (Exception. "tex2") storage))
      (swallow (request-with-exception (Exception. "tex3") storage))
      (let [response ((wrap-exception return-ok :storage storage ) (mock/request :get "/log"))]
        (is (re-find #"tex1" (:body response)))
        (is (re-find #"tex2" (:body response)))
        (is (re-find #"tex3" (:body response)))
        (is (not (re-find #"tex4" (:body response))))))))