(ns elv.memory-storage-test
  (:require [clojure.test :refer :all]
  			[elv.storage :refer :all]
            [elv.memory-storage :refer :all]
            [ring.mock.request :as mock]))

(deftest should-store-and-retrive
	(testing "storing map should enable retriving it as well"
		(let [s (->InMemoryStore (atom []))]
			(store s {:id "10" :result "test"})
			(let [r (retrive s "10")]
				(is (= (:result r) "test"))))))

(deftest paging-store
	(testing "paging shows just 10 result"
		(let [s (->InMemoryStore (atom []))
			  els (map (fn [e] {:id e :result (str "test" e)}) (range 20))]
			(dorun (map #(store s %) els))
			(is (= 10 (count (search s 1 10 nil nil))))))
	(testing "paging shows just 1 result"
		(let [s (->InMemoryStore (atom []))
			  els (map (fn [e] {:id e :result (str "test" e)}) (range 20))]
			(dorun (map #(store s %) els))
			(is (= 1 (count (search s 1 1 nil nil))))))
	(testing "paging shows second page"
		(let [s (->InMemoryStore (atom []))
			  els (map (fn [e] {:id e :result (str "test" e)}) (range 12))]
			(dorun (map #(store s %) els))
			(is (= 2 (count (search s 2 10 nil nil))))))
	(testing "paging shows second page"
		(let [s (->InMemoryStore (atom []))
			  els (map (fn [e] {:id e :result (str "test" e)}) (range 12))]
			(dorun (map #(store s %) els))
			(is (= 2 (count (search s 2 10 nil nil)))))))

(deftest searching-store
	(testing "should be able to search based on key"
		(let [s (->InMemoryStore (atom []))
			  els (map (fn [e] {:id e :result (str "test" e)}) (range 20))]
			(dorun (map #(store s %) els))
			(is (= (:id (first (search s 1 1 "#(= (:result %) \"test12\")" nil))) 12)))))

(deftest sorting-store
	(testing "should be able to sort on results"
		(let [s (->InMemoryStore (atom []))
			els [{:id 1 :res "k"} {:id 2 :res "g"} {:id 3 :res "a"} {:id 4 :res "b"}]]
			(dorun (map #(store s %) els))
			(let [sorted (search s 1 2 nil "#(compare (:res %1) (:res %2))")]
				(= (:res (first sorted)) "a")
				(= (:res (second sorted)) "b")))))