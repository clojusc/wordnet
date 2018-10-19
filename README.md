# Clojure WordNet
[![Build Status](https://secure.travis-ci.org/clojusc/wordnet.png)](http://travis-ci.org/clojusc/wordnet)

A thin/partial wrapper around some [JWI](http://projects.csail.mit.edu/jwi/)
functionality, for interfacing the [WordNet](http://wordnet.princeton.edu/)
database using idiomatic Clojure.

## Prerequisites

You will need [Leiningen](https://github.com/technomancy/leiningen)
2.3.4 or above installed.

## Building

To build and install the library locally, run:

    $ git submodule update --init data
    $ lein test
    $ lein install

## Including in your project

There is an initial version hosted at [Clojars](https://clojars.org/clojusc/wordnet).
For leiningen include a dependency:

```clojure
[clojusc/wordnet "1.0.0"]
```

For maven-based projects, add the following to your `pom.xml`:

```xml
<dependency>
  <groupId>clojusc</groupId>
  <artifactId>wordnet</artifactId>
  <version>1.0.0</version>
</dependency>
```

## WordNet Database

The WordNet database is not bundled in this project; it is _referenced_
via a git submodule, in order to run integration tests. In order to
ensure the submodule is properly initialised, follow the build
instructions above.

## Quick Examples

```clojure
(require '[wordnet.core :as wordnet])
(def dict (wordnet/make-dictionary "../path-to/wordnet/dict/"))

(def dog (first (dict "dog" :noun)))

(:lemma dog)
=> "dog"

(:pos dog)
=> :noun

(:gloss (wordnet/synset dog))
=> "a member of the genus Canis (probably descended from the common wolf) that
    has been domesticated by man since prehistoric times; occurs in many breeds;
    \"the dog barked all night\""

(map :lemma (wordnet/words (wordnet/synset dog))
=> ("dog" "domestic_dog", "Canis_familiaris")

(def frump (first (dict "frump" :noun)))

(map :lemma (wordnet/related-words frump :derivationally-related))
=> ("frumpy")

(->> :hypernym
     (wordnet/related-synsets (wordnet/synset dog))
     (mapcat wordnet/words)
     (reduce (fn [acc x] (conj acc (:lemma x)))
             #{}))
=> ("domestic_animal" "domesticated_animal" "canine" "canid")
```

The previous two have been merged into a single function now offered for
convenience:

```clj
(wordnet/synonyms dict "dog" :noun)
```


## Dictionary

The default dictionary will load definitions from the database as needed
and they will be cached as necessary. If higher performance is required
and there is sufficient memory available to the JVM, then the dictionary
can be made to be resident entirely in memory, as below. This will force
an immediate load of the dictionary into RAM, where there may be a
perceptible delay on startup.

```clojure
(def dict (wordnet/make-dictionary "../path-to/wordnet/dict/" :in-memory))
```

Note: Wordnet is quite large, and usually won’t fit into the standard heap on most
32-bit JVMs. You need to increase your heap size. On the Sun JVM, this involves
the command line flag -Xmx along with a reasonable heap size, say, 500 MB or 1 GB.

## Word Lookup

Word definitions can be fetched using the ```make-dictionary``` factory as per the
example below:

```clojure
(def dict (wordnet/make-dictionary "../path-to/wordnet/dict/"))

(dict "car#n#1")    ; fetch the first noun definition for car

(dict "bus")        ; fetch a list of all definitions for bus

(dict "row" :noun)  ; fetch a list of all noun definitions for row

(dict "row#v#1")    ; fetch the single verb definition for row

(dict "WID-02086723-N-01-dog" ; fetch the word with the specified ID

(dict "SID-02086723-N" ; fetch the synset with the specified ID
```

## TODO

* ~~Implement ```(make-dictionary "../path-to/wordnet/dict/" :in-memory)``` to use
  RAM-based dictionary~~
* ~~Coerce functions into separate namespace~~
* ~~Re-implement ```(related-synsets ...)``` and ```(related-words ...)```~~
* ~~Push JWI 2.2.4 to central repository~~
* ~~Unit tests & Travis CI~~
* Implement more similarity algorithms
* Improve performance

## License

Same as JWI: MIT / [Creative Commons 3.0](http://creativecommons.org/licenses/by/3.0/legalcode)
