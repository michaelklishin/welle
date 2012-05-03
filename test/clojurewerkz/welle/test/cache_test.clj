(ns clojurewerkz.welle.test.cache-test
  (:require [clojurewerkz.welle.core    :as wc]
            [clojurewerkz.welle.buckets :as wb])
  (:use clojure.core.cache clojure.test clojurewerkz.welle.cache
        [clojurewerkz.welle.testkit :only [drain]])
  (:import [clojure.core.cache BasicCache FIFOCache LRUCache TTLCache]
           java.util.UUID))

(wc/connect!)

;;
;; Playground/Tests. These were necessary because clojure.core.cache has
;; little documentation, incomplete test suite and
;; slightly non-standard (although necessary to support all those cache variations)
;; cache operations protocol.
;;
;; This is by no means clear or complete either but it did the job of helping me
;; explore the API.

(deftest ^{:cache true}
  test-has?-with-basic-cache
  (testing "that has? returns false for misses"
    (let [c (BasicCache. {})]
      (are [v] (is (false? (has? c v)))
           :missing-key
           "missing-key"
           (gensym "missing-key"))))
  (testing "that has? returns true for hits"
    (let [c (BasicCache. {:skey "Value" :lkey (Long/valueOf 10000) "kkey" :keyword})]
      (are [v] (is (has? c v))
           :skey
           :lkey
           "kkey"))))


(deftest ^{:cache true}
  test-lookup-with-basic-cache
  (testing "that lookup returns nil for misses"
    (let [c (BasicCache. {})]
      (are [v] (is (nil? (lookup c v)))
           :missing-key
           "missing-key"
           (gensym "missing-key"))))
  (testing "that lookup returns cached values for hits"
    (let [l (Long/valueOf 10000)
          c (BasicCache. {:skey "Value" :lkey l "kkey" :keyword})]
      (are [v k] (is (= v (lookup c k)))
           "Value"   :skey
           l         :lkey
           :keyword  "kkey"))))

(deftest ^{:cache true}
  test-evict-with-basic-cache
  (testing "that evict has no effect for keys that do not exist"
    (let [c (atom (BasicCache. {:a 1 :b 2}))]
      (swap! c evict :missing-key)
      (is (has? @c :a))
      (is (has? @c :b))))
  (testing "that evict removes keys that did exist"
    (let [c (atom (BasicCache. {:skey "Value" "kkey" :keyword}))]
      (is (has? @c :skey))
      (is (= "Value"  (lookup @c :skey)))
      (swap! c evict :skey)
      (is (not (has? @c :skey)))
      (is (= nil  (lookup @c :skey)))
      (is (has? @c "kkey"))
      (is (= :keyword (lookup @c "kkey"))))))

(deftest ^{:cache true}
  test-seed-with-basic-cache
  (testing "that seed returns a new value"
    (let [c (atom (BasicCache. {}))]
      (swap! c seed {:a 1 :b "b" "c" :d})
      (are [k v] (do
                   (is (has? @c k))
                   (is (= v (lookup @c k))))
           :a 1
           :b "b"
           "c" :d))))


;;
;; Tests
;;

(def ^{:private true :const true} bucket-name "welle.test.cache_entries")
(use-fixtures :each (fn [f]
                      (wb/update bucket-name :last-write-wins true :r 1 :w 1)
                      (f)
                      (drain bucket-name)))


(deftest ^{:cache true}
  test-has?-with-basic-welle-cache
  (testing "that has? returns false for misses"
    (let [c    (basic-welle-cache-factory bucket-name)]
      (is (not (has? c (str (UUID/randomUUID)))))
      (is (not (has? c (str (UUID/randomUUID)))))))
  (testing "that has? returns true for hits"
    (let [c    (basic-welle-cache-factory bucket-name {"a" 1 "b" "cache" "c" 3/4} "application/json" 1)]
      (is (has? c "a"))
      (is (has? c "b"))
      (is (has? c "c"))
      (is (not (has? c "d"))))))


(deftest ^{:cache true}
  test-lookup-with-basic-welle-cache
  (testing "that lookup returns nil for misses"
    (let [c    (basic-welle-cache-factory bucket-name)]
      (are [v] (is (nil? (lookup c v)))
           (str (UUID/randomUUID))
           "missing-key"
           (str (gensym "missing-key")))))
  (testing "that lookup returns cached values for hits"
    (let [l (Long/valueOf 10000)
          c    (basic-welle-cache-factory bucket-name {"skey" "Value" "lkey" l} "application/json" 1)]
      (are [k v] (is (= v (lookup c k)))
           "skey" "Value"
           "lkey" l ))))
