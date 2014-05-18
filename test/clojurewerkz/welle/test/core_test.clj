(ns clojurewerkz.welle.test.core-test
  (:import com.basho.riak.client.raw.RawClient
           [com.basho.riak.client.raw.http HTTPClientConfig$Builder HTTPClusterConfig])
  (:require [clojurewerkz.welle.core :as wc]
            [clojure.test :refer :all]))

(deftest connect-using-http-client-and-default-host-and-port
  (let [c (wc/connect)]
    (dotimes [x 10]
      (.ping c)
      (wc/ping c)
      (wc/shutdown c))))


(deftest connect-using-clustered-http-client
  (let [c (wc/connect-to-cluster ["http://127.0.0.1:8098/riak"
                                  "http://localhost:8098/riak"])]
    (dotimes [x 10]
      (.ping c)
      (wc/ping c)
      (wc/shutdown c))))

(deftest connect-using-clustered-http-client-with-port-specified
  (let [c (wc/connect-to-cluster ["http://127.0.0.1:8098/riak"
                                  "http://localhost:8098/riak"])]
    (dotimes [x 10]
      (.ping c)
      (wc/ping c)
      (wc/shutdown c))))

(deftest connect-using-clustered-http-client-with-config-instance
  (let [config (doto (HTTPClusterConfig. wc/default-cluster-connection-limit)
                 (.addClient (-> (HTTPClientConfig$Builder.)
                                 (.withUrl "http://127.0.0.1:8098/riak")
                                 (.build))))
        c      (wc/connect-to-cluster config)]
    (dotimes [x 10]
      (.ping c)
      (wc/ping c)
      (wc/shutdown c))))

(deftest connect-using-clustered-http-client-and-default-client-with-port-specified
  (let [c (wc/connect-to-cluster ["http://127.0.0.1:8098/riak"
                                  "http://localhost:8098/riak"])]
    (dotimes [x 10]
      (.ping c)
      (wc/ping c)
      (wc/shutdown c))))

(deftest connect-using-clustered-http-client-and-default-client-with-config-instance
  (let [config (doto (HTTPClusterConfig. wc/default-cluster-connection-limit)
                 (.addClient (-> (HTTPClientConfig$Builder.)
                                 (.withUrl "http://127.0.0.1:8098/riak")
                                 (.build))))
        c      (wc/connect-to-cluster config)]
    (dotimes [x 10]
      (.ping ^RawClient c)
      (wc/ping c)
      (wc/shutdown c))))
