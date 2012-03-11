(defproject com.novemberain/welle "0.1.0-SNAPSHOT"
  :description "An experimental wrapper around Riak Java client"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [com.basho.riak/riak-client "1.0.4"]]
  :profiles {:all { :dependencies [[com.basho.riak/riak-client "1.0.4"]] }
             :1.4 { :dependencies [[org.clojure/clojure "1.4.0-beta4"]] }}
  :repositories {"sonatype" {:url
                             "http://oss.sonatype.org/content/repositories/releases",
                             :snapshots false,
                             :releases {:checksum :fail, :update :always}}}
  :warn-on-reflection true)