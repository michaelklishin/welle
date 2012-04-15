(ns clojurewerkz.welle.buckets
  (:refer-clojure :exclude [list])
  (:use clojurewerkz.welle.core)
  (:import [com.basho.riak.client IRiakClient IRiakObject]
           [com.basho.riak.client.bucket Bucket WriteBucket]
           [com.basho.riak.client.operations StoreObject FetchObject]
           clojure.lang.Named))


;;
;; API
;;

(defn ^Bucket create
  "Creates a bucket"
  [^String bucket-name &{ :keys [allow-siblings last-write-wins n-val backend small-vclock big-vclock young-vclock old-vclock r w] }]
  (let [^WriteBucket op (.createBucket ^IRiakClient *riak-client* ^String bucket-name)]
    (when allow-siblings
      (.allowSiblings  op allow-siblings))
    (when last-write-wins
      (.lastWriteWins op last-write-wins))
    (when n-val
      (.nVal op n-val))
    (.execute op)))

(defn list
  "Returns buckets in the cluster as a set"
  []
  (set (.listBuckets ^IRiakClient *riak-client*)))
