(ns ^{:doc "Provides access to Riak Search via the Solr API.

            Only HTTP transport is supported."}
  clojurewerkz.welle.solr
  (:require [clojurewerkz.welle.core :as wc]
            [clj-http.client         :as http]
            [cheshire.core           :as json]))

;;
;; API
;;

(defn search
  [^String index ^String query & {:as options}]
  (let [url            (wc/get-solr-url index)
        qp             (merge options
                              {"wt" "json" "q" query})
        {:keys [body]} (http/get url {:query-params qp})]
    (json/parse-string body true)))

(defn total-hits
  [response]
  (get-in response [:response :numFound]))

(defn any-hits?
  "Returns true if a response has any search hits, false otherwise"
  [response]
  (> (total-hits response) 0))

(def no-hits? (complement any-hits?))

(defn hits-from
  "Returns search hits from a response as a collection. To retrieve hits overview, get the :hits
   key from the response"
  [response]
  (get-in response [:response :docs]))