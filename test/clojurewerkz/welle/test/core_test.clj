(ns clojurewerkz.welle.test.core-test
  (:import com.basho.riak.client.raw.RawClient)
  (:require [clojurewerkz.welle.core :as wc])
  (:use clojure.test))

(set! *warn-on-reflection* true)

(deftest connect-using-http-client-and-default-host-and-port
  (let [^RawClient client (wc/connect)]
    (dotimes [x 10]
      (.ping client)
      (wc/ping client)
      (wc/shutdown client))))

(deftest connect-using-http-client-default-host-and-port-and-default-client
  (wc/connect!)
  (dotimes [x 10]
    (.ping ^RawClient wc/*riak-client*)
    (wc/ping)
    (wc/shutdown)))


(deftest connect-using-clustered-http-client
  (let [^RawClient client (wc/connect-to-cluster ["http://127.0.0.1:8098/riak"
                                                  "http://localhost:8098/riak"])]
    (dotimes [x 10]
      (.ping client)
      (wc/ping client)
      (wc/shutdown client))))

(deftest connect-using-clustered-http-client-with-port-specified
  (let [^RawClient client (wc/connect-to-cluster ["http://127.0.0.1:8098/riak"
                                                  "http://localhost:8098/riak"])]
    (dotimes [x 10]
      (.ping client)
      (wc/ping client)
      (wc/shutdown client))))

(deftest connect-using-clustered-http-client-and-default-client
  (wc/connect-to-cluster! ["http://127.0.0.1:8098/riak"
                           "http://localhost:8098/riak"])
  (dotimes [x 10]
    (.ping ^RawClient wc/*riak-client*)
    (wc/ping)
    (wc/shutdown)))

(deftest connect-using-clustered-http-client-and-default-client-with-port-specified
  (wc/connect-to-cluster! ["http://127.0.0.1:8098/riak"
                           "http://localhost:8098/riak"])
  (dotimes [x 10]
    (.ping ^RawClient wc/*riak-client*)
    (wc/ping)
    (wc/shutdown)))
