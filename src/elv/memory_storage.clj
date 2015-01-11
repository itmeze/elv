(ns elv.memory-storage
	(:require [elv.storage :refer :all]))

(defrecord InMemoryStore [v-atom]
	LogStorage
	(store 
		[this error]
		(swap! v-atom conj error))
	(retrive 
		[this id] 
		(first (filter (fn [e] (= id (str (:id e)))) @v-atom)))
	(search 
		[this page size conditions order]
			(let [p (or page 1)
				  s (or size 25)
				  skip (* (- p 1) s)
				  f (load-string (or conditions "#(identity %)"))
				  filtered (filter f @v-atom)
				  sorted (if order (sort (load-string order) filtered) filtered)]
				(take s (drop skip sorted)))))