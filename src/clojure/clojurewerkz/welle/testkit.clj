(ns ^{:doc "Utility functions useful for unit and integration testing of applications
            that use Welle"}
  clojurewerkz.welle.testkit
  (:require [clojurewerkz.welle.objects :as wo]
            [clojurewerkz.welle.buckets :as wb]))

;;
;; API
;;

(defn drain
  "Drains the bucket with the provided name by deleting all the keys in it. For buckets with
   a large number of keys this may be a very expensive operation because it involves listing keys
   in the bucket."
  [^String bucket-name]
  (doseq [k (wb/keys-in bucket-name)]
    (wo/delete bucket-name k)))