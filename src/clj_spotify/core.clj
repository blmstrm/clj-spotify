(ns clj-spotify.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as string]
            )
  )

(def template-keys [:id :category_id :owner_id :playlist_id :user_id])

(def spotify-api-url "https://api.spotify.com/v1/")

(defn json-string-to-map
  "Read string and transform into json"
  [s]
  (json/read-str s :key-fn keyword))

(defn response-to-map
  "Parse body of http respose to json"
  [response]
  (with-meta
  (try
    (json-string-to-map (:body response))
    (catch java.lang.NullPointerException e {:error {:status "NullPointerException"  :message (.getMessage e)}})
    (catch Exception e {:error {:status "Exception"  :message (.getMessage e)}})
    ) {:status (:status response)}))

(defn- build-new-url
  "Do the building nescessary in replace-url-values here. url-parts is the split up url which values works as keys in param-map."
  [url url-parts param-map]
  (if (empty? url-parts)
    (str "https:/" url)
    (let [url-key (keyword (last url-parts))
          new-url (str "/" (if (contains? param-map url-key)
                             (url-key param-map)
                             (last url-parts))  url)]
      (recur new-url (pop url-parts) param-map))))

(defn replace-url-values
  "Build a new url by switching parts in url with values in param-map. Keys are substrings of the full url. If a key is not present in template-keys it won't be exchanged."
  [param-map url]
  (if (string/blank? url)
   "" 
  (let [act-tmplt-keys  (select-keys param-map template-keys)
        split-url (string/split (string/replace url "https://" "") #"/") 
        ]
    (build-new-url "" split-url act-tmplt-keys))))

(defn comma-separate
  "If v is a sequence, convert to a comma separated string." 
  [k v]
  (if (or (seq? v) (vector? v))
    [k (string/join "," v)]
    [k v]))

(defn convert-values
  "Convert values in query-params to match spotifys api."
  [m]
  (into {} (for [[k v] m] (comma-separate k v))))

(defn remove-path-keys
  "Remove keys that are used in modifying the path."
  [m]
  (apply dissoc m template-keys))

(defn modify-form-params
  "Do nescessary conversion of form parameters."
  [m]
  (->
    m
    (remove-path-keys)
    (convert-values)))

(defn set-params-type
  "Return either :query-params or :form-params key depending on value of verb"
  [verb]
 (let [verb-type (type verb)] 
  (if
    (or (= verb-type clj_http.client$put) (= verb-type clj_http.client$post))
    :form-params
    :query-params)))

;TODO - better doc string
(defmacro def-spotify-api-call
  "Creates a function f with doc-string d that calls the http-verb verb for url url."
  [f verb url doc-string]
  `(defn ~f 
     ~doc-string
     ([m#] (~f m# nil))
     ([m# t#]
      (let [query-params# {(set-params-type ~verb) (modify-form-params m#) :oauth-token t# :content-type :json}]
        (->
          (try
            (~verb (replace-url-values m# ~url) query-params#)
            (catch Exception e# (ex-data e#))
            )
          (response-to-map))))))

;Albums
(def-spotify-api-call get-an-album client/get (str spotify-api-url "albums/id")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id, optional key is :market.
  :id has to have the value of an existing album's id.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-an-album {:id \"0MnG7y5F1n4Wns63RjEItx\" :market \"SE\"} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call get-several-albums client/get (str spotify-api-url "albums")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids, optional key is :market.
  :ids has to be a comma separated string of spotify album ids.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-several-albums {:ids \"4hTil0JVdMyxK2vH11ZbiX,4RjjFbiqFaO8mKTDM2Kzna\" :market \"SE\"} \"BQBw-JtC..._7GvA\") ")

(def-spotify-api-call get-an-albums-tracks client/get (str spotify-api-url "albums/id/tracks")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id, optional keys are :limit, :offset and :market.
  :id has to have the value of an existing album's id.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-tracks-of-album {:id \"3XCGFOBqESirxxICswSity\" :market \"SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")")

;Artists
(def-spotify-api-call get-an-artist client/get (str spotify-api-url "artists/id")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id.
  :id has to have the value of an existing artist's id.

  Example: (get-an-artist {:id \"5CE2IfdYZEQGIDsfiRm8SI\"} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call get-several-artists client/get (str spotify-api-url "artists") 
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids.
  :ids has to be a comma separated string of spotify artist ids.

  Example: (get-several-artists {:ids \"1kK2LYgoP3raZNngbb1qMT,2RitCPbwEYyYNw5LkbXTGv\"} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call get-an-artists-albums client/get (str spotify-api-url "artists/id/albums")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id, optional keys are :album_type, :market, :limit and :offset.
  :id has to have the value of an existing artist's id.
  :album_type a comma-separated list of one or several of the following:
  album, single, appears_on and compilation. Default is to query for all types.
  :market is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-an-artists-albums {:id \"7lOJ7WXyopaxri0dbOiZkd\" :album_type \"album, single\" :market \"SE\" :limit 50 :offset 50}} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call get-an-artists-top-tracks client/get (str spotify-api-url "artists/id/top-tracks")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in m are :id and :country.
  :id has to be a spotify artist id.
  :country is an ISO 3166-1 alpha-2 country code.

  Example: (get-an-artists-top-tracks {:id \"7hCsRnXtcbez8msLPfjbkz\" :country \"SE\"} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call get-an-artists-related-artists client/get (str spotify-api-url "artists/id/related-artists")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id.
  :id has to be a spotify artist id.

  Example: (get-an-artists-related-artists {:id \"4EF5vIcCYKMM61oYOG2Tqa\"} \"BQBw-JtC..._7GvA\")")

;Browse
(def-spotify-api-call get-a-list-of-featured-playlists client/get (str spotify-api-url "browse/featured-playlists")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  There are no compulsary keys in m, optional keys are :locale, :country, :timestamp, :limit and :offset.
  :locale is an ISO 639 language code and a ISO 3166-1 alpha-2 country code joined with an underscore.
  :country is an ISO 3166-1 alpha-2 country code.
  :timestamp is ISO 8601 formatted timestamp. 
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-a-list-of-featured-playlists {:locale \"sv_SE\" :country \"SE\" :timestamp \"2015-10-23T09:00:00\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call get-a-list-of-new-releases client/get (str spotify-api-url "browse/new-releases")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  There are no compulsary keys in m, optional keys are :country, :limit and :offset.
  :country is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-a-list-of-new-releases {:country \"SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call get-a-list-of-categories client/get (str spotify-api-url "browse/categories")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  There are no compulsary keys in m, optional keys are :country, :limit and :offset.
  :country is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-a-list-of-categories {:country \"SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call get-a-category client/get (str spotify-api-url "browse/categories/category_id")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :category_id, optional keys are :country and :locale.
  :category_id has to be a spotify category id.
  :country is an ISO 3166-1 alpha-2 country code.
  :locale is an ISO 639 language code and a ISO 3166-1 alpha-2 country code joined with an underscore.

  Example: (get-a-category {:category_id \"dinner\" :country \"SE\" :locale \"sv_SE\"} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call get-a-categorys-playlists client/get (str spotify-api-url "browse/categories/category_id/playlists") 
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :category_id, optional keys are :country, :limit and :offset.
  :category_id has to be a spotify category id.
  :country is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-a-categorys-playlist {:category_id \"dinner\" :country \"SE\" :locale \"sv_SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\") ")

;Follow
(def-spotify-api-call get-users-followed-artists client/get (str spotify-api-url "me/following")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :type, optional keys are :limit and :after.
  :type has to be set to \"artist\".
  :limit is the maxium number of items to return, default is 20.
  :after the last artist id retrieved from a previous request. Use this to get next set of artists.

  Example: (get-users-followed-artists {:type \"artist\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\") ")

(def-spotify-api-call follow-artists-or-users client/put (str spotify-api-url "me/following")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :type and :ids.
  :type has to be set to \"artist\" or \"user\".
  :ids has to be a comma separated list of users or artists. 

  Example: (follow-artists-or-users {:type \"artist\" :ids \"2933wDUojoQmvqSdTAE5NB,1AUCkAIfT6Ig8lOegDGK3Z\" } \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call unfollow-artists-or-users client/delete (str spotify-api-url "me/following")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :type and :ids.
  :type has to be set to \"artist\" or \"user\".
  :ids has to be a comma separated list of users or artists. 

  Example: (unfollow-artists-or-users {:type \"artist\" :ids \"0uCCBpmg6MrPb1KY2msceF,1WsMRWV5KEC2AxpYkeb2Cf\" } \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call user-following-artists-or-users? client/get (str spotify-api-url "me/following/contains")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :type and :ids.
  :type has to be set to \"artist\" or \"user\".
  :ids has to be a comma separated list of users or artists. 

  Example: (user-following-artists-or-users? {:type \"artist\" :ids \"3H6Js0rhywEhHda3UwuhGW,4r8bVMyYyGyARs9OfBvyj4\" } \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call follow-a-playlist client/put (str spotify-api-url "users/owner_id/playlists/playlist_id/followers")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in m are :owner_id and :playlist_id.
  :owner_id has to be the spotify user id of the user owning the playlist.
  :playlist_id the spotify playlist id. 

  Example: (follow-a-playlist {:owner_id \"sondre_lerche\" :playlist_id \"7q8QuE0YbAgbvzR0NJVwV8\" } \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call unfollow-a-playlist client/delete (str spotify-api-url "users/owner_id/playlists/playlist_id/followers")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in m are :owner_id and :playlist_id.
  :owner_id has to be the spotify user id of the user owning the playlist.
  :playlist_id the spotify playlist id. 

  Example: (unfollow-a-playlist {:owner_id \"ulyssestone\" :playlist_id \"3WG4abOpUpocexRakPioAg\" } \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call user-following-playlist? client/get (str spotify-api-url "users/owner_id/playlists/playlist_id/followers/contains")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in m are :owner_id and :playlist_id.
  :owner_id has to be the spotify user id of the user owning the playlist.
  :playlist_id the spotify playlist id. 

  Example: (user-following-playlist? {:owner_id \"lecowboy\" :playlist_id \"2PftXV7dgcxFjMWe75GuSG\" } \"BQBw-JtC..._7GvA\")")

;Library
(def-spotify-api-call save-tracks-for-user client/put (str spotify-api-url "me/tracks")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids.
  :ids has to be a comma separated list of spotify track ids.

  Example: (save-tracks-for-user {:ids \"3BSpuy2yHhoEazfXVU7WGq,0BcaWLANkstu2v9kghrCAj\"} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call get-users-saved-tracks client/get (str spotify-api-url "me/tracks")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  There are no compulsory keys in m, optional keys are :market, :limit and :offset.
  :market is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-users-saved-tracks {:market \"SE\" :limit 50 :offset 50}} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call remove-users-saved-tracks client/delete (str spotify-api-url "me/tracks")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids.
  :ids has to be a comma separated list of spotify track ids.

  Example: (remove-users-saved-tracks {:ids \"1RyZgkIghj1eL2U1zlYOoE,6NwbeybX6TDtXlpXvnUOZC\"} \"BQBw-JtC..._7GvA\")")

;TODO - better name for this function
(def-spotify-api-call check-users-saved-tracks client/get (str spotify-api-url "me/tracks/contains")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids.
  :ids has to be a comma separated list of spotify track ids.

  Example: (check-users-saved-tracks {:ids \"28nlyt9KhVZQ0lGQsg8Yht,09ZGF6mwJVzw5jxqbtT53E\"} \"BQBw-JtC..._7GvA\")")

;Playlists
(def-spotify-api-call get-a-list-of-a-users-playlists client/get (str spotify-api-url "users/user_id/playlists")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in  m is :user_id, optional keys are :limit and :offset.
  :user_id is the users spotify id.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-a-list-of-a-users-playlists {:user_id \"elkalel\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")")

;TODO - Change this fields string to be a map?
(def-spotify-api-call get-a-playlist client/get (str spotify-api-url "users/owner_id/playlists/playlist_id")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in  m are :user_id and :playlist_id, optional keys are :fields, :market, :limit and :offset.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :fields is a comma-separated string of fields to return from the playlist.
  See developer.spotify.com for a full list of field names.
  :market is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (get-a-playlist {:owner_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :fields \"href,name,owner\" :market \"SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call get-a-playlists-tracks client/get (str spotify-api-url "users/owner_id/playlists/playlist_id/tracks")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in  m are :user_id and :playlist_id, optional keys are :fields, :limit and :offset and :market.
  :owner_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :fields is a comma-separated string of fields to return from the playlist.
  See developer.spotify.com for a full list of field names.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-a-playlists-tracks {:owner_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :fields \"href,name,owner\":limit 50 :offset 50 :market \"SE\"} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call create-a-playlist client/post (str spotify-api-url "users/user_id/playlists")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in m are :user_id and :name, optional key is :public.
  :user_id is the users spotify id.
  :name is the name for the new playlist.
  :public is set to true if a playlist should be public and false if not.

  Example: (create-a-playlist {:user_id \"elkalel\" :name \"The songs you didn't know you liked.\" :public true} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call add-tracks-to-a-playlist client/post (str spotify-api-url "users/user_id/playlists/playlist_id/tracks")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m are :user_id and :playlist_id, optional keys are :uris and :position.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :uris a list of spotify track uris.
  :position the position to insert the tracks. 

  Example: (add-tracks-to-playlist {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :uris \"spotify:track:4iV5W9uYEdYUVa79Axb7Rh,
  spotify:track:1301WleyT98MSxVHPZCA6M\" :position 2} \"BQBw-JtC..._7GvA\")")

;TODO - Deal with correct formatting of :tracks. 
;TODO - Additional doc string with optional and required values in :tracks.
(def-spotify-api-call remove-tracks-from-a-playlist client/delete (str spotify-api-url "users/user_id/playlists/playlist_id/tracks")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m are :user_id, :playlist_id and :tracks, optional keys is :snapshot_id.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :tracks an array of objects containing spotify URI strings and corresponding position maps.
  :snapshot_id a playlist snapshot ID.
  See the spotify developer API for the logic behind deleting tracks.

  Example: (remove-tracks-from-playlist {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :tracks [{:uri \"spotify:track:4iV5W9uYEdYUVa79Axb7Rh\", :positions [2]} {:uri \"spotify:track:1301WleyT98MSxVHPZCA6M\", :positions [7]}]}  \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call reorder-a-playlists-tracks client/put (str spotify-api-url "users/user_id/playlists/playlist_id/tracks")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m are :user_id, :playlist_id, :range_start and :insert_before, optional keys are :range_length and :snapshot_id.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :range_start the position of the first track to be reordered.
  :insert_before the positions where the tracks should be inserted.
  :range_length the amount of tracks to be reordered.
  :snapshot_id a playlist snapshot ID.

  Example: (reorder-a-playlists-tracks {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :range_start 1 :range_length 2 :insert_before 3} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call replace-a-playlists-tracks client/put (str spotify-api-url "users/user_id/playlists/playlist_id/tracks")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m are :user_id and :playlist_id, optional keys are :uris and :position.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :uris a list of spotify track uris.

  Example: (replace-a-playlists-tracks {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :uris \"spotify:track:4iV5W9uYEdYUVa79Axb7Rh,
  spotify:track:1301WleyT98MSxVHPZCA6M\"} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call change-a-playlists-details client/put (str spotify-api-url "users/user_id/playlists/playlist_id")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m are :user_id and :playlist_id, optional keys are :name and :public.
  :user_id is the users spotify id.
  :playlist_id is the playlist spotify id.
  :name is the name for the new playlist.
  :public is set to true if a playlist should be public and false if not.

  Example: (change-a-playlists-details {:user_id \"elkalel\" :playlist_id \"6IIjEBw2BrRXbrSLerA7A6\" :name \"Fantastic playlist\" :public true} \"BQBw-JtC..._7GvA\")")

;Profiles
(def-spotify-api-call get-a-users-profile client/get (str spotify-api-url "users/user_id")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :user_id.
  :user_id has to be a spotify user id.

  Example: (get-a-users-profile {:user_id \"elkalel\"} \"BQBw-JtC..._7GvA\")")

;TODO - what happens when map is not present or nil?
(def-spotify-api-call get-current-users-profile client/get (str spotify-api-url "me") 
  " Takes an oauth-token t.

  Example: (get-current-users-profile {} \"BQBw-JtC..._7GvA\")") 

;Search
(def-spotify-api-call search client/get (str spotify-api-url "search")
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory keys in m are :q and :type, optional keys are :market, :limit and :offset.
  :q is the search string.
  :type is a comma separated list of items types to search for, album, artist, playlist and track.
  :market is an ISO 3166-1 alpha-2 country code.
  :limit is the maxium number of tracks to return, default is 20.
  :offset is the index of the first track to return, default is 0.

  Example: (search {:q \"Gerry Rafferty\" :type \"artist\"  :market \"SE\" :limit 50 :offset 50}} \"BQBw-JtC..._7GvA\")")

;Tracks
(def-spotify-api-call get-a-track client/get (str spotify-api-url "tracks/id") 
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :id, optional key is :market.
  :id has to have the value of an existing track's id.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-a-track {:id \"3BEmmHuv5jm0tbCUVI6ceD\" :market \"SE\"} \"BQBw-JtC..._7GvA\")")

(def-spotify-api-call get-several-tracks client/get (str spotify-api-url "tracks") 
  " Takes two arguments, a map m with query parameters and an optional oauth-token t.
  Compulsory key in m is :ids, optional key is :market.
  :ids has to be a comma separated string of spotify track ids.
  :market is an ISO 3166-1 alpha-2 country code.

  Example: (get-several-tracks {:ids \"2Dlo9QsN9ohaz9kASYkKmv,6XYvLpqJFebcjnOTolwkGw\" :market \"SE\"} \"BQBw-JtC..._7GvA\")")
