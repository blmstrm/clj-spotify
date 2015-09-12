(ns clj-spotify.core-test
  (:require [clojure.test :refer :all]
            [clj-spotify.core :as sptfy]
            [clojure.data.json :as json]
            [clojure.data :as data]
            ))

;TODO - Create fixtures for json responses.
;TODO - Use spotifys client credentials flow to get token to perform tests.
;TODO - Encrypt client_id,client_secret to use with travis.
;TODO - Remove followers key from artist data as it changes over time.

(def album-data-file "./test/clj_spotify/test-data/album.json")
(def albums-data-file "./test/clj_spotify/test-data/albums.json")
(def track-of-album-data-file "./test/clj_spotify/test-data/tracks-of-album.json")
(def artist-data-file "./test/clj_spotify/test-data/artist.json")
(def artists-data-file "./test/clj_spotify/test-data/artists.json")
(def artists-albums-file "./test/clj_spotify/test-data/artists-albums.json")


(defn reset-volatile-vals
  "Function to reset values that change over time such as amount of followers or popularity ranking."
  [k v]
  (cond
  (= k :followers){:href nil, :total 0}
  (= k :popularity) 0
  :else v
  )
  )

(defn test-json-string-to-map [s]
  "Read string and transform to json but ignore key :followers"
  (json/read-str s :value-fn reset-volatile-vals :key-fn keyword)
  )

(defn parse-json [s]
  (json/read-str s :key-fn keyword))

(def correct-map {:test-key "test-value" :test-map {:a "a"} :test-vector [1 2 3] :test-null nil})

(def correctly-formatted-response {:body "{\"test-key\": \"test-value\", \"test-map\" : {\"a\": \"a\"}, \"test-vector\" : [1 2 3], \"test-null\" : null}"}) 


(def missing-body-tag-response {}) 

(def malformed-json-response {:body "{\"test-key\": \"test-value\", \"test-map\" : {\"a\": \"a\"}, \"test-vector\" : [1 2 3], \"test-null\" : }"}) 

(def nullpointer-error-map {:error {:status "NullPointerException", :message nil}}) 

(def json-missing-key-error-map {:error {:status "Exception", :message "JSON error (key missing value in object)"}})

(def test-url (str sptfy/spotify-api-url "users/user_id/playlists/playlist_id/tracks"))
(def correct-test-url (str sptfy/spotify-api-url "users/elkalel/playlists/6IIjEBw2BrRXbrSLerA7A6/tracks"))

(def correct-param-map {:user_id "elkalel" :playlist_id "6IIjEBw2BrRXbrSLerA7A6"})
(def keys-not-present-param-map {:category_id "pop" :owner_id "elkalel"})

(deftest test-response-to-map
  (testing "Conversion from string to clojure map"
    (is (= correct-map (sptfy/response-to-map correctly-formatted-response))))
  (testing "Missing body tag in response."
    (is (= nullpointer-error-map (sptfy/response-to-map missing-body-tag-response)))
    ) 
  (testing "Malformed json syntax in string."
    (is (= json-missing-key-error-map (sptfy/response-to-map malformed-json-response)))
    ) 
  )

(deftest test-replace-url-values
  (testing "Replace template values in spotify url and compare to correct url."
    (is (= correct-test-url (sptfy/replace-url-values correct-param-map test-url))))
  (testing "Call replace-url-values with empty map and empty url"
    (is (= "" (sptfy/replace-url-values {} "")))
    )
  (testing "Call replace-url-values with key in param-map not present in url"
    (is (= test-url (sptfy/replace-url-values keys-not-present-param-map test-url)))
    )
  )

(deftest test-get-an-album
  (testing "Get a spotify album and verify the json data to be equal to test data in album.json"
    ( with-redefs [sptfy/json-string-to-map test-json-string-to-map]
    (let [correct-test-data (parse-json (slurp album-data-file))
          differences (data/diff (sptfy/get-an-album {:id "0sNOF9WDwhWunNAHPD3Baj"}) correct-test-data)]  
      (is (= nil (first differences) (second differences)))))
    )
  )

(deftest test-get-several-albums
  (testing "Get several spotify albums and verify the json data to be equal to test data in albums.json"
    (with-redefs [sptfy/json-string-to-map test-json-string-to-map]
    (let [correct-test-data (parse-json (slurp albums-data-file))
          differences (data/diff (sptfy/get-several-albums {:ids ["41MnTivkwTO3UUJ8DrqEJJ" ,"6JWc4iAiJ9FjyK0B59ABb4","6UXCm6bOO4gFlDQZV5yL37"]}) correct-test-data)
          ]
      (is (= nil (first differences) (second differences)))
      ))
    )
  )

(deftest test-get-tracks-of-an-album
  (testing "Get the tracks of a spotify album and verify the json data to be equal to test data in tracks-of-an-album.json"
    (with-redefs [sptfy/json-string-to-map test-json-string-to-map]
    (let [correct-test-data (parse-json (slurp track-of-album-data-file))
          differences (data/diff (sptfy/get-tracks-of-album  {:id "6akEvsycLGftJxYudPjmqK"}) correct-test-data)
          ]
      (is (= nil (first differences) (second differences)))))
    )
  )

(deftest test-get-an-artist
  (testing "Get an artist and verify the json data to be equal to test data in artist.json"
    (with-redefs [sptfy/json-string-to-map test-json-string-to-map]
    (let [correct-test-data (parse-json (slurp artist-data-file))
          differences (data/diff  (sptfy/get-an-artist {:id "0OdUWJ0sBjDrqHygGUXeCF"}) correct-test-data) 
          ]
      (is (= nil (first differences) (second differences)) )))
    )
  )

(deftest test-get-several-artists
  (testing "Get several artists and verify the json data to be equal to test data in artist.json"
    (with-redefs [sptfy/json-string-to-map test-json-string-to-map]
      (let [correct-test-data (parse-json (slurp artists-data-file))
            differences (data/diff ( sptfy/get-several-artists {:ids ["0oSGxfWSnnOXhD2fKuz2Gy","3dBVyJ7JuOMt4GE9607Qin"]}) correct-test-data)
            ]
        (is (= nil (first differences) (second differences)))))
    )
  )

(deftest test-get-artists-albums
  (testing "Get an artists albums and verify the json data to be equal to test data in artist.json"
    (with-redefs [sptfy/json-string-to-map test-json-string-to-map]
      (let [correct-test-data (parse-json (slurp artists-albums-file))
            differences (data/diff ( sptfy/get-an-artists-albums {:id "1vCWHaC5f2uS3yhpwWbIA6"}) correct-test-data)
            ]
        (is (= nil (first differences) (second differences) ))))
    )
  )



