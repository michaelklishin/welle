(ns welle.test.buckets
  (:use     [clojure.test])
  (:require [welle core buckets objects])
  (:import  (com.basho.riak.client IRiakClient IRiakObject)
            (com.basho.riak.client.bucket Bucket WriteBucket)
            (com.basho.riak.client.operations StoreObject FetchObject)))

(welle.core/connect!)



;;
;; buckets/store, buckets/fetch
;;

(deftest basic-store-followed-by-a-fetch
  (let [bucket-name "welle.buckets/store-then-fetch-1"
        bucket      (welle.buckets/create bucket-name)
        k           "key"
        v           "value"]
    (welle.objects/store bucket k (.getBytes "value"))
    (is (= v
           (.getValueAsString (welle.objects/fetch bucket k))))))
