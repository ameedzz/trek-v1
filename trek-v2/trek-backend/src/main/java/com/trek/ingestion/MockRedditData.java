package com.trek.ingestion;

import com.trek.model.RedditPost;

import java.util.ArrayList;
import java.util.List;

/**
 * 30 realistic mock Reddit posts for offline dev/testing.
 * Mix of SFW and NSFW, various subreddits, real-looking data.
 * Used when PullPush is unreachable or for fast local testing.
 */
public class MockRedditData {

    public static List<RedditPost> load() {
        List<RedditPost> posts = new ArrayList<>();

        Object[][] data = {
            // {id, title, selftext, author, subreddit, permalink, url, score, comments, created, nsfw, isSelf, flair}
            {"t3_abc001","What's the best way to learn DSA in Java?",
             "I've been coding Java for 6 months and want to get serious about data structures. Should I start with arrays or linked lists? Currently doing Leetcode but struggling with trees.",
             "coder_noob99","learnprogramming","/r/learnprogramming/comments/abc001/","https://reddit.com/r/learnprogramming/comments/abc001/",
             4821,234,1704067200L,false,true,"Help"},

            {"t3_abc002","BM25 vs TF-IDF — which one should I implement first?",
             "Building a search engine from scratch. I understand TF-IDF conceptually but BM25 seems more complex. Is the performance difference worth the extra implementation effort for a portfolio project?",
             "search_dev_42","MachineLearning","/r/MachineLearning/comments/abc002/","https://reddit.com/r/MachineLearning/comments/abc002/",
             2341,89,1704153600L,false,true,"Question"},

            {"t3_abc003","Java Spring Boot REST API tutorial — complete guide 2024",
             "I wrote a comprehensive tutorial on building production-ready REST APIs with Spring Boot 3. Covers dependency injection, JPA, exception handling, and deployment to Railway.",
             "spring_sensei","java","/r/java/comments/abc003/","https://reddit.com/r/java/comments/abc003/",
             8932,445,1704240000L,false,true,"Tutorial"},

            {"t3_abc004","Reddit is making it impossible to scrape data and I'm frustrated",
             "After the 2023 API changes, getting bulk Reddit data is a nightmare. PullPush works but the rate limits are brutal. Anyone found better alternatives for historical data?",
             "data_eng_frustrated","datasets","/r/datasets/comments/abc004/","https://reddit.com/r/datasets/comments/abc004/",
             1567,312,1704326400L,false,true,"Discussion"},

            {"t3_abc005","I built a full-text search engine in Java from scratch — here's what I learned",
             "After 3 months of work, I finally got TF-IDF, BM25, and cosine similarity working on real data. The hardest part wasn't the algorithms — it was the text preprocessing pipeline. AMA.",
             "devjourney2024","programming","/r/programming/comments/abc005/","https://reddit.com/r/programming/comments/abc005/",
             15234,892,1704412800L,false,true,"Project"},

            {"t3_abc006","Climate change is accelerating faster than models predicted",
             "New study in Nature shows Arctic ice loss is 40% faster than 2020 IPCC projections. The feedback loops are kicking in earlier than expected.",
             "climate_scientist_ama","science","/r/science/comments/abc006/","https://reddit.com/r/science/comments/abc006/",
             42891,2341,1704499200L,false,true,"Environment"},

            {"t3_abc007","EV battery technology breakthrough — 500 mile range at half the cost",
             "Solid state batteries from QuantumScape finally hit production scale. Tesla, Ford, and BMW have all signed supply agreements. This changes everything for EVs.",
             "tech_watcher","technology","/r/technology/comments/abc007/","https://reddit.com/r/technology/comments/abc007/",
             31456,1234,1704585600L,false,true,""},

            {"t3_abc008","Hot takes on AI that will age badly",
             "My prediction: LLMs will plateau by 2026, we'll realize we built autocomplete on steroids, and everyone who said AGI was 2 years away will quietly delete their tweets.",
             "contrarian_techie","artificial","/r/artificial/comments/abc008/","https://reddit.com/r/artificial/comments/abc008/",
             7823,934,1704672000L,false,true,"Discussion"},

            {"t3_abc009","Graph algorithms explained with real examples",
             "Dijkstra's, A*, BFS, DFS — I always confuse when to use which. This post breaks them down with actual use cases: GPS routing, game pathfinding, social network analysis.",
             "algo_explainer","compsci","/r/compsci/comments/abc009/","https://reddit.com/r/compsci/comments/abc009/",
             5621,178,1704758400L,false,true,"Education"},

            {"t3_abc010","Stop using REST APIs for everything — gRPC is better for most internal services",
             "Unpopular opinion but after migrating 12 microservices to gRPC I'll never go back. Type safety, streaming, 10x performance. REST is for public APIs only.",
             "backend_takes","ExperiencedDevs","/r/ExperiencedDevs/comments/abc010/","https://reddit.com/r/ExperiencedDevs/comments/abc010/",
             3421,567,1704844800L,false,true,""},

            {"t3_abc011","[NSFW] The algorithm that powers recommendation systems — explained simply",
             "Most recommendation engines use collaborative filtering or matrix factorization. Here's how Netflix, Spotify, and Reddit's own feed algorithm work under the hood.",
             "ml_explainer","MachineLearning","/r/MachineLearning/comments/abc011/","https://reddit.com/r/MachineLearning/comments/abc011/",
             9234,423,1704931200L,true,true,"Educational"},

            {"t3_abc012","Git commands I wish I knew earlier",
             "git bisect, git reflog, git stash pop, git cherry-pick — these 4 commands alone would have saved me hundreds of hours. Post your most underrated git commands.",
             "gitpro_tips","git","/r/git/comments/abc012/","https://reddit.com/r/git/comments/abc012/",
             28341,1892,1705017600L,false,true,"Tips"},

            {"t3_abc013","Quantum computing is closer than you think",
             "IBM's 1000 qubit processor is now commercially available. We're 3-5 years from quantum advantage for drug discovery and optimization problems. Cryptography needs to prepare now.",
             "quantum_researcher","QuantumComputing","/r/QuantumComputing/comments/abc013/","https://reddit.com/r/QuantumComputing/comments/abc013/",
             6782,289,1705104000L,false,true,"News"},

            {"t3_abc014","Docker vs Kubernetes — when does k8s actually make sense?",
             "I keep seeing people use Kubernetes for apps that get 100 requests/day. Here's my framework: use Docker for anything under 10 services. k8s only when you need auto-scaling or >20 microservices.",
             "devops_realist","devops","/r/devops/comments/abc014/","https://reddit.com/r/devops/comments/abc014/",
             11234,678,1705190400L,false,true,"Discussion"},

            {"t3_abc015","Porter Stemming algorithm — implementing it from scratch was harder than I expected",
             "The paper makes it sound simple but implementing all 5 steps correctly took me a week. Edge cases in step 1b with the double consonant rule almost broke me.",
             "nlp_student","LanguageTechnology","/r/LanguageTechnology/comments/abc015/","https://reddit.com/r/LanguageTechnology/comments/abc015/",
             892,67,1705276800L,false,true,""},

            {"t3_abc016","CRISPR gene editing just cured sickle cell disease in a clinical trial",
             "100% of patients in the Phase 3 trial showed complete remission. FDA approval expected Q2 2024. This is what 30 years of molecular biology research was building toward.",
             "biotech_news","science","/r/science/comments/abc016/","https://reddit.com/r/science/comments/abc016/",
             67234,3421,1705363200L,false,true,"Biology"},

            {"t3_abc017","Why I switched from Python to Java for backend development",
             "Python was fine but type safety and performance at scale made me switch. Java 17+ with records, sealed classes, and virtual threads feels modern now. The ecosystem is unbeatable.",
             "java_convert","java","/r/java/comments/abc017/","https://reddit.com/r/java/comments/abc017/",
             4231,892,1705449600L,false,true,""},

            {"t3_abc018","The real reason Reddit killed third-party apps",
             "It wasn't about API costs. It was about training data. Every Reddit post, comment, and upvote is a goldmine for LLM training. They're monetizing your content without asking.",
             "tech_conspiracy","conspiracy","/r/conspiracy/comments/abc018/","https://reddit.com/r/conspiracy/comments/abc018/",
             34521,4231,1705536000L,false,true,""},

            {"t3_abc019","Inverted index from scratch — my implementation and benchmarks",
             "Built a HashMap<String, List<Posting>> inverted index for 1M documents. Query time is 3ms average. Here are my benchmarks vs Lucene and Elasticsearch on the same dataset.",
             "search_builder","programming","/r/programming/comments/abc019/","https://reddit.com/r/programming/comments/abc019/",
             5623,234,1705622400L,false,true,"Project"},

            {"t3_abc020","Neural networks are just curve fitting — change my mind",
             "Seriously. Backprop is gradient descent. Attention is a weighted sum. Transformers are matrix multiplications with clever masking. We've dressed up statistics in a Halloween costume.",
             "ml_takes","MachineLearning","/r/MachineLearning/comments/abc020/","https://reddit.com/r/MachineLearning/comments/abc020/",
             23456,2134,1705708800L,false,true,"Discussion"},

            {"t3_abc021","[NSFW] Data privacy is dead and nobody cares",
             "Your phone knows when you're sleeping, who you're sleeping with, how many calories you ate, and your political views. And you agreed to all of it for free apps.",
             "privacy_rant","privacy","/r/privacy/comments/abc021/","https://reddit.com/r/privacy/comments/abc021/",
             8934,1234,1705795200L,true,true,"Rant"},

            {"t3_abc022","How Reddit's ranking algorithm works — reverse engineered",
             "Reddit uses Wilson score confidence interval for comment ranking, and a custom hot algorithm for posts: log(score) + age_decay. Here's the full formula and why it works.",
             "algo_detective","redditdev","/r/redditdev/comments/abc022/","https://reddit.com/r/redditdev/comments/abc022/",
             7821,345,1705881600L,false,true,""},

            {"t3_abc023","Cosine similarity vs dot product — when to use which for search",
             "Short answer: dot product if all vectors are normalized (same length). Cosine if documents vary in length. Most information retrieval papers default to cosine but it's not always better.",
             "ir_prof","LanguageTechnology","/r/LanguageTechnology/comments/abc023/","https://reddit.com/r/LanguageTechnology/comments/abc023/",
             1234,89,1705968000L,false,true,""},

            {"t3_abc024","I scraped 10 million Reddit posts and here's what I found",
             "Top 3 topics across all subreddits in 2023: AI/ChatGPT (14%), Ukraine/politics (11%), personal finance/FIRE (8%). NSFW subreddits account for 23% of total comment volume.",
             "data_scientist_reddit","datasets","/r/datasets/comments/abc024/","https://reddit.com/r/datasets/comments/abc024/",
             18923,1456,1706054400L,false,true,"Analysis"},

            {"t3_abc025","Stop building microservices for your startup",
             "Monolith first. Always. Shopify, Stack Overflow, and GitHub ran on monoliths way past $1B valuation. Microservices are a solution to organizational problems, not technical ones.",
             "startup_eng","ExperiencedDevs","/r/ExperiencedDevs/comments/abc025/","https://reddit.com/r/ExperiencedDevs/comments/abc025/",
             45231,3421,1706140800L,false,true,"Opinion"},

            {"t3_abc026","Full text search in PostgreSQL is underrated",
             "Before reaching for Elasticsearch, try pg_trgm and tsvector. For most apps under 10M rows, Postgres FTS is fast enough and you don't need another infrastructure dependency.",
             "postgres_enjoyer","PostgreSQL","/r/PostgreSQL/comments/abc026/","https://reddit.com/r/PostgreSQL/comments/abc026/",
             12341,567,1706227200L,false,true,""},

            {"t3_abc027","The Trie data structure is criminally underused",
             "Autocomplete, spell checking, IP routing, longest prefix matching — Tries solve all of these elegantly. Yet I've never seen one in a production codebase. Why?",
             "ds_enthusiast","algorithms","/r/algorithms/comments/abc027/","https://reddit.com/r/algorithms/comments/abc027/",
             3421,234,1706313600L,false,true,"Discussion"},

            {"t3_abc028","Web scraping in 2024 — what actually works",
             "Playwright with stealth mode handles most JS-rendered sites. For Reddit specifically, PullPush is your friend. For Twitter/X... pray. Here's my complete toolkit.",
             "scraping_guide","webscraping","/r/webscraping/comments/abc028/","https://reddit.com/r/webscraping/comments/abc028/",
             8923,678,1706400000L,false,true,"Guide"},

            {"t3_abc029","[NSFW] The attention mechanism in transformers — visual explanation",
             "Attention is just asking: how relevant is each word to every other word? Mathematically: softmax(QK^T / sqrt(d_k)) * V. This post has the best visual explanation I've found.",
             "deep_learning_viz","MachineLearning","/r/MachineLearning/comments/abc029/","https://reddit.com/r/MachineLearning/comments/abc029/",
             21345,934,1706486400L,true,true,"Educational"},

            {"t3_abc030","Reddit search has always been terrible — here's why",
             "Reddit uses Lucene under the hood but their indexing pipeline is 6 hours behind. That's why you can't find recent posts. They deliberately deprioritize search to push you toward browsing.",
             "reddit_insider","reddit","/r/reddit/comments/abc030/","https://reddit.com/r/reddit/comments/abc030/",
             24561,1892,1706572800L,false,true,"Meta"},
        };

        for (Object[] row : data) {
            posts.add(new RedditPost(
                (String)  row[0],  // id
                (String)  row[1],  // title
                (String)  row[2],  // selftext
                (String)  row[3],  // author
                (String)  row[4],  // subreddit
                (String)  row[5],  // permalink
                (String)  row[6],  // url
                (int)     row[7],  // score
                (int)     row[8],  // numComments
                (long)    row[9],  // createdUtc
                (boolean) row[10], // isNsfw
                (boolean) row[11], // isSelf
                (String)  row[12]  // flair
            ));
        }

        System.out.println("loaded " + posts.size() + " mock Reddit posts (offline mode)");
        return posts;
    }
}
