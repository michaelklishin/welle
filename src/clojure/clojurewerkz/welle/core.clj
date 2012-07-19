(ns clojurewerkz.welle.core
  (:import com.basho.riak.client.raw.RawClient
           [com.basho.riak.client.raw.http HTTPClientAdapter HTTPClusterClient HTTPClusterConfig]
           [com.basho.riak.client.raw.pbc PBClientAdapter PBClusterClient PBClusterConfig]
           com.basho.riak.client.raw.config.ClusterConfig))



;;
;; API
;;

(def ^{:private true :const true} default-host "127.0.0.1")
(def ^{:private true :const true} default-port 8087)

(def ^{:private true :const true} default-url "http://127.0.0.1:8098/riak")

(def ^:dynamic ^RawClient *riak-client*)

(def ^{:const true} default-cluster-connection-limit 32)


(defn ^com.basho.riak.client.raw.RawClient
  connect
  ([]
     (connect default-url))
  ([^String url]
     (let [c (HTTPClientAdapter. (com.basho.riak.client.http.RiakClient. ^String url))]
       (.generateAndSetClientId c)
       c))
  ([^String url ^bytes client-id]
     (let [^RawClient c (connect url)]
       (.setClientId c client-id)
       c)))

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
     (alter-var-root (var *riak-client*) (constantly (connect-via-pb))))
  ([host port]
     (alter-var-root (var *riak-client*) (constantly (connect-via-pb host port)))))


(defn- http-cluster-config-from
  ([endpoints]
     (doto (HTTPClusterConfig. default-cluster-connection-limit)
       (.addHosts (into-array String endpoints)))))

(defn ^com.basho.riak.client.raw.RawClient
  connect-to-cluster
  [endpoints]
  (let [^ClusterConfig cc (http-cluster-config-from endpoints)]
    (HTTPClusterClient. cc)))

(defn connect-to-cluster!
  [endpoints]
  (alter-var-root (var *riak-client*) (constantly (connect-to-cluster endpoints))))



(defn- pbc-cluster-config-from
  ([endpoints]
     (doto (PBClusterConfig. default-cluster-connection-limit)
       (.addHosts (into-array String endpoints)))))

(defn ^com.basho.riak.client.raw.RawClient
  connect-to-cluster-via-pb
  [endpoints]
  (let [^ClusterConfig cc (pbc-cluster-config-from endpoints)]
    (PBClusterClient. cc)))

(defn connect-to-cluster-via-pb!
  [endpoints]
  (alter-var-root (var *riak-client*) (constantly (connect-to-cluster-via-pb endpoints))))



(defmacro with-client
  [client & forms]
  `(binding [*riak-client* ~client]
     (do ~@forms)))


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


(defn get-client-id
  []
  (.getClientId *riak-client*))

(defn stats
  []
  (.stats *riak-client*))
