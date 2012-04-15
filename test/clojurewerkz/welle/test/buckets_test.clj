(ns clojurewerkz.welle.test.buckets-test
  (:use     clojure.test)
  (:require [clojurewerkz.welle.core    :as wc]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.objects :as wo])
  (:import  [com.basho.riak.client IRiakClient IRiakObject]
            [com.basho.riak.client.bucket Bucket WriteBucket]
            [com.basho.riak.client.operations StoreObject FetchObject]))

(wc/connect!)


;;
;; buckets/create
;;

(deftest test-create-a-new-bucket-with-default-options
  (let [bucket-name "clojurewerkz.welle.buckets/create-bucket-1"
        bucket      (wb/create bucket-name)]
    (is (= (.getName bucket) bucket-name))))


(deftest test-create-a-new-bucket-with-allow-siblings
  (let [bucket-name "clojurewerkz.welle.buckets/create-bucket-2"
        bucket      (wb/create bucket-name :allow-siblings true)]
    (is (= (.getName bucket) bucket-name))))


(deftest test-create-a-new-bucket-with-last-write-wins
  (let [bucket-name "clojurewerkz.welle.buckets/create-bucket-3"
        bucket      (wb/create bucket-name :last-write-wins true)]
    (is (= (.getName bucket) bucket-name))))


(deftest test-create-a-new-bucket-with-explicitly-set-n-val
  (let [bucket-name "clojurewerkz.welle.buckets/create-bucket-4"
        bucket      (wb/create bucket-name :n-val 1)]
    (is (= (.getName bucket) bucket-name))))
