(ns wordnet.semantic-test
  (:require
    [clojure.test :refer :all]
    [wordnet.core :refer :all :as core]
    [wordnet.semantic :as sem]
    [wordnet.test-client :as test-client]))

(def test-dict (core/make-dictionary "data/dict/"))

(deftest test-path-similarity
  (is (== 0.250 (sem/path-similarity test-dict "job#n#2" "task#n#1")))
  (is (< 0.3332 (sem/path-similarity test-dict "hartford#n#1" "city#n#1") 0.3334))
  (is (< 0.1665 (sem/path-similarity test-dict "drink#v#1" "think#v#1") 0.1667)))

(deftest test-lch-similarity
  (is (< 2.3025 (sem/lch-similarity test-dict "job#n#2" "task#n#1") 2.3026))
  (is (< 2.5902 (sem/lch-similarity test-dict "hartford#n#1" "city#n#1") 2.5903))
  (is (< 1.5404 (sem/lch-similarity test-dict "drink#v#1"  "think#v#1") 1.54045)))

(deftest test-wup-similarity
  (is (< 0.8235 (sem/wup-similarity test-dict "job#n#2" "task#n#1") 0.8236))
  (is (< 0.8999 (sem/wup-similarity test-dict "hartford#n#1" "city#n#1") 0.9001))
  (is (< 0.2857 (sem/wup-similarity test-dict "drink#v#1"  "think#v#1") 0.2858))
  (is (< 0.5882 (sem/wup-similarity test-dict "neighborhood#n#1" "city#n#1") 0.5883)))


