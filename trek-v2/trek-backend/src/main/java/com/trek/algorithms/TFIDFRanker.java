package com.trek.algorithms;

import com.trek.model.RedditPost;
import com.trek.model.SearchResult;
import com.trek.preprocessing.PorterStemmer;
import com.trek.preprocessing.TextPreprocessor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TF-IDF ranking using an inverted index.
 *
 * Classic approach — been the backbone of search since the 70s.
 * Not perfect (BM25 beats it on most benchmarks) but it's simple
 * and easy to reason about, which is why I built this first.
 *
 * score(q, d) = sum of [ TF(term, doc) * IDF(term) ] for each query term
 *
 * TF  = how often a term appears in a doc (normalized by doc length)
 * IDF = log(N / (1 + df)) — rare terms get higher weight
 */
public class TFIDFRanker {

    // term -> list of (docId, tf) pairs
    private final Map<String, List<Posting>> invertedIndex = new HashMap<>();
    private final Map<String, Double> idfMap = new HashMap<>();
    private final List<RedditPost> corpus;
    private final TextPreprocessor preprocessor = new TextPreprocessor();

    public TFIDFRanker(List<RedditPost> corpus) {
        this.corpus = corpus;
        buildIndex();
    }

    private void buildIndex() {
        long start = System.currentTimeMillis();

        for (RedditPost article : corpus) {
            if (article.getTermFrequency() == null) continue;

            article.getTermFrequency().forEach((term, tf) ->
                invertedIndex
                    .computeIfAbsent(term, k -> new ArrayList<>())
                    .add(new Posting(article.getId(), tf))
            );
        }

        // IDF with +1 smoothing to avoid division by zero
        int N = corpus.size();
        invertedIndex.forEach((term, postings) -> {
            double idf = Math.log((double) N / (1.0 + postings.size()));
            idfMap.put(term, idf);
        });

        System.out.printf("TF-IDF index built: %d terms, %dms%n",
                invertedIndex.size(), System.currentTimeMillis() - start);
    }

    public List<SearchResult> search(String query, int topK) {
        List<String> terms = preprocessQuery(query);
        if (terms.isEmpty()) return Collections.emptyList();

        Map<String, Double> scores = new HashMap<>();
        Map<String, RedditPost> lookup = buildLookup();

        for (String term : terms) {
            List<Posting> postings = invertedIndex.get(term);
            if (postings == null) continue;

            double idf = idfMap.getOrDefault(term, 0.0);
            for (Posting p : postings) {
                scores.merge(p.docId, p.tf * idf, Double::sum);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new SearchResult(lookup.get(e.getKey()), e.getValue(), "TF-IDF"))
                .filter(r -> r.getPost() != null)
                .collect(Collectors.toList());
    }

    // useful for debugging why a term ranks the way it does
    public void explainTerm(String rawTerm) {
        String term = PorterStemmer.stem(rawTerm.toLowerCase());
        List<Posting> postings = invertedIndex.get(term);
        double idf = idfMap.getOrDefault(term, 0.0);

        System.out.printf("%nTF-IDF explanation: \"%s\" (stemmed: \"%s\")%n", rawTerm, term);
        System.out.printf("  IDF = log(%d / (1 + %d)) = %.4f%n",
                corpus.size(), postings == null ? 0 : postings.size(), idf);

        if (postings == null) {
            System.out.println("  not in index");
            return;
        }

        postings.stream()
                .sorted((a, b) -> Double.compare(b.tf, a.tf))
                .limit(5)
                .forEach(p -> System.out.printf("  %-40s tf=%.4f  tfidf=%.4f%n",
                        p.docId, p.tf, p.tf * idf));
    }

    public int vocabularySize() {
        return invertedIndex.size();
    }

    private List<String> preprocessQuery(String query) {
        String clean = query.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ").trim();
        List<String> tokens = preprocessor.removeStopWords(preprocessor.tokenize(clean));
        return tokens.stream().map(PorterStemmer::stem).collect(Collectors.toList());
    }

    private Map<String, RedditPost> buildLookup() {
        Map<String, RedditPost> map = new HashMap<>();
        corpus.forEach(a -> map.put(a.getId(), a));
        return map;
    }

    private static class Posting {
        final String docId;
        final double tf;

        Posting(String docId, double tf) {
            this.docId = docId;
            this.tf    = tf;
        }
    }
}
