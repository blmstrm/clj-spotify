(ns clj-spotify.core-test
  (:require [clojure.test :refer :all]
            [clj-spotify.core :as sptfy]
            [clojure.data.json :as json]
            [clojure.data :as data]
            [clj-http.client :as client]
            [clj-spotify.test-utils :as util]
            [clj-spotify.test-fixtures :as tf]))

(defn test-setup
  "Wrapper function for use with use-fixtures."
  [f]
  (with-redefs [sptfy/json-string-to-map util/test-json-string-to-map]
    (f)))

(use-fixtures :each test-setup)

(deftest test-response-to-map
  (testing "Conversion from string to clojure map"
    (is (= tf/correct-map (sptfy/response-to-map tf/correctly-formatted-response))))
  (testing "Missing body tag in response."
    (is (= (tf/nullpointer-error-map tf/missing-body-tag-response) (sptfy/response-to-map tf/missing-body-tag-response))))
  (testing "Malformed json syntax in string."
    (is (= (tf/json-missing-key-error-map tf/malformed-json-response) (sptfy/response-to-map tf/malformed-json-response)))))

(deftest test-replace-url-values
  (testing "Replace template values in spotify url and compare to correct url."
    (is (= tf/correct-test-url (sptfy/replace-url-values tf/correct-param-map tf/test-url))))
  (testing "Call replace-url-values with empty map and empty url"
    (is (= "" (sptfy/replace-url-values {} ""))))
  (testing "Call replace-url-values with key in param-map not present in url"
    (is (= tf/test-url (sptfy/replace-url-values tf/keys-not-present-param-map tf/test-url)))))

(defn generate-test
  "Create unit test code, keeping it DRY."
  [api-call infile]
  (let [correct-test-data (util/test-json-string-to-map (slurp infile))
        differences (data/diff api-call correct-test-data)]  
    (is (= nil (first differences) (second differences)))))

(deftest test-get-an-album
  (testing "Get a spotify album and verify the json data to be equal to test data in album.json"
    (generate-test (sptfy/get-an-album {:id "0sNOF9WDwhWunNAHPD3Baj"} util/spotify-oauth-token) tf/album-data-file)))

(deftest test-get-several-albums
  (testing "Get several spotify albums and verify the json data to be equal to test data in albums.json"
    (generate-test (sptfy/get-several-albums {:ids ["41MnTivkwTO3UUJ8DrqEJJ" ,"6JWc4iAiJ9FjyK0B59ABb4","6UXCm6bOO4gFlDQZV5yL37"]} util/spotify-oauth-token) tf/albums-data-file)))

(deftest test-get-tracks-of-an-album
  (testing "Get the tracks of a spotify album and verify the json data to be equal to test data in tracks-of-an-album.json"
    (generate-test (sptfy/get-an-albums-tracks {:id "6akEvsycLGftJxYudPjmqK"} util/spotify-oauth-token) tf/track-of-album-data-file)))

(deftest test-get-an-artist
  (testing "Get an artist and verify the json data to be equal to test data in artist.json"
    (generate-test (sptfy/get-an-artist {:id "0OdUWJ0sBjDrqHygGUXeCF"} util/spotify-oauth-token) tf/artist-data-file)))

(deftest test-get-several-artists
  (testing "Get several artists and verify the json data to be equal to test data in artist.json"
    (generate-test (sptfy/get-several-artists {:ids ["0oSGxfWSnnOXhD2fKuz2Gy","3dBVyJ7JuOMt4GE9607Qin"]} util/spotify-oauth-token) tf/artists-data-file)))

(deftest test-get-an-artists-albums
  (testing "Get an artists albums and verify the json data to be equal to test data in artist.json"
    (generate-test (sptfy/get-an-artists-albums {:id "1H1jG5SxsBzeuUk3ktdbeG"} util/spotify-oauth-token) tf/artists-albums-file)))

(deftest test-get-an-artists-top-tracks
  (testing "Get spotify information about an artists top tracks, due to ever changing data, only verify a status 200 response."
    (is  (= 200 (:status (meta (sptfy/get-an-artists-top-tracks {:id "0TnOYISbd1XYRBk9myaseg" :country "ES" } util/spotify-oauth-token)))))))

(deftest test-get-an-artists-related-artists
  (testing "Get spotify information about similar artists, due to ever changing data, only verify a status 200 response."
    (is (= 200 (:status (meta (sptfy/get-an-artists-related-artists {:id "5ZKMPRDHc7qElVJFh3uRqB"} util/spotify-oauth-token)))))))

(deftest test-get-a-list-of-featured-playlists
  (testing "Get a list of spotify featured playlists, due to the data changing according to the time of day, only verify status 200 in response."
    (is (= 200 (:status (meta (sptfy/get-a-list-of-featured-playlists {:country "US" :timestamp "2014-10-23T07:00:00"}  util/spotify-oauth-token)))))))

(deftest test-get-a-list-of-new-releases
  (testing "Get a list of new releases, due to ever changing data, only verify a status 200 response."
    (is (= 200 (:status (meta (sptfy/get-a-list-of-new-releases {:country "SE" :limit 2} util/spotify-oauth-token)))))))

(deftest test-get-a-list-of-browse-categories
  (testing "Get a list of spotify browse categories and verify the json data to be equal to test data in list-of-browse-categories.json"
    (generate-test ( sptfy/get-a-list-of-categories {:locale "sv_SE" :country "SE" :limit 10 :offset 5}  util/spotify-oauth-token) tf/browse-categories-file)))

(deftest test-get-a-category
  (testing "Get a spotify category and verify the json data to be equal to test data in category.json"
    (generate-test ( sptfy/get-a-category {:category_id "dinner" :locale "sv_SE" :country "SE"}  util/spotify-oauth-token) tf/category-file)))

(deftest test-get-a-categorys-playlists
  (testing "Get a spotify category's playlist, due to ever changing data, only verify a status 200 response."
    (is (= 200 (:status (meta (sptfy/get-a-categorys-playlists {:category_id "dinner" :country "SE" :limit 10 :offset 5} util/spotify-oauth-token)))))))

(deftest test-get-a-playlist
  (testing "Get a spotify playlist and verify the json data to be equal to test data in user-playlist.json"
    (generate-test (sptfy/get-a-playlist {:owner_id "elkalel" :playlist_id "5X6O7Wb8y3we9VzYTT9l64" :market "SE"} util/spotify-oauth-token) tf/playlist-file)))

(deftest test-get-a-playlists-tracks
  (testing "Get a spotify playlist's tracks and verify the json data to be equal to test data in playlists-tracks.json"
    (generate-test (sptfy/get-a-playlists-tracks {:owner_id "elkalel" :playlist_id "5X6O7Wb8y3we9VzYTT9l64" :offset 0 :limit 100 :market "SE"} util/spotify-oauth-token) tf/playlists-tracks-file)))

(deftest test-get-a-users-profile
  (testing "Get a users profile and verify the json data to be equal to test data in user-profile.json"
    (generate-test (sptfy/get-a-users-profile {:user_id "elkalel"} util/spotify-oauth-token) tf/user-profile-file)))

(deftest test-search
  (testing "Search spotify."
    (generate-test (sptfy/search {:q "Muse" :type "track,artist" :market "US" :limit 1 :offset 5} util/spotify-oauth-token) tf/search-file)))

(deftest test-get-a-track
  (testing "Get information about a single track."
    (generate-test (sptfy/get-a-track {:id "1zHlj4dQ8ZAtrayhuDDmkY" :market "ES"} util/spotify-oauth-token) tf/get-a-track-file)))

(deftest test-get-several-tracks
  (testing "Get information about several tracks."
    (generate-test (sptfy/get-several-tracks {:ids "7ouMYWpwJ422jRcDASZB7P,4VqPOruhp5EdPBeR92t6lQ,2takcwOaAZWiXQijPHIx7B" :market "ES"} util/spotify-oauth-token) tf/get-several-tracks-file)))
