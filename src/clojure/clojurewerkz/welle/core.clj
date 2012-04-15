(ns clojurewerkz.welle.core
  (:import [com.basho.riak.client IRiakClient RiakFactory]))



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
  (^IRiakClient [^String url]
                (RiakFactory/httpClient ^String url))
  (^IRiakClient [^String url ^String client-id]
                (doto (RiakFactory/httpClient ^String url)
                  (.setClientId (.getBytes client-id)))))

(defn connect!
  ([]
     (alter-var-root (var *riak-client*) (constantly (connect))))
  ([^String url]
     (alter-var-root (var *riak-client*) (constantly (connect url))))
  ([^String url ^String client-id]
     (alter-var-root (var *riak-client*) (constantly (connect url client-id)))))

(defn connect-via-pcb
  (^IRiakClient []
                (connect *riak-host* *riak-port*))
  (^IRiakClient [^String host ^long port]
                (RiakFactory/pbcClient host port)))

(defn connect-via-pcb!
  ([]
     (alter-var-root (var *riak-client*) (constantly (connect))))
  ([host port]
     (alter-var-root (var *riak-client*) (constantly (connect host port)))))


(defn ping
  ([]
     (.ping *riak-client*))
  ([^IRiakClient client]
     (.ping client)))

(defn shutdown
  ([]
     (.shutdown *riak-client*))
  ([^IRiakClient client]
     (.shutdown client)))
