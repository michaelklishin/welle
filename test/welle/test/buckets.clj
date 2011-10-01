(ns welle.test.buckets
  (:use     [clojure.test])
  (:require [welle core buckets objects])
  (:import  (com.basho.riak.client IRiakClient IRiakObject)
            (com.basho.riak.client.bucket Bucket WriteBucket)
            (com.basho.riak.client.operations StoreObject FetchObject)))

(welle.core/connect!)


;;
;; buckets/create
;;

(deftest create-a-new-bucket-with-default-options
  (let [bucket-name "welle.buckets/create-bucket-1"
        bucket      (welle.buckets/create bucket-name)]
    (is (= (.getName bucket) bucket-name))))


(deftest create-a-new-bucket-with-allow-siblings
  (let [bucket-name "welle.buckets/create-bucket-2"
        bucket      (welle.buckets/create bucket-name :allow-siblings true)]
    (is (= (.getName bucket) bucket-name))))


(deftest create-a-new-bucket-with-last-write-wins
  (let [bucket-name "welle.buckets/create-bucket-3"
        bucket      (welle.buckets/create bucket-name :last-write-wins true)]
    (is (= (.getName bucket) bucket-name))))


(deftest create-a-new-bucket-with-explicitly-set-n-val
  (let [bucket-name "welle.buckets/create-bucket-4"
        bucket      (welle.buckets/create bucket-name :n-val 1)]
    (is (= (.getName bucket) bucket-name))))
