(ns clojurewerkz.welle.test.kv-test
  (:use     clojure.test
            [clojurewerkz.welle.testkit :only [drain]])
  (:require [clojurewerkz.welle.core        :as wc]
            [clojurewerkz.welle.conversion :as conversion]
            [clojurewerkz.welle.buckets     :as wb]
            [clojurewerkz.welle.kv          :as kv]
            [cheshire.custom                :as json]
            [clojure.set                    :as set])
  (:import  com.basho.riak.client.http.util.Constants
            java.util.UUID))

(println (str "Using Clojure version " *clojure-version*))

(wc/connect!)

(defn- is-riak-object
  [m]
  (is (:vclock m))
  (is (:vtag m))
  (is (:last-modified m))
  (is (:content-type m))
  (is (:metadata m))
  (is (:links m))
  (is (:indexes m))
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
        {:keys [result] :as m} (kv/fetch bucket-name k :r 1)
        fetched     (first result)]
    (is (not (:has-value? stored)))
    (is (not (:has-siblings? stored)))
    (is (:modified? stored))
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
        {:keys [result] :as m} (kv/fetch bucket-name k)
        ;; ex.: {:metadata {},
        ;;       :deleted? false,
        ;;       :content-type "text/plain; charset=UTF-8",
        ;;       :vtag "\"2IxTVEGoIW8DCRT25rLl83\"",
        ;;       :vclock #<BasicVClock com.basho.riak.client.cap.BasicVClock@67d53134>,
        ;;       :indexes {},
        ;;       :links (),
        ;;       :last-modified #inst "2013-05-22T17:17:30.000-00:00",
        ;;       :value "value"}
        fetched     (first result)]
    (is (not (:has-value? stored)))
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
        {:keys [result] :as m} (kv/fetch bucket-name k)
        fetched     (first result)]
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
        {:keys [result] :as m} (kv/fetch bucket-name k)
        fetched     (first result)]
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
        {:keys [result] :as m} (kv/fetch bucket-name k)
        fetched     (first result)]
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
        {:keys [result] :as m} (kv/fetch-one bucket-name k)]
    ;; cannot use constant value here see https://github.com/basho/riak-java-client/issues/125
    (is (= ct  (:content-type result)))
    (is (= v (:value result)))
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
        {:keys [result] :as m} (kv/fetch bucket-name k)
        fetched     (first result)]
    (is (not (:has-value? stored)))
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
        {:keys [result]}      (kv/store bucket-name k v :content-type Constants/CTYPE_TEXT_UTF8 :return-body true)]
    (is (= v (-> result first :value)))
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
        _           (kv/store bucket-name k v :content-type Constants/CTYPE_TEXT_UTF8 :metadata metadata)
        {:keys [result] :as m} (kv/fetch bucket-name k)
        fetched     (first result)]
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
        _           (kv/store bucket-name k v :content-type Constants/CTYPE_TEXT_UTF8 :links links)
        {:keys [result] :as m} (kv/fetch-one bucket-name k)]
    (is (= Constants/CTYPE_TEXT_UTF8 (:content-type result)))
    (is (= links (:links result)))
    (is (= v (:value result)))
    (is-riak-object result)
    (drain bucket-name)))



;;
;; kv/fetch, kv/fetch-one
;;

(deftest test-fetching-a-non-existent-object
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        {:keys [result]}           (kv/fetch bucket-name (str (UUID/randomUUID)))]
    (is (empty? result))))

(deftest test-optimistic-fetching-of-a-single-object
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        k           "optimistic-fetch"
        v           "value"
        _                (kv/store bucket-name k v)
        {:keys [result]} (kv/fetch-one bucket-name k :r 1)]
    (is (= Constants/CTYPE_OCTET_STREAM (:content-type result)))
    (is (= {} (:metadata result)))
    (is (= v (String. ^bytes (:value result))))
    (is-riak-object result)
    (drain bucket-name)))


(deftest test-fetch-one-with-skip-deserialize
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        ct          Constants/CTYPE_JSON
        k           "skip-deserialize-fetch-one"
        v           {:name "Riak"}
        _                (kv/store bucket-name k v :content-type ct)
        {:keys [result]} (kv/fetch-one bucket-name k :r 1 :skip-deserialize true)]
    (is (= ct (:content-type result)))
    (is (= {} (:metadata result)))
    (is (= (json/encode v) (String. ^bytes (:value result))))
    (is-riak-object result)
    (drain bucket-name)))

(deftest test-fetch-with-skip-deserialize
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        ct          Constants/CTYPE_JSON
        k           "skip-deserialize-fetch"
        v           {:name "Riak"}
        _                (kv/store bucket-name k v :content-type ct)
        {:keys [result]} (kv/fetch bucket-name k :r 1 :skip-deserialize true)
        fetched          (first result)]
    (println )
    (is (= ct (:content-type fetched)))
    (is (= {} (:metadata fetched)))
    (is (= (json/encode v) (String. ^bytes (:value fetched))))
    (is-riak-object fetched)
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
    (is (not (:has-value? (kv/fetch bucket-name k :r 2))))
    (kv/store bucket-name k v)
    (is (:has-value? (kv/fetch bucket-name k)))
    (kv/delete bucket-name k :rw 2)
    (kv/fetch bucket-name k :r 2)
    ;; {:vclock #<BasicVClock com.basho.riak.client.cap.BasicVClock@43bed599>,
    ;;  :has-siblings? false,
    ;;  :has-value? false,
    ;;  :deleted? false,
    ;;  :modified? true,
    ;;  :result ()}
    (is (not (:has-value? (kv/fetch bucket-name k :r 2))))))


(deftest test-fetching-deleted-value-with-bucket-settings
  (let [bucket-name "clojurewerkz.welle.kv4"
        bucket      (wb/update bucket-name)
        k           "delete-me"
        v           "another value"]
    (drain bucket-name)
    (Thread/sleep 150)
    (is (not (:has-value? (kv/fetch-one bucket-name k :r 2))))
    (kv/store bucket-name k v)
    (is (:has-value? (kv/fetch-one bucket-name k)))
    (kv/delete bucket-name k :rw 2)
    (is (not (:has-value? (kv/fetch-one bucket-name k :r 2))))))


(deftest test-fetching-multiple-deleted-values-with-bucket-settings
  (let [bucket-name "clojurewerkz.welle.kv5"
        bucket      (wb/update bucket-name)
        key         "delete-me"
        value       "another value"
        key-values  (map (fn [i] [(str key i) (str value i)]) (range 10))]
    (drain bucket-name)
    (Thread/sleep 150)

    (doseq [[k v] key-values]
      (is (not (:has-value? (kv/fetch-one bucket-name k))))
      (kv/store bucket-name k v)
      (is (:has-value? (kv/fetch-one bucket-name k))))
    (kv/delete-all bucket-name (map first key-values))
    (doseq [[k v] key-values]
      (is (not (:has-value? (kv/fetch-one bucket-name k)))))))

;;
;; kv/modify
;;

(deftest test-modify-with-json-content-type-and-no-existing-value
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        k           (str (UUID/randomUUID))
        f           (fn [[m]]
                      (update-in m [:value :influenced-by] set/union #{"Java" "Haskell"}))
        updated     (kv/modify bucket-name k f :r 1 :w 1 :content-type "application/json")
        {:keys [result]} (kv/fetch-one  bucket-name k)]
    (is (= Constants/CTYPE_JSON (:content-type result)))
    (is (= {} (:metadata result)))
    (is (= (sort ["Java" "Haskell"])
           (sort (get-in result [:value :influenced-by]))))
    (is-riak-object result)
    (drain bucket-name)))


(deftest test-modify-with-json-content-type-and-one-existing-value
  (let [bucket-name "clojurewerkz.welle.kv"
        bucket      (wb/update bucket-name)
        k           (str (UUID/randomUUID))
        v           {:name "Clojure" :kind "Programming Language" :influenced-by #{"Common Lisp", "C#"}}
        stored      (kv/store  bucket-name k v :content-type Constants/CTYPE_JSON)
        f           (fn [[m]]
                      (update-in m [:value :influenced-by] set/union #{"Java" "Haskell"}))
        _           (kv/modify bucket-name k f :r 1 :w 1)
        {:keys [result]} (kv/fetch-one  bucket-name k)]
    (is (empty? (:result stored)))
    (is (= Constants/CTYPE_JSON (:content-type result)))
    (is (= {} (:metadata result)))
    (is (= (sort ["C#" "Common Lisp" "Java" "Haskell"])
           (sort (get-in result [:value :influenced-by]))))
    (is-riak-object result)
    (drain bucket-name)))

(defn union-resolver
  [default]
  (conversion/resolver-from
   (fn [siblings]
     (condp = (count siblings)
       0 [{:value default
           :content-type "application/clojure"
           :metadata {}}]
       1 siblings
       [(-> (first siblings)
            (select-keys [:content-type :metadata :links])
            (assoc :value (apply set/union (map :value siblings))))]))))

(deftest test-modify-vclocks
  (let [bucket-name "clojurewerkz.welle.kv.siblings"
        bucket      (wb/update bucket-name :allow-siblings true)
        k           (str (UUID/randomUUID))
        append!     (fn [x] (kv/modify bucket-name k
                                       (fn [[o]]
                                         (update-in o [:value] conj x))
                                       :resolver (union-resolver #{})
                                       :pr 2
                                       :pw 2))
        adds             (doall (map append! (range 10)))
        {:keys [result]} (kv/fetch bucket-name k :pr 3)]
    ;; There should not be 10 siblings.
    (is (< (count result) 4))
    (drain bucket-name)))

(deftest test-counter
  (let [bucket-name "clojurewerkz.welle.kv"
        counter "counter1"
        bucket  (wb/update bucket-name :allow-siblings true)
        v1      (kv/increment-counter bucket-name counter)
        v2      (kv/fetch-counter bucket-name counter)
        v3      (kv/increment-counter bucket-name counter :value 2)
        v4      (kv/fetch-counter bucket-name counter)
        v5      (kv/increment-counter bucket-name counter :value -1)
        v6      (kv/fetch-counter bucket-name counter)]
    (is (= 1 v1))
    (is (= 1 v2))
    (is (= 3 v3))
    (is (= 3 v4))
    (is (= 2 v5))
    (is (= 2 v6))
    (drain bucket-name)))
