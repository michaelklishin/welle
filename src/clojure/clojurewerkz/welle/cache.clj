;; Copyright (c) 2012-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.welle.cache
  "clojure.core.cache implementation(s) on top of Riak."
  (:require [clojurewerkz.welle.kv :as kv]
            [clojure.core.cache    :as cache])
  (:import com.basho.riak.client.raw.RawClient
           clojure.core.cache.CacheProtocol
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

(cache/defcache BasicWelleCache [^RawClient client ^String bucket ^String content-type ^Integer w]
  cache/CacheProtocol
  (lookup [c k]
    (get-in (kv/fetch-one (.client c) (.bucket c) k) [:result :value]))
  (has? [c k]
        (:has-value? (kv/fetch (.client c) (.bucket c) k {:head-only true})))
  (hit [this k]
    this)
  (miss [c k v]
    (kv/store (.client c) (.bucket c) k v {:content-type (.content-type c) :w (.w c)})
    c)
  (evict [c k]
    (kv/delete (.client c) (.bucket c) k {:w (.w c)})
    c)
  (seed [c m]
    (doseq [[k v] m]
      (kv/store (.client c) (.bucket c) k v {:content-type (.content-type c) :w (.w c)}))
    c))

(defn basic-welle-cache-factory
  ([^RawClient client]
     (BasicWelleCache. client default-cache-bucket default-content-type 1))
  ([^RawClient client ^String bucket]
     (BasicWelleCache. client bucket default-content-type 1))
  ([^RawClient client ^String bucket ^String content-type ^Integer w]
     (BasicWelleCache. client bucket content-type w))
  ([^RawClient client ^String bucket base ^String content-type ^Integer w]
     (cache/seed (BasicWelleCache. client bucket content-type w) base)))
