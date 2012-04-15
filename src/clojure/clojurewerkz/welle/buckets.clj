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
                                 vclock with-retrier ^Boolean not-found-ok ^Boolean basic-quorum ^Boolean enable-for-search] }]
  (let [^WriteBucket op (.createBucket ^IRiakClient *riak-client* ^String bucket-name)]
    (when allow-siblings  (.allowSiblings op allow-siblings))
    (when last-write-wins (.lastWriteWins op last-write-wins))
    (when n-val           (.nVal op ^Integer n-val))
    (when backend         (.backend op backend))
    (when small-vclock    (.smallVClock op ^Integer small-vclock))
    (when big-vclock      (.bigVClock op ^Integer big-vclock))
    (when young-vclock    (.youngVClock op ^Long young-vclock))
    (when old-vclock      (.oldVClock op ^Long old-vclock))
    (when r               (.r  op ^Integer r))
    (when pr              (.pr op ^Integer pr))
    (when w               (.w  op ^Integer w))
    (when dw              (.dw op ^Integer dw))
    (when pw              (.pw op ^Integer pw))
    (when rw              (.rw op ^Integer rw))
    (when with-retrier      (.withRetrier op with-retrier))
    (when enable-for-search (.enableForSearch op))
    (.execute op)))

(defn list
  "Returns buckets in the cluster as a set"
  []
  (set (.listBuckets ^IRiakClient *riak-client*)))
