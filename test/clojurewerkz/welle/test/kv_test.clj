(ns clojurewerkz.welle.test.kv-test
  (:use     clojure.test
            [clojurewerkz.welle.testkit :only [drain]])
  (:require [clojurewerkz.welle.core    :as wc]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.kv      :as kv])
  (:import  com.basho.riak.client.http.util.Constants
            java.util.UUID))

(println (str "Using Clojure version " *clojure-version*))

(wc/connect-to-cluster! ["127.0.0.1" "localhost"])

(defn- is-riak-object
  [m]
  (is (:vclock m))
  (is (:vtag m))
  (is (:last-modified m))
  (is (:content-type m))
  (is (:metadata m))
  (is (:value m)))

;;
;; Basics
;;

(deftest test-basic-store-with-all-defaults
  (let [bucket-name "clojurewerkz.welle.kv/store-with-all-defaults"
        bucket      (wb/update bucket-name)
        k           "store-with-all-defaults"
        v           "value"
        stored      (kv/store bucket-name k v)
        [fetched]   (kv/fetch bucket-name k :r 1)]
    (is (empty? stored))
    (is (= Constants/CTYPE_OCTET_STREAM (:content-type fetched)))
    (is (= {} (:metadata fetched)))
    (is (= v (String. ^bytes (:value fetched))))
    (is-riak-object fetched)
    (drain bucket-name)))


;;
;; Automatic serialization/deserialization for common content types
;;

(deftest test-basic-store-with-text-utf8-content-type
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        k           "store-as-utf8-text"
        v           "value"
        stored      (kv/store bucket-name k v :content-type Constants/CTYPE_TEXT_UTF8)
        [fetched]   (kv/fetch bucket-name k)]
    (is (empty? stored))
    (is (= Constants/CTYPE_TEXT_UTF8 (:content-type fetched)))
    (is (= {} (:metadata fetched)))
    (is (= v (:value fetched)))
    (is-riak-object fetched)
    (drain bucket-name)))


(deftest test-basic-store-with-json-content-type
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        k           "store-as-json"
        v           {:name "Riak" :kind "Data store" :influenced-by #{"Dynamo"}}
        stored      (kv/store bucket-name k v :content-type Constants/CTYPE_JSON)
        [fetched]   (kv/fetch bucket-name k)]
    (is (empty? stored))
    (is (= Constants/CTYPE_JSON (:content-type fetched)))
    (is (= {} (:metadata fetched)))
    (is (= {:kind "Data store" :name "Riak" :influenced-by ["Dynamo"]} (:value fetched)))
    (is-riak-object fetched)
    (drain bucket-name)))


(deftest test-basic-store-with-json-utf8-content-type
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        k           "store-as-utf8-json"
        v           {:name "Riak" :kind "Data store" :influenced-by #{"Dynamo"}}
        stored      (kv/store bucket-name k v :content-type Constants/CTYPE_JSON_UTF8)
        [fetched]   (kv/fetch bucket-name k)]
    ;; cannot use constant value here see https://github.com/basho/riak-java-client/issues/125
    (is (= "application/json; charset=UTF-8"  (:content-type fetched)))
    (is (= {:kind "Data store" :name "Riak" :influenced-by ["Dynamo"]} (:value fetched)))
    (drain bucket-name)))


(deftest test-basic-store-with-jackson-smile-content-type
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        ct          "application/jackson-smile"
        k           "store-as-jackson-smile"
        v           {:name "Riak" :kind "Data store" :influenced-by #{"Dynamo"}}
        stored      (kv/store bucket-name k v :content-type ct)
        [fetched]   (kv/fetch bucket-name k)]
    (is (= ct  (:content-type fetched)))
    (is (= {:kind "Data store" :name "Riak" :influenced-by ["Dynamo"]} (:value fetched)))
    (drain bucket-name)))


(deftest test-basic-store-with-application-clojure-content-type
  (let [bucket-name "clojurewerkz.welle.kv2"
        bucket      (wb/update bucket-name :last-write-wins true)
        k           "store-as-clojure-data"
        v           {:city "New York City" :state "NY" :year 2011 :participants #{"johndoe" "timsmith" "michaelblack"}
                     :venue {:name "Sheraton New York Hotel & Towers" :address "811 Seventh Avenue" :street "Seventh Avenue"}}
        ct          "application/clojure"
        stored      (kv/store bucket-name k v :content-type ct)
        fetched     (kv/fetch-one bucket-name k)]
    ;; cannot use constant value here see https://github.com/basho/riak-java-client/issues/125
    (is (= ct  (:content-type fetched)))
    (is (= v (:value fetched)))
    (drain bucket-name)))


(deftest test-basic-store-with-json+gzip-content-type
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        k           "store-as-gzipped-json"
        v           {:name "Riak" :kind "Data store" :influenced-by #{"Dynamo"}}
        ;; compatible with both HTTP and PB APIs. Content-Encoding would be a better
        ;; idea here but PB cannot support it (as of Riak 1.1). MK.
        ct          "application/json+gzip"
        stored      (kv/store bucket-name k v :content-type ct)
        [fetched]   (kv/fetch bucket-name k)]
    (is (empty? stored))
    (is (= ct (:content-type fetched)))
    (is (= {} (:metadata fetched)))
    (is (= {:kind "Data store" :name "Riak" :influenced-by ["Dynamo"]} (:value fetched)))
    (is-riak-object fetched)
    (drain bucket-name)))


(deftest test-basic-store-with-text-utf8-content-type-and-return-body
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        k           "store-as-utf8-text"
        v           "value"
        stored      (kv/store bucket-name k v :content-type Constants/CTYPE_TEXT_UTF8 :return-body true)]
    (is (= v (-> stored first :value)))
    (drain bucket-name)))


;;
;; Metadata
;;

(deftest test-basic-store-with-metadata
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        k           (str (UUID/randomUUID))
        v           "value"
        ;; metadata values currently have to be strings. MK.
        metadata    {:author "Joe" :density "5"}
        stored      (kv/store bucket-name k v :content-type Constants/CTYPE_TEXT_UTF8 :metadata metadata)
        [fetched]   (kv/fetch bucket-name k)]
    (is (= Constants/CTYPE_TEXT_UTF8 (:content-type fetched)))
    (is (= {"author" "Joe" "density" "5"} (:metadata fetched)))
    (is (= v (:value fetched)))
    (is-riak-object fetched)
    (drain bucket-name)))


;;
;; Links
;;

(deftest test-basic-store-with-links
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        k           "store-with-links"
        v           "value"
        links       [{:bucket "pages" :key "clojurewerkz.org" :tag "links"}]
        stored      (kv/store bucket-name k v :content-type Constants/CTYPE_TEXT_UTF8 :links links)
        [fetched]   (kv/fetch bucket-name k)]
    (is (= Constants/CTYPE_TEXT_UTF8 (:content-type fetched)))
    (is (= links (:links fetched)))
    (is (= v (:value fetched)))
    (is-riak-object fetched)
    (drain bucket-name)))



;;
;; kv/fetch, kv/fetch-one
;;

(deftest test-fetching-a-non-existent-object
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        result      (kv/fetch bucket-name (str (UUID/randomUUID)))]
    (is (empty? result))))

(deftest test-optimistic-fetching-of-a-single-object
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        k           "optimistic-fetch"
        v           "value"
        stored      (kv/store bucket-name k v)
        fetched     (kv/fetch-one bucket-name k :r 1)]
    (is (= Constants/CTYPE_OCTET_STREAM (:content-type fetched)))
    (is (= {} (:metadata fetched)))
    (is (= v (String. ^bytes (:value fetched))))
    (is-riak-object fetched)
    (drain bucket-name)))

(deftest test-fetching-of-a-group-of-objects
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        k1          "multifetch-key1"
        k2          "multifetch-key2"
        stored1     (kv/store bucket-name k1 "value1")
        stored2     (kv/store bucket-name k2 "value2")
        xs          (kv/fetch-all bucket-name [k1 k2])
        ft1         (first xs)
        ft2         (last xs)]
    (is (= "value1" (String. ^bytes (:value ft1))))
    (is (= "value2" (String. ^bytes (:value ft2))))
    (doseq [o [ft1 ft2]]
      (is (= Constants/CTYPE_OCTET_STREAM (:content-type o)))
      (is (= {} (:metadata o)))
      (is-riak-object o))
    (drain bucket-name)))

;;
;; kv/delete
;;

(deftest test-fetching-deleted-value-with-rw=2
  (let [bucket-name "clojurewerkz.welle.kv3"
        bucket      (wb/update bucket-name :last-write-wins true)
        k           "delete-me"
        v           "another value"]
    (drain bucket-name)
    (Thread/sleep 150)
    (is (empty? (kv/fetch bucket-name k :r 2)))
    (kv/store bucket-name k v)
    (is (first (kv/fetch bucket-name k)))
    (kv/delete bucket-name k :rw 2)
    (is (empty? (kv/fetch bucket-name k :r 2)))))


(deftest test-fetching-deleted-value-with-bucket-settings
  (let [bucket-name "clojurewerkz.welle.kv4"
        bucket      (wb/update bucket-name)
        k           "delete-me"
        v           "another value"]
    (drain bucket-name)
    (Thread/sleep 150)
    (is (nil? (kv/fetch-one bucket-name k :r 2)))
    (kv/store bucket-name k v)
    (is (kv/fetch-one bucket-name k))
    (kv/delete bucket-name k :rw 2)
    (is (nil? (kv/fetch-one bucket-name k :r 2)))))

;;
;; store with options
;;

(deftest test-store-if-none-modified
  (let [bucket-name "clojurewerkz.welle.kv"
        key         "none-modified-key"
        _           (kv/store bucket-name key "value1")
        res         (kv/fetch-one bucket-name key)
        vclock      (:vclock res)
        _           (kv/store bucket-name key "value2")
        _           (kv/store bucket-name key "value3"
                              :vclock vclock
                              :if-none-modified true)
        res         (kv/fetch-one bucket-name key)]
    (is (= "value2" (String. ^bytes (:value res))))
    (drain bucket-name)))
