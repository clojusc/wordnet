(defproject clojusc/wordnet "0.4.0-SNAPSHOT"
  :description "A WordNet/JWI wrapper library"
  :url "https://github.com/clojusc/wordnet"
  :license {
    :name "Creative Commons 3.0"
    :url "http://creativecommons.org/licenses/by/3.0/legalcode"}
  :dependencies [
    [clojusc/jwi "2.4.0-SNAPSHOT"]
    [org.clojure/clojure "1.9.0"]]
  :global-vars { *warn-on-reflection* true}
  :profiles {
    :dev {
      :plugins [
        [lein-shell "0.5.0"]]}
    :test {
      :plugins [
        [lein-ltest "0.3.0"]]}}
  :aliases {
    "install-jwi" ["shell" "resources/scripts/install-jwi.sh"]
    "test" ["with-profile" "+test" "ltest"]})
