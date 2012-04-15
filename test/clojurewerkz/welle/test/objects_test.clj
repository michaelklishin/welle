(ns clojurewerkz.welle.test.objects-test
  (:use     clojure.test)
  (:require [clojurewerkz.welle.core    :as wc]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.objects :as wo])
  (:import  [com.basho.riak.client IRiakClient IRiakObject]
            [com.basho.riak.client.bucket Bucket WriteBucket]
            [com.basho.riak.client.operations StoreObject FetchObject]
            java.util.UUID))

(wc/connect!)



;;
;; objects/store, objects/fetch
;;

(deftest test-basic-store-followed-by-a-fetch-with-r=1
  (let [bucket-name "clojurewerkz.welle.buckets/store-then-fetch-1-with-r=1"
        bucket      (wb/create bucket-name)
        k           "key"
        v           "value"]
    (wo/store bucket k (.getBytes v))
    (is (= v
           (.getValueAsString (wo/fetch bucket k :r 1))))))

(deftest test-basic-store-followed-by-a-fetch-with-pr=1
  (let [bucket-name "clojurewerkz.welle.buckets/store-then-fetch-2-with-pr=1"
        bucket      (wb/create bucket-name)
        k           "key"
        v           "another value"]
    (wo/store bucket k (.getBytes v))
    (is (= v
           (.getValueAsString (wo/fetch bucket k :pr 1))))))

(deftest fetching-a-non-existent-object
  (let [bucket-name "clojurewerkz.welle.buckets/fetch-a-non-existent-object-1"
        bucket      (wb/create bucket-name)
        result      (wo/fetch bucket (str (UUID/randomUUID)))]
    (is (nil? result))))


;;
;; objects/delete
;;

(deftest fetch-deleted-value
  (let [bucket-name "clojurewerkz.welle.buckets/fetch-deleted-value"
        bucket      (wb/create bucket-name)
        k           "key"
        v           "another value"]
    (wo/store bucket k (.getBytes v))
    (is (= v (.getValueAsString (wo/fetch bucket k))))
    (wo/delete bucket k)
    (is (nil? (wo/fetch bucket k)))))
