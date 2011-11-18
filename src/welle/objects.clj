(ns welle.objects
  (:use    [welle.core])
  (:import (com.basho.riak.client IRiakClient IRiakObject)
           (com.basho.riak.client.bucket Bucket WriteBucket)
           (com.basho.riak.client.operations StoreObject FetchObject)))


(defn store
  ^IRiakObject [^Bucket bucket ^String key ^bytes value, &{ :keys [r pr not-found-ok basic-quorum return-deleted-vclock
                                                                   w pw dw return-body if-non-match if-not-modified
                                                                   with-retrier with-mutator with-resolver with-converter with-value]}]
  (let [store-operation (.store bucket key value)]
    (when r (.r store-operation r))
    (.execute store-operation)))


(defn fetch
  ^IRiakObject [^Bucket bucket ^String key, &{ :keys [r pr not-found-ok basic-quorum
                                                      return-deleted-vclock if-modified modified-since
                                                      with-retrier with-resolver with-converter]}]
  (let [fetch-operation (.fetch bucket key)]
    (when r                     (.r  fetch-operation r))
    (when pr                    (.pr fetch-operation pr))
    (when not-found-ok          (.notFoundOK  fetch-operation not-found-ok))
    (when basic-quorum          (.basicQuorum fetch-operation basic-quorum))
    (when return-deleted-vclock (.returnDeletedVClock fetch-operation return-deleted-vclock))
    (when if-modified           (.ifModified    fetch-operation if-modified))
    (when modified-since        (.ifModified    fetch-operation modified-since))
    (when with-retrier          (.withRetrier   fetch-operation with-retrier))
    (when with-resolver         (.withResolver  fetch-operation with-resolver))
    (when with-converter        (.withConverter fetch-operation with-converter))
    (.execute fetch-operation)))

(defn delete
  ^IRiakObject [^Bucket bucket ^String key, &{ :keys [r pr w dw pw rw vclock with-retrier fetch-before-delete] }]
  (let [delete-operation (.delete bucket key)]
    (when r                     (.r  delete-operation r))
    (when pr                    (.pr delete-operation pr))
    (when w                     (.w  delete-operation w))
    (when dw                    (.dw delete-operation dw))
    (when pw                    (.pw delete-operation pw))
    (when rw                    (.rw delete-operation rw))
    (when with-retrier          (.withRetrier       delete-operation with-retrier))
    (when vclock                (.vclock            delete-operation vclock))
    (when fetch-before-delete   (.fetchBeforeDelete delete-operation fetch-before-delete))
    (.execute delete-operation)))