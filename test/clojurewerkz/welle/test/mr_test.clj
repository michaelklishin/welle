(ns clojurewerkz.welle.test.mr-test
  (:use clojure.test
        [clojurewerkz.welle.testkit :only [drain]])
  (:require [clojurewerkz.welle.core :as wc]
            [clojurewerkz.welle.kv   :as kv]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.mr   :as mr])
  (:import  com.basho.riak.client.http.util.Constants
            java.util.UUID))

(wc/connect!)

(deftest ^{:mr true} test-basic-map-reduce-with-erlang-builtins
  (let [bucket-name "clojurewerkz.welle.test.mr-test"
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
  (let [bucket-name "clojurewerkz.welle.test.mr-test"
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