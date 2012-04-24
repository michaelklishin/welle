(ns clojurewerkz.welle.objects
  (:use clojurewerkz.welle.core
        clojurewerkz.welle.conversion)
  (:import [com.basho.riak.client IRiakClient IRiakObject]
           [com.basho.riak.client.raw StoreMeta FetchMeta DeleteMeta RawClient RiakResponse]
           com.basho.riak.client.http.util.Constants))





;;
;; API
;;

(defn store
  "Stores an object"
  [^String bucket-name ^String key value &{ :keys [w dw pw
                                                   indexes vclock ^String vtag ^Long last-modified
                                                   ^Boolean return-body ^Boolean if-none-match ^Boolean if-not-modified
                                                   content-type metadata]
                                           :or {content-type Constants/CTYPE_OCTET_STREAM
                                                metadata     {}}}]
  (let [v               (serialize value content-type)
        ^StoreMeta   md (to-store-meta w dw pw return-body if-none-match if-not-modified)
        ^IRiakObject ro (to-riak-object {:bucket        bucket-name
                                         :key           key
                                         :value         v
                                         :content-type  content-type
                                         :metadata      metadata
                                         :indexes       indexes
                                         :vclock        vclock
                                         :vtag          vtag
                                         :last-modified last-modified})
        ;; implements Iterable. MK.
        ^RiakResponse xs (.store *riak-client* ro md)]
    (map from-riak-object xs)))


(defn- deserialize-value
  "Replaces :value key with its deserialized form using :content-type key to
   get value content type"
  [m]
  (assoc m :value (deserialize (:value m) (:content-type m))))

(defn fetch
  "Fetches an object"
  [^String bucket-name ^String key &{:keys [r pr not-found-ok basic-quorum head-only
                                            return-deleted-vlock if-modified-since if-modified-vclock]
                                     :or {}}]
  (let [^FetchMeta md (to-fetch-meta r pr not-found-ok basic-quorum head-only return-deleted-vlock if-modified-since if-modified-vclock)
        results       (.fetch *riak-client* bucket-name key md)]
    (map (comp deserialize-value from-riak-object) results)))

(defn delete
  "Deletes an object"
  [^String bucket-name ^String key &{:keys [r pr w dw pw rw vclock]}]
  (.delete *riak-client* bucket-name key (to-delete-meta r pr w dw pw rw vclock)))

(defn index-query
  [^String bucket-name field value]
  (.fetchIndex *riak-client* (to-index-query value bucket-name field)))