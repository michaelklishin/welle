(ns welle.test.buckets
  (:use     [clojure.test])
  (:require [welle core buckets])
  (:import  (com.basho.riak.client.bucket WriteBucket)))

(defonce riak-client (welle.core/connect))

(deftest create-a-new-bucket-with-default-options
  (let [write-bucket (welle.buckets/create riak-client "welle.buckets/create-bucket-1")]
    (instance? WriteBucket write-bucket)))
