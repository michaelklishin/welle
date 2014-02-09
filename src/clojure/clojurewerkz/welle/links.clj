;; Copyright (c) 2012-2014 Michael S. Klishin
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.welle.links
  (:require [clojurewerkz.welle.conversion :refer :all]
            [clojurewerkz.welle.core :refer [*riak-client*]])
  (:import com.basho.riak.client.raw.query.LinkWalkSpec
           java.util.LinkedList))

;;
;; Implementation
;;

(defn- ^java.util.LinkedList
  to-linked-list
  [xs]
  (let [l (LinkedList.)]
    (doseq [x xs]
      (.add l x))
    l))

(defn- map-links
  "Maps a single link walking operation step to deserialized Clojure representation
   of IRiakObject"
  [xs]
  (map (comp deserialize-value from-riak-object) xs))


;;
;; DSL
;;

(defn start-at
  [^String bucket-name ^String key]
  [bucket-name key])

(defn step
  ([^String bucket-name ^String tag]
     (to-link-walk-step bucket-name tag))
  ([^String bucket-name ^String tag accumulate?]
     (to-link-walk-step bucket-name tag accumulate?)))

(defn walk
  "Performs a link walk operation described by the provided starting point and one or more steps.
   Both starting point and steps are typically provided using `start-at` and `step` functions that
   together with `walk` form a DSL

   Examples:

   (walk
     (start-at \"people\" \"peter\")
     (step     \"people\" \"friend\" true))"
  [starting-point & steps]
  (let [[bucket-name key] starting-point
        lws               (LinkWalkSpec. (to-linked-list steps) bucket-name key)
        ;; iterable over a collection of linked lists, each of those has IRiakObjects
        raw-result        (.linkWalk *riak-client* lws)]
    (map map-links (into [] raw-result))))
