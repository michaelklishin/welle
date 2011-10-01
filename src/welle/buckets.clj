(ns welle.buckets
  (:use     [welle.core])
  (:import (com.basho.riak.client IRiakClient IRiakObject)
           (com.basho.riak.client.bucket Bucket WriteBucket)
           (com.basho.riak.client.operations StoreObject FetchObject)))


(defn create
  ^Bucket [^String bucket-name &{ :keys [allow-siblings, last-write-wins, n-val, backend, small-vclock, big-vclock, young-vclock, old-vclock, r, w] }]
  (let [write-bucket (.createBucket ^IRiakClient *riak-client* ^String bucket-name)]
    (when allow-siblings
      (.allowSiblings ^WriteBucket write-bucket allow-siblings))
    (when last-write-wins
      (.lastWriteWins ^WriteBucket write-bucket last-write-wins))
    (when n-val
      (.nVal ^WriteBucket write-bucket n-val))
    (.execute write-bucket)))
