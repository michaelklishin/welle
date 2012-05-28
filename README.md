# Welle, a Clojure client for Riak

Welle is a young idiomatic Clojure client for Riak built on top of the Riak Java driver.
Its API and code style closely follow other [ClojureWerkz Clojure libraries](http://clojurewerkz.org), namely [Neocons](https://github.com/michaelklishin/neocons), [Elastisch](https://github.com/clojurewerkz/elastisch)
and [Monger](https://github.com/michaelklishin/monger).

Welle is not the only Clojure client for Riak on the block: there is a client called [Sumo](https://github.com/reiddraper/sumo)
by one of Basho's engineers. If you are evaluating Welle, please consider Sumo as well.


## Project Goals

 * Be well maintained
 * Be [well documented](http://clojureriak.info)
 * Be [well tested](https://github.com/michaelklishin/welle/tree/master/test/clojurewerkz/welle/test)
 * Target Clojure 1.3.0 and later from the ground up
 * Batteries included: [clojure.core.cache](http://github.com/clojure/core.cache) implementation on top of Riak and so on
 * Be friendly to Heroku and other PaaS providers


## Supported Features

 * [HTTP and Protocol Buffers transports](http://clojureriak.info/articles/connecting.html)
 * [Bucket operations](http://clojureriak.info/articles/buckets.html): create, update, delete
 * [Key/Value operations](http://clojureriak.info/articles/kv.html): put, fetch, delete
 * [Secondary indexes](http://clojureriak.info/articles/2i.html) (2i): indexing, index queries
 * [Content-type based serialization of values](http://clojureriak.info/articles/kv.html#automatic_serialization_for_common_formats) in common formats (bytes, JSON, Clojure data/reader, UTF-8 text, gzipped JSON)
 * Storing links on values, [link walking](http://clojureriak.info/articles/links.html)
 * [Map/Reduce queries](http://clojureriak.info/articles/mapreduce.html)
 * [clojure.core.cache](https://github.com/clojure/core.cache) implementation on top of Riak
 * [Ring session store](https://github.com/mmcgrana/ring/blob/master/ring-core/src/ring/middleware/session/store.clj) implementation on top of Riak
 * [data.json](http://github.com/clojure/data.json) extensions for serialization of JodaTime and JDK dates


## Supported Clojure versions

Welle is built from the ground up for Clojure 1.3 and up. To store dates/instants with Clojure data serialization, Clojure 1.4.0
is the minimum required version because Clojure 1.3 reader cannot handle `java.util.Date` instances. As such, Clojure 1.4 is
recommended.


## Getting Started

Please refer to our [Getting Started with Clojure and Riak](http://clojureriak.info/articles/getting_started.html) guide.
Don't hesitate to join our [mailing list](https://groups.google.com/forum/#!forum/clojure-riak) and ask questions, too!


## Community

[Welle has a mailing list](https://groups.google.com/forum/#!forum/clojure-welle). Feel free to join it and ask any questions you may have.

To subscribe for announcements of releases, important changes and so on, please follow [@ClojureWerkz](https://twitter.com/#!/clojurewerkz) on Twitter.



## Maven Artifacts

### Most Recent Release

With Leiningen:

    [com.novemberain/welle "1.0.1"]


With Maven:

    <dependency>
      <groupId>com.novemberain</groupId>
      <artifactId>welle</artifactId>
      <version>1.0.1</version>
    </dependency>


## Documentation & Examples

Welle has [documentation guides](http://clojureriak.info). Documentation is one
of the top priorities for the project and we are improving things week after week.

For additional code examples, see our [test suite](https://github.com/michaelklishin/welle/tree/master/test/clojurewerkz/welle/test).


## Welle Is a ClojureWerkz Project

Welle is part of the [group of Clojure libraries known as ClojureWerkz](http://clojurewerkz.org), together with
[Monger](https://github.com/michaelklishin/monger), [Langohr](https://github.com/michaelklishin/langohr), [Elastisch](https://github.com/clojurewerkz/elastisch), [Quartzite](https://github.com/michaelklishin/quartzite), [Neocons](https://github.com/michaelklishin/neocons) and several others.



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

Copyright (C) 2011-2012 [Michael S. Klishin](http://twitter.com/michaelklishin)

Distributed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html), the same as Clojure.
