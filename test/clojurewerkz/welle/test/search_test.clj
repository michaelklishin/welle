(ns clojurewerkz.welle.test.search-test
  (:use clojure.test
        [clojurewerkz.welle.testkit :only [drain]])
  (:require [clojurewerkz.welle.core :as wc]
            [clojurewerkz.welle.kv   :as kv]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.solr    :as wsolr])
  (:import  com.basho.riak.client.http.util.Constants))

(wc/connect!)

(deftest ^{:search true} test-query-string-query-via-the-solr-api
  (let [bucket-name "clojurewerkz.welle.solr-search-api"
        bucket      (wb/update bucket-name :last-write-wins true :enable-search true)]
    (drain bucket-name)
    (kv/store bucket-name "1" {:field "one"}   :content-type Constants/CTYPE_JSON)
    (kv/store bucket-name "2" {:field "two"}   :content-type Constants/CTYPE_JSON)
    (kv/store bucket-name "3" {:field "three"} :content-type Constants/CTYPE_JSON)
    (kv/store bucket-name "4" {:field "four"}  :content-type Constants/CTYPE_JSON)
    (kv/store bucket-name "5" {:field "five"}  :content-type Constants/CTYPE_JSON)
    (kv/store bucket-name "6" {:field "six"}   :content-type Constants/CTYPE_JSON)
    (Thread/sleep 1000)
    (let [result (wsolr/search bucket-name "five")
          hits   (wsolr/hits-from result)]
      (println result)
      (println hits))
    (drain bucket-name)))
