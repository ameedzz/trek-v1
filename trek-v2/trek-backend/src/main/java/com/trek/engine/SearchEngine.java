package com.trek.engine;

import com.trek.algorithms.BM25Ranker;
import com.trek.algorithms.TFIDFRanker;
import com.trek.algorithms.VectorRanker;
import com.trek.ingestion.MockRedditData;
import com.trek.ingestion.PullPushFetcher;
import com.trek.model.RedditPost;
import com.trek.model.SearchResult;
import com.trek.preprocessing.TextPreprocessor;

import java.util.*;

/**
 * Core engine — owns the corpus, indexes, and routes queries.
 * Same RRF merge logic as before, just adapted for RedditPost.
 */
public class SearchEngine {

    public enum Algorithm { TFIDF, BM25, VECTOR, ALL }

    private final PullPushFetcher  fetcher     = new PullPushFetcher();
    private final TextPreprocessor preprocessor = new TextPreprocessor();

    private List<RedditPost> corpus  = new ArrayList<>();
    private TFIDFRanker      tfidf;
    private BM25Ranker       bm25;
    private VectorRanker     vector;
    private boolean          indexed = false;

    // ── loading ───────────────────────────────────────────────────────────

    public void loadMockData() {
        List<RedditPost> posts = MockRedditData.load();
        preprocessor.processAll(posts);
        corpus.addAll(posts);
        indexed = false;
    }

    public void loadPosts(List<RedditPost> posts) {
        preprocessor.processAll(posts);
        corpus.addAll(posts);
        indexed = false;
        System.out.println("corpus: " + corpus.size() + " posts");
    }

    public void fetchSubreddits(String[] subreddits, int perSub) {
        List<RedditPost> posts = fetcher.fetchMultipleSubreddits(subreddits, perSub);
        preprocessor.processAll(posts);
        corpus.addAll(posts);
        indexed = false;
        System.out.println("corpus: " + corpus.size() + " posts");
    }

    public void fetchQuery(String query, int size) {
        List<RedditPost> posts = fetcher.searchAll(query, size, "score");
        preprocessor.processAll(posts);
        corpus.addAll(posts);
        indexed = false;
    }

    // ── indexing ──────────────────────────────────────────────────────────

    public void buildIndex() {
        if (corpus.isEmpty()) { System.out.println("corpus is empty"); return; }
        System.out.println("building indexes over " + corpus.size() + " posts...");
        tfidf   = new TFIDFRanker(corpus);
        bm25    = new BM25Ranker(corpus);
        vector  = new VectorRanker(corpus);
        indexed = true;
        System.out.println("indexes ready");
    }

    // ── search ────────────────────────────────────────────────────────────

    public List<SearchResult> search(String query, int topK, Algorithm algorithm) {
        if (!indexed) buildIndex();
        return switch (algorithm) {
            case TFIDF  -> tfidf.search(query, topK);
            case BM25   -> bm25.search(query, topK);
            case VECTOR -> vector.search(query, topK);
            case ALL    -> mergeRRF(query, topK);
        };
    }

    // RRF — combines all three rankings
    private List<SearchResult> mergeRRF(String query, int topK) {
        Map<String, Double>     rrfScores = new HashMap<>();
        Map<String, RedditPost> lookup    = new HashMap<>();

        for (Algorithm alg : new Algorithm[]{Algorithm.TFIDF, Algorithm.BM25, Algorithm.VECTOR}) {
            List<SearchResult> ranked = switch (alg) {
                case TFIDF  -> tfidf.search(query, topK * 2);
                case BM25   -> bm25.search(query, topK * 2);
                case VECTOR -> vector.search(query, topK * 2);
                default     -> Collections.emptyList();
            };
            for (int i = 0; i < ranked.size(); i++) {
                String id = ranked.get(i).getPost().getId();
                rrfScores.merge(id, 1.0 / (i + 60), Double::sum);
                lookup.put(id, ranked.get(i).getPost());
            }
        }

        List<SearchResult> merged = new ArrayList<>();
        rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .forEach(e -> merged.add(
                    new SearchResult(lookup.get(e.getKey()), e.getValue(), "RRF")));
        return merged;
    }

    // ── utils ─────────────────────────────────────────────────────────────

    public void printStats() {
        System.out.println("corpus : " + corpus.size() + " posts");
        if (indexed) System.out.println("vocab  : " + tfidf.vocabularySize() + " terms");
    }

    public List<RedditPost> getCorpus()  { return corpus; }
    public TFIDFRanker      getTfidf()   { return tfidf; }
    public BM25Ranker       getBm25()    { return bm25; }
    public VectorRanker     getVector()  { return vector; }
    public boolean          isIndexed()  { return indexed; }
    public PullPushFetcher  getFetcher() { return fetcher; }
}
