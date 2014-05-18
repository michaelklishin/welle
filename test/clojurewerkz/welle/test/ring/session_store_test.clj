(ns clojurewerkz.welle.test.ring.session-store-test
  (:require [clojurewerkz.welle.core :as wc]
            [clojurewerkz.welle.kv   :as kv]
            [clojure.test :refer :all]
            [clojurewerkz.welle.testkit :refer [drain]]
            [clojurewerkz.welle.test.test-helpers :as th]
            [ring.middleware.session.store :refer :all]
            [clojurewerkz.welle.ring.session-store :refer :all]))


(let [conn (th/connect)]
  (defn purge-sessions
  [f]
  (drain conn "web_sessions")
  (drain conn "sessions")
  (f)
  (drain conn "web_sessions")
  (drain conn "sessions"))

(use-fixtures :each purge-sessions)


(deftest test-reading-a-session-that-does-not-exist
  (let [store (welle-store conn)]
    (is (= {} (read-session store "a-missing-key-1228277")))))


(deftest test-reading-a-session-that-does-exist
  (let [store (welle-store conn)
        sk    (write-session store nil {:library "Welle"})
        m     (read-session store sk)]
    (is sk)
    (is (and (:date m)))
    (is (= (dissoc m :date)
           {:library "Welle"}))))


(deftest test-updating-a-session
  (let [store (welle-store conn "sessions")
        sk1   (write-session store nil {:library "Welle"})
        sk2   (write-session store sk1 {:library "Ring"})
        m     (read-session store sk2)]
    (is (and sk1 sk2))
    (is (and (:date m)))
    (is (= sk1 sk2))
    (is (= (dissoc m :date)
           {:library "Ring"}))))


(deftest test-deleting-a-session
  (let [store (welle-store conn "sessions")
        sk    (write-session store nil {:library "Welle"})]
    (is (nil? (delete-session store sk)))
    (is (= {} (read-session store sk))))))
