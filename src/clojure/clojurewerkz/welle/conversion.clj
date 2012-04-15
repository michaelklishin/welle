(ns clojurewerkz.welle.conversion
  (:import [com.basho.riak.client.cap Quora Quorum]))

;;
;; API
;;

(defprotocol QuorumConversion
  (^com.basho.riak.client.cap.Quorum
    to-quorum [input] "Coerces input to a value suitable for representing a read, write or other quorum/quora.
                      Riak Java client supports passing those values as numerical primitives, Quorum and Quora."))


(extend-protocol QuorumConversion
  Integer
  (to-quorum [input]
    (Quorum. ^Integer input))

  Long
  (to-quorum [input]
    (Quorum. ^Long input))

  Quora
  (to-quorum [input]
    (Quorum. ^Quora input))

  Quorum
  (to-quorum [input]
    input))
