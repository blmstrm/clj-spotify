(ns clj-spotify.util-test
  (:require [clojure.test :refer :all]
            [clj-spotify.util :as util]
            [clj-spotify.test-fixtures :as tfixt])
  (:import [org.apache.commons.codec.binary Hex]))
 
(deftest test-base64-encode-image
  (testing "Testing encoding of image to base65."
  (let [base64-image (util/encode-to-base64 tfixt/test-album-cover)
        hex-string (Hex/encodeHexString base64-image)]
    (is (= hex-string tfixt/test-base64-encoded-image-hex-string)))))


