(defproject com.novemberain/welle "0.1.0-SNAPSHOT"
  :description "An experimental idiomatic Clojure library on top of the Riak Java client"
  :license {:name "Eclipse Public License"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [com.basho.riak/riak-client "1.0.5"]]
  :source-paths ["src/clojure"]
  :profiles {:1.4 { :dependencies [[org.clojure/clojure "1.4.0-beta7"]] }}
  :aliases { "all" ["with-profile" "dev:dev,1.4"] }
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases",
                             :snapshots false
                             :releases {:checksum :fail :update :always}}}
  :warn-on-reflection true)
