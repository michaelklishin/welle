## Changes between Welle 1.3.0 and 1.4.0

### clojurewekz.welle.kv/delete-all Is No Longer Lazily Evaluated

Contributed by Renaud Tircher..


### Automatic Deserialization Support For clojurewerkz.welle.kv/store

`clojurewerkz.welle.kv/store`, when used with the `:return-body` option, now will automatically deserialize it
the same way `clojurewerkz.welle.kv/fetch` and `clojurewerkz.welle.kv/fetch-one` do.

Suggested by Allen Johnson.


### clj-http Update

[clj-http](https://github.com/dakrone/clj-http/) dependency has been upgraded to version `0.5.5`.


### HTTP Cluster Connection URLs

Updated HTTP cluster connections to be full URLs.  This change is
primarily to allow users to specify alternate ports.

HTTP cluster connections now require a full url:

``` clojure
(require '[clojurewerkz.welle.core :as wc])

(wc/connect-to-cluster! ["http://node1:8098/riak" "http://node2:8098/riak"])
```

Also included in this change:

`clojurewerkz.welle.core/default-port` removed and replaced by the following:

  * `clojurewerkz.welle.core/default-http-port`
  * `clojurewerkz.welle.core/default-pb-port`


### Reasonable Vlock Pruning Defaults

Welle now uses reasonable vclock pruning setting defaults in `clojurewerkz.welle.buckets/update`.

Kudos to @mefesto for reporting the issue.


## Changes between Welle 1.2.0 and 1.3.0

### Riak Search Support

`clojurewerkz.welle.solr` is a new namespace that provides support for [Riak Search](http://wiki.basho.com/Riak-Search.html) using the Solr API.
Both indexing and querying are supported:

``` clojure
(require '[clojurewerkz.welle.solr    :as wsolr])

;; indexing
(wsolr/delete-via-query "an-index" "text:*")
(wsolr/index bucket-name {:username  "clojurewerkz"
                          :text      "Elastisch beta3 is out, several more @elasticsearch features supported github.com/clojurewerkz/elastisch, improved docs http://clojureelasticsearch.info #clojure"
                          :timestamp "20120802T101232+0100"
                          :id        1})
```

``` clojure
(require '[clojurewerkz.welle.solr    :as wsolr])

;; querying
(let [result (wsolr/search "an-index" "title:feature")
      hits   (wsolr/hits-from result)]
  (println result)
  (println hits))
```

Documents stored via Riak K/V (`clojurewerkz.welle.kv/store`) with the content type of `application/json`, `application/xml` or `text/plain` will be indexed
if Riak Search is enabled for the bucket.


### Support for SMILE 

Welle now provides transparent serialization and deserialization support for SMILE, just like it has for
JSON, compressed data and Clojure reader. To use it, set `:content-type` of a value to `"application/jackson-smile"`.

SMILE serialization can be extended to custom data types, see [Cheshire documentation](https://github.com/dakrone/cheshire) for
more information.

### Cheshire For JSON Serliazation

Welle now uses (and depends on) [Cheshire](https://github.com/dakrone/cheshire) for JSON serialization.
[clojure.data.json](https://github.com/clojure/data.json) is no longer a dependency.

### Clojure 1.4 By Default

Welle now depends on `org.clojure/clojure` version `1.4.0`. It is still compatible with Clojure 1.3 and if your `project.clj` depends
on 1.3, it will be used, but 1.4 is the default now.

We encourage all users to upgrade to 1.4, it is a drop-in replacement for the majority of projects out there.

### kv/fetch-all

`clojurewerkz.welle.kv/fetch-all` is a convenience functions that retrieves multiple keys concurrently (and in parallel, on multi-core
machines), optimistically assuming there will be no siblings for each one.




## Changes between Welle 1.1.0 and 1.2.0

### Validateur 1.2.0

Validateur, [Clojure validation library](http://github.com/michaelklishin/validateur) that Welle depends on, was updated to 1.2.0.


### Buckets with enabled search now get indexing precommit hook added automatically

Buckets with enabled search now get indexing precommit hook added automatically. Previously if the precommit hook
was not added in addition, values stored in the bucket were not indexed.


### Documentation correction for clojurewerkz.welle.buckets/update

`clojurewerkz.welle.buckets/update` doc string incorrectly listed the name of the option that enables search:
it is `:enable-search`, not `:enabled-for-search`.




## Changes between Welle 1.0.0 and 1.1.0

### Minor `core.cache` support improvement

`clojurewerkz.welle.cache/basic-welle-cache-factory` now has a 3-arity that accepts bucket name, content type and W to
use for cache writes.


### Cluster client support

`clojurewerkz.welle.core/connect-to-cluster` and `clojurewerkz.welle.core/connect-to-cluster!` are new functions that
use just like `clojurewerkz.welle.core/connect` and `clojurewerkz.welle.core/connect!` but use *cluster clients*.

Cluster client is a regular client (with exactly the same API) that does round-robin balancing of requests between multiple hosts
in a cluster.

`clojurewerkz.welle.core/connect-to-cluster-via-pb` and `clojurewerkz.welle.core/connect-to-cluster-via-pb!` are the PBC
transport equivalents.


### Bug fixes

`core.cache` implementation no longer fails to compile when building uberjars.


### kv/delete-all-via-2i

`clojurewerkz.welle.kv/delete-all-via-2i` is a new convenience function that combines `clojurewerkz.welle.kv/index-query`
and `clojurewerkz.welle.kv/delete-all`: it concurrently deletes multiple keys retrieved via a 2i query:

``` clojure
(ns my.app
  (:require [clojurewerkz.welle.kv :as kv]))

;; deletes multiple objects that have :stage index values between 10 and 20
(kv/delete-all-via-2i "pipeline" :stage [10 20])
```



## Changes between Welle 1.0.0-rc1 and 1.0.0

### Documentation improvements

[Documentation guides](http://clojureriak.info) have been greatly improved.

### clojurewerkz.support updated to 0.4.0

[ClojureWerkz Support](https://github.com/clojurewerkz/support) dependency has been bumped to `v0.4.0`.



## Changes between Welle 1.0.0-beta1 and 1.0.0-rc1

### ring.session.store implementation

Welle now features a [Ring session store](https://github.com/mmcgrana/ring/blob/master/ring-core/src/ring/middleware/session/store.clj) implementation.
To use it, require `clojurewerkz.welle.ring.session-store` and use `clojurewerkz.welle.ring.session-store/welle-store` function like so:

``` clojure
(ns my.service
  (:use clojurewerkz.welle.ring.session-store))

(let [store (welle-store "web_sessions")]
  ...)
```

It is possible to pass `:r`, `:w` and `:content-type` options that will be used for reads and writes:

``` clojure
(ns my.service
  (:use clojurewerkz.welle.ring.session-store))

(let [store (welle-store "web_sessions"
                         ;; r
                         3
                         ;; w
                         3
                         "application/json")]
  ...)
```

By default, `:w` and `:r` of 2 will be used and `:content-type` is `com.basho.riak.client.http.util.Constants/CTYPE_JSON_UTF8`



## Changes between Welle 1.0.0-alpha5 and 1.0.0-beta1

### Link walking support

`clojurewerkz.welle.links` namespace provides a DSL for [Riak link walking](http://wiki.basho.com/Links-and-Link-Walking.html) operations:

``` clojure
(kv/store bucket-name "joe" {:name "Joe" :age 30} :content-type "application/clojure")
(kv/store bucket-name "peter" {:name "Joe" :age 32}
          :content-type "application/clojure"
          :links [{:bucket bucket-name :key "joe" :tag "friend"}])

;; this assumes you did (:use clojurewerkz.welle.links)
;; or equivalent in the current namespace
(walk
  (start-at "people" "peter")
  (step     "people" "friend" true))
```


### Links support

`clojurewerkz.welle.kv/store` now takes the new `:links` option that lets you store
Riak links with the value. `clojurewerkz.welle.kv/fetch` then transparently deserializes when the value
is fetched back:

``` clojure
(kv/store bucket-name k v :content-type Constants/CTYPE_TEXT_UTF8 :links [{:bucket "pages" :key "clojurewerkz.org" :tag "links"}])
(let [fetched (kv/fetch-one bucket-name k)]
  (println (:links fetched)))
```


## Changes between Welle 1.0.0-alpha4 and 1.0.0-alpha5

### clojurewerkz.welle.buckets/create is now clojurewerkz.welle.buckets/update

`clojurewerkz.welle.buckets/update` better reflects what the function really does. However, `clojurewerkz.welle.buckets/create` may
reflect the intent a bit better in certain cases so it is kept for backwards compatibility.


## Changes between Welle 1.0.0-alpha3 and 1.0.0-alpha4

### Map/Reduce support: clojurewerkz.welle.mr

Initial map/reduce queries support has been implemented, everything related to it
resides under the new `clojurewerkz.welle.mr` namespace.


### clojure.core.cache implementation on top of Riak: clojurewerkz.welle.cache

`clojurewerkz.welle.cache` provides an implementation of [clojure.core.cache](https://github.com/clojure/core.cache) cache store
protocol on top of Riak.


### clojurewerkz.welle.kv/fetch-one

`clojurewerkz.welle.kv/fetch-one` is a convenience function for fetching objects in cases where conflicts/siblings are not expected.
For example, it is common to use "last write wins" strategy for caches and such. `clojurewerkz.welle.kv/fetch-one` works
the same way `clojurewerkz.welle.kv/fetch` does (and accepts exactly the same arguments) but always returns a single object,
not a list.

In case the response contains siblings, a `IllegalStateException` will be thrown.


### Validateur 1.1.0

[Validateur](https://github.com/michaelklishin/validateur) dependency has been upgraded to 1.1.0.


### Client Id Support

`clojurewerkz.welle.core/connect` now has one more arity that lets client id to be specified. In addition,
if client id is not specified explicitly, it will be generated and set.


### clojurewerkz.welle.objects is now clojurewerkz.welle.kv

`clojurewerkz.welle.objects` namespace was renamed to `clojurewerkz.welle.kv`



## Changes between Welle 1.0.0-alpha2 and 1.0.0-alpha3

### GZipped JSON Serialization Support

New `application/json+gzip` content type serializer allows Riak object values to be serialized as JSON and compressed
with gzip (using JDK's GZip implementation). Compatible with both HTTP and PB interfaces.


## Changes between Welle 1.0.0-alpha1 and 1.0.0-alpha2

### Clojure Serialization Support

If content type passed to `clojurewerkz.welle.objects/store` is `application/clojure`, Clojure reader will be used to serialize
and deserialize object value. On Clojure 1.4+, this means you can transparently store and fetch objects that include dates, too
(thanks to 1.4's extensible reader/instant literal support).


### JSON Serialization Bug Fixes

Welle now works around [this Java client bug](https://github.com/basho/riak-java-client/issues/125) that used to break
`application/json; charset=UTF-8` serialization.



### User Metadata Normalization

`clojurewerkz.welle.objects/store` now normalizes metadata by stringifying all keys and requiring that all values
are strings. This is due to the current (Riak 1.1) Java client limitations.


### clojurewerkz.welle.objects/delete-all

`clojurewerkz.welle.objects/delete-all` is a convenient way to delete multiple keys. Since Riak (as of version 1.1) does
not provide a way to delete multiple objects in a single request, this function will use [clojure.core/pmap](http://clojuredocs.org/clojure_core/clojure.core/pmap) to perform
multiple concurrent deletion requests. This implementation may or may not be suitable for your use case.



## Changes between Welle 0.1.0 and 1.0.0-alpha1

### Secondary Indexes Support

Welle now supports 2i, both indexing and querying:

``` clojure
(wo/store "people" k v :indexes {:email #{"johndoe@example.com" "timsmith@example.com"})
```

``` clojure
;; returns a list of keys
(wo/index-query "people" :email "johndoe@example.com")
```


### Switch to RawClient

Welle now uses RawClient API of the underlying Riak Java driver. This makes the API a lot more Clojuric and
much closer to [Sumo](https://github.com/reiddraper/sumo). This also makes it more flexible, allowing us
to perform automatic serialization/deserialization for stored objects for a few most commonly used content
types and control conflicts resolution without heavy boilerplate Java interoperability code in end user
applications.


### Leiningen 2

Welle now uses [Leiningen 2](https://github.com/technomancy/leiningen/wiki/Upgrading).
