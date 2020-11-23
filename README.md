[![Build Status](https://travis-ci.org/blmstrm/clj-spotify.svg?branch=master)](https://travis-ci.org/blmstrm/clj-spotify)
[![Clojars](https://img.shields.io/clojars/v/clj-spotify.svg)](http://clojars.org/clj-spotify)
[![Coverage Status](https://coveralls.io/repos/blmstrm/clj-spotify/badge.svg?branch=master&service=github)](https://coveralls.io/github/blmstrm/clj-spotify?branch=master)
[![Dependencies Status](https://versions.deps.co/Blmstrm/clj-spotify/status.svg)](https://versions.deps.co/Blmstrm/clj-spotify)
[![Downloads](https://versions.deps.co/Blmstrm/clj-spotify/downloads.svg)](https://versions.deps.co/Blmstrm/clj-spotify)
# clj-spotify
clj-spotify is a client library for accessing the Spotify Web API. clj-spotify aims to be identical to Spotify's Web API when it comes to naming of endpoints, path elements and query parameters therefore the documentation found here is very short. Please read through the [Spotify API Endpoint Reference](https://developer.spotify.com/web-api/endpoint-reference/), a one to one mapping from the endpoints to functions in this library should be possible.

## Usage
Each function takes a map `m` of parameters and a sometimes optional oauth token `t`. Function names are the same as the names found in [Spotify's API Endpoint Reference](https://developer.spotify.com/web-api/endpoint-reference/). A function call to retrieve a track from Spotify then has the function signature `(get-a-track m t)` and a function call to get an album's tracks has the function signature `(get-an-albums-tracks m t)` and so on.

### map `m`
The map `m` holds both path elements and query parameters.
Key names and value types in `m` are the same as those found in [Spotify's API Endpoint Reference](https://developer.spotify.com/web-api/endpoint-reference/) for path elements and query parameters. Integer values such as `:limit` and `:offset` can be specified either as integers or strings in `m`. 

### oauth token `t`
For a simple method to obtain an access token through the Client Credentials flow, use `clj-spotify.util/get-access-token`. If you need oauth2 authentication through the Authorization Code Flow (for example, to use endpoints which access private user information), see this blog post on how to roll your own: [OAuth2 is easy - illustrated in 50 lines of Clojure](http://leonid.shevtsov.me/en/oauth2-is-easy).

### Return values
clj-spotify returns the data received from the Spotify Web API unaltered but the response will be converted from json to a Clojure map.

### Error handling
Error messages received from Spotify are returned unaltered as received from the servers. If an API call would result in an exception in clj-spotify this will be returned on the same format as Spotify's error messages but with the `:status` key set to `Exception`and the result of calling `(.getMessage e)` on the Exception `e` associated with the `:message` key.

## Development
### Registering a Client
Head to the [developer dashboard](https://developer.spotify.com/dashboard) and register a new application.
If you wish to make use of the [`dev-resources`](./dev-resources/user.clj) in this project:
- Navigate to [your new app](https://developers.spotify.com/dashboard/applications), then > _edit settings_
- Setup two redirects: `http://localhost:3000/interact`, `http://localhost:3000/oauth2`
  * NOTE: The port/endpoint is arbitrary; just make sure your jetty server + handler, and redirects are pointing to the same URIs
- Take note of `CLIENTID, CLIENTSECRET`
- If you want to grab your user's URI while you're there, the [`/v1/me`](https://developer.spotify.com/console/get-current-user/) console is by far the most straightforward way.
  * Find the "uri" property in the payload, e.g. `"uri": "spotify:user:1134745600"`

### Hello Spotify (oauth2)
With credentials in hand, we're ready to make our first request!
- Ensure you've exported ClientId / ClientSecret as `SPOTIFY_OAUTH2_CLIENT_ID` & `SPOTIFY_OAUTH2_CLIENT_SECRET` respectively.
  * e.g, in a POSIX shell `EXPORT SPOTIFY_OAUTH2_CLIENT_ID='7bf224e8jn954215s3g11degh599e3an'`
- Open [`dev-resources/user.clj`](./dev-resources/user.clj), and a REPL
- Eval the buffer to load the forms, then call `(run)` to start the server
- Open http://localhost:3000/interact, go through the oauth flow
- Eval the following `(sptfy/get-current-users-profile {} (lm/oauth-token :spotify))`

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
