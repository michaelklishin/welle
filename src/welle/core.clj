(ns welle.core
  (:import (com.basho.riak.client IRiakClient RiakFactory)))



;;
;; API
;;

(def ^:dynamic *riak-host* "127.0.0.1")
(def ^:dynamic *riak-port* 8087)

(def ^:dynamic *riak-url* "http://127.0.0.1:8098/riak")

(declare ^:dynamic ^IRiakClient *riak-client*)


(defn connect
  (^IRiakClient []
     (connect *riak-url*))
  (^IRiakClient [url]
     (RiakFactory/httpClient ^String url)))

(defn connect!
  ([]
     (defonce ^:dynamic *riak-client* (connect)))
  ([url]
     (defonce ^:dynamic *riak-client* (connect url))))





(defn connect-via-pcb
  (^IRiakClient []
     (connect *riak-host* *riak-port*))
  (^IRiakClient [host port]
     (RiakFactory/pbcClient ^String host ^long port)))

(defn connect-via-pcb!
  ([]
     (defonce ^:dynamic *riak-client* (connect)))
  ([host port]
     (defonce ^:dynamic *riak-client* (connect host port))))
