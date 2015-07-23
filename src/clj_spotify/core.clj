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

;TODO - deal with search for nice formatting. Do you want docstrings?
(defmacro spotify-api-call
  [f verb uri doc-string]
  `(defn ~f 
     ~doc-string
     ([args#] (~f args# nil))
     ([args# oauth-token#]
      (let [query-params# {:query-params (apply dissoc args# template-keys) :oauth-token oauth-token#}]
        (->
          (try
            (~verb (replace-url-values args# ~uri) query-params#)
            (catch Exception e# (ex-data e#))
            )
          (response-to-map)
          )))))

;Albums
(spotify-api-call get-album client/get (str spotify-api-url "albums/id") "")
(spotify-api-call get-several-albums client/get (str spotify-api-url "albums") "")
(spotify-api-call get-tracks-of-album client/get (str spotify-api-url "albums/id/tracks") "")

;Artists
(spotify-api-call get-artist client/get (str spotify-api-url "artists/id") "")
(spotify-api-call get-several-artists client/get (str spotify-api-url "artists") "")
(spotify-api-call get-artists-albums client/get (str spotify-api-url "artists/id/albums") "")
(spotify-api-call get-artists-top-tracks client/get (str spotify-api-url "artists/id/top-tracks") "")
(spotify-api-call get-artists-related-artists client/get (str spotify-api-url "artists/id/related-artists") "")

;Browse
(spotify-api-call get-a-list-of-featured-playlists client/get (str spotify-api-url "browse/featured-playlists") "")
(spotify-api-call get-a-list-of-new-releases client/get (str spotify-api-url "browse/new-releases") "")
(spotify-api-call get-a-list-of-categories client/get (str spotify-api-url "browse/categories") "")
(spotify-api-call get-a-category client/get (str spotify-api-url "browse/categories/category_id") "")
(spotify-api-call get-a-categorys-playlists client/get (str spotify-api-url "browse/categories/category_id/playlists") "")

;Follow
(spotify-api-call get-users-followed-artists client/get (str spotify-api-url "me/following") "")
(spotify-api-call follow-artists-or-users client/put (str spotify-api-url "me/following") "")
(spotify-api-call unfollow-artists-or-users client/delete (str spotify-api-url "me/following") "")
(spotify-api-call user-following-artists-or-users? client/get (str spotify-api-url "me/following/contains") "")
(spotify-api-call follow-a-playlist client/put (str spotify-api-url "users/owner_id/playlists/playlist_id/followers") "")
(spotify-api-call unfollow-a-playlist client/delete (str spotify-api-url "users/owner_id/playlists/playlist_id/followers") "")
(spotify-api-call user-following-playlist? client/get (str spotify-api-url "users/owner_id/playlists/playlist_id/followers/contains") "")

;Library
(spotify-api-call save-tracks-for-user client/put (str spotify-api-url "me/tracks") "")
(spotify-api-call get-users-saved-tracks client/get (str spotify-api-url "me/tracks") "")
(spotify-api-call remove-users-saved-tracks client/delete (str spotify-api-url "me/tracks") "")
(spotify-api-call check-users-saved-tracks client/get (str spotify-api-url "me/tracks/contains") "")

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
