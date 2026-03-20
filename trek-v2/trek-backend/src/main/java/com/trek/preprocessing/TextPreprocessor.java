package com.trek.preprocessing;

import com.trek.model.RedditPost;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Cleans and tokenizes Reddit post text before indexing.
 *
 * Reddit-specific additions vs the Guardian version:
 *   - strips markdown formatting (**, *, >, #, ~~, etc.)
 *   - strips Reddit-specific syntax (u/user, r/sub, urls)
 *   - handles [deleted] and [removed] bodies
 *   - indexes title + selftext combined
 */
public class TextPreprocessor {

    private static final Pattern URL      = Pattern.compile("https?://\\S+");
    private static final Pattern MARKDOWN = Pattern.compile("[*_~`#>|\\[\\]()]+");
    private static final Pattern NON_ALPHA= Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern SPACES   = Pattern.compile("\\s+");
    private static final Pattern REDDITOR = Pattern.compile("u/\\w+|r/\\w+");

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a","about","above","after","again","against","all","am","an","and",
        "any","are","as","at","be","because","been","before","being","below",
        "between","both","but","by","can","could","did","do","does","doing",
        "down","during","each","few","for","from","further","get","got","had",
        "has","have","having","he","her","here","hers","herself","him","himself",
        "his","how","i","if","in","into","is","it","its","itself","let","me",
        "more","most","my","myself","no","nor","not","of","off","on","once",
        "only","or","other","our","ours","ourselves","out","over","own","re",
        "same","she","should","so","some","such","than","that","the","their",
        "theirs","them","themselves","then","there","these","they","this","those",
        "through","to","too","under","until","up","very","was","we","were",
        "what","when","where","which","while","who","whom","why","will","with",
        "would","you","your","yours","yourself","yourselves","s","t","ve","d",
        "m","ll","just","also","said","says","say","one","two","three","new",
        "year","years","time","times","way","make","made","use","used","using",
        "take","come","go","good","well","may","might","much","many","first",
        "last","us","mr","ms","people","now","still","even","back","since",
        "really","like","think","know","want","actually","just","gt","amp",
        "deleted","removed","edit","update","imgur","reddit","https","http",
        "www","com","org","net","nbsp","post","comment","comments","thread"
    ));

    public void process(RedditPost post) {
        String raw = post.getIndexableText();
        String clean = cleanRedditText(raw);
        post.setCleanText(clean);

        List<String> tokens = tokenize(clean);
        tokens = removeStopWords(tokens);

        List<String> stems = tokens.stream()
                .map(PorterStemmer::stem)
                .collect(Collectors.toList());

        post.setTokens(tokens);
        post.setStems(stems);
        post.setTermFrequency(computeTF(stems));
    }

    public void processAll(List<RedditPost> posts) {
        System.out.println("preprocessing " + posts.size() + " posts...");
        posts.forEach(this::process);
        System.out.println("done.");
    }

    // strips URLs, markdown, reddit-specific syntax
    private String cleanRedditText(String text) {
        if (text == null) return "";
        text = URL.matcher(text).replaceAll(" ");
        text = REDDITOR.matcher(text).replaceAll(" ");
        text = MARKDOWN.matcher(text).replaceAll(" ");
        text = text.toLowerCase(Locale.ENGLISH);
        text = NON_ALPHA.matcher(text).replaceAll(" ");
        text = SPACES.matcher(text).replaceAll(" ").trim();
        return text;
    }

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        return Arrays.stream(text.split("\\s+"))
                .filter(t -> t.length() >= 2)
                .collect(Collectors.toList());
    }

    public List<String> removeStopWords(List<String> tokens) {
        return tokens.stream()
                .filter(t -> !STOP_WORDS.contains(t))
                .collect(Collectors.toList());
    }

    public Map<String, Double> computeTF(List<String> tokens) {
        if (tokens.isEmpty()) return Collections.emptyMap();
        Map<String, Integer> counts = new HashMap<>();
        for (String t : tokens) counts.merge(t, 1, Integer::sum);
        double total = tokens.size();
        Map<String, Double> tf = new HashMap<>();
        counts.forEach((term, count) -> tf.put(term, count / total));
        return tf;
    }
}
