;; Copyright (c) 2012-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.welle.testkit
  "Utility functions useful for unit and integration testing of applications
   that use Welle"
  (:require [clojurewerkz.welle.kv      :as kv]
            [clojurewerkz.welle.buckets :as wb])
  (:import com.basho.riak.client.raw.RawClient))

;;
;; API
;;

(defn drain
  "Drains the bucket with the provided name by deleting all the keys in it. For buckets with
   a large number of keys this may be a very expensive operation because it involves listing keys
   in the bucket."
  [^RawClient client ^String bucket-name]
  (doseq [k (wb/keys-in client bucket-name)]
    (kv/delete client bucket-name k {:w 1})))
o
