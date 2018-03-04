(ns clj-spotify.test-fixtures
  (:require [clj-spotify.core :as sptfy]))

;Test data files
(def album-data-file "./test/clj_spotify/test-data/album.json")
(def albums-data-file "./test/clj_spotify/test-data/albums.json")
(def track-of-album-data-file "./test/clj_spotify/test-data/tracks-of-album.json")
(def artist-data-file "./test/clj_spotify/test-data/artist.json")
(def artists-data-file "./test/clj_spotify/test-data/artists.json")
(def artists-albums-file "./test/clj_spotify/test-data/artists-albums.json")
(def artists-top-tracks-file "./test/clj_spotify/test-data/artists-top-tracks.json")
(def artists-related-artists-file "./test/clj_spotify/test-data/artists-related-artists.json")
(def featured-playlists-file "./test/clj_spotify/test-data/featured-playlists.json")
(def browse-categories-file "./test/clj_spotify/test-data/list-of-browse-categories.json")
(def category-file "./test/clj_spotify/test-data/category.json")
(def categorys-playlists-file "./test/clj_spotify/test-data/categorys-playlists.json")
(def playlist-file "./test/clj_spotify/test-data/playlist.json")
(def playlists-tracks-file "./test/clj_spotify/test-data/playlists-tracks.json")
(def user-profile-file "./test/clj_spotify/test-data/user-profile.json")
(def search-file "./test/clj_spotify/test-data/search.json")
(def get-a-track-file "./test/clj_spotify/test-data/get-a-track.json")
(def get-several-tracks-file "./test/clj_spotify/test-data/get-several-tracks.json")

(def test-album-cover "./test/clj_spotify/test-data/test-album-cover.png")

(def test-base64-encoded-image-hex-string 
  (slurp "./test/clj_spotify/test-data/test-album-cover-png.hex"))

()

(def correct-map {:test-key 0, :test-map 0, :test-vector 0, :test-null 0})

(def correctly-formatted-response {:body "{\"test-key\": \"test-value\", \"test-map\" : {\"a\": \"a\"}, \"test-vector\" : [1 2 3], \"test-null\" : null}"}) 

(def missing-body-tag-response {}) 

(def malformed-json-response {:body "{\"test-key\": \"test-value\", \"test-map\" : {\"a\": \"a\"}, \"test-vector\" : [1 2 3], \"test-null\" : }"}) 

(defn nullpointer-error-map [response] {:error {:status "NullPointerException", :message nil, :response response}})

(defn json-missing-key-error-map [response]
  {:error {:status "Exception", :message "JSON error (key missing value in object)", :response response}})
 
(def test-url (str sptfy/spotify-api-url "users/user_id/playlists/playlist_id/tracks"))

(def correct-test-url (str sptfy/spotify-api-url "users/elkalel/playlists/6IIjEBw2BrRXbrSLerA7A6/tracks"))

(def correct-param-map {:user_id "elkalel" :playlist_id "6IIjEBw2BrRXbrSLerA7A6"})

(def keys-not-present-param-map {:category_id "pop" :owner_id "elkalel"})
 


