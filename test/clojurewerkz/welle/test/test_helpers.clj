(ns clojurewerkz.welle.test.test-helpers
  (:require [clojurewerkz.welle.core :as wc])
  (:import java.util.Random))

(defn connect
  "Connects using either HTTP or PB transport (randomly chosen)"
  []
  (let [rnd (Random.)]
    (if (.nextBoolean rnd)
      (wc/connect)
      (wc/connect-via-pb))))
