package com.trek.ingestion;

import com.trek.model.RedditPost;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Loads Reddit posts from NDJSON files written by the Trek crawler.
 *
 * The crawler writes to trek-data/posts/*.ndjson — one post per line.
 * This loader reads all those files and converts them into RedditPost
 * objects that the search engine can index.
 *
 * Usage:
 *   NdjsonLoader loader = new NdjsonLoader("../trek-crawler/trek-data");
 *   List<RedditPost> posts = loader.loadAll();
 */
public class NdjsonLoader {

    private final Path dataDir;

    public NdjsonLoader(String dataDirPath) {
        this.dataDir = Paths.get(dataDirPath).resolve("posts");
    }

    /**
     * Load every post from every .ndjson file in the data directory.
     * Deduplicates by post ID automatically.
     */
    public List<RedditPost> loadAll() throws IOException {
        if (!Files.exists(dataDir)) {
            System.out.println("[NdjsonLoader] data dir not found: " + dataDir);
            System.out.println("[NdjsonLoader] run the crawler first: mvn exec:java -Dexec.args=\"quick\"");
            return Collections.emptyList();
        }

        Map<String, RedditPost> deduped = new LinkedHashMap<>();

        List<Path> files = new ArrayList<>();
        try (var stream = Files.walk(dataDir)) {
            stream.filter(p -> p.toString().endsWith(".ndjson"))
                  .sorted()
                  .forEach(files::add);
        }

        if (files.isEmpty()) {
            System.out.println("[NdjsonLoader] no .ndjson files found in " + dataDir);
            return Collections.emptyList();
        }

        System.out.println("[NdjsonLoader] loading from " + files.size() + " files...");

        for (Path file : files) {
            int before = deduped.size();
            try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    RedditPost post = parse(line);
                    if (post != null) deduped.putIfAbsent(post.getId(), post);
                }
            } catch (Exception e) {
                System.err.println("[NdjsonLoader] error reading " + file.getFileName() + ": " + e.getMessage());
            }
            int added = deduped.size() - before;
            System.out.printf("  %s → %d posts%n", file.getFileName(), added);
        }

        List<RedditPost> posts = new ArrayList<>(deduped.values());
        System.out.printf("[NdjsonLoader] loaded %d unique posts total%n", posts.size());
        return posts;
    }

    /**
     * Parse a single NDJSON line into a RedditPost.
     * Field names match what the crawler's PostStore writes.
     */
    private RedditPost parse(String json) {
        try {
            JSONObject obj = new JSONObject(json);

            String id = obj.optString("id", "");
            if (id.isEmpty()) return null;

            String title = obj.optString("title", "").trim();
            if (title.isEmpty()) return null;

            return new RedditPost(
                id,
                title,
                obj.optString("selftext",      ""),
                obj.optString("author",        "[deleted]"),
                obj.optString("subreddit",     ""),
                obj.optString("permalink",     ""),
                obj.optString("url",           ""),
                obj.optInt   ("score",         0),
                obj.optInt   ("num_comments",  0),
                obj.optLong  ("created_utc",   0L),
                obj.optBoolean("over_18",      false),
                obj.optBoolean("is_self",      false),
                obj.optString("flair",         "")
            );
        } catch (Exception e) {
            return null; // skip malformed lines
        }
    }
}
