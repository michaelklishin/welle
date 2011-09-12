(ns welle.core
  (:import (com.basho.riak.client IRiakClient RiakFactory)))



;;
;; API
;;

(def ^:dynamic *riak-host* "127.0.0.1")
(def ^:dynamic *riak-port* 8087)

(def ^:dynamic *riak-client*)


(defn connect
  ([]
     (connect *riak-host* *riak-port*))
  ([host port]
     (RiakFactory/pbcClient ^String host ^long port)))
