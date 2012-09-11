(ns ^{:doc "Provides access to Riak Search via the Solr API.

            Only HTTP transport is supported."}
  clojurewerkz.welle.solr
  (:require [clojurewerkz.welle.core :as wc]
            [clj-http.client         :as http]
            [cheshire.core           :as json]
            [clojure.data.xml        :as x])
  (:import clojurewerkz.welle.HTTPClient))

;;
;; Implementation
;;

(defn- get-base-solr-url
  "Returns base Sorl API URL (e.g. http://127.0.0.1:8098/solr)"
  ([]
     (get-base-solr-url wc/*riak-client*))
  ([^HTTPClient client]
     (str (.getBaseUrl client) "/solr")))

(defn- get-solr-query-url
  "Returns Sorl query endpoint URL for the given index (e.g. http://127.0.0.1:8098/solr/production_index/select)"
  ([^String index]
     (get-solr-query-url wc/*riak-client* index))
  ([^HTTPClient client ^String index]
     (str (get-base-solr-url wc/*riak-client*) "/" index "/select")))

(defn- get-solr-update-url
  "Returns Sorl update (index, delete, etc) endpoint URL for the given index (e.g. http://127.0.0.1:8098/solr/production_index/update)"
  ([^String index]
     (get-solr-update-url wc/*riak-client* index))
  ([^HTTPClient client ^String index]
     (str (get-base-solr-url wc/*riak-client*) "/" index "/update")))

(defn- delete-via-query-body
  [^String query]
  (x/emit-str
   (x/element :delete {}
              (x/element :query {} query))))

(defn- ->xml-field
  [[k v]]
  (x/element :field {:name (name k)} (str v)))

(defn- doc->xml-fields
  [m]
  (map ->xml-field m))

(defn- doc->xml
  [m]
  (x/element :doc {}
             (doc->xml-fields m)))

(defn- as-vec
  [xs]
  (if (sequential? xs)
    xs
    [xs]))

(defn- index-document-body
  [xs]
  (x/emit-str (x/element :add {}
                         (map doc->xml (as-vec xs)))))

(def ^{:const true}
  application-xml "application/xml")

;;
;; API
;;

(defn delete-via-query
  ([^String query]
     (let [url            (get-solr-update-url)
           {:keys [body]} (http/post url {:content-type application-xml :body (delete-via-query-body query)})]
       nil))
  ([^String index ^String query]
     (let [url            (get-solr-update-url index)
           {:keys [body]} (http/post url {:content-type application-xml :body (delete-via-query-body query)})]
       ;; looks like the response is always empty
       nil)))

(defn index
  ([doc]
     (let [url            (get-solr-update-url)
           {:keys [body]} (http/post url {:content-type application-xml :body (index-document-body doc)})]
       doc))
  ([^String idx doc]
     (let [url            (get-solr-update-url idx)
           {:keys [body]} (http/post url {:content-type application-xml :body (index-document-body doc)})]
       ;; looks like the response is always empty
       doc)))

(defn search
  [^String index ^String query & {:as options}]
  (let [url            (get-solr-query-url index)
        qp             (merge options {"wt" "json" "q" query})
        {:keys [body]} (http/get url {:query-params qp})]
    (json/parse-string body true)))

(defn search-across-all-indexes
  [^String query & {:as options}]
  (let [url            (get-solr-query-url)
        qp             (merge options {"wt" "json" "q" query})
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