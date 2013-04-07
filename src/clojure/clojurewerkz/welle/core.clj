(ns clojurewerkz.welle.core
  (:import com.basho.riak.client.raw.RawClient
           [com.basho.riak.client.raw.http HTTPClientConfig HTTPClientConfig$Builder
            HTTPClusterClient HTTPClusterConfig]
           [com.basho.riak.client.raw.pbc PBClientAdapter PBClusterClient PBClusterConfig]
           com.basho.riak.client.raw.config.ClusterConfig
           clojurewerkz.welle.HTTPClient))



;;
;; API
;;

(def ^{:private true :const true} default-host "127.0.0.1")
(def ^{:private true :const true} default-http-port 8098)
(def ^{:private true :const true} default-pb-port 8087)

(def ^{:private true :const true} default-url "http://127.0.0.1:8098/riak")

(def ^:dynamic ^RawClient *riak-client*)

(def ^{:const true} default-cluster-connection-limit 32)


(defn ^clojurewerkz.welle.HTTPClient
  connect
  "Creates an HTTP client for a given URL, optionally with a custom client ID.
  With no arguments, connects to localhost on the default Riak port."
  ([]
     (connect default-url))
  ([^String url]
     (let [c (HTTPClient. (com.basho.riak.client.http.RiakClient. ^String url))]
       (.generateAndSetClientId c)
       c))
  ([^String url ^bytes client-id]
     (let [^HTTPClient c (connect url)]
       (.setClientId c client-id)
       c)))

(defn connect!
  "Creates an HTTP client for a given URL, and sets the global variable
  *riak-client*. All Welle functions which are not passed a client will use
  this client by default."
  ([]
     (alter-var-root (var *riak-client*) (constantly (connect))))
  ([^String url]
     (alter-var-root (var *riak-client*) (constantly (connect url))))
  ([^String url ^String client-id]
     (alter-var-root (var *riak-client*) (constantly (connect url client-id)))))

(defn connect-via-pb
  "Creates a Protocol Buffers client for the given host and port, or, by
  default, to localhost on the default Riak PB port."
  ([]
     (connect-via-pb default-host default-pb-port))
  ([^String host ^long port]
     (PBClientAdapter. (com.basho.riak.pbc.RiakClient. host port))))

(defn connect-via-pb!
  "Creates a Protocol Buffers client for the given host and port, and sets the
  global variable *riak-client*. All Welle functions which are not passed a
  client will use this client by default."
  ([]
     (alter-var-root (var *riak-client*) (constantly (connect-via-pb))))
  ([host port]
     (alter-var-root (var *riak-client*) (constantly (connect-via-pb host port)))))

(defprotocol HTTPClusterConfigurator
  (http-cluster-config-from [self]))

(extend-type HTTPClusterConfig
  HTTPClusterConfigurator
  (http-cluster-config-from [self] self))

(extend-type java.util.Collection
  HTTPClusterConfigurator
  (http-cluster-config-from [endpoints]
    (let [res (HTTPClusterConfig. default-cluster-connection-limit)]
      (doseq [^String endpoint endpoints]
        (.addClient res (-> (HTTPClientConfig$Builder.)
                            (.withUrl endpoint)
                            (.build))))
      res)))

(defn ^com.basho.riak.client.raw.RawClient
  connect-to-cluster
  "Creates an HTTP cluster client."
  [endpoints]
  (let [^ClusterConfig cc (http-cluster-config-from endpoints)]
    (HTTPClusterClient. cc)))

(defn connect-to-cluster!
  "Creates an HTTP cluster client, and sets the global variable *riak-client*.
  All Welle functions which are not passed a client will use this client by
  default."
  [endpoints]
  (alter-var-root (var *riak-client*) (constantly (connect-to-cluster endpoints))))



(defn- pbc-cluster-config-from
  ([endpoints]
     (doto (PBClusterConfig. default-cluster-connection-limit)
       (.addHosts (into-array String endpoints)))))

(defn ^com.basho.riak.client.raw.RawClient
  connect-to-cluster-via-pb
  "Creates a Protocol Buffers cluster client given a sequence of string
  endpoints."
  [endpoints]
  (let [^ClusterConfig cc (pbc-cluster-config-from endpoints)]
    (PBClusterClient. cc)))

(defn connect-to-cluster-via-pb!
  "Creates a Protocol Buffers cluster client given a sequence of string
  endpoints, and sets the global variable *riak-client*.  All Welle functions
  which are not passed a client will use this client by default."
  [endpoints]
  (alter-var-root (var *riak-client*) (constantly (connect-to-cluster-via-pb endpoints))))



(defmacro with-client
  "Evaluates body within an implicit do, with the Welle client *riak-client*
  bound to the given Riak client."
  [client & forms]
  `(binding [*riak-client* ~client]
     (do ~@forms)))


(defn ping
  "Pings a client."
  ([]
     (.ping *riak-client*))
  ([^RawClient client]
     (.ping client)))

(defn shutdown
  "Shuts down a client."
  ([]
     (.shutdown *riak-client*))
  ([^RawClient client]
     (.shutdown client)))


(defn get-client-id
  "The client ID used by a given client."
  []
  (.getClientId *riak-client*))

(defn stats
  "Returns statistics for a client."
  []
  (.stats *riak-client*))

(defn get-base-url
  "Returns base HTTP transport URL (e.g. http://127.0.0.1:8098)"
  ([]
     (.getBaseUrl ^HTTPClient *riak-client*))
  ([^HTTPClient client]
     (.getBaseUrl client)))
