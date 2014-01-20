# Welle, a Clojure client for Riak

Welle is an expressive Clojure client for Riak with batteries included.
Its API and code style closely follow other [ClojureWerkz Clojure libraries](http://clojurewerkz.org), namely [Neocons](https://github.com/michaelklishin/neocons), [Elastisch](https://github.com/clojurewerkz/elastisch)
and [Monger](https://github.com/michaelklishin/monger).

Welle is not the only Clojure client for Riak on the block: there is a client called [Sumo](https://github.com/reiddraper/sumo)
by one of Basho's engineers. If you are evaluating Welle, please consider Sumo as well.


## Project Goals

 * Be well maintained
 * Be [well documented](http://clojureriak.info)
 * Be [well tested](https://github.com/michaelklishin/welle/tree/master/test/clojurewerkz/welle/test)
 * Target modern Clojure versions
 * Batteries included: [clojure.core.cache](http://github.com/clojure/core.cache) implementation on top of Riak and so on
 * Be friendly to Heroku and other PaaS providers


## Project Maturity

Welle is a mature project that is over 1 year old and has been used to build systems that process at least tens of gigabytes
of data per day.

### Supported Features

 * [HTTP and Protocol Buffers transports](http://clojureriak.info/articles/connecting.html)
 * [Bucket operations](http://clojureriak.info/articles/buckets.html): create, update, delete
 * [Key/Value operations](http://clojureriak.info/articles/kv.html): put, fetch, delete
 * [Secondary indexes](http://clojureriak.info/articles/2i.html) (2i): indexing, index queries
 * Counters (Riak 1.4)
 * [Content-type based serialization of values](http://clojureriak.info/articles/kv.html#automatic_serialization_for_common_formats) in common formats (bytes, JSON, Clojure data/reader, UTF-8 text, gzipped JSON)
 * [Riak Search](http://clojureriak.info/articles/search.html) support
 * Storing links on values, [link walking](http://clojureriak.info/articles/links.html)
 * [Map/Reduce queries](http://clojureriak.info/articles/mapreduce.html)
 * [clojure.core.cache](https://github.com/clojure/core.cache) implementation on top of Riak
 * [Ring session store](https://github.com/mmcgrana/ring/blob/master/ring-core/src/ring/middleware/session/store.clj) implementation on top of Riak
 * [Cheshire](https://github.com/dakrone/cheshire) and [data.json](http://github.com/clojure/data.json) extensions for serialization of JodaTime and JDK dates


## Supported Clojure versions

Welle requires Clojure 1.4.
The most recent stable Clojure release is highly recommended.


## Supported Riak Versions

Welle `2.0` targets Riak 1.4+ and has breaking API changes compared to `1.5.0`.

Welle `1.5.0` is compatible with Riak `1.3.0`.


## Getting Started

Please refer to our [Getting Started with Clojure and Riak](http://clojureriak.info/articles/getting_started.html) guide.
Don't hesitate to join our [mailing list](https://groups.google.com/forum/#!forum/clojure-riak) and ask questions, too!


## Community

[Welle has a mailing list](https://groups.google.com/forum/#!forum/clojure-riak). Feel free to join it and ask any questions you may have.

To subscribe for announcements of releases, important changes and so on, please follow [@ClojureWerkz](https://twitter.com/#!/clojurewerkz) on Twitter.



## Maven Artifacts

Welle artifacts are [released to Clojars](https://clojars.org/com.novemberain/welle).

### With Leiningen

Add dependency in your `project.clj`:

``` clojure
[com.novemberain/welle "2.0.0-beta1"]
```

### With Maven

Add Clojars repository definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

and then the dependency:

``` xml
<dependency>
  <groupId>com.novemberain</groupId>
  <artifactId>welle</artifactId>
  <version>2.0.0-beta1</version>
</dependency>
```


## Documentation & Examples

Welle has [documentation guides](http://clojureriak.info).

For additional code examples, see our [test suite](https://github.com/michaelklishin/welle/tree/master/test/clojurewerkz/welle/test).


## Welle Is a ClojureWerkz Project

Welle is part of the [group of Clojure libraries known as ClojureWerkz](http://clojurewerkz.org), together with

 * [Monger](https://clojuremongodb.info)
 * [Cassaforte](https://clojurecassandra.info)
 * [Elastisch](https://clojureelasticsearch.info)
 * [Neocons](https://clojureneo4j.info)
 * [Meltdown](https://github.com/clojurewerkz/meltdown)

and many others.


## Project Maturity

Welle has been around since 2011. The API is generally stable with some (badly
needed) breaking changes coming in 2.0.


## Continuous Integration

[![Continuous Integration status](https://secure.travis-ci.org/michaelklishin/welle.png)](http://travis-ci.org/michaelklishin/welle)

CI is hosted by [travis-ci.org](http://travis-ci.org)


## Development

Welle uses [Leiningen 2](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md). Make
sure you have it installed and then run tests against all supported Clojure versions using

    lein2 all test

Then create a branch and make your changes on it. Once you are done with your changes and all
tests pass, submit a pull request on Github.


## License

Copyright (C) 2011-2014 [Michael S. Klishin](http://twitter.com/michaelklishin)

Double licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) (the same as Clojure) or
the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).


[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/michaelklishin/welle/trend.png)](https://bitdeli.com/free "Bitdeli Badge")

