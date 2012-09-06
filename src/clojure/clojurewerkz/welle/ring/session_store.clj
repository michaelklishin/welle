(ns clojurewerkz.welle.ring.session-store
  (:require [ring.middleware.session.store :as ringstore]
            [clojurewerkz.welle.kv         :as kv])
  (:import [java.util UUID Date]
           com.basho.riak.client.http.util.Constants))

;;
;; Implementation
;;

(def ^{:const true}
  default-session-store-bucket "web_sessions")

(def ^{:const true}
  default-r 2)

(def ^{:const true}
  default-w 2)



;;
;; API
;;

(defrecord RiakSessionStore [^String bucket-name r w content-type])

(extend-protocol ringstore/SessionStore
  RiakSessionStore

  (read-session [store key]
    (if-let [m (and key
                    (:value (kv/fetch-one (.bucket-name store) key :r (.r store))))]
      m
      {}))

  (write-session [store key data]
    (let [key (or key (str (UUID/randomUUID)))]
      (kv/store (.bucket-name store) key (assoc data :date (Date.))
             :content-type (.content-type store) :w (.w store))
      key))

  (delete-session [store key]
    (kv/delete (.bucket-name store) key :w (.w store))
    nil))


(defn welle-store
  ([]
     (RiakSessionStore. default-session-store-bucket default-r default-w Constants/CTYPE_JSON_UTF8))
  ([^String bucket-name]
     (RiakSessionStore. bucket-name default-r default-w Constants/CTYPE_JSON_UTF8))
  ([^String bucket-name r]
     (RiakSessionStore. bucket-name r default-w Constants/CTYPE_JSON_UTF8))
  ([^String bucket-name r w]
     (RiakSessionStore. bucket-name r w Constants/CTYPE_JSON_UTF8))
  ([^String bucket-name r w ^String content-type]
     (RiakSessionStore. bucket-name r w content-type)))
