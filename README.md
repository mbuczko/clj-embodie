
# oEmbed / open-graph info fetcher

[![Clojars Project](https://img.shields.io/clojars/v/defunkt/embodie.svg)](https://clojars.org/defunkt/embodie)

The goal of this project is to fetch [oEmbed](http://oembed.com/), [open-graph](http://ogp.me/) and basic HTML information stored at given URL.

Entire API comes down to:

``` clojure
(require '[embodie.core :as core]
         '[embodie.oembed :as oembed])

(core/fetch url)
(core/fetch url providers & opts)

(oembed/init-oembed-providers)
(oembed/init-oembed-providers location)

(oembed/with-oembed-providers providers)
```

_providers_ is by vector of defined providers, `[:oembed :open-graph :html]` by default, and can be adjusted, eg. to limit amount of data gathered or simply to fetch data from one particular provider.

The `opts` map is a way to pass additional options, used currently only by `:html` provider to limit number of returned images (3 by default):

``` edn
{:max-images 3}
```

Additionally:

`init-oembed-providers` initializes a map of defined oembed providers and defaults to json stored at http://oembed.com/providers.json if no alternative location was provided.

`with-oembed-providers` is a convenience macro which (re)binds variable with oembed providers initialized before.

## Fetching in action

Typical use of API functions:

``` clojure

;; initialize oembed providers from default list
(def providers (oembed/init-oembed-providers))

;; fetch info using :oembed, :open-graph and :html providers
(oembed/with-oembed-providers providers
  (core/fetch "https://www.instagram.com/p/BoRz3GpAhsg/"))

;; only :html provider and max 1 image 
(core/fetch "https://www.instagram.com/p/BoRz3GpAhsg/" [:html] {:max-images 1})

```

