(ns clj-spotify.util
  (:require [clj-http.client :as client]))

(defn get-access-token
  "Requests an access token from Spotify's API via the Client Credentials flow.
  The returned token cannot be used for endpoints which access private user information;
  use the OAuth 2 Authorization Code flow for that."
  [client-id client-secret]
  (-> "https://accounts.spotify.com/api/token"
      (client/post {:form-params {:grant_type "client_credentials"}
                    :basic-auth [client-id client-secret]
                    :as :json})
      :body
      :access_token))
