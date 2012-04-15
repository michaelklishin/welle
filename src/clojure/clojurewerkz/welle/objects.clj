(ns clojurewerkz.welle.objects
  (:use clojurewerkz.welle.core
        clojurewerkz.welle.conversion)
  (:import [com.basho.riak.client IRiakClient IRiakObject]
           [com.basho.riak.client.bucket Bucket WriteBucket]
           [com.basho.riak.client.operations StoreObject FetchObject DeleteObject]
           [com.basho.riak.client.cap ConflictResolver Retrier]
           [com.basho.riak.client.convert Converter]))


(defn ^IRiakObject store
  "Stores an object"
  [^Bucket bucket ^String key ^bytes value &{ :keys [r pr ^Boolean not-found-ok ^Boolean basic-quorum ^Boolean return-deleted-vclock
                                                     w pw dw ^Boolean return-body if-non-match if-not-modified
                                                     ^Retrier with-retrier with-mutator ^ConflictResolve with-resolver ^Converter with-converter with-value]}]
  (let [^StoreObject op (.store bucket key value)]
    (when r (.r op (to-quorum r)))
    (.execute op)))


(defn ^IRiakObject fetch
  "Fetches an object"
  [^Bucket bucket ^String key &{ :keys [r pr ^Boolean not-found-ok ^Boolean basic-quorum
                                        return-deleted-vclock if-modified modified-since
                                        ^Retrier with-retrier ^ConflictResolver with-resolver ^Converter with-converter]}]
  (let [^FetchObject op (.fetch bucket key)]
    (when r                     (.r  op (to-quorum r)))
    (when pr                    (.pr op (to-quorum pr)))
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
  "Delete an object"
  [^Bucket bucket ^String key &{ :keys [r pr w dw pw rw vclock with-retrier fetch-before-delete] }]
  (let [^DeleteObject op (.delete bucket key)]
    (when r                     (.r  op (to-quorum r)))
    (when pr                    (.pr op (to-quorum pr)))
    (when w                     (.w  op (to-quorum w)))
    (when dw                    (.dw op (to-quorum dw)))
    (when pw                    (.pw op (to-quorum pw)))
    (when rw                    (.rw op (to-quorum rw)))
    (when with-retrier          (.withRetrier       op with-retrier))
    (when vclock                (.vclock            op vclock))
    (when fetch-before-delete   (.fetchBeforeDelete op fetch-before-delete))
    (.execute op)))
