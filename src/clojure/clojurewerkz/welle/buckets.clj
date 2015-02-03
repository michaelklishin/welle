;; Copyright (c) 2012-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.welle.buckets
  (:refer-clojure :exclude [list update])
  (:require [clojurewerkz.welle.core :refer :all]
            [clojurewerkz.welle.conversion :refer :all])
  (:import com.basho.riak.client.raw.RawClient
           [com.basho.riak.client IRiakClient IRiakObject]
           [com.basho.riak.client.bucket Bucket WriteBucket]
           [com.basho.riak.client.http.response BucketResponse ListBucketsResponse]
           [com.basho.riak.client.operations StoreObject FetchObject]
           [com.basho.riak.client.cap ConflictResolver Retrier]
           [com.basho.riak.client.convert Converter]
           [com.basho.riak.client.cap Quora Quorum]))


;;
;; API
;;

(defn fetch
  "Fetches bucket properties"
  [^RawClient client ^String bucket-name]
  (merge {:name bucket-name}
         (from-bucket-properties (.fetchBucket client bucket-name))))

(defn update
  "Updates bucket properties.

   Quorum values (r, w, dw and so on) can be integer, Quora or Quorum instances

   Options:

   * allow-siblings
   * last-write-wins
   * n-val (default: 3)
   * r (quorum value)
   * pr (quorum value)
   * w (quorum value)
   * dw (quorum value)
   * pw (quorum value)
   * rw (quorum value)
   * not-found-ok
   * basic-quorum
   * enable-search (default: false)
   * backend
   * pre-commit-hooks (a collection of pairs [\"erlang_module\", \"fn_name\"])
   * post-commit-hooks (a collection of pairs [\"erlang_module\", \"fn_name\"])
   * small-vclock
   * big-vclock
   * young-vclock
   * old-vclock"
  ([^RawClient client ^String bucket-name]
     (update client bucket-name {}))
  ([^RawClient client ^String bucket-name options]
     (.updateBucket client bucket-name (to-bucket-properties (or options {})))
     (merge {:name bucket-name}
            (from-bucket-properties (.fetchBucket client bucket-name)))))

(defn ^{:deprecated true} create
  "The same as update. This name reveals the intent a bit better in some cases.
   Kept for backwards compatibility, will be removed in the future"
  [& args]
  (apply update args))

(defn list
  "Returns buckets in the cluster as a set"
  [^RawClient client]
  (set (.listBuckets client)))


(defn keys-in
  "Returns list of keys in the bucket. With any non-trivial number of keys, this is a VERY EXPENSIVE operation
   and typically should be avoided"
  [^RawClient client ^String bucket-name]
  (.listKeys client bucket-name))
