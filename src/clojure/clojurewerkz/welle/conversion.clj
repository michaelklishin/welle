(ns clojurewerkz.welle.conversion
  (:require [cheshire.custom   :as json]
            [cheshire.core     :as json2]
            [clojure.set       :as cs]
            [clojure.java.io   :as io])
  (:use     [clojure.walk :only [stringify-keys]]
            clojurewerkz.welle.hooks)
  (:import [com.basho.riak.client.cap Quora Quorum VClock BasicVClock]
           [com.basho.riak.client.raw StoreMeta FetchMeta DeleteMeta]
           [com.basho.riak.client IRiakObject RiakLink]
           [com.basho.riak.client.builders RiakObjectBuilder BucketPropertiesBuilder]
           [com.basho.riak.client.bucket BucketProperties TunableCAPProps]
           [com.basho.riak.client.query LinkWalkStep LinkWalkStep$Accumulate]
           com.basho.riak.client.http.util.Constants
           [com.basho.riak.client.query.indexes RiakIndex IntIndex BinIndex]
           com.basho.riak.client.query.functions.NamedErlangFunction
           [com.basho.riak.client.raw.query.indexes BinValueQuery BinRangeQuery IntValueQuery IntRangeQuery]
           java.util.Date
           [java.io ByteArrayOutputStream PrintWriter InputStreamReader ByteArrayInputStream]
           [java.util.zip GZIPOutputStream GZIPInputStream]))

;;
;; Implementation
;;

;; clojure.java.io has these as private, so we had to copy them. MK.
(def ^{:doc "Type object for a Java primitive byte array."}
  byte-array-type (class (make-array Byte/TYPE 0)))


;;
;; API
;;

;; Quorum

(defprotocol QuorumConversion
  (^com.basho.riak.client.cap.Quorum
    to-quorum [input] "Coerces input to a value suitable for representing a read, write or other quorum/quora.
                      Riak Java client supports passing those values as numerical primitives, Quorum and Quora."))


(extend-protocol QuorumConversion
  Integer
  (to-quorum [input]
    (Quorum. ^Integer input))

  Long
  (to-quorum [input]
    (Quorum. ^Long input))

  Quora
  (to-quorum [input]
    (Quorum. ^Quora input))

  Quorum
  (to-quorum [input]
    input)

  ;; in certain places Riak Java client accepts nulls as valid values
  ;; for quorum, this is the easiest way to avoid repetitive (if v (to-quorum v) nil)
  ;; kind of code. Several very experienced Clojure developers confirmed that extending
  ;; protocols to nil is a reasonable idea. MK.
  nil
  (to-quorum [input]
    input))


;; VClock

(defprotocol VClockConversion
  (to-vclock [input] "Converts input to a VClock instance"))

(extend-protocol VClockConversion
  String
  (^com.basho.riak.client.cap.VClock to-vclock [^String s]
    (BasicVClock. (.getBytes s "UTF-8")))

  VClock
  (to-vclock [^VClock v]
    v))

(extend byte-array-type
  VClockConversion
  {:to-vclock (fn [^bytes input]
                (BasicVClock. input))})



;; {Store,Fetch,Delete}Meta

(defn to-store-meta
  "Builds a StoreMeta instance from provided arguments"
  (^com.basho.riak.client.raw.StoreMeta
   [w dw pw return-body if-none-match if-not-modified]
   (StoreMeta. (to-quorum w)
               (to-quorum dw)
               (to-quorum pw)
               ^Boolean return-body nil
               ^Boolean if-none-match
               ^Boolean if-not-modified)))

(defn to-fetch-meta
  "Builds a FetchMeta instance from provided arguments"
  (^com.basho.riak.client.raw.FetchMeta
   [r pr not-found-ok basic-quorum head-only return-deleted-vclock if-modified-since if-modified-vclock]
   (FetchMeta. (to-quorum r)
               (to-quorum pr)
               ^Boolean not-found-ok
               ^Boolean basic-quorum
               ^Boolean head-only
               ^Boolean return-deleted-vclock
               ^Date if-modified-since
               ^VClock if-modified-vclock)))

(defn to-delete-meta
  "Builds a DeleteMeta instance from provided arguments"
  (^com.basho.riak.client.raw.DeleteMeta
   [r pr w dw pw rw vclock]
   (DeleteMeta. (to-quorum r)
                (to-quorum pr)
                (to-quorum w)
                (to-quorum dw)
                (to-quorum pw)
                (to-quorum rw)
                ^VClock vclock)))


;; Clojure <=> IRiakObject

(defn ^com.basho.riak.client.RiakLink
  to-riak-link
  "Converts a Clojure map to a RiakLink instance"
  [m]
  (let [m (stringify-keys m)]
    (RiakLink. (get m "bucket") (get m "key") (get m "tag"))))

(defn from-riak-link
  "Converts a RiakLink instance to a Clojure map"
  [^RiakLink rl]
  {:bucket (.getBucket rl) :key (.getKey rl) :tag (.getTag rl)})

(declare deserialize)
(defn to-riak-object
  "Builds a Riak object from a Clojure map of well-known attributes:
   :value, :content-type, :metadata, :indexes, :vclock, :vtag, :last-modified"
  (^com.basho.riak.client.IRiakObject
   [{:keys [^String bucket ^String key value content-type metadata indexes
            vclock vtag last-modified links]
     :or {content-type Constants/CTYPE_OCTET_STREAM
          metadata     {}}
     :as options}]
   (let [^RiakObjectBuilder bldr (doto (RiakObjectBuilder/newBuilder (name bucket) (name key))
                                   (.withValue        value)
                                   (.withContentType  content-type)
                                   (.withUsermeta     metadata))]
     (when vclock        (.withVClock bldr ^VClock (to-vclock vclock)))
     (when vtag          (.withVtag bldr vtag))
     (when last-modified (.withLastModified bldr last-modified))
     (when-let [indexes (seq indexes)]
       ;; TODO: this code breaks when indexed values are not collections
       (doseq [[idx-key idx-vals] indexes
               idx-val (if (coll? idx-vals) idx-vals [idx-vals])]
         (.addIndex bldr ^String (name idx-key) idx-val)))
     (when-let [xs (seq links)]
       (.withLinks bldr (map to-riak-link xs)))
     (.build bldr))))

(defn indexes-from
  "Returns indexes on the given IRiakObject as a Clojure map where values are keywords
   and values are sets"
  [^IRiakObject ro]
  (let [indexes (concat (seq (.allBinIndexes ro))
                        (seq (.allIntIndexes ro)))
        step    (fn [acc-m ^java.util.HashMap$Entry idx]
                  (let [idx-name   (keyword (.getName ^RiakIndex (.getKey idx)))
                        idx-fields (set ^java.util.Set (.getValue idx))]
                    (merge-with cs/union acc-m {idx-name idx-fields})))]
    (reduce step {} indexes)))

(defn links-from
  "Returns links on the given IRiakObject as a lazy sequence of Clojure maps"
  [^IRiakObject ro]
  (map from-riak-link (.getLinks ro)))

(defn from-riak-object
  "Converts IRiakObjects to a Clojure map"
  [^IRiakObject ro]
  {:vclock        (.getVClock ro)
   :content-type  (.getContentType ro)
   :vtag          (.getVtag ro)
   :last-modified (.getLastModified ro)
   :metadata      (into {} (.getMeta ro))
   :value         (.getValue ro)
   :indexes       (indexes-from ro)
   :links         (links-from ro)
   ;; true for tombstones
   :deleted?      (.isDeleted ro)})


;; Index queries

(defmacro bin-index
  [index-name]
  `(BinIndex/named (name ~index-name)))

(defmacro int-index
  [index-name]
  `(IntIndex/named (name ~index-name)))

(defprotocol IndexQueryConversion
  (to-range-query [start end bucket-name index-name] "Builds a range 2i query")
  (to-value-query [value bucket-name index-name] "Builds a value 2i query"))

(extend-protocol IndexQueryConversion
  String
  (to-range-query [^String start ^String end ^String bucket-name index-name]
    (BinRangeQuery. (bin-index index-name) bucket-name start end))
  (to-value-query [^String value ^String bucket-name index-name]
    (BinValueQuery. (bin-index index-name) bucket-name value))


  Integer
  (to-range-query [^Integer start ^Integer end ^String bucket-name index-name]
    (IntRangeQuery. (int-index index-name) bucket-name start end))
  (to-value-query [^Integer value ^String bucket-name index-name]
    (IntValueQuery. (int-index index-name) bucket-name value))


  Long
  (to-range-query [^Long start ^Long end ^String bucket-name index-name]
    (IntRangeQuery. (int-index index-name) bucket-name (Long/valueOf start) (Long/valueOf end)))
  (to-value-query [^Long value ^String bucket-name index-name]
    (IntValueQuery. (int-index index-name) bucket-name (Long/valueOf value))))




(defmulti ^com.basho.riak.client.raw.query.indexes.IndexQuery
  to-index-query (fn [value _ _]
                   (if (coll? value)
                     :range
                     :value)))
(defmethod to-index-query :range
  [value ^String bucket-name index-name]
  (let [start (first value)
        end   (last  value)]
    (to-range-query start end bucket-name index-name)))
(defmethod to-index-query :value
  [value ^String bucket-name index-name]
  (to-value-query value bucket-name index-name))





;; Serialization

(defprotocol BytesConversion
  (^bytes to-bytes [input] "Converts input to a byte array value that can be stored in a bucket"))

(extend-protocol BytesConversion
  String
  (to-bytes [^String input]
    (.getBytes input)))

(extend byte-array-type
  BytesConversion
  {:to-bytes (fn [^bytes input]
               input) })


(defmulti serialize (fn [_ content-type]
                      content-type))

;; byte streams, strings
(defmethod serialize Constants/CTYPE_OCTET_STREAM
  [value _]
  (to-bytes value))
(defmethod serialize Constants/CTYPE_TEXT
  [value _]
  (to-bytes value))
(defmethod serialize Constants/CTYPE_TEXT_UTF8
  [value _]
  (to-bytes value))


;; JSON
(defmethod serialize Constants/CTYPE_JSON
  [value _]
  (json/encode value))
(defmethod serialize Constants/CTYPE_JSON_UTF8
  [value _]
  (json/encode value))
;; a way to support GZip content encoding for both HTTP and PB interfaces.
(defmethod serialize "application/json+gzip"
  [value _]
  (with-open [out    (ByteArrayOutputStream.)
              gzip   (GZIPOutputStream. out)
              writer (PrintWriter. gzip)]
    (json2/generate-stream value writer)
    (.flush writer)
    (.finish gzip)
    (.toByteArray out)))

;; Clojure
(defmethod serialize "application/jackson-smile"
  [value _]
  (json/generate-smile value))

(defmethod serialize "application/smile"
  [value _]
  (json/generate-smile value))

;; Clojure
(defmethod serialize "application/clojure"
  [value _]
  (binding [*print-dup* true]
    (pr-str value)))



(defmulti deserialize (fn [_ content-type]
                        content-type))
(defmethod deserialize Constants/CTYPE_OCTET_STREAM
  [value _]
  value)
(defmethod deserialize Constants/CTYPE_TEXT
  [value _]
  (String. ^bytes value))
(defmethod deserialize :text
  [value _]
  (String. ^bytes value))
(defmethod deserialize Constants/CTYPE_TEXT_UTF8
  [value _]
  (String. ^bytes value "UTF-8"))

;; JSON
(defmethod deserialize Constants/CTYPE_JSON
  [value _]
  (json/parse-string (String. ^bytes value) true))
;; as of Riak Java client 1.1, this constant's value is "application/json;charset=UTF-8"
;; (no space between base content type and parameters). However, Riak returns content type *with*
;; the space so we have to cover both. Reported to Basho at https://github.com/basho/riak-java-client/issues/125.
;; MK.
(defmethod deserialize Constants/CTYPE_JSON_UTF8
  [value _]
  (json/decode (String. ^bytes value "UTF-8") true))
(defmethod deserialize "application/json; charset=UTF-8"
  [value _]
  (json/decode (String. ^bytes value "UTF-8") true))
(defmethod deserialize "application/json+gzip"
  [value _]
  (with-open [in (GZIPInputStream. (ByteArrayInputStream. ^bytes value))]
    (json/decode-stream (InputStreamReader. in "UTF-8") true)))

;; SMILE (binary JSON)
(defmethod deserialize "application/jackson-smile"
  [value _]
  (json/decode-smile value true))

(defmethod deserialize "application/smile"
  [value _]
  (json/decode-smile value true))

;; Clojure
(defmethod deserialize "application/clojure"
  [value _]
  (binding [*print-dup* true]
    (read-string (String. ^bytes value))))


(defmethod deserialize :default
  [value content-type]
  (throw (UnsupportedOperationException. (str "Deserializer for content type " content-type " is not defined"))))


(defn deserialize-value
  "Replaces :value key with its deserialized form using :content-type key to
   get value content type"
  [m]
  (assoc m :value (deserialize (:value m) (:content-type m))))


(def ^{:private true} not-nil? (comp not nil?))


(defn ^com.basho.riak.client.bucket.BucketProperties
  to-bucket-properties
  [{:keys [^Boolean allow-siblings ^Boolean last-write-wins ^Integer n-val ^String backend
           ^Integer big-vclock
           ^Integer small-vclock
           ^Long    old-vclock
           ^Long    young-vclock
           ^Boolean not-found-ok
           ^Boolean basic-quorum
           ^Boolean enable-search
           r w pr dw rw pw
           pre-commit-hooks
           post-commit-hooks]
    :or {allow-siblings  false
         n-val           3
         enable-search   false
         old-vclock      86400
         young-vclock    20
         small-vclock    50
         big-vclock      50}}]
  (let [bldr (doto (BucketPropertiesBuilder.)
               (.r             (to-quorum r))
               (.w             (to-quorum w))
               (.pr            (to-quorum pr))
               (.dw            (to-quorum dw))
               (.rw            (to-quorum rw))
               (.pw            (to-quorum pw))
               (.allowSiblings allow-siblings)
               (.search        enable-search)
               (.nVal          n-val)
               (.backend       backend)
               (.smallVClock   small-vclock)
               (.bigVClock     big-vclock)
               (.oldVClock     old-vclock)
               (.youngVClock   young-vclock))]
    (when (not-nil? not-found-ok)    (.notFoundOK    bldr not-found-ok))
    (when (not-nil? last-write-wins) (.lastWriteWins bldr last-write-wins))
    (when (not-nil? basic-quorum)    (.basicQuorum   bldr basic-quorum))
    (when (or (seq pre-commit-hooks) enable-search)
      (.precommitHooks bldr (concat pre-commit-hooks (if enable-search
                                                       [NamedErlangFunction/SEARCH_PRECOMMIT_HOOK]
                                                       []))))
    (when (seq post-commit-hooks)
      (.postcommitHooks bldr post-commit-hooks))
    (.build bldr)))

(defn from-bucket-properties
  [^BucketProperties props]
  {:r  (.getR props)
   :w  (.getW props)
   :pr (.getPR props)
   :dw (.getDW props)
   :rw (.getRW props)
   :pw (.getPW props)
   :search         (.getSearch props)
   :not-found-ok   (.getNotFoundOK props)
   :basic-quorum   (.getBasicQuorum props)
   :allow-siblings (.getAllowSiblings props)
   :last-write-wins (.getLastWriteWins props)
   :n-val           (.getNVal props)
   :backend         (.getBackend props)
   :small-vclock    (.getSmallVClock props)
   :big-vclock      (.getBigVClock props)
   :old-vclock      (.getOldVClock props)
   :young-vclock    (.getYoungVClock props)
   :pre-commit-hooks  (.getPrecommitHooks props)
   :post-commit-hooks (.getPostcommitHooks props)})

(defn ^com.basho.riak.client.bucket.TunableCAPProps
  to-tunable-cap-props
  "Build a TunableCAPProps instance from Clojure map"
  [{:keys [r w dw rw pr pw basic-quorum not-found-ok] :or {not-found-ok false}}]
  (TunableCAPProps. (to-quorum r)
                    (to-quorum w)
                    (to-quorum dw)
                    (to-quorum rw)
                    (to-quorum pr)
                    (to-quorum pw)
                    ^Boolean basic-quorum ^Boolean not-found-ok))


;;
;; Link Walking
;;

(defprotocol LinkWalkingAccumulationFlagConversion
  (^com.basho.riak.client.query.LinkWalkStep$Accumulate
    to-link-walk-step-accumulate [input] "Converts given input to a LinkWalkStep$Accumulate value"))

(extend-protocol LinkWalkingAccumulationFlagConversion
  LinkWalkStep$Accumulate
  (to-link-walk-step-accumulate [input]
    input)

  Boolean
  (to-link-walk-step-accumulate [input]
    (if input
      LinkWalkStep$Accumulate/YES
      LinkWalkStep$Accumulate/NO))

  nil
  (to-link-walk-step-accumulate [input]
    LinkWalkStep$Accumulate/NO)

  Object
  (to-link-walk-step-accumulate [input]
    (if input
      LinkWalkStep$Accumulate/YES
      LinkWalkStep$Accumulate/NO)))


(defn ^com.basho.riak.client.query.LinkWalkStep
  to-link-walk-step
  "Builds a LinkWalkStep instance from provided arguments"
  ([^String bucket-name ^String tag]
     (LinkWalkStep. bucket-name tag LinkWalkStep$Accumulate/DEFAULT))
  ([^String bucket-name ^String tag accumulate?]
     (LinkWalkStep. bucket-name tag (to-link-walk-step-accumulate accumulate?))))
