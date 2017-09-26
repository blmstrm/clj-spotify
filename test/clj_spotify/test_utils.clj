(ns clj-spotify.test-utils
  (:require
            [clojure.data.json :as json]
            [clj-http.client :as client]
            ))

(defonce spotify-oauth-token
  (-> "https://accounts.spotify.com/api/token"
      (client/post {:form-params {:grant_type "client_credentials"}
                    :basic-auth [(System/getenv "SPOTIFY_CLIENT_ID")
                                 (System/getenv "SPOTIFY_SECRET_TOKEN")]
                    :as :json})
      :body
      :access_token))

(defn reset-volatile-vals
  "Function to reset values that change over time such as amount of followers or popularity ranking."
  [k v]
  (condp
    :followers {:href nil, :total 0}
    :popularity 0
    :total 0
    :snapshot_id "123456"
    (and (string? v) (.contains v "scdn.co")) "https://scdn.co/preview/ref"
    :else v))

 (defn test-json-string-to-map [s]
  "Read string and transform to json but ignore certain keys."
  (json/read-str s :value-fn reset-volatile-vals :key-fn keyword))
 
