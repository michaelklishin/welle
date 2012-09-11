(ns clojurewerkz.welle.test.search-test
  (:use clojure.test
        [clojurewerkz.welle.testkit :only [drain]])
  (:require [clojurewerkz.welle.core :as wc]
            [clojurewerkz.welle.kv   :as kv]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.solr    :as wsolr])
  (:import  com.basho.riak.client.http.util.Constants))

(wc/connect!)

(deftest ^{:search true} test-term-query-via-the-solr-api
  (let [bucket-name "clojurewerkz.welle.solr.tweets"
        bucket      (wb/update bucket-name :last-write-wins true :enable-search true)
        doc         {:username  "clojurewerkz"
                     :text      "Elastisch beta3 is out, several more @elasticsearch features supported github.com/clojurewerkz/elastisch, improved docs http://clojureelasticsearch.info #clojure"
                     :timestamp "20120802T101232+0100"
                     :id        1}]
    (drain bucket-name)
    (kv/store bucket-name "a-key" doc :content-type "application/json")
    (let [result (wsolr/search bucket-name "username:clojurewerkz")
          hits   (wsolr/hits-from result)]
      (is (= "a-key" (-> hits first :id)))
      (is (> (count hits) 0)))
    (drain bucket-name)))
