(ns clojurewerkz.welle.mr
  (:require [cheshire.custom :as json])
  (:use clojurewerkz.welle.core)
  (:import com.basho.riak.client.raw.query.MapReduceSpec))


;;
;; API
;;

(defn map-reduce
  "Runs a map/reduce query"
  [query]
  (let [result (.mapReduce *riak-client* (MapReduceSpec. (json/encode query)))]
    (json/decode (.getResultRaw result) true)))
