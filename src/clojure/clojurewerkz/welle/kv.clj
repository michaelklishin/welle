(ns clojurewerkz.welle.kv
  (:require [clojurewerkz.welle.mr :as mr])
  (:use clojurewerkz.welle.core
        clojurewerkz.welle.conversion
        [clojure.walk :only [stringify-keys]])
  (:import [com.basho.riak.client IRiakClient IRiakObject]
           [com.basho.riak.client.raw StoreMeta FetchMeta DeleteMeta RawClient RiakResponse]
           com.basho.riak.client.http.util.Constants
           [com.basho.riak.client.raw RiakResponse]
           [com.basho.riak.client.cap Retrier DefaultRetrier ConflictResolver]
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
                                                   ^Integer timeout
                                                   content-type metadata
                                                   ^Retrier retrier
                                                   ^ConflictResolver resolver]
                                           :or {content-type Constants/CTYPE_OCTET_STREAM
                                                metadata     {}
                                                retrier default-retrier}}]
  (let [v               (serialize value content-type)
        ^StoreMeta   md (to-store-meta w dw pw return-body if-none-match if-not-modified timeout)
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
                           from-riak-object)
        ys               (map mf xs)
        result           (if resolver
                           (.resolve resolver ys)
                           ys)]
    {:vclock (.getVclock xs) :has-siblings? (.hasSiblings xs) :has-value? (.hasValue xs) :deleted? (.isDeleted xs) :modified? (not (.isUnmodified xs)) :result result}))

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
   `:return-deleted-vclock` (true or false): should tombstones (objects that have been deleted but not yet resolved/GCed) be returned?
   `:head-only` (true or false): should the response only return object metadata, not its value?
   `:skip-deserialize` (true or false): should the deserialization of the value be skipped?
  "
  [^String bucket-name ^String key &{:keys [r pr not-found-ok basic-quorum head-only
                                            return-deleted-vclock if-modified-since if-modified-vclock skip-deserialize
                                            ^Integer timeout
                                            ^Retrier retrier ^ConflictResolver resolver]
                                     :or {retrier default-retrier}}]
  (let [^FetchMeta    md  (to-fetch-meta r pr not-found-ok basic-quorum head-only return-deleted-vclock if-modified-since if-modified-vclock timeout)
        ^RiakResponse res (.attempt retrier ^Callable (fn []
                                                        (.fetch *riak-client* bucket-name key md)))
        ;; return-deleted-vclock = we should return tombstones. See
        ;; https://github.com/basho/riak-java-client/commit/416a901ff1de8e4eb559db21ac5045078d278e86 for more info. MK.
        ros           (if return-deleted-vclock
                        res
                        (remove #(.isDeleted ^IRiakObject %) res))
        xs            (if skip-deserialize
                        (map from-riak-object ros)
                        (map (comp deserialize-value from-riak-object) ros))
        result        (if resolver
                        (.resolve resolver xs)
                        xs)]
    {:vclock (.getVclock res) :has-siblings? (.hasSiblings res) :has-value? (> (count ros) 0) :deleted? (.isDeleted res) :modified? (not (.isUnmodified res)) :result result}))

(defn fetch-one
  "Fetches a single object. If siblings are found, passes the on to the provided resolver or raises an exception.
   In situations when you are interested in getting all siblings back, use `clojurewerkz.welle.kv/fetch`
   instead."
  [& args]
  (let [{:keys [has-siblings? result] :as m} (apply fetch args)]
    (if has-siblings?
      (throw (IllegalStateException.
              "Riak response to clojurewerkz.welle.kv/fetch-one contains siblings. If conflicts/siblings are expected here, provide a resolver or use clojurewerkz.welle.kv/fetch"))
      (assoc m :result (if (coll? result)
                         (first result)
                         result)))))

(defn modify
  "Modifies an object with the given bucket and key by fetching it, applying a function to it
   and storing it back. Assumes that the fetch operation returns no siblings (a collection with
   just one Riak object), either via request or after applying a resolver.

   The mutating function is passed the entire Riak object as an immutable map, not just value.
   Mutation sets :last-modified of the object to the current timestamp.

   Takes the same options as clojurewerkz.welle.kv/fetch and clojurewerkz.welle.kv/store.

   Returns the same results as clojurewerkz.welle.kv/store"
  [^String bucket-name ^String key f &{:keys [r pr not-found-ok basic-quorum head-only
                                              return-deleted-vclock if-modified-since if-modified-vclock skip-deserialize
                                              ^Retrier retrier ^ConflictResolver resolver
                                              w dw pw
                                              indexes links ^String vtag ^Date last-modified
                                              ^Boolean return-body ^Boolean if-none-match ^Boolean if-not-modified
                                              content-type metadata]
                                       :or {retrier default-retrier}}]
  (let [{:keys [result vclock] :as m} (fetch bucket-name key
                                             :r r :pr pr :not-found-ok not-found-ok :basic-quorum basic-quorum :head-only head-only
                                             :return-deleted-vclock return-deleted-vclock :if-modified-since if-modified-since :if-modified-vclock if-modified-vclock
                                             :skip-deserialize skip-deserialize :retrier retrier :resolver resolver)
        m' (f result)]
    (store bucket-name key (:value m')
           :w w :dw dw :pw pw
           :indexes (get m' :indexes indexes) :links (get m' :links links) :vclock vclock :vtag (get m' :vtag vtag) :last-modified (.getTime (Date.))
           :return-body return-body :if-none-match if-none-match :if-not-modified if-not-modified
           :content-type (get m' :content-type content-type)
           :metadata     (get m' :metadata metadata)
           :retrier      retrier
           :resolver     resolver)))

(defn index-query
  "Performs a secondary index (2i) query. Provided value can be either non-collection
   or a collection (typically vector). In the former case, a value query is performed. In the latter
   case, a range query is performed.

   Learn more in Riak's documentation on secondary indexes at http://wiki.basho.com/Secondary-Indexes.html"
  [^String bucket-name field value]
  (.attempt default-retrier ^Callable (fn []
                                        (.fetchIndex *riak-client* (to-index-query value bucket-name field)))))



(defn delete
  "Deletes an object"
  ([^String bucket-name ^String key]
     (.attempt default-retrier ^Callable (fn []
                                           (.delete *riak-client* bucket-name key))))
  ([^String bucket-name ^String key &{:keys [r pr w dw pw rw vclock timeout ^Retrier retrier]
                                      :or {retrier default-retrier}}]
     (.attempt retrier ^Callable (fn []
                                   (.delete *riak-client* bucket-name key (to-delete-meta r pr w dw pw rw vclock timeout))))))

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

(defn has-value?
  "Returns true if a given Riak response is empty"
  [m]
  (:has-value? m))

(defn increment-counter
  "Increment counter in Riak.
   Available options:

  `:value` (default 1): value to increment by
   `:timeout`: query timeout
"
  [^String bucket-name ^String counter-name &{ :keys [w dw pw
                                                      ^long value
                                                       ^Boolean return-body
                                                      ^Integer timeout
                                                      ^Retrier retrier]
                                              :or {value 1
                                                   return-body true
                                                   retrier default-retrier}}]
  (let [^StoreMeta   md (to-store-meta w dw pw return-body nil nil timeout)
        ^Long value (or value 1)
        ^Long result (.attempt retrier ^Callable (fn []
                                                           (.incrementCounter *riak-client* bucket-name counter-name value md)))]
    result))

(defn fetch-counter
  "Fetches Riak counter.
   Available options:

   `:basic-quorum` (true or false): whether to return early in some failure cases (eg. when `:r` is 1 and you get 2 errors and a success `:basic-quorum` set to true would return an error)
   `:notfound-ok` (true or false): whether to treat notfounds as successful reads for the purposes of `:r`
   `:if-modified-vclock`: a vclock instance to use for conditional get. Only supported by Protocol Buffers transport.
   `:timeout`: query timeout
  "
  [^String bucket-name ^String counter &{:keys [r pr not-found-ok basic-quorum
                                                return-deleted-vclock
                                                if-modified-since if-modified-vclock
                                                ^Retrier retrier
                                                ^Integer timeout]
                                         :or {retrier default-retrier}}]
  (let [^FetchMeta    md  (to-fetch-meta r pr not-found-ok basic-quorum nil nil if-modified-since if-modified-vclock timeout)
        ^Long result (.attempt retrier ^Callable (fn []
                                                   (.fetchCounter *riak-client* bucket-name counter md)))]
    result))
