(defproject com.novemberain/welle "1.2.0-SNAPSHOT"
  :description "Welle is an expressive Clojure client for Riak with batteries included"
  :url "http://clojureriak.info"  
  :license {:name "Eclipse Public License"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure        "1.3.0"]
                 [com.basho.riak/riak-client "1.0.5"]
                 [org.clojure/data.json      "0.1.2"]
                 [clojurewerkz/support       "0.5.0"]
                 [com.novemberain/validateur "1.1.0"]]
  :source-paths ["src/clojure"]
  :profiles       {:1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
                   :1.5 {:dependencies [[org.clojure/clojure "1.5.0-master-SNAPSHOT"]]}
                   :dev {:resource-paths ["test/resources"]
                         :dependencies [[org.clojure/core.cache "0.6.2" :exclusions [org.clojure/clojure]]
                                        [ring/ring-core         "1.1.1"]]
                         :plugins [[codox "0.6.1"]]
                         :codox {:sources ["src/clojure"]
                                 :output-dir "doc/api"}}}
  :mailing-list {:name "clojure-riak"
                 :archive "https://groups.google.com/group/clojure-riak"
                 :post "clojure-riak@googlegroups.com"}  
  :aliases        {"all" ["with-profile" "dev:dev,1.4:dev,1.5"]}
  :test-selectors {:focus   :focus
                   :2i      :2i
                   :cache   :cache
                   :mr      :mr
                   :links   :links
                   :search  :search
                   :default (constantly true)}
  :repositories   {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                               :snapshots false
                               :releases {:checksum :fail :update :always}}
                   "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                         :snapshots true
                                         :releases {:checksum :fail :update :always}}}
  :warn-on-reflection true)
