(ns clojurewerkz.welle.objects
  (:use clojurewerkz.welle.core)
  (:import [com.basho.riak.client IRiakClient IRiakObject]
           [com.basho.riak.client.bucket Bucket WriteBucket]
           [com.basho.riak.client.operations StoreObject FetchObject DeleteObject]))


(defn ^IRiakObject store
  [^Bucket bucket ^String key ^bytes value &{ :keys [r pr not-found-ok basic-quorum return-deleted-vclock
                                                     w pw dw return-body if-non-match if-not-modified
                                                     with-retrier with-mutator with-resolver with-converter with-value]}]
  (let [^StoreObject op (.store bucket key value)]
    (when r (.r op r))
    (.execute op)))


(defn ^IRiakObject fetch
  [^Bucket bucket ^String key &{ :keys [r pr not-found-ok basic-quorum
                                        return-deleted-vclock if-modified modified-since
                                        with-retrier with-resolver with-converter]}]
  (let [^FetchObject op (.fetch bucket key)]
    (when r                     (.r  op r))
    (when pr                    (.pr op pr))
    (when not-found-ok          (.notFoundOK  op not-found-ok))
    (when basic-quorum          (.basicQuorum op basic-quorum))
    (when return-deleted-vclock (.returnDeletedVClock op return-deleted-vclock))
    (when if-modified           (.ifModified    op if-modified))
    (when modified-since        (.ifModified    op modified-since))
    (when with-retrier          (.withRetrier   op with-retrier))
    (when with-resolver         (.withResolver  op with-resolver))
    (when with-converter        (.withConverter op with-converter))
    (.execute op)))

(defn ^IRiakObject delete
  [^Bucket bucket ^String key &{ :keys [r pr w dw pw rw vclock with-retrier fetch-before-delete] }]
  (let [^DeleteObject op (.delete bucket key)]
    (when r                     (.r  op r))
    (when pr                    (.pr op pr))
    (when w                     (.w  op w))
    (when dw                    (.dw op dw))
    (when pw                    (.pw op pw))
    (when rw                    (.rw op rw))
    (when with-retrier          (.withRetrier       op with-retrier))
    (when vclock                (.vclock            op vclock))
    (when fetch-before-delete   (.fetchBeforeDelete op fetch-before-delete))
    (.execute op)))