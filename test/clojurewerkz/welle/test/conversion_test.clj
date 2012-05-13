(ns clojurewerkz.welle.test.conversion-test
  (:use     clojure.test clojurewerkz.welle.conversion)
  (:import [com.basho.riak.client.cap Quora Quorum VClock BasicVClock]
           com.basho.riak.client.bucket.TunableCAPProps
           com.basho.riak.client.util.CharsetUtils
           com.basho.riak.client.http.util.Constants
           [com.basho.riak.client IRiakObject RiakLink]
           [com.basho.riak.client.query LinkWalkStep LinkWalkStep$Accumulate]
           com.basho.riak.client.raw.query.LinkWalkSpec
           [java.util Date UUID]))

(set! *warn-on-reflection* true)

(defn vclock-for
  [^String s]
  (BasicVClock. (CharsetUtils/utf8StringToBytes s)))


(deftest test-quorum-conversion
  (testing "int-to-quorum conversion"
    (are [i q] (is (= (to-quorum i) q))
         1 (Quorum. 1)
         2 (Quorum. 2)
         3 (Quorum. 3)
         4 (Quorum. 4)
         5 (Quorum. 5))
    (are [i q] (is (not (= (to-quorum i) q)))
         1 (Quorum. 5)
         2 (Quorum. 4)
         3 (Quorum. 2)
         4 (Quorum. 1)
         5 (Quorum. 3)))
  (testing "quora-to-quorum conversion"
    (are [qa] (is (= (to-quorum qa) (Quorum. ^Quora qa)))
         Quora/ONE
         Quora/QUORUM
         Quora/ALL
         Quora/DEFAULT))
  (testing "quorum-to-quorum conversion"
    (are [qm] (is (= (to-quorum qm) qm))
         (Quorum. Quora/ONE)
         (Quorum. Quora/QUORUM)
         (Quorum. Quora/ALL)
         (Quorum. Quora/DEFAULT)
         (Quorum. 1)
         (Quorum. 2)
         (Quorum. 3)
         (Quorum. 10)
         (Quorum. 50)
         (Quorum. 100))))

(deftest test-to-store-meta
  (testing "arity-6"
    (let [w               1
          dw              2
          pw              3
          return-body     true
          if-none-match   true
          if-not-modified false
          meta            (to-store-meta w dw pw return-body if-none-match if-not-modified)]
      (is (= (to-quorum 1) (.getW meta)))
      (is (= (to-quorum 2) (.getDw meta)))
      (is (= (to-quorum 3) (.getPw meta)))
      (is (.getReturnBody meta))
      (is (.getIfNoneMatch meta))
      (is (not (.getIfNotModified meta))))))

(deftest test-to-fetch-meta
  (testing "arity-8"
    (let [r                     1
          pr                    2
          not-found-ok          false
          basic-quorum          false
          head-only             true
          return-deleted-vclock true
          if-modified-since     (Date.)
          if-modified-vclock    (vclock-for "I am a vclock")
          meta                  (to-fetch-meta r pr not-found-ok basic-quorum head-only return-deleted-vclock if-modified-since if-modified-vclock)]
      (is (= (to-quorum 1) (.getR meta)))
      (is (= (to-quorum 2) (.getPr meta)))
      (is (not (.getNotFoundOK meta)))
      (is (not (.getBasicQuorum meta)))
      (is (.getHeadOnly meta))
      (is (.getReturnDeletedVClock meta))
      (is (= if-modified-since  (.getIfModifiedSince meta)))
      (is (= if-modified-vclock (.getIfModifiedVClock meta))))))

(deftest test-to-delete-meta
  (testing "arity-7"
    (let [r      1
          pr     2
          w      3
          dw     4
          pw     5
          rw     6
          vclock (vclock-for "I am a vclock")
          meta   (to-delete-meta r pr w dw pw rw vclock)]
      (is (= (to-quorum 1) (.getR meta)))
      (is (= (to-quorum 2) (.getPr meta)))
      (is (= (to-quorum 3) (.getW meta)))
      (is (= (to-quorum 4) (.getDw meta)))
      (is (= (to-quorum 5) (.getPw meta)))
      (is (= (to-quorum 6) (.getRw meta)))
      (is (= vclock (.getVclock meta))))))

(deftest test-to-riak-object
  (testing "building an object with all fields set"
    (let [bucket       (str (UUID/randomUUID))
          key          (str (UUID/randomUUID))
          value        (to-bytes "A value")
          content-type Constants/CTYPE_OCTET_STREAM
          metadata     {"metakey" "metavalue"}
          indexes      {"handle"  #{"johnnyriak"}}
          vclock       (vclock-for "vclock for a riak object")
          links        [{:bucket "pages" :key "http://clojurewerkz.org" :tag "links"}
                        {:bucket "pages" :key "http://clojureriak.info" :tag "links"}]
          ro           (to-riak-object {:bucket bucket :key key :value value :content-type content-type :metadata metadata :indexes indexes :vclock vclock :links links})]
      (is (= bucket       (.getBucket ro)))
      (is (= key          (.getKey ro)))
      (is (= "A value"    (.getValueAsString ro)))
      (is (= content-type (.getContentType ro)))
      (is (= vclock       (.getVClock ro)))
      (is (.hasLinks ro))
      (is (= 2 (.numLinks ro))))))

(deftest test-to-bucket-properties
  (testing "case 1"
    (let [allow-siblings  true
          last-write-wins true
          n-val           5
          backend         "bitcask"
          big-vclock      10
          small-vclock    1
          old-vclock      3
          young-vclock    5
          not-found-ok    true
          basic-quorum    true
          r               1
          w               2
          pr              3
          dw              4
          rw              5
          pw              6
          enable-search   false
          props           (to-bucket-properties {:allow-siblings  allow-siblings
                                                 :last-write-wins last-write-wins
                                                 :not-found-ok    true
                                                 :basic-quorum    true
                                                 :r               r
                                                 :w               w
                                                 :pr              pr
                                                 :dw              dw
                                                 :rw              rw
                                                 :pw              pw
                                                 :backend        "bitcask"
                                                 :big-vclock     10
                                                 :small-vclock   1
                                                 :old-vclock     3
                                                 :young-vclock   5
                                                 :enable-search  enable-search})]
      (is (= (.getR props)  (to-quorum 1)))
      (is (= (.getW props)  (to-quorum 2)))
      (is (= (.getPR props) (to-quorum 3)))
      (is (= (.getDW props) (to-quorum 4)))
      (is (= (.getRW props) (to-quorum 5)))
      (is (= (.getPW props) (to-quorum 6)))
      ;; we must use stricter true/false assertions here because of the way builder
      ;; treats nils. MK.
      (is (true? (.getNotFoundOK props)))
      (is (true? (.getBasicQuorum props)))
      (is (true? (.getAllowSiblings props)))
      (is (true? (.getLastWriteWins props)))
      (is (false? (.getSearch props)))))
  (testing "case 2"
    (let [allow-siblings  false
          last-write-wins false
          n-val           5
          backend         "bitcask"
          big-vclock      10
          small-vclock    1
          old-vclock      3
          young-vclock    5
          not-found-ok    true
          basic-quorum    true
          r               1
          w               2
          pr              3
          dw              4
          rw              5
          pw              6
          enable-search   true
          props           (to-bucket-properties {:allow-siblings  allow-siblings
                                                 :last-write-wins last-write-wins
                                                 :not-found-ok    true
                                                 :basic-quorum    true
                                                 :r               r
                                                 :w               w
                                                 :pr              pr
                                                 :dw              dw
                                                 :rw              rw
                                                 :pw              pw
                                                 :backend        "bitcask"
                                                 :big-vclock     10
                                                 :small-vclock   1
                                                 :old-vclock     3
                                                 :young-vclock   5
                                                 :enable-search  enable-search})]
      (is (false? (.getAllowSiblings props)))
      (is (false? (.getLastWriteWins props)))
      (is (true? (.getSearch props))))))

(deftest test-to-vclock
  (testing "with byte array inputs"
    (let [s                "vclocky"
          ^VClock expected (vclock-for s)
          ^VClock result   (to-vclock s)]
      (is (= (.asString expected) (.asString result)))))
  (testing "with VClock inputs"
    (let [v (vclock-for "vclock")]
      (is (= v (to-vclock v))))))

(deftest test-to-tunable-cap-props
  (let [input  {:r 1 :w 2 :dw 3 :rw 4 :pr 5 :pw 6 :basic-quorum true :not-found-ok false}
        ^TunableCAPProps result (to-tunable-cap-props input)]
    (is (= (Quorum. 1) (.getR result)))
    (is (= (Quorum. 2) (.getW result)))
    (is (= (Quorum. 3) (.getDW result)))
    (is (= (Quorum. 4) (.getRW result)))
    (is (= (Quorum. 5) (.getPR result)))
    (is (= (Quorum. 6) (.getPW result)))
    (is (.getBasicQuorum result))
    (is (not (.getNotFoundOK result)))))


(deftest test-to-index-query
  (testing "with int values"
    (let [bucket-name "bucket-o"
          index-name  "email"
          value       "johndoe@example.com"
          query       (to-index-query value bucket-name index-name)]
      (is (= "email_bin" (.getIndex query)))
      (is (= bucket-name (.getBucket query))))))


(deftest ^{:links true} test-to-riak-link
  (testing "positive scenario"
    (let [m  {:bucket "pages" :key "http://clojureriak.info" :tag "links"}
          rl (to-riak-link m)]
      (is (= "pages" (.getBucket rl)))
      (is (= "http://clojureriak.info" (.getKey rl)))
      (is (= "links" (.getTag rl))))))

(deftest ^{:links true} test-from-riak-link
  (testing "positive scenario"
    (let [m  {:bucket "pages" :key "http://clojureriak.info" :tag "links"}
          rl (RiakLink. "pages" "http://clojureriak.info" "links")]
      (is (= m (from-riak-link rl))))))


(deftest ^{:links true} test-to-link-walk-step-accumulate
  (testing "Accumulate enum inputs"
    (are [x] (is (= x (to-link-walk-step-accumulate x)))
      LinkWalkStep$Accumulate/YES
      LinkWalkStep$Accumulate/NO
      LinkWalkStep$Accumulate/DEFAULT))
  (testing "boolean inputs"
    (are [i o] (is (= o (to-link-walk-step-accumulate i)))
      true  LinkWalkStep$Accumulate/YES
      :yes  LinkWalkStep$Accumulate/YES
      ;; boolean evaluation is used
      ;; (only false and nil evaluate to false)
      :no   LinkWalkStep$Accumulate/YES
      false LinkWalkStep$Accumulate/NO
      nil  LinkWalkStep$Accumulate/NO)))

(deftest ^{:links true} test-to-link-walk-step
  (testing "with boolean accumulation flag"
    (let [bucket-name "things"
          tag         "_"
          lws         (to-link-walk-step bucket-name tag true)]
      (is (= LinkWalkStep$Accumulate/YES (.getKeep lws)))
      (is (= bucket-name (.getBucket lws)))
      (is (= tag (.getTag lws))))
    (let [bucket-name "things"
          tag         "_"
          lws         (to-link-walk-step bucket-name tag false)]
      (is (= LinkWalkStep$Accumulate/NO (.getKeep lws)))))
  (testing "with enum accumulation flag"
    (let [bucket-name "things"
          tag         "_"
          lws         (to-link-walk-step bucket-name tag LinkWalkStep$Accumulate/DEFAULT)]
      (is (= LinkWalkStep$Accumulate/DEFAULT (.getKeep lws)))
      (is (= bucket-name (.getBucket lws)))
      (is (= tag (.getTag lws))))
    (let [bucket-name "things"
          tag         "_"
          lws         (to-link-walk-step bucket-name tag LinkWalkStep$Accumulate/NO)]
      (is (= LinkWalkStep$Accumulate/NO (.getKeep lws))))))
