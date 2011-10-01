(ns welle.objects
  (:use    [welle.core])
  (:import (com.basho.riak.client IRiakClient IRiakObject)
           (com.basho.riak.client.bucket Bucket WriteBucket)
           (com.basho.riak.client.operations StoreObject FetchObject)))


(defn store
  ^IRiakObject [^Bucket bucket ^String key ^bytes value, &{ :keys [r pr not-found-ok basic-quorum return-deleted-vclock
                                                                   w pw dw return-body if-non-match if-not-modified
                                                                   with-retrier with-mutator with-resolver with-converter with-value]}]
  (let [store-object (.store ^Bucket bucket key value)]
    (when r
      (.r ^StoreObject store-object r))
    (.execute store-object)))


(defn fetch
  ^IRiakObject [^Bucket bucket ^String key, &{ :keys [r pr not-found-ok basic-quorum
                                                      return-deleted-vclock if-modified modified-since
                                                      with-retrier with-resolver with-converter]}]
  (let [fetch-object (.fetch ^Bucket bucket key)]
    (when r
      (.r ^FetchObject fetch-object r))
    (.execute fetch-object)))
