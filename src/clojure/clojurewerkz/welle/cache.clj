(ns ^{:doc "clojure.core.cache implementation(s) on top of MongoDB."
      :author "Michael S. Klishin"}
  clojurewerkz.welle.cache
  (:require [clojurewerkz.welle.kv :as kv]
            [clojure.core.cache    :as cache])
  (:import clojure.core.cache.CacheProtocol
           com.basho.riak.client.http.util.Constants))

;;
;; Implementation
;;

(def ^{:const true}
  default-cache-bucket "cache_entries")
(def ^{:const true}
  default-content-type Constants/CTYPE_JSON_UTF8)

;;
;; API
;;

(cache/defcache BasicWelleCache [^String bucket ^String content-type ^Integer w]
  cache/CacheProtocol
  (lookup [c k]
    (:value (kv/fetch-one (.bucket c) k)))
  (has? [c k]
    (not (empty? (kv/fetch (.bucket c) k :head-only true))))
  (hit [this k]
    this)
  (miss [c k v]
    (kv/store (.bucket c) k v :content-type (.content-type c) :w (.w c))
    c)
  (evict [c k]
    (kv/delete (.bucket c) k :w (.w c))
    c)
  (seed [c m]
    (doseq [[k v] m]
      (kv/store (.bucket c) k v :content-type (.content-type c) :w (.w c)))
    c))

(defn basic-welle-cache-factory
  ([]
     (BasicWelleCache. default-cache-bucket default-content-type 1))
  ([^String bucket]
     (BasicWelleCache. bucket default-content-type 1))
  ([^String bucket base ^String content-type ^Integer w]
     (cache/seed (BasicWelleCache. bucket content-type w) base)))
