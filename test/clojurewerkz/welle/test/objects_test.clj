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
  [^String bucket-name]
  (doseq [k (wb/keys-in bucket-name)]
    (wo/delete bucket-name k)))

(defn- is-riak-object
  [m]
  (is (:vclock m))
  (is (:vtag m))
  (is (:last-modified m))
  (is (:content-type m))
  (is (:metadata m))
  (is (:value m)))

;;
;; objects/store
;;

(deftest test-basic-store-with-all-defaults
  (let [bucket-name "clojurewerkz.welle.buckets/store-with-all-defaults"
        bucket      (wb/create bucket-name)
        k           (str (UUID/randomUUID))
        v           "value"
        stored      (wo/store bucket-name k v)
        [fetched]   (wo/fetch bucket-name k :r 1)]
    (is (empty? stored))
    (is (= Constants/CTYPE_OCTET_STREAM (:content-type fetched)))
    (is (= {} (:metadata fetched)))
    (is (= v (String. ^bytes (:value fetched))))
    (is-riak-object fetched)))

(deftest test-basic-store-with-given-content-type
  (let [bucket-name "clojurewerkz.welle.buckets/store-with-given-content-type"
        bucket      (wb/create bucket-name)
        k           (str (UUID/randomUUID))
        v           "value"
        stored      (wo/store bucket-name k v :content-type Constants/CTYPE_TEXT_UTF8)
        [fetched]   (wo/fetch bucket-name k)]
    (is (empty? stored))
    (is (= Constants/CTYPE_TEXT_UTF8 (:content-type fetched)))
    (is (= {} (:metadata fetched)))
    (is (= v (:value fetched)))
    (is-riak-object fetched)))




;;
;; objects/store, objects/fetch
;;

(deftest fetching-a-non-existent-object
  (let [bucket-name "clojurewerkz.welle.buckets/fetch-a-non-existent-object-1"
        bucket      (wb/create bucket-name)
        result      (wo/fetch bucket-name (str (UUID/randomUUID)))]
    (is (empty? result))))


;;
;; objects/delete
;;

(deftest fetch-deleted-value
  (let [bucket-name "clojurewerkz.welle.buckets/fetch-deleted-value"
        bucket      (wb/create bucket-name)
        k           (str (UUID/randomUUID))
        v           "another value"]
    (drain bucket-name)
    (is (empty? (wo/fetch bucket-name k)))
    (wo/store bucket-name k v)
    (is (first (wo/fetch bucket-name k)))
    (wo/delete bucket-name k :w 1)
    ;; TODO: need to investigate why fetch does not return empty results here.
    #_ (is (empty? (wo/fetch bucket-name k)))))
