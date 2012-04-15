(ns clojurewerkz.welle.test.objects-test
  (:use     clojure.test)
  (:require [clojurewerkz.welle.core    :as wc]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.objects :as wo])
  (:import  [com.basho.riak.client IRiakClient IRiakObject]
            [com.basho.riak.client.bucket Bucket WriteBucket]
            [com.basho.riak.client.operations StoreObject FetchObject]
            com.basho.riak.client.http.util.Constants
            java.util.UUID))

(wc/connect!)

(defn- drain
  [^Bucket bucket]
  (doseq [k (wb/keys-in bucket)]
    (wo/delete bucket k)))


;;
;; objects/store, objects/fetch
;;

(deftest test-basic-store-followed-by-a-fetch-with-r=1
  (let [bucket-name "clojurewerkz.welle.buckets/store-then-fetch-1-with-r=1"
        bucket      (wb/create bucket-name)
        k           (str (UUID/randomUUID))
        v           "value"]
    (wo/store bucket k (.getBytes v))
    (is (= v
           (.getValueAsString (wo/fetch bucket k :r 1))))))

(deftest test-basic-store-followed-by-a-fetch-with-pr=1
  (let [bucket-name "clojurewerkz.welle.buckets/store-then-fetch-2-with-pr=1"
        bucket      (wb/create bucket-name)
        k           (str (UUID/randomUUID))
        v           "another value"]
    (wo/store bucket k (.getBytes v))
    (is (= v
           (.getValueAsString (wo/fetch bucket k :pr 1))))))

(deftest fetching-a-non-existent-object
  (let [bucket-name "clojurewerkz.welle.buckets/fetch-a-non-existent-object-1"
        bucket      (wb/create bucket-name)
        result      (wo/fetch bucket (str (UUID/randomUUID)))]
    (is (nil? result))))

;; see ITestBucket#basicStore in the Java client test suite.
(deftest test-integration-case1
  (let [bucket-name (str (UUID/randomUUID))
        bucket      (wb/create bucket-name)
        _           (drain bucket)
        k           "k"
        v1          "v"
        v2          "a new value"]
    (wo/store bucket k v1)
    (let [o1 (wo/fetch bucket k)]
      (is (= v1 (.getValueAsString o1)))
      (is (= Constants/CTYPE_OCTET_STREAM (.getContentType o1))))
    (wo/store bucket k v2)
    (let [o2 (wo/fetch bucket k)]
      (is (= v2 (.getValueAsString o2)))
      (is (= Constants/CTYPE_OCTET_STREAM (.getContentType o2))))))


;;
;; objects/delete
;;

(deftest fetch-deleted-value
  (let [bucket-name "clojurewerkz.welle.buckets/fetch-deleted-value"
        bucket      (wb/create bucket-name)
        k           (str (UUID/randomUUID))
        v           "another value"]
    (wo/store bucket k (.getBytes v))
    (is (= v (.getValueAsString (wo/fetch bucket k))))
    (wo/delete bucket k)
    (is (nil? (wo/fetch bucket k)))))
