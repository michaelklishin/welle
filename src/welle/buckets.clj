(ns welle.buckets
  (:use    [welle.core])
  (:import (com.basho.riak.client IRiakClient)
           (com.basho.riak.client.bucket WriteBucket)))


(defn create
  [client bucket &{ :keys [allow-siblings, last-write-wins, n-val, backend, small-vclock, big-vclock, young-vclock, old-vclock, r, w] }]
  (let [write-bucket (.createBucket ^IRiakClient client ^String bucket)]
    (when allow-siblings
      (.allowSiblings ^WriteBucket write-bucket allow-siblings))
    (when last-write-wins
      (.lastWriteWind ^WriteBucket write-bucket last-write-wins))
    (.execute write-bucket)
    write-bucket))