(ns clj-spotify.test-util
  (:require
            [clojure.data.json :as json]
            [clojure.data :as data]
            [clj-http.client :as client]
            [clojure.data.codec.base64 :as b64]
            ))

(def enc-auth-string
  (str "Basic "
  (->
    (str (System/getenv "SPOTIFY_CLIENT_ID") ":" (System/getenv "SPOTIFY_SECRET_TOKEN"))
    (.getBytes)
    (b64/encode)
    (String. "UTF-8"))))

(defonce spotify-oauth-token (->
                           "https://accounts.spotify.com/api/token" 
                           (client/post {:form-params {:grant_type "client_credentials"} :headers {:Authorization enc-auth-string}})
                           :body 
                           (json/read-str :key-fn keyword)
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
 
