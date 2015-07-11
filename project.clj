(defproject com.novemberain/welle "3.1.0-SNAPSHOT"
  :description "Welle is an expressive Clojure client for Riak with batteries included"
  :url "http://clojureriak.info"
  :license {:name "Eclipse Public License"}
  :min-lein-version "2.5.1"
  :dependencies [[org.clojure/clojure        "1.6.0"]
                 [com.basho.riak/riak-client "1.4.4" :exclusions [org.apache.httpcomponents/httpclient]]
                 [cheshire                   "5.3.1"]
                 [clojurewerkz/support       "1.1.0"]
                 ;; for the Riak Search Solr API support. When Riak Client supports
                 ;; search natively, we should be able to just use what it provides.
                 [clj-http                   "0.9.1"]
                 [org.clojure/data.xml       "0.0.7" :exclusions [org.clojure/clojure]]]
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options     ["-target" "1.6" "-source" "1.6"]
  :profiles       {:1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
                   :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
                   :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
                   :master {:dependencies [[org.clojure/clojure "1.7.0-master-SNAPSHOT"]]}
                   :dev {:resource-paths ["test/resources"]
                         :dependencies [[org.clojure/core.cache "0.6.3" :exclusions [org.clojure/clojure]]
                                        [ring/ring-core         "1.2.1"]]
                         :plugins [[codox "0.8.10"]]
                         :codox {:sources ["src/clojure"]
                                 :output-dir "doc/api"}}}
  :mailing-list {:name "clojure-riak"
                 :archive "https://groups.google.com/group/clojure-riak"
                 :post "clojure-riak@googlegroups.com"}
  :aliases        {"all" ["with-profile" "dev:dev,1.4:dev,1.5:dev,1.7:dev,master"]}
  :test-selectors {:focus   :focus
                   :2i      :2i
                   :cache   :cache
                   :mr      :mr
                   :links   :links
                   :search  :search
                   ;; as in, edge Riak features
                   ::edge-features :edge-features
                   :default (fn [m] (not (or (:edge-features m)
                                             (:search m))))
                   :all     (constantly true)}
  :repositories   {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                               :snapshots false
                               :releases {:checksum :fail :update :always}}
                   "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                         :snapshots true
                                         :releases {:checksum :fail :update :always}}}
  :global-vars {*warn-on-reflection* true})
