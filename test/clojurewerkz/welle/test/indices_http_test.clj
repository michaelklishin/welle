(ns clojurewerkz.welle.test.indices-http-test
  (:use clojure.test
        [clojurewerkz.welle.testkit :only [drain]]
        [clojure.set :only [subset?]])
  (:require [clojurewerkz.welle.core    :as wc]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.kv      :as kv])
  (:import java.util.UUID
           com.basho.riak.client.http.util.Constants))

(wc/connect!)

(deftest ^{:2i true} test-indexes-on-converted-riak-objects
  (let [bucket-name "clojurewerkz.welle.test.indices-http-test"
        bucket      (wb/update bucket-name)
        k           (str (UUID/randomUUID))
        v           "value"
        indexes     {:email    #{"john@example.com"}
                     :username #{"johndoe"}}
        stored      (kv/store bucket-name k v :indexes indexes)
        [fetched]   (kv/fetch bucket-name k)]
    (is (:indexes fetched))
    (is (= indexes (:indexes fetched)))
    (kv/delete bucket-name k)))


(deftest ^{:2i true} test-basic-index-query-with-a-single-string-value
  (let [bucket-name "clojurewerkz.welle.test.indices-http-test"
        bucket      (wb/update bucket-name)
        k           (str (UUID/randomUUID))
        v           (.getBytes "value")
        indexes     {:email #{"johndoe@example.com" "timsmith@example.com"}}
        stored      (kv/store bucket-name k v :indexes indexes :content-type Constants/CTYPE_OCTET_STREAM)
        [idx-key]   (kv/index-query bucket-name :email "johndoe@example.com")
        [fetched]   (kv/fetch bucket-name idx-key)]
    (is (:indexes fetched))
    (is (= (String. ^bytes (:value fetched))
           (String. ^bytes v)))
    (is (= (:indexes fetched) indexes))
    (kv/delete bucket-name k)))


(deftest ^{:2i true} test-basic-index-query-with-a-range-of-string-values
  (let [bucket-name "clojurewerkz.welle.test.indices-http-test"
        bucket      (wb/update bucket-name)
        k1          (str (UUID/randomUUID))
        k2          (str (UUID/randomUUID))
        k3          (str (UUID/randomUUID))
        k4          (str (UUID/randomUUID))
        v           (.getBytes "value1")
        _           (kv/store bucket-name k1 v :indexes {:username #{"abc"}} :content-type Constants/CTYPE_OCTET_STREAM)
        _           (kv/store bucket-name k2 v :indexes {:username #{"bcd"}} :content-type Constants/CTYPE_OCTET_STREAM)
        _           (kv/store bucket-name k3 v :indexes {:username #{"cde"}} :content-type Constants/CTYPE_OCTET_STREAM)
        _           (kv/store bucket-name k4 v :indexes {:username #{"def"}} :content-type Constants/CTYPE_OCTET_STREAM)
        keys        (set (kv/index-query bucket-name :username ["b" "d"]))]
    (is (subset? #{k2 k3} keys))
    (kv/delete-all bucket-name [k1 k2 k3 k4])
    (kv/delete-all bucket-name keys)))


(deftest ^{:2i true} test-basic-index-query-with-a-single-integer-value
  (let [bucket-name "clojurewerkz.welle.test.alt-indices-test"
        bucket      (wb/update bucket-name)
        k           (str (UUID/randomUUID))
        v           "value"
        indexes     {:age 27}
        stored      (kv/store bucket-name k v :indexes indexes :content-type Constants/CTYPE_TEXT_UTF8)
        [idx-key]   (kv/index-query bucket-name :age 27)
        [fetched]   (kv/fetch bucket-name idx-key)]
    (is (:indexes fetched))
    (is (= (:value fetched) v))
    (is (= (:indexes fetched) {:age #{27}}))
    (kv/delete bucket-name k)))


(deftest ^{:2i true} test-basic-index-query-with-a-range-of-integer-values
  (let [bucket-name "clojurewerkz.welle.test.indices-http-test"
        bucket      (wb/update bucket-name)
        k1          (str (UUID/randomUUID))
        k2          (str (UUID/randomUUID))
        k3          (str (UUID/randomUUID))
        k4          (str (UUID/randomUUID))
        v           (.getBytes "value1")
        _           (kv/store bucket-name k1 v :indexes {:hops #{1 2 3 4}} :content-type Constants/CTYPE_OCTET_STREAM)
        _           (kv/store bucket-name k2 v :indexes {:hops #{5 6 7 8}} :content-type Constants/CTYPE_OCTET_STREAM)
        _           (kv/store bucket-name k3 v :indexes {:hops #{9 10 11 12}} :content-type Constants/CTYPE_OCTET_STREAM)
        _           (kv/store bucket-name k4 v :indexes {:hops #{13 14 18 77}} :content-type Constants/CTYPE_OCTET_STREAM)
        keys        (set (kv/index-query bucket-name :hops [2 11]))]
    (is (subset? #{k1 k2 k3} keys))
    (kv/delete-all bucket-name [k1 k2 k3 k4])
    (kv/delete-all bucket-name keys)))
