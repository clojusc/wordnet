(ns wordnet.semantic
  "Implement semantic similarity measures including Path, Wu-Palmer, and Leacock-Chodorow"
  (:require
    [loom.alg]
    [loom.graph]
    [ubergraph.core :as uber]
    [wordnet.core   :as core]))

;;;  Parameters that are "term" are strings of the form <lemma>#<pos>#<number>, e.g. "job#n#2".
;;;  Credits: The text of some comments was derived from that of Python NLTK wordnet.py.

(defn graph-roots
  "Return root nodes of the graph"
  [graph]
  (reduce (fn [roots node]
            (when-not (loom.graph/successors graph node)
              (conj roots node)))
          []
          (loom.graph/nodes graph)))

(defn- build-graph-aux
  [graph synset edge-types]
  (if-let [children (not-empty
                     (reduce (fn [chil et] (into chil (core/related-synsets synset et)))
                             []
                             edge-types))]
    (reduce (fn [g c]
              (build-graph-aux
               (uber/add-edges g (vector (:id synset) (:id c)))
               c
               edge-types))
            graph
            children)
    graph))

(defn build-graph
  "Build a graph by following edges of the specifed types, edge-types, a collection."
  [synset edge-types]
  (let [graph (build-graph-aux (uber/digraph (:id synset)) synset edge-types)
        roots (graph-roots graph)]
    ;; Verbs don't have a common hypernym. Make one. 
    (if (= :verb (-> synset core/words first :pos))
      (reduce (fn [g root]
                (uber/add-edges g (vector root :common-verb-root)))
              graph
              roots)
      graph)))

(defn- path-pair-length
  "Return the length of path connecting the the two paths (their ends)."
  [p1 p2]
  (let [freqs (-> (into p1 p2) frequencies vals)]
    (cond (every? #(= % 1) freqs) ##Inf
          (every? #(= % 2) freqs) 1.0
          :else (->> freqs (filter #(= % 1)) count inc))))

(defn shortest-path
  "Return the shortest path from node n1 to n2, synset IDs (strings)."
  [graph n1 n2]
  (first (loom.alg/dijkstra-path-dist graph n1 n2)))

(defn shortest-path-to-root
  "Following hypernyms, return the shortest path from the synset to the root."
  [synset]
  (let [graph (build-graph synset [:hypernym :hypernym-instance])
        roots (graph-roots graph)
        paths (map #(shortest-path graph (:id synset) %) roots)
        candidates (group-by count paths)]
    (first (get candidates (apply min (keys candidates))))))

(defn- shortest-connecting-len
  "Return the length of the shortest path connecting path pairs (paths1 X paths2)."
  [paths1 paths2]
  (let [shortest (atom 999999)]
   (doall
     (for [p1 paths1
           p2 paths2]
       (swap! shortest #(min % (path-pair-length p1 p2)))))
   @shortest))

(defn general-hypernyms
  "Return all the :hypernym and :hypernym-instances of the synset."
  [synset]
  (into (core/related-synsets synset :hypernym)
        (core/related-synsets synset :hypernym-instance)))

(defn hypernym-paths
  "Return a vector of hypernym paths (back to root) for the argument term."
  [synset]
  (let [verb? (= :verb (-> synset core/words first :pos))
        result
        (loop [paths [[synset]]]
          (let [more (map general-hypernyms (map last paths))]
            (if (not (some #(not-empty %) more))
              paths
              (recur (mapv #(vec (concat %1 %2)) paths more)))))]
    (if verb?
      (mapv #(conj % {:id :common-verb-root}) result)
      result)))

(defn- max-depth
  "Return the max depth of the taxonomy for the give sequence of synsets"
  [syn-seq]
  (loop [sseq syn-seq
         max-len 0]
    (if (not sseq) 
      max-len
      (let [synset (first sseq)
            len (long (apply max (map count (hypernym-paths synset))))]
        (recur (next sseq)
               (max len max-len))))))

;;; For maximum startup speed, use the cached value commented below.
;;; They need only be updated when you upgrade the dictionary. 
(def max-depths-memo (atom {})) 
;;;(def max-depths-memo (atom {:noun-synsets 20, :verb-synsets 14}))

(defn taxonomy-max-depth
  "The maximum depth of the taxonomy for the POS.
   Call it with one of #{:noun-synsets :verb-synsets :adverb-synsets :adjective-synsets}
   It is computationally expensive, but memoized."
  [dict synset-pos]
  (or (get @max-depths-memo synset-pos)
      (let [d (max-depth (dict synset-pos))]
        (swap! max-depths-memo #(assoc % synset-pos d))
        d)))

(defn path-similarity
  "Path Distance Similarity:
   Return a score denoting how similar two word senses are, based on the
   shortest path that connects the senses in the is-a (hypernym/hypnoym)
   taxonomy. The score is in the range 0 to 1, except in those cases where
   a path cannot be found (will only be true for verbs as there are many
   distinct verb taxonomies), in which case None is returned. A score of
   1 represents identity i.e. comparing a sense with itself will return 1."
  [dict term1 term2]
  (let [s1 (-> term1 dict core/synset)
        g1 (build-graph s1 [:hypernym :hypernym-instance])
        paths1 (map #(shortest-path g1 (:id s1) %) (graph-roots g1))
        s2 (-> term2 dict core/synset)
        g2 (build-graph s2 [:hypernym :hypernym-instance])
        paths2 (map #(shortest-path g2 (:id s2) %) (graph-roots g2))]
    (/ 1.0 (shortest-connecting-len paths1 paths2))))

(defn lch-similarity
  "Return a score denoting how similar two word senses are, based on the
   shortest path that connects the senses (as above) and the maximum depth
   of the taxonomy in which the senses occur. The relationship is given as
   -log(p/2d) where p is the shortest path length and d is the taxonomy depth."
  [dict term1 term2]
  (let [s1 (-> term1 dict core/synset)
        s2 (-> term2 dict core/synset)
        pos1 (-> s1 core/words first :pos)
        pos2 (-> s2 core/words first :pos)]
    (when-not (= pos1 pos2)
      (throw (ex-info "Terms must of the the same Part of Speech." {:t1 term1 :t2 term2})))
    (let [g1 (build-graph s1 [:hypernym :hypernym-instance])
          paths1 (map #(shortest-path g1 (:id s1) %) (graph-roots g1))
          g2 (build-graph s2 [:hypernym :hypernym-instance])
          paths2 (map #(shortest-path g2 (:id s2) %) (graph-roots g2))
          p (shortest-connecting-len paths1 paths2)
          d (taxonomy-max-depth dict (keyword (str (name pos1) "-synsets")))]
      (- (Math/log (/ p (* 2.0 d)))))))

(defn least-common-subsumer
  "Return the Least Common Subsumer (most specific ancestor) common to the args paths.

   The LCS does not necessarily feature in the shortest path connecting
   the two senses, as it is by definition the common ancestor deepest in
   the taxonomy, not closest to the two senses. Typically, however, it
   will so feature. Where multiple candidates for the LCS exist, that
   whose shortest path to the root node is the longest will be selected."
  [dict term1 term2]
  (let [paths1 (map #(map :id %) (-> term1 dict core/synset hypernym-paths))
        paths2 (map #(map :id %) (-> term2 dict core/synset hypernym-paths))
        candidates (let [cands (atom [])]
                     (doall
                      (for [p1 paths1
                            p2 paths2]
                        (swap! cands conj (some #((set p1) %) p2))))
                     @cands)
        scored (group-by (fn [c]
                           (if (= c :common-verb-root)
                             1
                             (apply max (map count (-> c dict hypernym-paths)))))
                         candidates)
        best-few (get scored (->> scored keys (apply max)))]
    (if (= 1 (count best-few))
      (first best-few) ; "Where multiple candidates for the LCS exist,..."
      (let [scored (group-by #(count (shortest-path-to-root (dict %))) best-few)]
        ;; "...that whose shortest path to the root node is longest will be selected.
        (first (get scored (->> scored keys (apply max))))))))

(defn wup-similarity
  "Wu-Palmer Similarity:
   Return a score denoting how similar two word senses are, based on the
   depth of the two senses in the taxonomy and that of their Least Common
   Subsumer (most specific ancestor node, LCS).
  
   Where the LCS has multiple paths to the root, the longer path is used
   for the purposes of the calculation."
  [dict term1 term2]
  (let [lcs-sid (least-common-subsumer dict term1 term2)
        lcs-path (if (= lcs-sid :common-verb-root)
                   [:common-verb-root]
                   (shortest-path-to-root (dict lcs-sid)))
        depth (count lcs-path)
        ;; Get the shortest path from the LCS to each of the synsets it is subsuming.
        s1 (-> term1 dict core/synset)
        s2 (-> term2 dict core/synset)
        g1 (build-graph s1 [:hypernym :hypernym-instance])
        g2 (build-graph s2 [:hypernym :hypernym-instance])
        t1-path (rest (shortest-path g1 (:id s1) lcs-sid)) ; rest: don't count subsumer step???
        t2-path (rest (shortest-path g2 (:id s2) lcs-sid))
        ;; Add this to the LCS path length to get the path length from each synset to the root.
        len1 (+ (count t1-path) depth)
        len2 (+ (count t2-path) depth)]
      (/ (* 2.0 depth) (+ len1 len2))))
