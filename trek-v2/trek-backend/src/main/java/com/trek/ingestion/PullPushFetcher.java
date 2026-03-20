package com.trek.ingestion;

import com.trek.model.RedditPost;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches Reddit posts from PullPush.io — a community-maintained
 * archive of Reddit data including historical posts and NSFW content.
 *
 * API docs: https://pullpush.io
 * Base URL:  https://api.pullpush.io/reddit/search/submission/
 *
 * Rate limits (as of 2024):
 *   soft limit: 15 req/min
 *   hard limit: 30 req/min
 *   long-term:  1000 req/hr
 *
 * We stay well under by sleeping 2s between requests.
 *
 * Includes NSFW posts — the over18 filter is intentionally not applied
 * so Trek can index everything and let users filter on their end.
 */
public class PullPushFetcher {

    private static final String BASE = "https://api.pullpush.io/reddit/search/submission/";
    private static final int    SLEEP_MS = 2100; // ~28 req/min, safe margin

    /**
     * Fetch posts from a specific subreddit.
     *
     * @param subreddit  subreddit name without r/ prefix
     * @param size       number of posts (max 100 per request)
     * @param sortBy     "score", "num_comments", "created_utc"
     */
    public List<RedditPost> fetchBySubreddit(String subreddit, int size, String sortBy) {
        String url = BASE + "?subreddit=" + enc(subreddit)
                + "&size=" + Math.min(size, 100)
                + "&sort=" + sortBy
                + "&sort_type=desc";
        return fetch(url, "r/" + subreddit);
    }

    /**
     * Full-text search across all of Reddit — this is Trek's main power.
     * PullPush is one of the only services that supports Reddit-wide FTS.
     *
     * @param query   search terms
     * @param size    number of results
     * @param sortBy  "score", "num_comments", "created_utc"
     */
    public List<RedditPost> searchAll(String query, int size, String sortBy) {
        String url = BASE + "?q=" + enc(query)
                + "&size=" + Math.min(size, 100)
                + "&sort=" + sortBy
                + "&sort_type=desc";
        return fetch(url, "search:" + query);
    }

    /**
     * Search within a specific subreddit.
     */
    public List<RedditPost> searchSubreddit(String subreddit, String query, int size) {
        String url = BASE + "?subreddit=" + enc(subreddit)
                + "&q=" + enc(query)
                + "&size=" + Math.min(size, 100)
                + "&sort=score&sort_type=desc";
        return fetch(url, "r/" + subreddit + ":" + query);
    }

    /**
     * Fetch posts from multiple subreddits. Used to seed the initial index.
     */
    public List<RedditPost> fetchMultipleSubreddits(String[] subreddits, int perSub) {
        List<RedditPost> all = new ArrayList<>();
        for (String sub : subreddits) {
            List<RedditPost> posts = fetchBySubreddit(sub, perSub, "score");
            all.addAll(posts);
            System.out.printf("  r/%-30s → %d posts (total: %d)%n", sub, posts.size(), all.size());
            pause(SLEEP_MS);
        }
        System.out.println("bulk fetch done: " + all.size() + " posts");
        return all;
    }

    /**
     * Fetch top posts from a time range.
     * after/before are unix timestamps.
     */
    public List<RedditPost> fetchByTimeRange(String subreddit, long after, long before, int size) {
        String url = BASE + "?subreddit=" + enc(subreddit)
                + "&after=" + after
                + "&before=" + before
                + "&size=" + Math.min(size, 100)
                + "&sort=score&sort_type=desc";
        return fetch(url, "r/" + subreddit + " (time range)");
    }

    // ── core HTTP + parse ─────────────────────────────────────────────────

    private List<RedditPost> fetch(String urlStr, String label) {
        try {
            String json = get(urlStr);
            List<RedditPost> posts = parse(json);
            System.out.printf("fetched %d posts [%s]%n", posts.size(), label);
            return posts;
        } catch (Exception e) {
            System.err.println("PullPush fetch failed [" + label + "]: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Parse PullPush submission response.
     *
     * Response shape:
     * {
     *   "data": [
     *     {
     *       "id": "abc123",
     *       "title": "...",
     *       "selftext": "...",
     *       "author": "username",
     *       "subreddit": "java",
     *       "permalink": "/r/java/comments/abc123/...",
     *       "url": "https://...",
     *       "score": 1842,
     *       "num_comments": 234,
     *       "created_utc": 1704067200,
     *       "over_18": false,
     *       "is_self": true,
     *       "link_flair_text": "Discussion"
     *     }
     *   ]
     * }
     */
    private List<RedditPost> parse(String json) {
        List<RedditPost> posts = new ArrayList<>();

        JSONObject root = new JSONObject(json);
        JSONArray  data = root.optJSONArray("data");
        if (data == null) return posts;

        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.getJSONObject(i);

            String id        = item.optString("id",             "");
            String title     = item.optString("title",          "").trim();
            String selftext  = item.optString("selftext",       "");
            String author    = item.optString("author",         "[deleted]");
            String subreddit = item.optString("subreddit",      "");
            String permalink = item.optString("permalink",      "");
            String url       = item.optString("url",            "");
            int    score     = item.optInt("score",             0);
            int    comments  = item.optInt("num_comments",      0);
            long   created   = item.optLong("created_utc",      0L);
            boolean nsfw     = item.optBoolean("over_18",       false);
            boolean isSelf   = item.optBoolean("is_self",       false);
            String  flair    = item.optString("link_flair_text","");

            // skip deleted/removed posts with no content
            if (title.isEmpty()) continue;
            if (selftext.equals("[removed]") && !isSelf) continue;

            posts.add(new RedditPost(id, title, selftext, author, subreddit,
                    permalink, url, score, comments, created, nsfw, isSelf, flair));
        }
        return posts;
    }

    private String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Trek-Search-Engine/1.0 (academic project)");

        int status = conn.getResponseCode();
        if (status == 429) throw new RuntimeException("rate limited — sleeping");
        if (status == 503) throw new RuntimeException("PullPush server unavailable");
        if (status != 200) throw new RuntimeException("HTTP " + status);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
