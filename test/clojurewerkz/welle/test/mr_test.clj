(ns clojurewerkz.welle.test.mr-test
  (:use clojure.test
        [clojurewerkz.welle.testkit :only [drain]])
  (:require [clojurewerkz.welle.core :as wc]
            [clojurewerkz.welle.kv   :as kv]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.mr   :as mr]
            [clojurewerkz.support.js :as js])
  (:import  com.basho.riak.client.http.util.Constants))

(wc/connect!)

(deftest ^{:mr true} test-basic-map-reduce-with-erlang-builtins
  (let [bucket-name "clojurewerkz.welle.mr1"
        bucket      (wb/update bucket-name)]
    (doseq [l (range 0 200)]
      (kv/store bucket-name (str "java_" l) (str l) :content-type "text/plain"))
    (let [result (mr/map-reduce {:inputs bucket-name
                                 :query [{:map {:language "erlang"
                                               :module   "riak_kv_mapreduce"
                                               :function "map_object_value",
                                               :keep false}}
                                         {:reduce {:language "erlang"
                                                   :module "riak_kv_mapreduce"
                                                   :function "reduce_string_to_integer"}}
                                         {:reduce {:language "erlang"
                                                   :module "riak_kv_mapreduce"
                                                   :function "reduce_sort"}}]})]
      (is (= (vec (range 0 200)) result)))
    (drain bucket-name)))


(deftest ^{:mr true} test-basic-map-reduce-with-mixed-builtins
  (let [bucket-name "clojurewerkz.welle.mr2"
        bucket      (wb/update bucket-name)]
    (doseq [l (range 0 200)]
      (kv/store bucket-name (str "java_" l) (str l) :content-type "text/plain"))
    (let [result (mr/map-reduce {:inputs bucket-name
                                 :query [{:map {:language "erlang"
                                               :module   "riak_kv_mapreduce"
                                               :function "map_object_value",
                                               :keep false}}
                                         {:reduce {:language "erlang"
                                                   :module "riak_kv_mapreduce"
                                                   :function "reduce_string_to_integer"}}
                                         {:reduce {:language "javascript"
                                                   :name "Riak.reduceSum"}}]})]
      (is (= [19900] result)))
    (drain bucket-name)))


(deftest ^{:mr true} test-map-reduce-with-a-source-js-function
  (let [bucket-name "clojurewerkz.welle.mr3"
        _           (wb/update bucket-name)]
    (drain bucket-name)
    (kv/store bucket-name "1" {:state "CA" :quantity 1 :price 199.00} :content-type Constants/CTYPE_JSON_UTF8)
    (kv/store bucket-name "2" {:state "NY" :quantity 2 :price 199.00} :content-type Constants/CTYPE_JSON_UTF8)
    (kv/store bucket-name "3" {:state "NY" :quantity 1 :price 299.00} :content-type Constants/CTYPE_JSON_UTF8)
    (kv/store bucket-name "4" {:state "IL" :quantity 2 :price 11.50 } :content-type Constants/CTYPE_JSON_UTF8)
    (kv/store bucket-name "5" {:state "CA" :quantity 2 :price 2.95  } :content-type Constants/CTYPE_JSON_UTF8)
    (kv/store bucket-name "6" {:state "IL" :quantity 3 :price 5.50  } :content-type Constants/CTYPE_JSON_UTF8)
    (let [result (mr/map-reduce {:inputs bucket-name
                                 :query [{:map    {:language "javascript" :source (js/load-resource "js/fn1") :keep false}}
                                         {:reduce {:language "javascript" :name "Riak.reduceSum"}}]})]
      (is (= [941.4] result)))
    (kv/delete-all bucket-name ["1" "2" "3" "4" "5" "6"])))
