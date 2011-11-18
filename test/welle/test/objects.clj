(ns welle.test.objects
  (:use     [clojure.test])
  (:require [welle core buckets objects])
  (:import  [com.basho.riak.client IRiakClient IRiakObject]
            [com.basho.riak.client.bucket Bucket WriteBucket]
            [com.basho.riak.client.operations StoreObject FetchObject]
            [java.util UUID]))

(welle.core/connect!)



;;
;; objects/store, objects/fetch
;;

(deftest basic-store-followed-by-a-fetch-with-r=1
  (let [bucket-name "welle.buckets/store-then-fetch-1-with-r=1"
        bucket      (welle.buckets/create bucket-name)
        k           "key"
        v           "value"]
    (welle.objects/store bucket k (.getBytes v))
    (is (= v
           (.getValueAsString (welle.objects/fetch bucket k :r 1))))))

(deftest basic-store-followed-by-a-fetch-with-pr=1
  (let [bucket-name "welle.buckets/store-then-fetch-2-with-pr=1"
        bucket      (welle.buckets/create bucket-name)
        k           "key"
        v           "another value"]
    (welle.objects/store bucket k (.getBytes v))
    (is (= v
           (.getValueAsString (welle.objects/fetch bucket k :pr 1))))))

(deftest fetching-a-non-existent-object
  (let [bucket-name "welle.buckets/fetch-a-non-existent-object-1"
        bucket      (welle.buckets/create bucket-name)
        result      (welle.objects/fetch bucket (str (UUID/randomUUID)))]
    (is (nil? result))))


;;
;; objects/delete
;;

(deftest fetch-deleted-value
  (let [bucket-name "welle.buckets/fetch-deleted-value"
        bucket      (welle.buckets/create bucket-name)
        k           "key"
        v           "another value"]
    (welle.objects/store bucket k (.getBytes v))
    (is (= v (.getValueAsString (welle.objects/fetch bucket k))))
    (welle.objects/delete bucket k)
    (is (nil? (welle.objects/fetch bucket k)))))
