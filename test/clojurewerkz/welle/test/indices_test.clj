(ns clojurewerkz.welle.test.indices-test
  (:use clojure.test
        [clojurewerkz.welle.testkit :only [drain]])
  (:require [clojurewerkz.welle.core    :as wc]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.objects :as wo])
  (:import java.util.UUID))

(wc/connect!)

(deftest ^{:2i true} test-indexes-on-converted-riak-objects
  (let [bucket-name "clojurewerkz.welle.test.indices-test"
        bucket      (wb/create bucket-name)
        k           (str (UUID/randomUUID))
        v           "value"
        indexes     {:email    #{"john@example.com"}
                     :username #{"johndoe"}}
        stored      (wo/store bucket-name k v :indexes indexes)
        [fetched]   (wo/fetch bucket-name k)]
    (is (:indexes fetched))
    (is (= indexes (:indexes fetched)))))
