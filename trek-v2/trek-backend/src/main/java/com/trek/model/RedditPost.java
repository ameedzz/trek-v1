package com.trek.model;

import java.util.List;
import java.util.Map;

/**
 * Represents one Reddit submission (post).
 *
 * Fields map directly to what PullPush and Arctic Shift return.
 * Both NSFW and SFW posts use the same model — isNsfw flag lets
 * the frontend decide whether to show a warning label.
 */
public class RedditPost {

    private final String  id;
    private final String  title;
    private final String  selftext;
    private final String  author;
    private final String  subreddit;
    private final String  permalink;
    private final String  url;
    private final int     score;
    private final int     numComments;
    private final long    createdUtc;
    private final boolean isNsfw;
    private final boolean isSelf;
    private final String  flair;

    // filled by TextPreprocessor
    private String              cleanText;
    private List<String>        tokens;
    private List<String>        stems;
    private Map<String, Double> termFrequency;

    public RedditPost(String id, String title, String selftext, String author,
                      String subreddit, String permalink, String url,
                      int score, int numComments, long createdUtc,
                      boolean isNsfw, boolean isSelf, String flair) {
        this.id          = id;
        this.title       = title;
        this.selftext    = selftext == null ? "" : selftext;
        this.author      = author;
        this.subreddit   = subreddit;
        this.permalink   = permalink;
        this.url         = url;
        this.score       = score;
        this.numComments = numComments;
        this.createdUtc  = createdUtc;
        this.isNsfw      = isNsfw;
        this.isSelf      = isSelf;
        this.flair       = flair == null ? "" : flair;
    }

    // title + body text combined — this is what gets indexed
    public String getIndexableText() {
        if (selftext.isBlank() || selftext.equals("[deleted]") || selftext.equals("[removed]"))
            return title;
        return title + " " + selftext;
    }

    public String getRedditUrl() {
        return "https://www.reddit.com" + permalink;
    }

    public String  getId()           { return id; }
    public String  getTitle()        { return title; }
    public String  getSelftext()     { return selftext; }
    public String  getAuthor()       { return author; }
    public String  getSubreddit()    { return subreddit; }
    public String  getPermalink()    { return permalink; }
    public String  getUrl()          { return url; }
    public int     getScore()        { return score; }
    public int     getNumComments()  { return numComments; }
    public long    getCreatedUtc()   { return createdUtc; }
    public boolean isNsfw()          { return isNsfw; }
    public boolean isSelf()          { return isSelf; }
    public String  getFlair()        { return flair; }

    public String              getCleanText()     { return cleanText; }
    public void                setCleanText(String t) { this.cleanText = t; }
    public List<String>        getTokens()        { return tokens; }
    public void                setTokens(List<String> t) { this.tokens = t; }
    public List<String>        getStems()         { return stems; }
    public void                setStems(List<String> s) { this.stems = s; }
    public Map<String, Double> getTermFrequency() { return termFrequency; }
    public void                setTermFrequency(Map<String, Double> tf) { this.termFrequency = tf; }

    @Override
    public String toString() {
        return String.format("[r/%s] %s (u/%s ▲%d%s)",
                subreddit, title, author, score, isNsfw ? " [NSFW]" : "");
    }
}
