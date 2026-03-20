package com.trek.algorithms;

import com.trek.model.RedditPost;
import com.trek.model.SearchResult;
import com.trek.preprocessing.PorterStemmer;
import com.trek.preprocessing.TextPreprocessor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BM25 (Okapi BM25) ranking.
 *
 * This is what Elasticsearch uses under the hood, and it consistently
 * outperforms plain TF-IDF because:
 *   1. TF is saturated — adding more occurrences of a term gives
 *      diminishing returns (controlled by k1)
 *   2. Longer documents are penalized — a 10k word article mentioning
 *      "climate" 5 times isn't more relevant than a 500 word article
 *      mentioning it 3 times (controlled by b)
 *
 * Formula:
 *   score(q,d) = sum [ IDF(t) * tf*(k1+1) / (tf + k1*(1 - b + b*|d|/avgdl)) ]
 *
 * I'm using k1=1.5 and b=0.75 which are the standard defaults.
 * Tweaking these is a whole rabbit hole — left it for later.
 */
public class BM25Ranker {

    // standard BM25 hyperparameters — good enough for most corpora
    private static final double K1 = 1.5;
    private static final double B  = 0.75;

    private final Map<String, List<BM25Posting>> index = new HashMap<>();
    private final Map<String, Double> idfMap = new HashMap<>();
    private final Map<String, Integer> docLengths = new HashMap<>();
    private final List<RedditPost> corpus;
    private final TextPreprocessor preprocessor = new TextPreprocessor();

    private double avgDocLength;

    public BM25Ranker(List<RedditPost> corpus) {
        this.corpus = corpus;
        buildIndex();
    }

    private void buildIndex() {
        long start = System.currentTimeMillis();

        for (RedditPost article : corpus) {
            List<String> stems = article.getStems();
            if (stems == null || stems.isEmpty()) continue;

            docLengths.put(article.getId(), stems.size());

            // count raw occurrences per term
            Map<String, Integer> counts = new HashMap<>();
            for (String s : stems) counts.merge(s, 1, Integer::sum);

            counts.forEach((term, count) ->
                index.computeIfAbsent(term, k -> new ArrayList<>())
                     .add(new BM25Posting(article.getId(), count, stems.size()))
            );
        }

        avgDocLength = docLengths.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(1.0);

        // Robertson IDF — slightly different from the standard log formula,
        // handles edge cases better when df is close to N
        int N = corpus.size();
        index.forEach((term, postings) -> {
            int df = postings.size();
            double idf = Math.log(((double)(N - df) + 0.5) / (df + 0.5) + 1);
            idfMap.put(term, idf);
        });

        System.out.printf("BM25 index built: %d terms, avgDocLen=%.1f, %dms%n",
                index.size(), avgDocLength, System.currentTimeMillis() - start);
    }

    public List<SearchResult> search(String query, int topK) {
        List<String> terms = preprocessQuery(query);
        if (terms.isEmpty()) return Collections.emptyList();

        Map<String, Double> scores = new HashMap<>();
        Map<String, RedditPost> lookup = buildLookup();

        for (String term : terms) {
            List<BM25Posting> postings = index.get(term);
            if (postings == null) continue;

            double idf = idfMap.getOrDefault(term, 0.0);

            for (BM25Posting p : postings) {
                double tf    = p.termCount;
                double dl    = p.docLength;
                double norm  = tf + K1 * (1.0 - B + B * (dl / avgDocLength));
                double score = idf * (tf * (K1 + 1.0)) / norm;
                scores.merge(p.docId, score, Double::sum);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new SearchResult(lookup.get(e.getKey()), e.getValue(), "BM25"))
                .filter(r -> r.getPost() != null)
                .collect(Collectors.toList());
    }

    /**
     * Shows how BM25 score grows slower than TF-IDF as term frequency increases.
     * Good for demos — makes the saturation effect obvious.
     */
    public void demonstrateSaturation(String term) {
        double idf = idfMap.getOrDefault(term, 1.0);
        System.out.printf("%nBM25 saturation demo for \"%s\" (idf=%.3f)%n", term, idf);
        System.out.printf("%-8s %-12s %-12s%n", "rawTF", "TF-IDF", "BM25");
        for (int tf = 1; tf <= 20; tf++) {
            double tfidfScore = (tf / 100.0) * idf;
            double bm25Score  = idf * (tf * (K1 + 1)) / (tf + K1 * (1 - B + B));
            System.out.printf("%-8d %-12.4f %-12.4f%n", tf, tfidfScore, bm25Score);
        }
    }

    public int vocabularySize() { return index.size(); }

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

    private static class BM25Posting {
        final String docId;
        final int termCount;
        final int docLength;

        BM25Posting(String docId, int termCount, int docLength) {
            this.docId     = docId;
            this.termCount = termCount;
            this.docLength = docLength;
        }
    }
}
