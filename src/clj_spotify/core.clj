(ns clj-spotify.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as string]))

(def template-keys [:id :category_id :owner_id :playlist_id :user_id])

(def spotify-api-url "https://api.spotify.com/v1/")

(def defined-query-params [:fields :ids :limit :offset :market :position :state :position_ms :volume_percent :device_id :locale :country :timestamp :type]) 

(defn json-string-to-map
  "Read string and transform into json"
  [s]
  (json/read-str s :key-fn keyword))

(defn response-to-map
  "Parse body of http response to json.
  Include header as meta-data.
  If http status 204 (No content) return an empty map."
  [response]
  (with-meta
    (try
      (if (= (:status response) 204)
        {}
        (json-string-to-map (:body response)))
      (catch java.lang.NullPointerException e
        {:error {:status "NullPointerException"
                 :message (.getMessage e)
                 :response response}})
      (catch Exception e
        {:error {:status "Exception"
                 :message (.getMessage e)
                 :response response}}))
    {:status (:status response)
     :headers (:headers response)}))

(defn- build-new-url
  "Do the building necessary in replace-url-values here. url-parts is the split up url which values works as keys in param-map."
  [url url-parts param-map]
  (if (empty? url-parts)
    (str "https:/" url)
    (let [url-key (keyword (last url-parts))
          new-url (str "/" (if (contains? param-map url-key)
                             (url-key param-map)
                             (last url-parts))
                       url)]
      (recur new-url (pop url-parts) param-map))))

(defn replace-url-values
  "Build a new url by switching parts in url with values in param-map. Keys are substrings of the full url. If a key is not present in template-keys it won't be exchanged."
  [param-map url]
  (if (string/blank? url)
    ""
    (let [act-tmplt-keys (select-keys param-map template-keys)
          split-url (string/split (string/replace url "https://" "") #"/")]
      (build-new-url "" split-url act-tmplt-keys))))

(defn comma-separate
  "If v is a sequence, convert to a comma separated string."
  [v]
  (if (or (seq? v) (vector? v))
    (string/join "," v)
    v))

(defn- map-vals
  "Applies f to each value in m."
  [f m]
  (zipmap (keys m)
          (map f (vals m))))

(defn convert-values
  "Convert values in query-params to match spotifys api."
  [m]
  (map-vals comma-separate m))

(defn remove-path-keys
  "Remove keys that are used in modifying the path."
  [m]
  (apply dissoc m template-keys))

(defn filter-map-keys [map keys]
  (into {} (filter #(.contains keys (key %)) map)))

(defn api-request
  "Returns a request map for a particular api call.
  The change spotify playlist cover api call demands raw data in the :form-params call
  and content-type to be set to image/jpeg that's why we do the if statements below."
  [method endpoint query-params-spec m t]
  (let [url (replace-url-values m (str spotify-api-url endpoint))
        params (remove-path-keys m)
        query-params (convert-values (if (or (= method :put) (= method :post))
                                       (filter-map-keys params query-params-spec)
                                       params))
        form-params (apply dissoc params (keys query-params))]
    {:method method
     :url url
     :query-params query-params
     :form-params (if (:image_data form-params)
                    (:image_data form-params)
                    form-params)
     :oauth-token t
     :content-type (if (:image_data form-params)
                     :image/jpeg
                     :json)}))

(defn spotify-api-call
  "Returns a function that takes a map m and an optional oauth-token t as arguments."
  [method endpoint & {:keys [query-params] :or {query-params []}}]
  (let [merged-query-params (apply conj query-params defined-query-params)]
    (fn f
      ([m] (f m nil))
      ([m t]
       (response-to-map
         (try
           (client/request (api-request method endpoint merged-query-params m t))
           (catch Exception e (ex-data e))))))))

(def api-get    (partial spotify-api-call :get))
(def api-post   (partial spotify-api-call :post))
(def api-put    (partial spotify-api-call :put))
(def api-delete (partial spotify-api-call :delete))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;  Public API


;Albums
(def get-an-album
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id, optional key is :market.
  :id has to have the value of an existing album's id.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-an-album {:id \"0MnG7y5F1n4Wns63RjEItx\" :market \"SE\"} \"BQBw-JtC..._7GvA\")"
  (api-get "albums/id"))

(def get-several-albums
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids, optional key is :market.
  :ids has to be a comma separated string of spotify album ids.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-several-albums {:ids \"4hTil0JVdMyxK2vH11ZbiX,4RjjFbiqFaO8mKTDM2Kzna\" :market \"SE\"} \"BQBw-JtC..._7GvA\") "
  (api-get "albums"))

(def get-an-albums-tracks
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id, optional keys are :limit, :offset and :market.
  :id has to have the value of an existing album's id.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-tracks-of-album {:id \"3XCGFOBqESirxxICswSity\" :market \"SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")"
  (api-get "albums/id/tracks"))

;Artists
(def get-an-artist
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id.
  :id has to have the value of an existing artist's id.

  Example: (get-an-artist {:id \"5CE2IfdYZEQGIDsfiRm8SI\"} \"BQBw-JtC..._7GvA\")"
  (api-get "artists/id"))

(def get-several-artists
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids.
  :ids has to be a comma separated string of spotify artist ids.

  Example: (get-several-artists {:ids \"1kK2LYgoP3raZNngbb1qMT,2RitCPbwEYyYNw5LkbXTGv\"} \"BQBw-JtC..._7GvA\")"
  (api-get "artists"))

(def get-an-artists-albums
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id, optional keys are :album_type, :market, :limit and :offset.
  :id has to have the value of an existing artist's id.
  :album_type a comma-separated list of one or several of the following:
  album, single, appears_on and compilation. Default is to query for all types.
  :market is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-an-artists-albums {:id \"7lOJ7WXyopaxri0dbOiZkd\" :album_type \"album, single\" :market \"SE\" :limit 50 :offset 50}} \"BQBw-JtC..._7GvA\")"
  (api-get "artists/id/albums"))

(def get-an-artists-top-tracks
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in m are :id and :country.
  :id has to be a spotify artist id.
  :country is an ISO 3166-1 alpha-2 country code.

  Example: (get-an-artists-top-tracks {:id \"7hCsRnXtcbez8msLPfjbkz\" :country \"SE\"} \"BQBw-JtC..._7GvA\")"
  (api-get "artists/id/top-tracks"))

(def get-an-artists-related-artists
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id.
  :id has to be a spotify artist id.

  Example: (get-an-artists-related-artists {:id \"4EF5vIcCYKMM61oYOG2Tqa\"} \"BQBw-JtC..._7GvA\")"
  (api-get "artists/id/related-artists"))

;Browse
(def get-a-list-of-featured-playlists
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  There are no compulsary keys in m, optional keys are :locale, :country, :timestamp, :limit and :offset.
  :locale is an ISO 639 language code and a ISO 3166-1 alpha-2 country code joined with an underscore.
  :country is an ISO 3166-1 alpha-2 country code.
  :timestamp is ISO 8601 formatted timestamp.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-a-list-of-featured-playlists {:locale \"sv_SE\" :country \"SE\" :timestamp \"2015-10-23T09:00:00\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")"
  (api-get "browse/featured-playlists"))

(def get-a-list-of-new-releases
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  There are no compulsary keys in m, optional keys are :country, :limit and :offset.
  :country is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-a-list-of-new-releases {:country \"SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")"

  (api-get "browse/new-releases"))

(def get-a-list-of-categories
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  There are no compulsary keys in m, optional keys are :country, :limit and :offset.
  :country is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-a-list-of-categories {:country \"SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")"
  (api-get "browse/categories"))

(def get-a-category
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :category_id, optional keys are :country and :locale.
  :category_id has to be a spotify category id.
  :country is an ISO 3166-1 alpha-2 country code.
  :locale is an ISO 639 language code and a ISO 3166-1 alpha-2 country code joined with an underscore.

  Example: (get-a-category {:category_id \"dinner\" :country \"SE\" :locale \"sv_SE\"} \"BQBw-JtC..._7GvA\")"
  (api-get "browse/categories/category_id"))

(def get-a-categorys-playlists
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :category_id, optional keys are :country, :limit and :offset.
  :category_id has to be a spotify category id.
  :country is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-a-categorys-playlist {:category_id \"dinner\" :country \"SE\" :locale \"sv_SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")"
  (api-get "browse/categories/category_id/playlists"))

;Follow
(def get-users-followed-artists
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :type, optional keys are :limit and :after.
  :type has to be set to \"artist\".
  :limit is the maxium number of items to return, default is 20.
  :after the last artist id retrieved from a previous request. Use this to get next set of artists.

  Example: (get-users-followed-artists {:type \"artist\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\") "
  (api-get "me/following"))

(def follow-artists-or-users
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :type and :ids.
  :type has to be set to \"artist\" or \"user\".
  :ids has to be a comma separated list of users or artists.

  Example: (follow-artists-or-users {:type \"artist\" :ids \"2933wDUojoQmvqSdTAE5NB,1AUCkAIfT6Ig8lOegDGK3Z\" } \"BQBw-JtC..._7GvA\")"
  (api-put "me/following"))

(def unfollow-artists-or-users
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :type and :ids.
  :type has to be set to \"artist\" or \"user\".
  :ids has to be a comma separated list of users or artists.

  Example: (unfollow-artists-or-users {:type \"artist\" :ids \"0uCCBpmg6MrPb1KY2msceF,1WsMRWV5KEC2AxpYkeb2Cf\" } \"BQBw-JtC..._7GvA\")"
  (api-delete "me/following"))

(def user-following-artists-or-users?
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :type and :ids.
  :type has to be set to \"artist\" or \"user\".
  :ids has to be a comma separated list of users or artists.

  Example: (user-following-artists-or-users? {:type \"artist\" :ids \"3H6Js0rhywEhHda3UwuhGW,4r8bVMyYyGyARs9OfBvyj4\" } \"BQBw-JtC..._7GvA\")"
  (api-get "me/following/contains"))

(def follow-a-playlist
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in m are :owner_id and :playlist_id.
  :owner_id has to be the spotify user id of the user owning the playlist.
  :playlist_id the spotify playlist id.

  Example: (follow-a-playlist {:owner_id \"sondre_lerche\" :playlist_id \"7q8QuE0YbAgbvzR0NJVwV8\" } \"BQBw-JtC..._7GvA\")"
  (api-put "users/owner_id/playlists/playlist_id/followers"))

(def unfollow-a-playlist
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in m are :owner_id and :playlist_id.
  :owner_id has to be the spotify user id of the user owning the playlist.
  :playlist_id the spotify playlist id.

  Example: (unfollow-a-playlist {:owner_id \"ulyssestone\" :playlist_id \"3WG4abOpUpocexRakPioAg\" } \"BQBw-JtC..._7GvA\")"
  (api-delete "users/owner_id/playlists/playlist_id/followers"))

(def user-following-playlist?
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in m are :owner_id and :playlist_id.
  :owner_id has to be the spotify user id of the user owning the playlist.
  :playlist_id the spotify playlist id.

  Example: (user-following-playlist? {:owner_id \"lecowboy\" :playlist_id \"2PftXV7dgcxFjMWe75GuSG\" } \"BQBw-JtC..._7GvA\")"
  (api-get "users/owner_id/playlists/playlist_id/followers/contains"))

;Library
(def save-tracks-for-user
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids.
  :ids has to be a comma separated list of spotify track ids.

  Example: (save-tracks-for-user {:ids \"3BSpuy2yHhoEazfXVU7WGq,0BcaWLANkstu2v9kghrCAj\"} \"BQBw-JtC..._7GvA\")"
  (api-put "me/tracks"))

(def get-users-saved-tracks
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  There are no compulsory keys in m, optional keys are :market, :limit and :offset.
  :market is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-users-saved-tracks {:market \"SE\" :limit 50 :offset 50}} \"BQBw-JtC..._7GvA\")"
  (api-get "me/tracks"))

(def remove-users-saved-tracks
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids.
  :ids has to be a comma separated list of spotify track ids.

  Example: (remove-users-saved-tracks {:ids \"1RyZgkIghj1eL2U1zlYOoE,6NwbeybX6TDtXlpXvnUOZC\"} \"BQBw-JtC..._7GvA\")"
  (api-delete "me/tracks"))

;TODO - better name for this function
(def check-users-saved-tracks
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids.
  :ids has to be a comma separated list of spotify track ids.

  Example: (check-users-saved-tracks {:ids \"28nlyt9KhVZQ0lGQsg8Yht,09ZGF6mwJVzw5jxqbtT53E\"} \"BQBw-JtC..._7GvA\")"
  (api-get "me/tracks/contains"))

;Playlists
(def get-a-list-of-a-users-playlists
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in  m is :user_id, optional keys are :limit and :offset.
  :user_id is the users spotify id.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-a-list-of-a-users-playlists {:user_id \"elkalel\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")"
  (api-get "users/user_id/playlists"))

;TODO - Change this fields string to be a map?
(def get-a-playlist
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in  m are :user_id and :playlist_id, optional keys are :fields, :market, :limit and :offset.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :fields is a comma-separated string of fields to return from the playlist.
  See developer.spotify.com for a full list of field names.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-a-playlist {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :fields \"href,name,owner\" :market \"SE\"} \"BQBw-JtC..._7GvA\")"
  (api-get "users/user_id/playlists/playlist_id"))

(def get-a-playlists-tracks
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in  m are :user_id and :playlist_id, optional keys are :fields, :limit and :offset and :market.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :fields is a comma-separated string of fields to return from the playlist.
  See developer.spotify.com for a full list of field names.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-a-playlists-tracks {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :fields \"href,name,owner\":limit 50 :offset 50 :market \"SE\"} \"BQBw-JtC..._7GvA\")"
  (api-get "users/user_id/playlists/playlist_id/tracks"))

(def create-a-playlist
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in m are :user_id and :name, optional key is :public.
  :user_id is the users spotify id.
  :name is the name for the new playlist.
  :public is set to true if a playlist should be public and false if not.

  Example: (create-a-playlist {:user_id \"elkalel\" :name \"The songs you didn't know you liked.\" :public true} \"BQBw-JtC..._7GvA\")"
  (api-post "users/user_id/playlists"))

(def add-tracks-to-a-playlist
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m are :user_id and :playlist_id, optional keys are :uris and :position.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :uris a list of spotify track uris.
  :position the position to insert the tracks.

  Example: (add-tracks-to-playlist {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\"
  :uris [\"spotify:track:4iV5W9uYEdYUVa79Axb7Rh\", \"spotify:track:1301WleyT98MSxVHPZCA6M\"]
  :position 2} \"BQBw-JtC..._7GvA\")"
  (api-post "users/user_id/playlists/playlist_id/tracks"))

;TODO - Deal with correct formatting of :tracks.
;TODO - Additional doc string with optional and required values in :tracks.
(def remove-tracks-from-a-playlist
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m are :user_id, :playlist_id and :tracks, optional keys is :snapshot_id.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :tracks an array of objects containing spotify URI strings and corresponding position maps.
  :snapshot_id a playlist snapshot ID.
  See the spotify developer API for the logic behind deleting tracks.

  Example: (remove-tracks-from-playlist {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :tracks [{:uri \"spotify:track:4iV5W9uYEdYUVa79Axb7Rh\", :positions [2]} {:uri \"spotify:track:1301WleyT98MSxVHPZCA6M\", :positions [7]}]}  \"BQBw-JtC..._7GvA\")"
  (api-delete "users/user_id/playlists/playlist_id/tracks"))

(def reorder-a-playlists-tracks
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m are :user_id, :playlist_id, :range_start and :insert_before, optional keys are :range_length and :snapshot_id.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :range_start the position of the first track to be reordered.
  :insert_before the positions where the tracks should be inserted.
  :range_length the amount of tracks to be reordered.
  :snapshot_id a playlist snapshot ID.

  Example: (reorder-a-playlists-tracks {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :range_start 1 :range_length 2 :insert_before 3} \"BQBw-JtC..._7GvA\")"
  (api-put "users/user_id/playlists/playlist_id/tracks"))

(def replace-a-playlists-tracks
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m are :user_id and :playlist_id, optional keys are :uris and :position.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :uris a list of spotify track uris.

  Example: (replace-a-playlists-tracks {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :uris \"spotify:track:4iV5W9uYEdYUVa79Axb7Rh,
  spotify:track:1301WleyT98MSxVHPZCA6M\"} \"BQBw-JtC..._7GvA\")"
  (api-put "users/user_id/playlists/playlist_id/tracks"))

(def change-a-playlists-details
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m are :user_id and :playlist_id, optional keys are :name and :public.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :name is the name for the new playlist.
  :public is set to true if a playlist should be public and false if not.

  Example: (change-a-playlists-details {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :name \"Fantastic playlist\" :public true} \"BQBw-JtC..._7GvA\")"
  (api-put "users/user_id/playlists/playlist_id"))

;;TODO Upload a custom playlist cover
(def upload-custom-playlist-cover
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  Compulsory key in m are :user_id, :playlist_id and :image_data.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :image_data is a base64 encoded jpeg image. 

  Example: (upload-custom-playlist-cover {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :image_data \"iVBORw0KGgoAAA...\" } \"BQBw-JtC..._7GvA\")"
  (api-put "users/user_id/playlists/playlist_id/images"))

;Profiles
(def get-a-users-profile
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :user_id.
  :user_id has to be a spotify user id.

  Example: (get-a-users-profile {:user_id \"elkalel\"} \"BQBw-JtC..._7GvA\")"
  (api-get "users/user_id"))

;TODO - what happens when map is not present or nil?
(def get-current-users-profile
  " Takes an oauth-token t.
  Example: (get-current-users-profile {} \"BQBw-JtC..._7GvA\")"
  (api-get "me"))

(def get-current-users-recently-played-tracks
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  There are no compulsary keys in m, optional keys are :limit, :after and :before.
  :limit is the maximum of items to return.
  :after is a unix timestamp in milliseconds. All items after this timestamp will be returned.
  :before is a unix timestamp in milliseconds. All items before this timestamp will be returned.
  Example: (get-current-users-profile {:limit 5 :before 1508531199 :after 380934000} \"BQBw-JtC..._7GvA\")"
  (api-get "me/player/recently-played"))

;Player / Device
(def get-current-users-available-devices
  " Takes an oauth-token t.
  Example: (get-current-users-available-devices  \"BQBw-JtC..._7GvA\")"
  (api-get "me/player/devices"))

(def get-info-about-current-users-current-playback
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  There are no compulsary keys in m, optional key is :market.
  :market is an ISO 3166-1 alpha-2 country code.
  Example: (get-info-about-current-users-current-playback {:market \"SE\"}  \"BQBw-JtC..._7GvA\")"
  (api-get "me/player"))

(def get-users-currently-playing-track
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  There are no compulsary keys in m, optional key is :market.
  :market is an ISO 3166-1 alpha-2 country code.
  Example: (get-users-currently-playing-track {:market \"SE\"}  \"BQBw-JtC..._7GvA\")"
  (api-get "me/player/currently-playing"))

(def transfer-current-users-playback
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  Compulsary key in m is :device_ids, optional key is :play.
  :device_ids is an array of devices IDs to where the playback should be transfered. 
  Even though it is an array, only a single device_id is currently supported.
  :play is a boolean value, if true - ensure playback happens on new device, else keep current playback state.
  Example: (transfer-current-users-playback {:device_ids [\"74ASZWbe4lXaubB36ztrGX\"] :play true}  \"BQBw-JtC..._7GvA\")"
  (api-put "me/player"))

(def start-or-resume-a-users-playback
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  There are no compulsary keys in m, optional keys are :device_id, : context_uri, :uris and :offset.
  :device_id is the device id of the device the command is targeting. If not supplied, the users current device is the target.
  :context_uri is the Spotify URI to play. Can be albums,artists or playlists.
  :uris is An array of Spotify track URIs to play.
  :offset is a map indicating from where playback should start. This is only available when either :context_uri is set to an artist or an album or when the :uris parameter is populated. The map has to contain one of two keys, :position or :uri. 
  :position is an integer describing a position in a playlist and :uri is the Spotify URI representing the item to start at.
  Example: (start-or-resume-a-users-playback {:device_id [\"74ASZWbe4lXaubB36ztrGX\"] :context_uri \"spotify:album:2cX9e3renOX5bUQEXWFrJr\"} :offset {:position 5}} \"BQBw-JtC..._7GvA\")"
  (api-put "me/player/play"))

(def pause-a-users-playback
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  There are no compulsary keys in m, optional key is :device_id.
  :device_id is the device id of the device the command is targeting. If not supplied, the users current device is the target.
  Example: (pause-a-users-playback {:device_id \"74ASZWbe4lXaubB36ztrGX\"} \"BQBw-JtC..._7GvA\")"
  (api-put "me/player/pause"))

(def skip-users-playback-to-the-next-track
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  There are no compulsary keys in m, optional key is :device_id.
  :device_id is the device id of the device the command is targeting. If not supplied, the users current device is the target.
  Example: (skip-users-playback-to-the-next-track {:device_id \"74ASZWbe4lXaubB36ztrGX\"} \"BQBw-JtC..._7GvA\")"
  (api-post "me/player/next"))

(def skip-users-playback-to-the-previous-track
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  There are no compulsary keys in m, optional key is :device_id.
  :device_id is the device id of the device the command is targeting. If not supplied, the users current device is the target.
  Example: (skip-users-playback-to-the-previous-track {:device_id \"74ASZWbe4lXaubB36ztrGX\"} \"BQBw-JtC..._7GvA\")"
  (api-post "me/player/previous"))

(def seek-to-position-in-currently-playing-track
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  Compulsary key in m is :position_ms, optional key is :device_id.
  :position_ms is the position in milliseconds to seek to.
  :device_id is the device id of the device the command is targeting. If not supplied, the users current device is the target.
  Example: (seek-to-position-in-currently-playing-track {:position_ms 10000 :device_id \"74ASZWbe4lXaubB36ztrGX\"} \"BQBw-JtC..._7GvA\")"
  (api-put "me/player/seek"))

(def set-repeat-mode-on-users-playback
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  Compulsary key in m is :state, optional key is :device_id.
  :state is one of \"track\",\"context\" or \"off\". track will repeat the current track, context will repeat the current context (album, playlist) and off will turn repeat off.
  :device_id is the device id of the device the command is targeting. If not supplied, the users current device is the target.
  Example: (set-repeat-mode-on-users-playback {:state \"context\" :device_id \"74ASZWbe4lXaubB36ztrGX\"} \"BQBw-JtC..._7GvA\")"
  (api-put "me/player/repeat"))

(def set-volume-for-users-playback
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  Compulsary key in m is :volume_percent, optional key is :device_id.
  :volume_percent is the volume to set in percent. An integer between 0 and 100.
  :device_id is the device id of the device the command is targeting. If not supplied, the users current device is the target.
  Example: (set-volume-for-users-playback {:volume_percent 11 :device_id \"74ASZWbe4lXaubB36ztrGX\"} \"BQBw-JtC..._7GvA\")"
  (api-put "me/player/volume"))

(def toggle-shuffle-for-users-playback
  " Takes two arguments, a map m with query parameters and an oauth-token t.
  Compulsary key in m is :state, optional key is :device_id.
  :state is either true or false.
  :device_id is the device id of the device the command is targeting. If not supplied, the users current device is the target.
  Example: (toogle-shuffle-for-users-playback {:state true :device_id \"74ASZWbe4lXaubB36ztrGX\"} \"BQBw-JtC..._7GvA\")"
  (api-put "me/player/shuffle"))

;Search
(def search
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in m are :q and :type, optional keys are :market, :limit and :offset.
  :q is the search string.
  :type is a comma separated list of items types to search for, album, artist, playlist and track.
  :market is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (search {:q \"Gerry Rafferty\" :type \"artist\"  :market \"SE\" :limit 50 :offset 50}} \"BQBw-JtC..._7GvA\")"
  (api-get "search"))

;Tracks
(def get-a-track
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id, optional key is :market.
  :id has to have the value of an existing track's id.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-a-track {:id \"3BEmmHuv5jm0tbCUVI6ceD\" :market \"SE\"} \"BQBw-JtC..._7GvA\")"
  (api-get "tracks/id"))

(def get-several-tracks
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids, optional key is :market.
  :ids has to be a comma separated string of spotify track ids.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-several-tracks {:ids \"2Dlo9QsN9ohaz9kASYkKmv,6XYvLpqJFebcjnOTolwkGw\" :market \"SE\"} \"BQBw-JtC..._7GvA\")"
  (api-get "tracks"))

(def get-audio-analysis-for-a-track
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id.
  :id has to have the value of an existing track's id.

  Example: (get-audio-analysis-for-a-track {:id \"2Dlo9QsN9ohaz9kASYkKmv\"} \"BQBw-JtC..._7GvA\")"
  (api-get "audio-analysis/id"))

(def get-audio-features-for-a-track
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id.
  :id has to have the value of an existing track's id.

  Example: (get-audio-features-for-a-track {:id \"2Dlo9QsN9ohaz9kASYkKmv\"} \"BQBw-JtC..._7GvA\")"
  (api-get "audio-features/id"))

(def get-audio-features-for-several-tracks
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids.
  :ids has to be a comma separated string of spotify track ids.

  Example: (get-audio-features-for-several-tracks {:ids \"2Dlo9QsN9ohaz9kASYkKmv,6XYvLpqJFebcjnOTolwkGw\"} \"BQBw-JtC..._7GvA\")"
  (api-get "audio-features"))

