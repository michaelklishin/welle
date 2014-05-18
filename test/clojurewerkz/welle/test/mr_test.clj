(ns clojurewerkz.welle.test.mr-test
  (:require [clojurewerkz.welle.core :as wc]
            [clojurewerkz.welle.kv   :as kv]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.mr   :as mr]
            [clojurewerkz.support.js :as js]
            [clojure.test :refer :all]
            [clojurewerkz.welle.testkit :refer [drain]]
            [clojurewerkz.welle.test.test-helpers :as th])
  (:import  com.basho.riak.client.http.util.Constants))

(let [conn (th/connect)]
  (deftest ^{:mr true} test-basic-map-reduce-with-erlang-builtins
    (let [bucket-name "clojurewerkz.welle.mr1"
          bucket      (wb/update conn bucket-name {:last-write-wins true})
          store-fn    #(kv/store conn bucket-name (str "java_" %) (str %) {:content-type "text/plain"})]
      ;; Make sure we supply unordered values
      (doseq [l (range 100 200)] (store-fn l))
      (doseq [l (range 0 100)] (store-fn l))
      (let [result (mr/map-reduce conn {:inputs bucket-name
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
      (drain conn bucket-name)))


  (deftest ^{:mr true} test-basic-map-reduce-with-mixed-builtins
    (let [bucket-name "clojurewerkz.welle.mr2"
          bucket      (wb/update conn bucket-name)]
      (doseq [l (range 0 200)]
        (kv/store conn bucket-name (str "java_" l) (str l) {:content-type "text/plain"}))
      (let [result (mr/map-reduce conn {:inputs bucket-name
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
      (drain conn bucket-name)))


  (deftest ^{:mr true} test-map-reduce-with-a-source-js-function
    (let [bucket-name "clojurewerkz.welle.mr3"
          _           (wb/update conn bucket-name)]
      (drain conn bucket-name)
      (kv/store conn bucket-name "1" {:state "CA" :quantity 1 :price 199.00} {:content-type Constants/CTYPE_JSON_UTF8})
      (kv/store conn bucket-name "2" {:state "NY" :quantity 2 :price 199.00} {:content-type Constants/CTYPE_JSON_UTF8})
      (kv/store conn bucket-name "3" {:state "NY" :quantity 1 :price 299.00} {:content-type Constants/CTYPE_JSON_UTF8})
      (kv/store conn bucket-name "4" {:state "IL" :quantity 2 :price 11.50 } {:content-type Constants/CTYPE_JSON_UTF8})
      (kv/store conn bucket-name "5" {:state "CA" :quantity 2 :price 2.95  } {:content-type Constants/CTYPE_JSON_UTF8})
      (kv/store conn bucket-name "6" {:state "IL" :quantity 3 :price 5.50  } {:content-type Constants/CTYPE_JSON_UTF8})
      (let [result (mr/map-reduce conn {:inputs bucket-name
                                        :query [{:map    {:language "javascript" :source (js/load-resource "js/fn1") :keep false}}
                                                {:reduce {:language "javascript" :name "Riak.reduceSum"}}]})]
        (is (= [941.4] result)))
      (kv/delete-all bucket-name ["1" "2" "3" "4" "5" "6"])))


  ;; this tests needs Riak Search to be enabled
  (deftest ^{:mr true :search true} test-map-reduce-with-search-input-and-a-source-js-function
    (let [bucket-name "clojurewerkz.welle.mr4"
          _           (wb/update conn bucket-name {:enable-search true})]
      (drain conn bucket-name)
      (kv/store conn bucket-name "1" {:field "one"}   {:content-type Constants/CTYPE_JSON})
      (kv/store conn bucket-name "2" {:field "two"}   {:content-type Constants/CTYPE_JSON})
      (kv/store conn bucket-name "3" {:field "three"} {:content-type Constants/CTYPE_JSON})
      (kv/store conn bucket-name "4" {:field "four"}  {:content-type Constants/CTYPE_JSON})
      (kv/store conn bucket-name "5" {:field "five"}  {:content-type Constants/CTYPE_JSON})
      (kv/store conn bucket-name "6" {:field "six"}   {:content-type Constants/CTYPE_JSON})
      (let [result (mr/map-reduce conn {:inputs {:bucket bucket-name
                                                 :query "field:five"}
                                        :query [{:map {:language "javascript" :source "function(v) { return [v]; }" :keep true}}]})]
        (is (= bucket-name (-> result first :bucket)))
        (is (= "5" (-> result first :key))))
      (kv/delete-all conn bucket-name ["1" "2" "3" "4" "5" "6"])))

  (deftest ^{:mr true} test-map-reduce-with-reduce-slice
    (let [bucket-name "clojurewerkz.welle.mr5"
          bucket      (wb/update conn bucket-name {:last-write-wins true})]
      (dotimes [i 20]
        (kv/store conn bucket-name (str i) {:field i} {:content-type Constants/CTYPE_JSON}))
      (let [result (mr/map-reduce conn {:inputs bucket-name
                                        :query [{:map {:language "javascript" :name "Riak.mapValuesJson"}}
                                                {:reduce {:language "javascript" :name "Riak.reduceSort" :arg "function (a,b) {return parseInt(a.field) > parseInt(b.field)}"}}
                                                {:reduce {:language "javascript", :name "Riak.reduceSlice", :arg [0, 2]}}]})]
        (is (= [{:field 0} {:field 1}] result)))
      (drain conn bucket-name))))
