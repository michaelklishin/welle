(ns clojurewerkz.welle.test.links-test
  (:require [clojurewerkz.welle.core :as wc]
            [clojurewerkz.welle.kv   :as kv]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.mr   :as mr])
  (:use clojure.test
        [clojurewerkz.welle.testkit :only [drain]]
        clojurewerkz.welle.links))

(wc/connect!)


(deftest ^{:links true} test-link-walking-with-a-single-step
  (let [bucket-name "people"
        _           (wb/update bucket-name)]
    (drain bucket-name)
    (kv/store bucket-name "joe" {:name "Joe" :age 30} :content-type "application/clojure")
    (kv/store bucket-name "jane" {:name "Jane" :age 32}
              :content-type "application/clojure"
              :links [{:bucket bucket-name :key "joe" :tag "friend"}])
    (let [result (walk
                  (start-at "people" "jane")
                  (step     "people" "friend" true))]
      (is (= {:name "Joe" :age 30} (:value (ffirst result)))))
    (drain bucket-name)))
  