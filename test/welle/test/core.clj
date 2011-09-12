(ns welle.test.core
  (:use [welle.core])
  (:use [clojure.test]))


(deftest connect-using-pcb-client-and-default-host-and-port
  (let [client (welle.core/connect)]
    (dotimes [x 10]
      (.ping client))))

