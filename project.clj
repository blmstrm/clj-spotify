(defproject clj-spotify "0.1.2"
  :description "A client library for the Spotify Web API"
  :url "https://github.com/blmstrm/clj-spotify"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}
  :plugins [[lein-cloverage "1.0.6"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.codec "0.1.0"]
                 [cheshire "5.6.1"]
                 [clj-http "3.1.0"]])
