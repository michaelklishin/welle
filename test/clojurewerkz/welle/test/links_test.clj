(ns clojurewerkz.welle.test.links-test
  (:require [clojurewerkz.welle.kv   :as kv]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.mr   :as mr]
            [clojure.test :refer :all]
            [clojurewerkz.welle.testkit :refer [drain]]
            [clojurewerkz.welle.test.test-helpers :as th]
            [clojurewerkz.welle.links :refer :all]))

(deftest ^{:links true} test-link-walking-with-a-single-step
  (let [conn        (th/connect)
        bucket-name "people"
        _           (wb/update conn bucket-name)]
    (drain conn bucket-name)
    (kv/store conn bucket-name "joe" {:name "Joe" :age 30} {:content-type "application/clojure"})
    (kv/store conn bucket-name "jane" {:name "Jane" :age 32}
              {:content-type "application/clojure"
               :links [{:bucket bucket-name :key "joe" :tag "friend"}]})
    (let [result (walk conn
                       (start-at "people" "jane")
                       (step     "people" "friend" true))]
      (is (= {:name "Joe" :age 30} (:value (ffirst result)))))
    (drain conn bucket-name)))
