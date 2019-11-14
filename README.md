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

For a lein-based project, add the following to your `:dependencies`:

```clojure
[clojusc/wordnet "1.1.0-SNAPSHOT"]
```

For maven-based projects, add the following to your `pom.xml`:

```xml
<dependency>
  <groupId>clojusc</groupId>
  <artifactId>wordnet</artifactId>
  <version>1.1.0-SNAPSHOT</version>
</dependency>
```


## WordNet Database

The WordNet database is not bundled in this project; it is _referenced_
via a git submodule, in order to run integration tests. In order to
ensure the submodule is properly initialised, follow the build
instructions above.


## Quick Examples

Pull in the namespace and set up the dictionary:

```clj
(require '[wordnet.core :as wordnet])

(def dict (wordnet/make-dictionary "../path-to/wordnet/dict/"))
```

Define a word:

```clj
(def dog (first (dict "dog" :noun)))
```

or, alternatively:

```clj
(def dog (dict "dog#n#1"))
```

Now we can do things with our defined word:

```clj
(:lemma dog)
=> "dog"

(:pos dog)
=> :noun

(:gloss (wordnet/synset dog))
=> "a member of the genus Canis (probably descended from the common wolf) that
    has been domesticated by man since prehistoric times; occurs in many breeds;
    \"the dog barked all night\""

(mapv :lemma (wordnet/words (wordnet/synset dog)))
=> ["dog" "domestic_dog", "Canis_familiaris"]

(def frump (dict "frump#n#1"))

(mapv :lemma (wordnet/related-words frump :derivationally-related))
=> ["frumpy"]

(->> :hypernym
     (wordnet/related-synsets (wordnet/synset dog))
     (mapcat wordnet/words)
     (reduce (fn [acc x] (conj acc (:lemma x)))
             []))
=> ["canine" "canid" "domestic_animal" "domesticated_animal"]
```

The previous examples just used the first definition of dog; the following use
all nouns in the WordNet dictionary for "dog":

```clj
(->> (dict "dog" :noun)
     (mapcat (comp wordnet/words wordnet/synset))
     (reduce (fn [acc x] (conj acc (:lemma x)))
             []))
=> ["dog" "domestic_dog" "Canis_familiaris" "frump"
    "dog" "dog" "cad" "bounder" "blackguard" "dog"
    "hound" "heel" "frank" "frankfurter" "hotdog"
    "hot_dog" "dog" "wiener" "wienerwurst" "weenie"
    "pawl" "detent" "click" "dog" "andiron" "firedog"
    "dog" "dog-iron"]

(->> (dict "dog" :noun)
     (mapcat (comp #(wordnet/related-synsets % :hypernym) wordnet/synset))
     (mapcat wordnet/words)
     (reduce (fn [acc x] (conj acc (:lemma x)))
             []))
=> ["canine" "canid" "domestic_animal" "domesticated_animal"
    "unpleasant_woman" "disagreeable_woman" "chap" "fellow"
    "feller" "fella" "lad" "gent" "blighter" "cuss" "bloke"
    "villain" "scoundrel" "sausage" "catch" "stop" "support"]
```

The previous examples have been merged into a single function now offered for
convenience:

```clj
(wordnet/synonyms dict "dog" :noun)
=> ["dog" "domestic_dog" "Canis_familiaris" "frump"
    "dog" "dog" "cad" "bounder" "blackguard" "dog"
    "hound" "heel" "frank" "frankfurter" "hotdog"
    "hot_dog" "dog" "wiener" "wienerwurst" "weenie"
    "pawl" "detent" "click" "dog" "andiron" "firedog"
    "dog" "dog-iron" "canine" "canid" "domestic_animal"
    "domesticated_animal" "unpleasant_woman"
    "disagreeable_woman" "chap" "fellow" "feller" "fella"
    "lad" "gent" "blighter" "cuss" "bloke" "villain"
    "scoundrel" "sausage" "catch" "stop" "support")
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

## Word Similarity 

Wu-Palmer, Leacock-Chodorow, and Path Semantic Similarity can be computed: 

```clojure
(require '[wordnet.semantic :as sem])

(sem/path-similarity dict "task#n#1" "job#n#2")  ; path similarity

(sem/wup-similarity dict "task#n#1" "job#n#2")  ; Wu-Palmer similarity

(sem/lch-similarity dict "task#n#1" "job#n#2")  ; Leacock-Chodorow similarity
```
Leacock-Chodorow can take a few seconds the first time it is used because it needs to 
compute the maximum depth of the taxonomy for the given part of speech.  After the first 
call subsequent calls use the memoized value, so they run quickly. 
If you are using the modern dictionary you can sidestep the wait by uncommenting the
line in the code `(def max-depths-memo (atom {:noun-synsets 20, :verb-synsets 14}))`, since those are the values.

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
