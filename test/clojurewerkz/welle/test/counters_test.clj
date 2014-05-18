(ns clojurewerkz.welle.test.counters-test
  (:require [clojurewerkz.welle.core        :as wc]
            [clojurewerkz.welle.conversion  :as conversion]
            [clojurewerkz.welle.buckets     :as wb]
            [clojurewerkz.welle.counters    :as cnt]
            [clojure.test :refer :all]
            [clojurewerkz.welle.testkit :refer [drain]]))

(deftest test-counter
  (let [conn        (wc/connect)
        bucket-name "clojurewerkz.welle.kv"
        counter "counter1"
        bucket  (wb/update conn bucket-name {:allow-siblings true})
        v1      (cnt/increment-counter conn bucket-name counter)
        v2      (cnt/fetch-counter conn bucket-name counter)
        v3      (cnt/increment-counter conn bucket-name counter {:value 2})
        v4      (cnt/fetch-counter conn bucket-name counter)
        v5      (cnt/increment-counter conn bucket-name counter {:value -1})
        v6      (cnt/fetch-counter conn bucket-name counter)]
    (is (= 1 v1))
    (is (= 1 v2))
    (is (= 3 v3))
    (is (= 3 v4))
    (is (= 2 v5))
    (is (= 2 v6))
    (drain conn bucket-name)))
