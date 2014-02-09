;; Copyright (c) 2012-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.welle.hooks
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
