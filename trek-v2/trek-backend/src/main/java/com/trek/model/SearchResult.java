package com.trek.model;

public class SearchResult implements Comparable<SearchResult> {

    private final RedditPost post;
    private final double     score;
    private final String     algorithm;

    public SearchResult(RedditPost post, double score, String algorithm) {
        this.post      = post;
        this.score     = score;
        this.algorithm = algorithm;
    }

    public RedditPost getPost()       { return post; }
    public double     getScore()      { return score; }
    public String     getAlgorithm()  { return algorithm; }

    @Override
    public int compareTo(SearchResult other) {
        return Double.compare(other.score, this.score);
    }

    @Override
    public String toString() {
        return String.format("%.4f [%s] %s", score, algorithm, post.getTitle());
    }
}
