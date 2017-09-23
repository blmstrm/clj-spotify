[![Build Status](https://travis-ci.org/blmstrm/clj-spotify.svg?branch=master)](https://travis-ci.org/blmstrm/clj-spotify)
[![Clojars](https://img.shields.io/clojars/v/clj-spotify.svg)](http://clojars.org/clj-spotify)
[![Coverage Status](https://coveralls.io/repos/blmstrm/clj-spotify/badge.svg?branch=master&service=github)](https://coveralls.io/github/blmstrm/clj-spotify?branch=master)
[![Dependencies Status](http://jarkeeper.com/blmstrm/clj-spotify/status.png)](http://jarkeeper.com/blmstrm/clj-spotify)
# clj-spotify
clj-spotify is a client library for accessing the Spotify Web API. clj-spotify aims to be identical to Spotify's Web API when it comes to naming of endpoints, path elements and query parameters therefore the documentation found here is very short. Please read through the [Spotify API Endpoint Reference](https://developer.spotify.com/web-api/endpoint-reference/), a one to one mapping from the endpoints to functions in this library should be possible.

Please note that at the moment `clj-spotify 0.1.2` needs [cheshire](https://github.com/dakrone/cheshire) as a dependency in your `project.clj`. See this [PR](https://github.com/blmstrm/clj-spotify/pull/4).

## Usage
Each function takes a map `m` of parameters and a sometimes optional oauth token `t`. Function names are the same as the names found in [Spotify's API Endpoint Reference](https://developer.spotify.com/web-api/endpoint-reference/). A function call to retrieve a track from Spotify then has the function signature `(get-a-track m t)` and a function call to get an album's tracks has the function signature `(get-an-albums-tracks m t)` and so on.

### map `m`
The map `m` holds both path elements and query parameters.
Key names and value types in `m` are the same as those found in [Spotify's API Endpoint Reference](https://developer.spotify.com/web-api/endpoint-reference/) for path elements and query parameters. Integer values such as `:limit` and `:offset` can be specified either as integers or strings in `m`. 

### oauth token `t`
For a simple method to deal with authentication through the Client Credentials Flow see the `spotify-oauth-token` variable in `core_test.clj`. If you need oauth2 authentication through the Authorization Code Flow see this blog post on how to roll your own: [OAuth2 is easy - illustrated in 50 lines of Clojure](http://leonid.shevtsov.me/en/oauth2-is-easy). 

### Return values
clj-spotify returns the data received from the Spotify Web API unaltered but the response will be converted from json to a Clojure map.

### Error handling
Error messages received from Spotify are returned unaltered as received from the servers. If an API call would result in an exception in clj-spotify this will be returned on the same format as Spotify's error messages but with the `:status` key set to `Exception`and the result of calling `(.getMessage e)` on the Exception `e` associated with the `:message` key.

## Additional documentation
Each function has a basic doc string but if the usage still is unclear please consult [Spotify's API Endpoint Reference](https://developer.spotify.com/web-api/endpoint-reference/).

## License

The MIT License (MIT)

Copyright (c) 2017 Karl Blomström

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
