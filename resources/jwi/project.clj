(defproject clojusc/jwi "2.4.0-SNAPSHOT"
  :description "The MIT Java Wordnet Interface"
  :url "https://projects.csail.mit.edu/jwi/"
  :license {
    :name "Creative Commons 3.0"
    :url "http://creativecommons.org/licenses/by/3.0/legalcode"}
  :java-source-paths ["src/java"]
  :source-paths ^:replace []
  :signing {:gpg-key "C4BEFF6B"}
  :repositories {"local" ~(str (.toURI (java.io.File. "~/.m2/repository")))})
