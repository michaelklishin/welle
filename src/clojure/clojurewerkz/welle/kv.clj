(ns clojurewerkz.welle.kv
  (:require [clojurewerkz.welle.mr :as mr])
  (:use clojurewerkz.welle.core
        clojurewerkz.welle.conversion
        [clojure.walk :only [stringify-keys]])
  (:import [com.basho.riak.client IRiakClient IRiakObject]
           [com.basho.riak.client.raw StoreMeta FetchMeta DeleteMeta RawClient RiakResponse]
           com.basho.riak.client.http.util.Constants
           [com.basho.riak.client.raw RiakResponse]
           [com.basho.riak.client.cap Retrier DefaultRetrier]
           java.util.Date))


;;
;; Implementation
;;

(def ^DefaultRetrier default-retrier
  "Default operation retrier that will be used by operations such as fetch and store"
  (counting-retrier 3))

;;
;; API
;;

(defn store
  "Stores an object in Riak"
  [^String bucket-name ^String key value &{ :keys [w dw pw
                                                   indexes links vclock ^String vtag ^Date last-modified
                                                   ^Boolean return-body ^Boolean if-none-match ^Boolean if-not-modified
                                                   content-type metadata
                                                   ^Retrier retrier]
                                           :or {content-type Constants/CTYPE_OCTET_STREAM
                                                metadata     {}
                                                retrier default-retrier}}]
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
        ^RiakResponse xs (.attempt retrier ^Callable (fn []
                                                       (.store *riak-client* ro md)))
        mf               (if return-body
                           (comp deserialize-value from-riak-object)
                           from-riak-object)]
    (map mf xs)))

(declare tombstone?)
(defn fetch
  "Fetches an object and all its siblings (if there are any). As such, it always returns a list. In cases you are
   sure will produce no siblings, consider using `clojurewerkz.welle.kv/fetch-one`.

   This function will filter out tombstones (objects that were deleted but not yet
   resolved/garbage collected by the storage engine) unless :return-deleted-vclock
   is passed as true.

   Available options:

   `:basic-quorum` (true or false): whether to return early in some failure cases (eg. when `:r` is 1 and you get 2 errors and a success `:basic-quorum` set to true would return an error)
   `:notfound-ok` (true or false): whether to treat notfounds as successful reads for the purposes of `:r`
   `:vtag`: when accessing an object with siblings, which sibling to retrieve.
   `:if-none-match` (date): a date for conditional get. Only supported by HTTP transport.
   `:if-modified-vclock`: a vclock instance to use for conditional get. Only supported by Protocol Buffers transport.
   `:return-deleted-vlock` (true or false): should tombstones (objects that have been deleted but not yet resolved/GCed) be returned?
   `:head-only` (true or false): should the response only return object metadata, not its value?
   `:skip-deserialize` (true or false): should the deserialization of the value be skipped?
  "
  [^String bucket-name ^String key &{:keys [r pr not-found-ok basic-quorum head-only
                                            return-deleted-vclock if-modified-since if-modified-vclock skip-deserialize
                                            ^Retrier retrier]
                                     :or {retrier default-retrier}}]
  (let [^FetchMeta md (to-fetch-meta r pr not-found-ok basic-quorum head-only return-deleted-vclock if-modified-since if-modified-vclock)
        results       (.attempt retrier ^Callable (fn []
                                                    (.fetch *riak-client* bucket-name key md)))
        ;; return-deleted-vclock = we should return tombstones. See
        ;; https://github.com/basho/riak-java-client/commit/416a901ff1de8e4eb559db21ac5045078d278e86 for more info. MK.
        ros           (if return-deleted-vclock
                        results
                        (remove #(.isDeleted ^IRiakObject %) results))]
    (if skip-deserialize
      (map from-riak-object ros)
      (map (comp deserialize-value from-riak-object) ros))))

(defn fetch-one
  "Fetches a single object. This is a convenience function: it optimistically assumes there will be only one
   objects and no siblings. In situations when you are not sure about this, consider using `clojurewerkz.welle.kv/fetch`
   instead."
  [^String bucket-name ^String key &{:keys [r pr not-found-ok basic-quorum head-only
                                            return-deleted-vclock if-modified-since if-modified-vclock skip-deserialize
                                            ^Retrier retrier]
                                     :or {retrier default-retrier}}]
  (let [^FetchMeta    md      (to-fetch-meta r pr not-found-ok basic-quorum head-only return-deleted-vclock if-modified-since if-modified-vclock)
        ^RiakResponse results (.attempt retrier ^Callable (fn []
                                                            (.fetch *riak-client* bucket-name key md)))]
    (if (.hasSiblings results)
      (throw (IllegalStateException.
              "Riak response to clojurewerkz.welle.kv/fetch-one contains siblings. If conflicts/siblings are expected here, use clojurewerkz.welle.kv/fetch"))
      (when (not (empty? results))
        (let [fr (first results)]
          (when (not (.isDeleted ^IRiakObject fr))
            (if skip-deserialize
              (from-riak-object fr)
              (-> fr from-riak-object deserialize-value))))))))

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

(defn tombstone?
  "Returns true if a given Riak object is a tombstone
   (was deleted but not yet GCed)"
  [m]
  (:deleted? m))
