(ns user
  (:require
    [loudmoauth.core :as lm]
    [ring.adapter.jetty :as ringj]
    [ring.util.response :as ringr]
    [ring.middleware.params :as ringp]
    [ring.middleware.keyword-params :as ringkp]
    [clj-spotify.core :as sptfy]
    [clj-spotify.util :as util]))

(defn handler [request]
  (condp = (:uri request)
    "/oauth2" (lm/parse-params request)
    "/interact"  (ringr/redirect (lm/user-interaction))  
    {:status 200
     :body (:uri request)}))

(def spotify-oauth2-params
  {:base-url "https://accounts.spotify.com"
   :auth-endpoint "/authorize"
   :token-endpoint "/api/token"
   :client-id (System/getenv "SPOTIFY_OAUTH2_CLIENT_ID")
   :redirect-uri "http://localhost:3000/oauth2"
   :scope "playlist-read-private playlist-modify-public playlist-modify-private ugc-image-upload user-follow-modify user-read-playback-state user-modify-playback-state user-read-currently-playing streaming"
   :custom-query-params {:show-dialog "true"}
   :client-secret (System/getenv "SPOTIFY_OAUTH2_CLIENT_SECRET")
   :provider :spotify})

(defn run
  "Starts our test web server."
  []
  (future (ringj/run-jetty (ringp/wrap-params (ringkp/wrap-keyword-params handler))  {:port 3000}))
 (lm/add-provider spotify-oauth2-params))
