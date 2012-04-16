(ns clojurewerkz.welle.core
  (:import com.basho.riak.client.raw.RawClient
           com.basho.riak.client.raw.pbc.PBClientAdapter
           com.basho.riak.client.raw.http.HTTPClientAdapter))



;;
;; API
;;

(def ^{:private true :const true} default-host "127.0.0.1")
(def ^{:private true :const true} default-port 8087)

(def ^{:private true :const true} default-url "http://127.0.0.1:8098/riak")

(def ^:dynamic ^RawClient *riak-client*)


(defn ^com.basho.riak.client.raw.RawClient
  connect
  ([]
     (connect default-url))
  ([^String url]
     (HTTPClientAdapter. (com.basho.riak.client.http.RiakClient. ^String url))))

(defn connect!
  ([]
     (alter-var-root (var *riak-client*) (constantly (connect))))
  ([^String url]
     (alter-var-root (var *riak-client*) (constantly (connect url))))
  ([^String url ^String client-id]
     (alter-var-root (var *riak-client*) (constantly (connect url client-id)))))

(defn connect-via-pb
  ([]
     (connect-via-pb default-host default-port))
  ([^String host ^long port]
     (PBClientAdapter. (com.basho.riak.pbc.RiakClient. host port))))

(defn connect-via-pb!
  ([]
     (alter-var-root (var *riak-client*) (constantly (connect))))
  ([host port]
     (alter-var-root (var *riak-client*) (constantly (connect host port)))))


(defn ping
  ([]
     (.ping *riak-client*))
  ([^RawClient client]
     (.ping client)))

(defn shutdown
  ([]
     (.shutdown *riak-client*))
  ([^RawClient client]
     (.shutdown client)))
