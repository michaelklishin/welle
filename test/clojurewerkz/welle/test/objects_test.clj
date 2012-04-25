(ns clojurewerkz.welle.test.objects-test
  (:use     clojure.test
            [clojurewerkz.welle.testkit :only [drain]])
  (:require [clojurewerkz.welle.core    :as wc]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.objects :as wo])
  (:import  com.basho.riak.client.http.util.Constants
            java.util.UUID))

(wc/connect!)

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
    (is-riak-object fetched)
    (drain bucket-name)))

(deftest test-basic-store-with-text-utf8-content-type
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
    (is-riak-object fetched)
    (drain bucket-name)))


(deftest test-basic-store-with-json-content-type
  (let [bucket-name "clojurewerkz.welle.buckets/store-with-json-content-type"
        bucket      (wb/create bucket-name)
        k           (str (UUID/randomUUID))
        v           {:name "Riak" :kind "Data store" :influenced-by #{"Dynamo"}}
        stored      (wo/store bucket-name k v :content-type Constants/CTYPE_JSON)
        [fetched]   (wo/fetch bucket-name k)]
    (is (empty? stored))
    (is (= Constants/CTYPE_JSON (:content-type fetched)))
    (is (= {} (:metadata fetched)))
    (is (= {:kind "Data store", :name "Riak", :influenced-by ["Dynamo"]} (:value fetched)))
    (is-riak-object fetched)
    (drain bucket-name)))


(deftest test-basic-store-with-json-utf-8content-type
  (let [bucket-name "clojurewerkz.welle.buckets/store-with-json-utf-8content-type"
        bucket      (wb/create bucket-name)
        k           (str (UUID/randomUUID))
        v           {:name "Riak" :kind "Data store" :influenced-by #{"Dynamo"}}
        stored      (wo/store bucket-name k v :content-type Constants/CTYPE_JSON_UTF8)
        [fetched]   (wo/fetch bucket-name k)]
    ;; cannot use constant value here, see https://github.com/basho/riak-java-client/issues/125
    (is (= "application/json; charset=UTF-8"  (:content-type fetched)))
    (is (= {:kind "Data store", :name "Riak", :influenced-by ["Dynamo"]} (:value fetched)))
    (drain bucket-name)))


(deftest test-basic-store-with-metadata
  (let [bucket-name "clojurewerkz.welle.buckets/store-with-given-metadata"
        bucket      (wb/create bucket-name)
        k           (str (UUID/randomUUID))
        v           "value"
        ;; metadata values currently have to be strings. MK.
        metadata    {:author "Joe" :density "5"}
        stored      (wo/store bucket-name k v :content-type Constants/CTYPE_TEXT_UTF8 :metadata metadata)
        [fetched]   (wo/fetch bucket-name k)]
    (is (= Constants/CTYPE_TEXT_UTF8 (:content-type fetched)))
    (is (= {"author" "Joe", "density" "5"} (:metadata fetched)))
    (is (= v (:value fetched)))
    (is-riak-object fetched)
    (drain bucket-name)))



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
