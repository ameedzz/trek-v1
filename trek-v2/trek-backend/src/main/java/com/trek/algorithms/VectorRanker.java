package com.trek.algorithms;

import com.trek.model.RedditPost;
import com.trek.model.SearchResult;
import com.trek.preprocessing.PorterStemmer;
import com.trek.preprocessing.TextPreprocessor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Vector Space Model with cosine similarity.
 *
 * The idea: represent every document and query as a vector in
 * high-dimensional space (one dimension per unique term). The
 * angle between two vectors tells you how similar they are.
 *
 * cos(theta) = (q . d) / (|q| * |d|)
 *
 * Using sparse HashMaps instead of dense arrays — a 100k-term
 * vocabulary would be way too much memory otherwise. Most documents
 * only touch a few hundred unique terms anyway.
 *
 * The TF-IDF weighting here is the same as TFIDFRanker, but we're
 * using cosine similarity instead of raw score sum. This makes the
 * score angle-based rather than magnitude-based, which tends to
 * handle queries with repeated terms better.
 *
 * TODO: try normalizing query vectors differently, might help short queries
 */
public class VectorRanker {

    // docId -> sparse TF-IDF vector
    private final Map<String, Map<String, Double>> docVectors  = new HashMap<>();
    private final Map<String, Double>              magnitudes  = new HashMap<>();
    private final Map<String, Double>              idfMap      = new HashMap<>();

    // inverted index for finding candidate docs quickly
    private final Map<String, List<String>> postingList = new HashMap<>();

    private final List<RedditPost> corpus;
    private final TextPreprocessor preprocessor = new TextPreprocessor();

    public VectorRanker(List<RedditPost> corpus) {
        this.corpus = corpus;
        buildVectors();
    }

    private void buildVectors() {
        long start = System.currentTimeMillis();
        int N = corpus.size();

        // document frequency pass
        Map<String, Integer> df = new HashMap<>();
        for (RedditPost a : corpus) {
            if (a.getStems() == null) continue;
            Set<String> unique = new HashSet<>(a.getStems());
            unique.forEach(t -> df.merge(t, 1, Integer::sum));
            unique.forEach(t -> postingList.computeIfAbsent(t, k -> new ArrayList<>()).add(a.getId()));
        }

        // compute IDF
        df.forEach((term, freq) ->
            idfMap.put(term, Math.log((double) N / (1.0 + freq)))
        );

        // build TF-IDF vector per doc
        for (RedditPost a : corpus) {
            if (a.getTermFrequency() == null) continue;

            Map<String, Double> vec = new HashMap<>();
            a.getTermFrequency().forEach((term, tf) -> {
                double idf = idfMap.getOrDefault(term, 0.0);
                if (idf > 0) vec.put(term, tf * idf);
            });

            docVectors.put(a.getId(), vec);
            magnitudes.put(a.getId(), magnitude(vec));
        }

        System.out.printf("Vector index built: %d doc vectors, %d terms, %dms%n",
                docVectors.size(), idfMap.size(), System.currentTimeMillis() - start);
    }

    public List<SearchResult> search(String query, int topK) {
        Map<String, Double> qVec = vectorizeQuery(query);
        if (qVec.isEmpty()) return Collections.emptyList();

        double qMag = magnitude(qVec);
        if (qMag == 0) return Collections.emptyList();

        // only score docs that share at least one term with the query
        Set<String> candidates = new HashSet<>();
        qVec.keySet().forEach(term -> {
            List<String> hits = postingList.get(term);
            if (hits != null) candidates.addAll(hits);
        });

        Map<String, RedditPost> lookup = buildLookup();

        // min-heap of size topK so we don't sort the whole candidate list
        PriorityQueue<SearchResult> heap =
                new PriorityQueue<>(Comparator.comparingDouble(SearchResult::getScore));

        for (String docId : candidates) {
            Map<String, Double> dVec = docVectors.get(docId);
            if (dVec == null) continue;

            double dMag = magnitudes.getOrDefault(docId, 0.0);
            if (dMag == 0) continue;

            // dot product — iterate over the smaller vector (query)
            double dot = 0.0;
            for (Map.Entry<String, Double> e : qVec.entrySet()) {
                dot += e.getValue() * dVec.getOrDefault(e.getKey(), 0.0);
            }

            double cosine = dot / (qMag * dMag);
            RedditPost article = lookup.get(docId);
            if (article == null) continue;

            heap.offer(new SearchResult(article, cosine, "VECTOR"));
            if (heap.size() > topK) heap.poll(); // drop the lowest
        }

        List<SearchResult> results = new ArrayList<>(heap);
        Collections.sort(results);
        return results;
    }

    public void printQueryVector(String query) {
        Map<String, Double> vec = vectorizeQuery(query);
        System.out.printf("%nQuery vector for \"%s\":%n", query);
        vec.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> System.out.printf("  %-20s %.4f%n", e.getKey(), e.getValue()));
        System.out.printf("  magnitude = %.4f%n", magnitude(vec));
    }

    public int vocabularySize() { return idfMap.size(); }

    private Map<String, Double> vectorizeQuery(String query) {
        String clean = query.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ").trim();
        List<String> tokens = preprocessor.removeStopWords(preprocessor.tokenize(clean));
        List<String> stems  = tokens.stream().map(PorterStemmer::stem).collect(Collectors.toList());

        Map<String, Double> tf = preprocessor.computeTF(stems);
        Map<String, Double> vec = new HashMap<>();
        tf.forEach((term, tfVal) -> {
            double idf = idfMap.getOrDefault(term, 0.0);
            if (idf > 0) vec.put(term, tfVal * idf);
        });
        return vec;
    }

    private double magnitude(Map<String, Double> vec) {
        return Math.sqrt(vec.values().stream().mapToDouble(v -> v * v).sum());
    }

    private Map<String, RedditPost> buildLookup() {
        Map<String, RedditPost> map = new HashMap<>();
        corpus.forEach(a -> map.put(a.getId(), a));
        return map;
    }
}
