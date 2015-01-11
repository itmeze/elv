(ns elv.storage)

(defprotocol LogStorage
	(store [this error])
	(retrive [this id])
	(search [this page size conditions order]))