(defproject clj-spotify "0.1.11"
  :description "A client library for the Spotify Web API"
  :url "https://github.com/blmstrm/clj-spotify"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}
  :plugins [[lein-cloverage "1.2.2"]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "1.0.0"]
                 [org.clojure/data.codec "0.1.1"]
                 [cheshire "5.10.1"]
                 [clj-http "3.12.3"]]
  :profiles {
             :dev [:project/dev]
             :test [:project/dev :profiles/test]
             :project/dev {
                           :source-paths ["dev-resources"]
                           :dependencies [[org.clojure/tools.namespace "1.0.0"]
                                          [loudmoauth "0.1.3"]
                                          [ring "1.8.2"]
                                          [org.clojure/tools.nrepl "0.2.13"]]}
             :profiles/test {}})
