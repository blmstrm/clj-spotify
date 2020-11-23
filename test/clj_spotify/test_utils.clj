(ns clj-spotify.test-utils
  (:require [clojure.data.json :as json]
            [clj-spotify.util :refer [get-access-token]]))

(defonce spotify-oauth-token
  (get-access-token
    (System/getenv "SPOTIFY_OAUTH2_CLIENT_ID")
    (System/getenv "SPOTIFY_OAUTH2_CLIENT_SECRET")))

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

 (defn test-json-string-to-map
  "Read string and transform to json but ignore certain keys."
   [s]
  (json/read-str s :value-fn reset-volatile-vals :key-fn keyword))
 
