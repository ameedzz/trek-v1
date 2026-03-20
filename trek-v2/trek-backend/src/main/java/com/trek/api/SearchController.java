package com.trek.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SearchController {

    private final SearchEngineService service;

    private static final List<String> SUGGESTION_POOL = List.of(
        "artificial intelligence", "machine learning", "deep learning",
        "climate change", "renewable energy", "electric vehicle",
        "quantum computing", "space exploration", "gene editing",
        "neural networks", "cryptocurrency", "blockchain",
        "react javascript", "java programming", "python tutorial",
        "data structures", "algorithms", "system design",
        "spring boot", "docker kubernetes", "microservices",
        "reddit api", "search engine", "nlp text processing",
        "natural language processing", "computer vision",
        "reinforcement learning", "open source", "web scraping",
        "software engineering", "database design"
    );

    public SearchController(SearchEngineService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status",  "UP");
        r.put("engine",  service.isReady() ? "ready" : "starting");
        r.put("mode",    "real-time");
        r.put("source",  "PullPush live API");
        r.put("project", "Trek v2");
        return ResponseEntity.ok(r);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("project",    "Trek v2 - Reddit Search Engine");
        r.put("mode",       "real-time - no static index");
        r.put("source",     "PullPush.io live API");
        r.put("algorithms", List.of("TF-IDF", "BM25", "VECTOR", "RRF"));
        r.put("endpoints",  List.of(
            "GET /api/search?q=...&algo=bm25&top=10&fetch=100",
            "GET /api/search?q=...&subreddit=java",
            "GET /api/compare?q=...&top=5",
            "GET /api/suggest?q=...",
            "GET /api/health"
        ));
        return ResponseEntity.ok(r);
    }

    @GetMapping("/suggest")
    public ResponseEntity<?> suggest(@RequestParam String q) {
        if (q == null || q.isBlank()) return ResponseEntity.ok(List.of());
        String lower = q.toLowerCase().trim();
        List<String> matches = SUGGESTION_POOL.stream()
                .filter(s -> s.contains(lower))
                .sorted(Comparator.comparingInt(s -> s.indexOf(lower)))
                .limit(6)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("suggestions", matches));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam                String  q,
            @RequestParam(defaultValue = "bm25") String  algo,
            @RequestParam(defaultValue = "10")   int     top,
            @RequestParam(defaultValue = "100")  int     fetch,
            @RequestParam(required = false)      String  subreddit) {

        if (!service.isReady()) return busy();
        if (q.isBlank()) return bad("q param required");

        SearchEngineService.SearchResponse resp = subreddit != null && !subreddit.isBlank()
                ? service.liveSubredditSearch(subreddit, q.trim(), algo, top)
                : service.liveSearch(q.trim(), algo, top, fetch);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query",        resp.query());
        body.put("algorithm",    resp.algorithm());
        body.put("fetchedPosts", resp.fetchedPosts());
        body.put("totalHits",    resp.rawResults().size());
        body.put("timeTakenMs",  resp.timeTakenMs());
        body.put("mode",         "real-time");
        body.put("results",      service.toItems(resp.rawResults()));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/compare")
    public ResponseEntity<?> compare(
            @RequestParam               String q,
            @RequestParam(defaultValue = "5") int top) {
        if (!service.isReady()) return busy();
        if (q.isBlank()) return bad("q param required");
        return ResponseEntity.ok(service.liveCompare(q.trim(), top));
    }

    private ResponseEntity<Map<String, Object>> busy() {
        return ResponseEntity.accepted().body(Map.of("status", "starting up, try again shortly"));
    }
    private ResponseEntity<Map<String, Object>> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }
}
