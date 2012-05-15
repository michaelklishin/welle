(ns clojurewerkz.welle.fn
  (:import [com.basho.riak.client.query.functions NamedErlangFunction NamedJSFunction]))


;;
;; API
;;

(defn named-js-fn
  [^String name]
  (NamedJSFunction. name))

(defn named-erlang-fn
  [^String mod ^String fun]
  (NamedErlangFunction. mod fun))