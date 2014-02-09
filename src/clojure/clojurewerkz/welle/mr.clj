;; Copyright (c) 2012-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.welle.mr
  (:require [cheshire.custom :as json]
            [clojurewerkz.welle.core :refer :all])
  (:import com.basho.riak.client.raw.query.MapReduceSpec))


;;
;; API
;;

(defn map-reduce
  "Runs a map/reduce query"
  [query]
  (let [result (.mapReduce *riak-client* (MapReduceSpec. (json/encode query)))]
    (json/decode (.getResultRaw result) true)))
