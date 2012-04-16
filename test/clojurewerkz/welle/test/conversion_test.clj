(ns clojurewerkz.welle.test.conversion-test
  (:use     clojure.test clojurewerkz.welle.conversion)
  (:import [com.basho.riak.client.cap Quora Quorum BasicVClock]
           [com.basho.riak.client.bucket TunableCAPProps]
           [com.basho.riak.client.util CharsetUtils]
           java.util.Date))


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
