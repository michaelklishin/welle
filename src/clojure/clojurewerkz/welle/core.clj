;; Copyright (c) 2012-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.welle.core
  (:import com.basho.riak.client.raw.RawClient
           [com.basho.riak.client.raw.http HTTPClientConfig HTTPClientConfig$Builder
            HTTPClusterClient HTTPClusterConfig]
           [com.basho.riak.client.raw.pbc PBClientAdapter PBClusterClient
            PBClusterConfig PBClientConfig$Builder]
           com.basho.riak.client.raw.config.ClusterConfig
           clojurewerkz.welle.HTTPClient))



;;
;; API
;;

(def ^{:private true :const true} default-host "127.0.0.1")
(def ^{:private true :const true} default-http-port 8098)
(def ^{:private true :const true} default-pb-port 8087)

(def ^{:private true :const true} default-url "http://127.0.0.1:8098/riak")

(def ^{:const true} default-cluster-connection-limit 32)


(defn ^HTTPClient
  connect
  "Creates an HTTP client for a given URL, optionally with a custom client ID.
  With no arguments, connects to localhost on the default Riak port."
  ([]
     (connect default-url))
  ([^String url]
     (doto (HTTPClient. (com.basho.riak.client.http.RiakClient. ^String url))
       (.generateAndSetClientId)))
  ([^String url ^bytes client-id]
     (let [^HTTPClient c (connect url)]
       (.setClientId c client-id)
       c)))

(defn ^PBClientAdapter connect-via-pb
  "Creates a Protocol Buffers client for the given host and port, or, by
  default, to localhost on the default Riak PB port."
  ([]
     (connect-via-pb default-host default-pb-port))
  ([^String host ^long port]
     (doto (PBClientAdapter. (com.basho.riak.pbc.RiakClient. host port))
       (.generateAndSetClientId))))

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

(defn ^RawClient
  connect-to-cluster
  "Creates an HTTP cluster client."
  [endpoints]
  (let [^ClusterConfig cc (http-cluster-config-from endpoints)]
    (HTTPClusterClient. cc)))

(defprotocol PBClusterConfigurator
  (pbc-cluster-config-from [self]))

(extend-type PBClusterConfig
  PBClusterConfigurator
  (pbc-cluster-config-from [self] self))

(extend-type java.util.Collection
  PBClusterConfigurator
  (pbc-cluster-config-from [endpoints]
    (let [res (PBClusterConfig. default-cluster-connection-limit)]
      (doseq [^String endpoint endpoints]
        (let [[host port-str] (seq (.split endpoint ":" 2))
              port (when port-str (Integer/parseInt port-str))]
          (.addClient res (-> (PBClientConfig$Builder.)
                              (.withHost host)
                              (.withPort (or port
                                             default-pb-port))
                              (.build)))))
      res)))

(defn ^PBClusterClient
  connect-to-cluster-via-pb
  "Creates a Protocol Buffers cluster client given a sequence of string
  endpoints."
  [endpoints]
  (let [^ClusterConfig cc (pbc-cluster-config-from endpoints)]
    (PBClusterClient. cc)))

(defn ping
  "Pings a client."
  [^RawClient client]
  (.ping client))

(defn shutdown
  "Shuts down a client."
  [^RawClient client]
  (.shutdown client))


(defn get-client-id
  "The client ID used by a given client."
  [^RawClient client]
  (.getClientId client))

(defn stats
  "Returns statistics for a client."
  [^RawClient client]
  (.stats client))

(defn get-base-url
  "Returns base HTTP transport URL (e.g. http://127.0.0.1:8098)"
  [^HTTPClient client]
  (.getBaseUrl client))
