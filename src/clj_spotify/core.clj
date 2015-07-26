(ns clj-spotify.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as string]
            )
  )

(def template-keys [:id :category_id :owner_id :playlist_id :user_id])

(def spotify-api-url "https://api.spotify.com/v1/")

(defn response-to-map
  "Parse body of http respose to json"
  [response]
  (try
    (json/read-str (:body response) :key-fn keyword)
    (catch java.lang.NullPointerException e {:error {:status "NullPointerException"  :message (.getMessage e)}})
    (catch Exception e {:error {:status "Exception"  :message (.getMessage e)}})
    )
  )

(defn- build-new-url
  "Do the building nescessary in replace-url-values here. url-parts is the split up url which values works as keys in param-map."
  [url url-parts param-map]
  (if (empty? url-parts)
    (str "https:/" url)
    (let [url-key (keyword (last url-parts))
          new-url (str "/" (if (contains? param-map url-key)
                             (url-key param-map)
                             (last url-parts))  url)]
      (recur new-url (pop url-parts) param-map))
    ) 
  )

(defn replace-url-values
  "Build a new url by switching parts in url with values in param-map. Keys are substrings of the full url. If a key is not present in template-keys it won't be exchanged."
  [param-map url]
  (if (string/blank? url)
   "" 
  (let [act-tmplt-keys  (select-keys param-map template-keys)
        split-url (string/split (string/replace url "https://" "") #"/") 
        ]
    (build-new-url "" split-url act-tmplt-keys)
    )))

;TODO - better doc string
(defmacro spotify-api-call
  "Creates a function f with doc-string d that calls the http-verb verb for url url."
  [f verb url doc-string]
  `(defn ~f 
     ~doc-string
     ([m#] (~f m# nil))
     ([m# t#]
      (let [query-params# {:query-params (apply dissoc m# template-keys) :oauth-token t#}]
        (->
          (try
            (~verb (replace-url-values m# ~url) query-params#)
            (catch Exception e# (ex-data e#))
            )
          (response-to-map)
          )))))

;Albums
(spotify-api-call get-album client/get (str spotify-api-url "albums/id")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :id, optional key is :market.
    :id has to have the value of an existing album's id.
    :market is an ISO 3166-1 alpha-2 country code.

    Example: (get-album {:id \"0MnG7y5F1n4Wns63RjEItx\" :market \"SE\"} \"BQBw-JtC..._7GvA\")")

(spotify-api-call get-several-albums client/get (str spotify-api-url "albums")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :ids, optional key is :market.
    :ids has to be a comma separated string of spotify album ids.
    :market is an ISO 3166-1 alpha-2 country code.

    Example: (get-several-albums {:ids \"4hTil0JVdMyxK2vH11ZbiX,4RjjFbiqFaO8mKTDM2Kzna\" :market \"SE\"} \"BQBw-JtC..._7GvA\") ")

(spotify-api-call get-tracks-of-album client/get (str spotify-api-url "albums/id/tracks")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :id, optional keys are :limit, :offset and :market.
    :id has to have the value of an existing album's id.
    :limit is the maxium number of tracks to return, default is 20.
    :offset is the index of the first track to return, default is 0.
    :market is an ISO 3166-1 alpha-2 country code.

    Example: (get-tracks-of-album {:id \"3XCGFOBqESirxxICswSity\" :market \"SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")")

;Artists
(spotify-api-call get-an-artist client/get (str spotify-api-url "artists/id")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :id.
    :id has to have the value of an existing artist's id.

    Example: (get-an-artist {:id \"5CE2IfdYZEQGIDsfiRm8SI\"} \"BQBw-JtC..._7GvA\")")

(spotify-api-call get-several-artists client/get (str spotify-api-url "artists") 
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :ids.
    :ids has to be a comma separated string of spotify artist ids.

    Example: (get-several-artists {:ids \"1kK2LYgoP3raZNngbb1qMT,2RitCPbwEYyYNw5LkbXTGv\"} \"BQBw-JtC..._7GvA\")")

(spotify-api-call get-an-artists-albums client/get (str spotify-api-url "artists/id/albums")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :id, optional keys are :album_type, :market, :limit and :offset.
    :id has to have the value of an existing artist's id.
    :album_type a comma-separated list of one or several of the following:
     album, single, appears_on and compilation. Default is to query for all types.
    :market is an ISO 3166-1 alpha-2 country code.
    :limit is the maxium number of tracks to return, default is 20.
    :offset is the index of the first track to return, default is 0.

    Example: (get-an-artists-albums {:id \"7lOJ7WXyopaxri0dbOiZkd\" :album_type \"album, single\" :market \"SE\" :limit 50 :offset 50}} \"BQBw-JtC..._7GvA\")")

(spotify-api-call get-an-artists-top-tracks client/get (str spotify-api-url "artists/id/top-tracks")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory keys in m are :id and :country.
    :id has to be a spotify artist id.
    :country is an ISO 3166-1 alpha-2 country code.

    Example: (get-an-artists-top-tracks {:id \"7hCsRnXtcbez8msLPfjbkz\" :country \"SE\"} \"BQBw-JtC..._7GvA\")")

(spotify-api-call get-an-artists-related-artists client/get (str spotify-api-url "artists/id/related-artists")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :id.
    :id has to be a spotify artist id.

    Example: (get-an-artists-related-artists {:id \"4EF5vIcCYKMM61oYOG2Tqa\"} \"BQBw-JtC..._7GvA\")")

;Browse
(spotify-api-call get-a-list-of-featured-playlists client/get (str spotify-api-url "browse/featured-playlists")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    There are no compulsary keys in m, optional keys are :locale, :country, :timestamp, :limit and :offset.
    :locale is an ISO 639 language code and a ISO 3166-1 alpha-2 country code joined with an underscore.
    :country is an ISO 3166-1 alpha-2 country code.
    :timestamp is ISO 8601 formatted timestamp. 
    :limit is the maxium number of tracks to return, default is 20.
    :offset is the index of the first track to return, default is 0.

    Example: (get-a-list-of-featured-playlists {:locale \"sv_SE\" :country \"SE\" :timestamp \"2015-10-23T09:00:00\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")")

(spotify-api-call get-a-list-of-new-releases client/get (str spotify-api-url "browse/new-releases")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    There are no compulsary keys in m, optional keys are :country, :limit and :offset.
    :country is an ISO 3166-1 alpha-2 country code.
    :limit is the maxium number of tracks to return, default is 20.
    :offset is the index of the first track to return, default is 0.

    Example: (get-a-list-of-new-releases {:country \"SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")")

(spotify-api-call get-a-list-of-categories client/get (str spotify-api-url "browse/categories")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    There are no compulsary keys in m, optional keys are :country, :limit and :offset.
    :country is an ISO 3166-1 alpha-2 country code.
    :limit is the maxium number of tracks to return, default is 20.
    :offset is the index of the first track to return, default is 0.

    Example: (get-a-list-of-categories {:country \"SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\")")

(spotify-api-call get-a-category client/get (str spotify-api-url "browse/categories/category_id")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :category_id, optional keys are :country and :locale.
    :category_id has to be a spotify category id.
    :country is an ISO 3166-1 alpha-2 country code.
    :locale is an ISO 639 language code and a ISO 3166-1 alpha-2 country code joined with an underscore.

    Example: (get-a-category {:category_id \"dinner\" :country \"SE\" :locale \"sv_SE\"} \"BQBw-JtC..._7GvA\")")

(spotify-api-call get-a-categorys-playlists client/get (str spotify-api-url "browse/categories/category_id/playlists") 
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :category_id, optional keys are :country, :limit and :offset.
    :category_id has to be a spotify category id.
    :country is an ISO 3166-1 alpha-2 country code.
    :limit is the maxium number of tracks to return, default is 20.
    :offset is the index of the first track to return, default is 0.

    Example: (get-a-categorys-playlist {:category_id \"dinner\" :country \"SE\" :locale \"sv_SE\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\") ")

;Follow
(spotify-api-call get-users-followed-artists client/get (str spotify-api-url "me/following")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :type, optional keys are :limit and :after.
    :type has to be set to \"artist\".
    :limit is the maxium number of items to return, default is 20.
    :after the last artist id retrieved from a previous request. Use this to get next set of artists.

    Example: (get-users-followed-artists {:type \"artist\" :limit 50 :offset 50} \"BQBw-JtC..._7GvA\") ")

(spotify-api-call follow-artists-or-users client/put (str spotify-api-url "me/following")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :type and :ids.
    :type has to be set to \"artist\" or \"user\".
    :ids has to be a comma separated list of users or artists. 

    Example: (follow-artists-or-users {:type \"artist\" :ids \"2933wDUojoQmvqSdTAE5NB,1AUCkAIfT6Ig8lOegDGK3Z\" } \"BQBw-JtC..._7GvA\")")

(spotify-api-call unfollow-artists-or-users client/delete (str spotify-api-url "me/following")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :type and :ids.
    :type has to be set to \"artist\" or \"user\".
    :ids has to be a comma separated list of users or artists. 

    Example: (unfollow-artists-or-users {:type \"artist\" :ids \"0uCCBpmg6MrPb1KY2msceF,1WsMRWV5KEC2AxpYkeb2Cf\" } \"BQBw-JtC..._7GvA\")")

(spotify-api-call user-following-artists-or-users? client/get (str spotify-api-url "me/following/contains")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :type and :ids.
    :type has to be set to \"artist\" or \"user\".
    :ids has to be a comma separated list of users or artists. 

    Example: (user-following-artists-or-users? {:type \"artist\" :ids \"3H6Js0rhywEhHda3UwuhGW,4r8bVMyYyGyARs9OfBvyj4\" } \"BQBw-JtC..._7GvA\")")

;TODO - public:true in request body.
(spotify-api-call follow-a-playlist client/put (str spotify-api-url "users/owner_id/playlists/playlist_id/followers")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory keys in m are :owner_id and :playlist_id.
    :owner_id has to be the spotify user id of the user owning the playlist.
    :playlist_id the spotify playlist id. 

    Example: (follow-a-playlist {:owner_id \"sondre_lerche\" :playlist_id \"7q8QuE0YbAgbvzR0NJVwV8\" } \"BQBw-JtC..._7GvA\")")

(spotify-api-call unfollow-a-playlist client/delete (str spotify-api-url "users/owner_id/playlists/playlist_id/followers")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory keys in m are :owner_id and :playlist_id.
    :owner_id has to be the spotify user id of the user owning the playlist.
    :playlist_id the spotify playlist id. 

    Example: (unfollow-a-playlist {:owner_id \"ulyssestone\" :playlist_id \"3WG4abOpUpocexRakPioAg\" } \"BQBw-JtC..._7GvA\")")

(spotify-api-call user-following-playlist? client/get (str spotify-api-url "users/owner_id/playlists/playlist_id/followers/contains")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory keys in m are :owner_id and :playlist_id.
    :owner_id has to be the spotify user id of the user owning the playlist.
    :playlist_id the spotify playlist id. 

    Example: (user-following-playlist? {:owner_id \"lecowboy\" :playlist_id \"2PftXV7dgcxFjMWe75GuSG\" } \"BQBw-JtC..._7GvA\")")

;Library
(spotify-api-call save-tracks-for-user client/put (str spotify-api-url "me/tracks")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :ids.
    :ids has to be a comma separated list of spotify track ids.

    Example: (save-tracks-for-user {:ids \"3BSpuy2yHhoEazfXVU7WGq,0BcaWLANkstu2v9kghrCAj\"} \"BQBw-JtC..._7GvA\")")
                                                                    
(spotify-api-call get-users-saved-tracks client/get (str spotify-api-url "me/tracks")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    There are no compulsory keys in m, optional keys are :market, :limit and :offset.
    :market is an ISO 3166-1 alpha-2 country code.
    :limit is the maxium number of tracks to return, default is 20.
    :offset is the index of the first track to return, default is 0.

    Example: (get-users-saved-tracks {:market \"SE\" :limit 50 :offset 50}} \"BQBw-JtC..._7GvA\")")

(spotify-api-call remove-users-saved-tracks client/delete (str spotify-api-url "me/tracks")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :ids.
    :ids has to be a comma separated list of spotify track ids.

    Example: (remove-users-saved-tracks {:ids \"1RyZgkIghj1eL2U1zlYOoE,6NwbeybX6TDtXlpXvnUOZC\"} \"BQBw-JtC..._7GvA\")")

;TODO - better name for this function
(spotify-api-call check-users-saved-tracks client/get (str spotify-api-url "me/tracks/contains")
" Takes two arguments, a map m with query parameters and an optional oauth-token t.
    Compulsory key in m is :ids.
    :ids has to be a comma separated list of spotify track ids.

    Example: (check-users-saved-tracks {:ids \"28nlyt9KhVZQ0lGQsg8Yht,09ZGF6mwJVzw5jxqbtT53E\"} \"BQBw-JtC..._7GvA\")")

;Playlists
(spotify-api-call get-a-list-of-a-users-playlists client/get (str spotify-api-url "users/user_id/playlists") "")
(spotify-api-call get-a-playlist client/get (str spotify-api-url "users/user_id/playlists/playlist_id") "")
(spotify-api-call get-a-playlists-tracks client/get (str spotify-api-url "users/user_id/playlists/playlist_id/tracks") "")
(spotify-api-call create-a-playlist client/post (str spotify-api-url "users/user_id/playlists") "")
(spotify-api-call add-tracks-to-a-playlist client/post (str spotify-api-url "users/user_id/playlists/playlist_id/tracks") "")
(spotify-api-call remove-tracks-from-a-playlist client/delete (str spotify-api-url "users/user_id/playlists/playlist_id/tracks") "")
(spotify-api-call reorder-a-playlists-tracks client/put (str spotify-api-url "users/user_id/playlists/playlist_id/tracks") "")
(spotify-api-call replace-a-playlists-tracks client/put (str spotify-api-url "users/user_id/playlists/playlist_id/tracks") "")
(spotify-api-call change-a-playlists-details client/put (str spotify-api-url "users/user_id/playlists/playlist_id") "")

;Profiles
(spotify-api-call get-a-users-profile client/get (str spotify-api-url "users/user_id") "")
(spotify-api-call get-current-users-profile client/get (str spotify-api-url "me") "")

;Search
(spotify-api-call search client/get (str spotify-api-url "search") "")

;Tracks
(spotify-api-call get-a-track client/get (str spotify-api-url "tracks/id") "")
(spotify-api-call get-several-tracks client/get (str spotify-api-url "tracks") "")

