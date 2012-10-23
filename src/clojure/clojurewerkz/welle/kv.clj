(ns clojurewerkz.welle.kv
  (:require [clojurewerkz.welle.mr :as mr])
  (:use clojurewerkz.welle.core
        clojurewerkz.welle.conversion
        [clojure.walk :only [stringify-keys]])
  (:import [com.basho.riak.client IRiakClient IRiakObject]
           [com.basho.riak.client.raw StoreMeta FetchMeta DeleteMeta RawClient RiakResponse]
           com.basho.riak.client.http.util.Constants
           java.util.Date))


;;
;; API
;;

(defn store
  "Stores an object in Riak"
  [^String bucket-name ^String key value &{ :keys [w dw pw
                                                   indexes links vclock ^String vtag ^Date last-modified
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
                                         :metadata      (stringify-keys metadata)
                                         :indexes       indexes
                                         :links         links
                                         :vclock        vclock
                                         :vtag          vtag
                                         :last-modified last-modified})
        ;; implements Iterable. MK.
        ^RiakResponse xs (.store *riak-client* ro md)
        mf               (if return-body
                           (comp deserialize-value from-riak-object)
                           from-riak-object)]
    (map mf xs)))


(defn fetch
  "Fetches an object and all its siblings (if there are any). As such, it always returns a list. In cases you are
   sure will produce no siblings, consider using `clojurewerkz.welle.kv/fetch-one`."
  [^String bucket-name ^String key &{:keys [r pr not-found-ok basic-quorum head-only
                                            return-deleted-vlock if-modified-since if-modified-vclock]
                                     :or {}}]
  (let [^FetchMeta md (to-fetch-meta r pr not-found-ok basic-quorum head-only return-deleted-vlock if-modified-since if-modified-vclock)
        results       (.fetch *riak-client* bucket-name key md)]
    (map (comp deserialize-value from-riak-object) results)))

(defn fetch-one
  "Fetches a single object. This is a convenience function: it optimistically assumes there will be only one
   objects and no siblings. In situations when you are not sure about this, consider using `clojurewerkz.welle.kv/fetch`
   instead."
  [^String bucket-name ^String key &{:keys [r pr not-found-ok basic-quorum head-only
                                            return-deleted-vlock if-modified-since if-modified-vclock]
                                     :or {}}]
  (let [^FetchMeta md (to-fetch-meta r pr not-found-ok basic-quorum head-only return-deleted-vlock if-modified-since if-modified-vclock)
        results       (.fetch *riak-client* bucket-name key md)]
    (if (.hasSiblings results)
      (throw (IllegalStateException.
              "Riak response to clojurewerkz.welle.kv/fetch-one contains siblings. If conflicts/siblings are expected here, use clojurewerkz.welle.kv/fetch"))
      (when-let [ro (first results)]
        (-> (first results)
            from-riak-object
            deserialize-value)))))

(defn fetch-all
  "Fetches multiple objects concurrently. This is a convenience function: it optimistically assumes there will be only one
   objects for each key and no siblings. In situations when you are not sure about this, use `clojurewerkz.welle.kv/fetch`
   `clojure.core/pmap` in combination instead.

   This function relies on clojure.core/pmap to fetch multiple keys,
   so it may be inappropriate for cases where results need to be retrieved pre-ordered. In such cases, use map/reduce queries
   instead."
  [^String bucket-name keys]
  (pmap (fn [^String k]
          (fetch-one bucket-name k))
        keys))


(defn index-query
  "Performs a secondary index (2i) query. Provided value can be either non-collection
   or a collection (typically vector). In the former case, a value query is performed. In the latter
   case, a range query is performed.

   Learn more in Riak's documentation on secondary indexes at http://wiki.basho.com/Secondary-Indexes.html"
  [^String bucket-name field value]
  (.fetchIndex *riak-client* (to-index-query value bucket-name field)))



(defn delete
  "Deletes an object"
  ([^String bucket-name ^String key]
     (.delete *riak-client* bucket-name key))
  ([^String bucket-name ^String key &{:keys [r pr w dw pw rw vclock]}]
     (.delete *riak-client* bucket-name key (to-delete-meta r pr w dw pw rw vclock))))

(defn delete-all
  "Deletes multiple objects. This function relies on clojure.core/pmap to delete multiple keys,
   so it may be inappropriate for cases where any potential race conditions between individual delete
   operations is a problem. For deleting a very large number of keys (say, thousands), consider using
   map/reduce"
  ([^String bucket-name keys]
     (doall (pmap (fn [^String k]
                    (delete bucket-name k))
                  keys)))
  ([^String bucket-name keys & rest]
     (doall (pmap (fn [^String k]
                    (apply delete (concat [bucket-name k] rest)))
                  keys))))


(defn delete-all-via-2i
  "Concurrently deletes multiple objects with keys retrieved via a secondary index (2i) query."
  ([^String bucket-name field value]
     (delete-all bucket-name (set (index-query bucket-name field value))))
  ([^String bucket-name field value & rest]
     (let [keys (set (index-query bucket-name field value))]
       (apply delete-all (concat [bucket-name keys] rest)))))
