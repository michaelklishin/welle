(ns clojurewerkz.welle.buckets
  (:refer-clojure :exclude [list])
  (:use clojurewerkz.welle.core
        clojurewerkz.welle.conversion)
  (:import [com.basho.riak.client IRiakClient IRiakObject]
           [com.basho.riak.client.bucket Bucket WriteBucket]
           [com.basho.riak.client.operations StoreObject FetchObject]
           [com.basho.riak.client.cap ConflictResolver Retrier]
           [com.basho.riak.client.convert Converter]
           [com.basho.riak.client.cap Quora Quorum]))


;;
;; API
;;

(defn ^Bucket create
  "Creates a bucket"
  [^String bucket-name &{ :keys [allow-siblings last-write-wins n-val ^String backend
                                 small-vclock big-vclock young-vclock old-vclock
                                 r pr w dw pw rw
                                 vclock ^Boolean not-found-ok ^Boolean basic-quorum ^Boolean enable-for-search]}]
  )

(defn list
  "Returns buckets in the cluster as a set"
  []
  )


(defn keys-in
  "Returns list of keys in the bucket. This is an expensive operation and typically should be avoided."
  [^String bucket-name]
  (.keys bucket))