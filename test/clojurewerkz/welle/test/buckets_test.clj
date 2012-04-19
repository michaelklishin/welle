(ns clojurewerkz.welle.test.buckets-test
  (:use     clojure.test clojurewerkz.welle.conversion)
  (:require [clojurewerkz.welle.core    :as wc]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.objects :as wo])
  (:import  [com.basho.riak.client IRiakClient IRiakObject]
            [com.basho.riak.client.bucket Bucket WriteBucket]
            [com.basho.riak.client.operations StoreObject FetchObject]))

(wc/connect!)

(defn- has-bucket-props
  [props]
  (doseq [prop [:allow-siblings :last-write-wins :r :w :pr :dw :rw :pw :search :n-val :backend
                :not-found-ok :small-vclock :big-vclock :young-vclock :old-vclock]]
    (is (contains? props prop))))

;;
;; buckets/create
;;

(deftest test-create-a-new-bucket-with-default-options
  (let [bucket-name  "clojurewerkz.welle.buckets/create-bucket-1"
        bucket-props (wb/create bucket-name)]
    (has-bucket-props bucket-props)))


(deftest test-create-a-new-bucket-with-allow-siblings
  (let [bucket-name  "clojurewerkz.welle.buckets/create-bucket-2"
        bucket-props (wb/create bucket-name :allow-siblings true)]
    (has-bucket-props bucket-props)
    (is (:allow-siblings bucket-props))))


(deftest test-create-a-new-bucket-with-last-write-wins
  (let [bucket-name  "clojurewerkz.welle.buckets/create-bucket-3"
        bucket-props (wb/create bucket-name :last-write-wins true)]
    (has-bucket-props bucket-props)
    (is (:last-write-wins bucket-props))))


(deftest test-create-a-new-bucket-with-explicitly-set-n-val
  (let [bucket-name  "clojurewerkz.welle.buckets/create-bucket-4"
        bucket-props (wb/create bucket-name :n-val 4)]
    (has-bucket-props bucket-props)
    (is (= 4 (:n-val bucket-props)))))


(deftest test-create-a-new-bucket-with-explicitly-cap-values
  (let [bucket-name  "clojurewerkz.welle.buckets/create-bucket-5"
        bucket-props (wb/create bucket-name :r 1 :pr 2 :w 3 :dw 4 :pw 5 :rw 6)]
    (has-bucket-props bucket-props)
    (are [k v] (is (= (to-quorum v) (k bucket-props)))
      :r 1 :pr 2 :w 3 :dw 4 :pw 5 :rw 6)))
