(defproject clj-spotify "0.1.9"
  :description "A client library for the Spotify Web API"
  :url "https://github.com/blmstrm/clj-spotify"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}
  :plugins [[lein-cloverage "1.0.13"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.codec "0.1.1"]
                 [cheshire "5.8.1"]
                 [clj-http "3.9.1"]]
  :profiles {
             :dev [:project/dev]
             :test [:project/dev :profiles/test]
             :project/dev {
                           :source-paths ["dev-resources"]
                           :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                          [loudmoauth "0.1.3"]
                                          [ring "1.7.0"]
                                          [org.clojure/tools.nrepl "0.2.13"]]}
             :profiles/test {}})
