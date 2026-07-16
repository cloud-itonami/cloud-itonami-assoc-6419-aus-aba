(ns association.facts-test
  (:require [clojure.test :refer [deftest is]]
            [association.facts :as facts]))

(deftest aba-has-spec-basis
  (let [sb (facts/spec-basis "aba")]
    (is (= 2 (count sb)))
    (is (every? #(= "6419" (:association-rule/isic %)) sb))
    (is (every? #(= "AUS" (:association-rule/country %)) sb))))

(deftest unknown-association-has-no-spec-basis
  (is (nil? (facts/spec-basis "fbf")))
  (is (nil? (facts/spec-basis "zzz"))))

(deftest coverage-is-honest
  (let [c (facts/coverage ["aba" "fbf"])]
    (is (= 2 (:requested c)))
    (is (= 1 (:covered c)))
    (is (= ["fbf"] (:missing-associations c)))))

(deftest by-topic-filters
  (is (= 2 (count (facts/by-topic "aba" :governance))))
  (is (empty? (facts/by-topic "aba" :labor)))
  (is (empty? (facts/by-topic "fbf" :governance))))
