(ns clojurewerkz.welle.mr
  (:require [clojure.data.json :as json])
  (:use clojurewerkz.welle.core)
  (:import com.basho.riak.client.raw.query.MapReduceSpec))


;;
;; API
;;

(defn map-reduce
  "Runs a map/reduce query"
  [query]
  (let [result (.mapReduce *riak-client* (MapReduceSpec. (json/json-str query)))]
    (json/read-json (.getResultRaw result))))