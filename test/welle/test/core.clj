(ns welle.test.core
  (:import (com.basho.riak.client IRiakClient))
  (:use [clojure.test]
        [welle.core]))

(set! *warn-on-reflection* true)

(deftest connect-using-pcb-client-and-default-host-and-port
  (let [client (welle.core/connect)]
    (dotimes [x 10]
      (.ping client))))

(deftest connect-using-pcb-client-default-host-and-port-and-default-client
  (welle.core/connect!)
  (dotimes [x 10]
    (.ping ^IRiakClient welle.core/*riak-client*)))
