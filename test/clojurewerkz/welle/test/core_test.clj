(ns clojurewerkz.welle.test.core-test
  (:import [com.basho.riak.client IRiakClient])
  (:require [clojurewerkz.welle.core :as wc])
  (:use clojure.test))

(set! *warn-on-reflection* true)

(deftest connect-using-pcb-client-and-default-host-and-port
  (let [client (wc/connect)]
    (dotimes [x 10]
      (.ping client)
      (wc/ping client)
      (wc/shutdown client))))

(deftest connect-using-pcb-client-default-host-and-port-and-default-client
  (wc/connect!)
  (dotimes [x 10]
    (.ping ^IRiakClient wc/*riak-client*)
    (wc/ping)
    (wc/shutdown)))
