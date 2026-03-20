package com.trek.api;

import com.trek.algorithms.BM25Ranker;
import com.trek.algorithms.TFIDFRanker;
import com.trek.algorithms.VectorRanker;
import com.trek.ingestion.PullPushFetcher;
import com.trek.model.RedditPost;
import com.trek.model.SearchResult;
import com.trek.preprocessing.TextPreprocessor;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Real-time search service.
 *
 * Every query hits PullPush live — no local index, no pre-crawling.
 * Fetches fresh Reddit posts, processes them on the fly, ranks them,
 * and returns results. Index is built per-query and discarded after.
 */
@Service
public class SearchEngineService {

    private final PullPushFetcher fetcher      = new PullPushFetcher();
    private final TextPreprocessor preprocessor = new TextPreprocessor();

    private boolean ready = false;

    @PostConstruct
    public void init() {
        System.out.println("Trek v1 — Real-Time Reddit Search Engine");
        System.out.println("Mode: LIVE — every query hits PullPush directly");
        System.out.println("No pre-crawling. No static index. Pure real-time.");
        ready = true;
        System.out.println("API ready: http://localhost:8080/api/search?q=java");
    }

    /**
     * Main real-time search.
     *
     * Flow:
     *   1. Hit PullPush with the query → fresh Reddit posts
     *   2. Preprocess them on the fly
     *   3. Build a temporary BM25/TF-IDF/Vector index
     *   4. Score and rank
     *   5. Return results (temp index is discarded)
     *
     * @param query   raw search query
     * @param algo    bm25 | tfidf | vector | all
     * @param topK    number of results
     * @param fetchN  how many posts to fetch from PullPush (default 100)
     */
    public SearchResponse liveSearch(String query, String algo, int topK, int fetchN) {
        long start = System.currentTimeMillis();

        // Step 1 — fetch live from PullPush
        System.out.printf("[Live] searching PullPush: \"%s\" (fetching %d posts)%n", query, fetchN);
        List<RedditPost> posts = fetcher.searchAll(query, fetchN, "score");

        if (posts.isEmpty()) {
            return new SearchResponse(query, algo.toUpperCase(), 0,
                    System.currentTimeMillis() - start, Collections.emptyList(), 0);
        }

        // Step 2 — preprocess on the fly
        preprocessor.processAll(posts);

        // Step 3 — build temp index + rank
        List<SearchResult> results = rankPosts(posts, query, algo, topK);

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("[Live] done: %d results in %dms%n", results.size(), elapsed);

        return new SearchResponse(query, algo.toUpperCase(), posts.size(), elapsed, results, fetchN);
    }

    /**
     * Real-time subreddit search — search within a specific subreddit.
     */
    public SearchResponse liveSubredditSearch(String subreddit, String query,
                                               String algo, int topK) {
        long start = System.currentTimeMillis();

        System.out.printf("[Live] r/%s search: \"%s\"%n", subreddit, query);
        List<RedditPost> posts = fetcher.searchSubreddit(subreddit, query, 100);

        if (posts.isEmpty()) {
            return new SearchResponse(query, algo.toUpperCase(), 0,
                    System.currentTimeMillis() - start, Collections.emptyList(), 0);
        }

        preprocessor.processAll(posts);
        List<SearchResult> results = rankPosts(posts, query, algo, topK);
        long elapsed = System.currentTimeMillis() - start;

        return new SearchResponse(query, algo.toUpperCase(), posts.size(), elapsed, results, 100);
    }

    /**
     * Compare all 3 algorithms on the same live query.
     */
    public Map<String, Object> liveCompare(String query, int topK) {
        long start = System.currentTimeMillis();

        List<RedditPost> posts = fetcher.searchAll(query, 100, "score");
        if (!posts.isEmpty()) preprocessor.processAll(posts);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("fetchedPosts", posts.size());

        if (posts.isEmpty()) {
            result.put("tfidf",  Collections.emptyList());
            result.put("bm25",   Collections.emptyList());
            result.put("vector", Collections.emptyList());
            result.put("timeTakenMs", System.currentTimeMillis() - start);
            return result;
        }

        result.put("tfidf",  toItems(rankPosts(posts, query, "tfidf",  topK)));
        result.put("bm25",   toItems(rankPosts(posts, query, "bm25",   topK)));
        result.put("vector", toItems(rankPosts(posts, query, "vector", topK)));
        result.put("timeTakenMs", System.currentTimeMillis() - start);
        return result;
    }

    // ── Ranking ───────────────────────────────────────────────────────────

    private List<SearchResult> rankPosts(List<RedditPost> posts, String query,
                                          String algo, int topK) {
        return switch (algo.toLowerCase()) {
            case "tfidf", "tf-idf" -> new TFIDFRanker(posts).search(query, topK);
            case "vector", "cosine"-> new VectorRanker(posts).search(query, topK);
            case "all", "rrf"      -> mergeRRF(posts, query, topK);
            default                -> new BM25Ranker(posts).search(query, topK);
        };
    }

    private List<SearchResult> mergeRRF(List<RedditPost> posts, String query, int topK) {
        List<SearchResult> tfidf  = new TFIDFRanker(posts).search(query, topK * 2);
        List<SearchResult> bm25   = new BM25Ranker(posts).search(query, topK * 2);
        List<SearchResult> vector = new VectorRanker(posts).search(query, topK * 2);

        Map<String, Double>    scores = new HashMap<>();
        Map<String, RedditPost> lookup = new HashMap<>();

        for (List<SearchResult> ranked : List.of(tfidf, bm25, vector)) {
            for (int i = 0; i < ranked.size(); i++) {
                String id = ranked.get(i).getPost().getId();
                scores.merge(id, 1.0 / (i + 60), Double::sum);
                lookup.put(id, ranked.get(i).getPost());
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new SearchResult(lookup.get(e.getKey()), e.getValue(), "RRF"))
                .collect(Collectors.toList());
    }

    // ── Response helpers ──────────────────────────────────────────────────

    public List<Map<String, Object>> toItems(List<SearchResult> results) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            SearchResult sr   = results.get(i);
            RedditPost   post = sr.getPost();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank",        i + 1);
            item.put("score",       Math.round(sr.getScore() * 10000.0) / 10000.0);
            item.put("id",          post.getId());
            item.put("title",       post.getTitle());
            item.put("author",      "u/" + post.getAuthor());
            item.put("subreddit",   "r/" + post.getSubreddit());
            item.put("upvotes",     post.getScore());
            item.put("comments",    post.getNumComments());
            item.put("isNsfw",      post.isNsfw());
            item.put("flair",       post.getFlair());
            item.put("createdUtc",  post.getCreatedUtc());
            item.put("url",         post.getRedditUrl());
            item.put("algorithm",   sr.getAlgorithm());
            items.add(item);
        }
        return items;
    }

    public boolean isReady() { return ready; }

    // ── SearchResponse DTO ────────────────────────────────────────────────

    public record SearchResponse(
        String query,
        String algorithm,
        int    fetchedPosts,
        long   timeTakenMs,
        List<SearchResult> rawResults,
        int    fetchSize
    ) {}
}
