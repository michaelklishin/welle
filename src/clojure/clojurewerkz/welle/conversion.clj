(ns clojurewerkz.welle.conversion
  (:import [com.basho.riak.client.cap Quora Quorum]
           [com.basho.riak.client.bucket TunableCAPProps]))

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



(defn ^com.basho.riak.client.bucket.TunableCAPProps
  to-tunable-cap-props
  "Build a TunableCAPProps instance from Clojure map"
  [{:keys [r w dw rw pr pw basic-quorum not-found-ok] :or {not-found-ok false}}]
  (TunableCAPProps. (to-quorum r)
                    (to-quorum w)
                    (to-quorum dw)
                    (to-quorum rw)
                    (to-quorum pr)
                    (to-quorum pw)
                    ^Boolean basic-quorum ^Boolean not-found-ok))

