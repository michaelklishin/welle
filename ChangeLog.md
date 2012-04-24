## Changes between Welle 1.0.0-alpha1 and 1.0.0-alpha2

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
