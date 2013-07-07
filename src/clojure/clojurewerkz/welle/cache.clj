(ns clojurewerkz.welle.cache
  "clojure.core.cache implementation(s) on top of Riak."
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
    (get-in (kv/fetch-one (.bucket c) k) [:result :value]))
  (has? [c k]
        (println (kv/fetch (.bucket c) k :head-only true))
    (not (:has-value? (kv/fetch (.bucket c) k :head-only true))))
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
  ([^String bucket ^String content-type ^Integer w]
     (BasicWelleCache. bucket content-type w))
  ([^String bucket base ^String content-type ^Integer w]
     (cache/seed (BasicWelleCache. bucket content-type w) base)))
