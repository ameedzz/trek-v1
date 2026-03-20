package com.trek.preprocessing;

/**
 * PorterStemmer — Classic Porter Stemming Algorithm (1980)
 *
 * Reduces English words to their base/root form.
 * Examples:
 *   "running"   → "run"
 *   "articles"  → "articl"
 *   "happiness" → "happi"
 *   "computing" → "comput"
 *
 * Reference: M.F. Porter, "An algorithm for suffix stripping", 1980
 */
public class PorterStemmer {

    private PorterStemmer() {}

    /** Stem a single lowercase word. Input must already be lowercase. */
    public static String stem(String word) {
        if (word == null || word.length() <= 2) return word;
        char[] w = word.toCharArray();
        w = step1a(w);
        w = step1b(w);
        w = step1c(w);
        w = step2(w);
        w = step3(w);
        w = step4(w);
        w = step5a(w);
        w = step5b(w);
        return new String(w);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private static boolean isVowelAt(char[] w, int i) {
        char c = w[i];
        if (isVowel(c)) return true;
        return c == 'y' && i > 0 && !isVowel(w[i - 1]);
    }

    private static int measure(char[] w, int end) {
        if (end < 0) return 0;
        int n = 0, i = 0;
        while (i <= end && !isVowelAt(w, i)) i++;
        while (i <= end) {
            while (i <= end && isVowelAt(w, i)) i++;
            if (i > end) break;
            n++;
            while (i <= end && !isVowelAt(w, i)) i++;
        }
        return n;
    }

    private static boolean containsVowel(char[] w, int end) {
        if (end < 0) return false;
        for (int i = 0; i <= end; i++)
            if (isVowelAt(w, i)) return true;
        return false;
    }

    private static boolean endsWith(char[] w, String s) {
        int sl = s.length(), wl = w.length;
        if (sl > wl) return false;
        for (int i = 0; i < sl; i++)
            if (w[wl - sl + i] != s.charAt(i)) return false;
        return true;
    }

    private static char[] replace(char[] w, String suffix, String rep) {
        int base = w.length - suffix.length();
        char[] result = new char[base + rep.length()];
        System.arraycopy(w, 0, result, 0, base);
        for (int i = 0; i < rep.length(); i++) result[base + i] = rep.charAt(i);
        return result;
    }

    private static char[] dropLast(char[] w) {
        char[] r = new char[w.length - 1];
        System.arraycopy(w, 0, r, 0, r.length);
        return r;
    }

    private static char[] append(char[] w, char c) {
        char[] r = new char[w.length + 1];
        System.arraycopy(w, 0, r, 0, w.length);
        r[w.length] = c;
        return r;
    }

    private static boolean endsWithCVC(char[] w) {
        int len = w.length;
        if (len < 3) return false;
        char c1 = w[len - 3], c2 = w[len - 2], c3 = w[len - 1];
        return !isVowel(c1) && isVowel(c2) && !isVowel(c3)
                && c3 != 'w' && c3 != 'x' && c3 != 'y';
    }

    // ── Step 1a: plurals ─────────────────────────────────────────────────────
    private static char[] step1a(char[] w) {
        if (endsWith(w, "sses")) return replace(w, "sses", "ss");
        if (endsWith(w, "ies"))  return replace(w, "ies",  "i");
        if (endsWith(w, "ss"))   return w;
        if (endsWith(w, "s"))    return replace(w, "s",    "");
        return w;
    }

    // ── Step 1b: -ed / -ing ──────────────────────────────────────────────────
    private static char[] step1b(char[] w) {
        if (endsWith(w, "eed")) {
            char[] stem = replace(w, "eed", "");
            if (stem.length > 0 && measure(stem, stem.length - 1) > 0)
                return replace(w, "eed", "ee");
            return w;
        }
        boolean flag = false;
        char[] stem = w;
        if (endsWith(w, "ed")) {
            char[] cand = replace(w, "ed", "");
            if (cand.length > 0 && containsVowel(cand, cand.length - 1)) { stem = cand; flag = true; }
        } else if (endsWith(w, "ing")) {
            char[] cand = replace(w, "ing", "");
            if (cand.length > 0 && containsVowel(cand, cand.length - 1)) { stem = cand; flag = true; }
        }
        if (flag) {
            if (endsWith(stem, "at") || endsWith(stem, "bl") || endsWith(stem, "iz"))
                return append(stem, 'e');
            int len = stem.length;
            if (len >= 2 && stem[len-1] == stem[len-2] && !isVowel(stem[len-1])
                    && stem[len-1] != 'l' && stem[len-1] != 's' && stem[len-1] != 'z')
                return dropLast(stem);
            if (measure(stem, stem.length - 1) == 1 && endsWithCVC(stem))
                return append(stem, 'e');
            return stem;
        }
        return w;
    }

    // ── Step 1c: y → i ───────────────────────────────────────────────────────
    private static char[] step1c(char[] w) {
        if (endsWith(w, "y") && w.length > 1) {
            char[] stem = replace(w, "y", "");
            if (stem.length > 0 && containsVowel(stem, stem.length - 1))
                return replace(w, "y", "i");
        }
        return w;
    }

    // ── Step 2 ───────────────────────────────────────────────────────────────
    private static char[] step2(char[] w) {
        if (w.length < 1 || measure(w, w.length - 1) <= 0) return w;
        String[][] map = {
            {"ational","ate"},{"tional","tion"},{"enci","ence"},{"anci","ance"},
            {"izer","ize"},{"abli","able"},{"alli","al"},{"entli","ent"},
            {"eli","e"},{"ousli","ous"},{"ization","ize"},{"ation","ate"},
            {"ator","ate"},{"alism","al"},{"iveness","ive"},{"fulness","ful"},
            {"ousness","ous"},{"aliti","al"},{"iviti","ive"},{"biliti","ble"}
        };
        for (String[] p : map) {
            if (endsWith(w, p[0])) {
                char[] stem = replace(w, p[0], "");
                if (stem.length > 0 && measure(stem, stem.length - 1) > 0)
                    return replace(w, p[0], p[1]);
            }
        }
        return w;
    }

    // ── Step 3 ───────────────────────────────────────────────────────────────
    private static char[] step3(char[] w) {
        String[][] map = {
            {"icate","ic"},{"ative",""},{"alize","al"},
            {"iciti","ic"},{"ical","ic"},{"ful",""},{"ness",""}
        };
        for (String[] p : map) {
            if (endsWith(w, p[0])) {
                char[] stem = replace(w, p[0], "");
                if (stem.length > 0 && measure(stem, stem.length - 1) > 0)
                    return replace(w, p[0], p[1]);
            }
        }
        return w;
    }

    // ── Step 4 ───────────────────────────────────────────────────────────────
    private static char[] step4(char[] w) {
        // Longer suffixes first to avoid partial matches
        String[] suffixes = {
            "ement","ment","ance","ence","able","ible","ant","ent",
            "ion","er","ic","al","ou","ism","ate","iti","ous","ive","ize"
        };
        for (String suffix : suffixes) {
            if (endsWith(w, suffix)) {
                char[] stem = replace(w, suffix, "");
                if (stem.length == 0) continue;
                if (measure(stem, stem.length - 1) > 1) {
                    if (suffix.equals("ion")) {
                        char last = stem[stem.length - 1];
                        if (last != 's' && last != 't') continue;
                    }
                    return stem;
                }
            }
        }
        return w;
    }

    // ── Step 5a ──────────────────────────────────────────────────────────────
    private static char[] step5a(char[] w) {
        if (endsWith(w, "e") && w.length > 1) {
            char[] stem = replace(w, "e", "");
            if (stem.length == 0) return w;
            int m = measure(stem, stem.length - 1);
            if (m > 1) return stem;
            if (m == 1 && !endsWithCVC(stem)) return stem;
        }
        return w;
    }

    // ── Step 5b ──────────────────────────────────────────────────────────────
    private static char[] step5b(char[] w) {
        if (w.length >= 1 && measure(w, w.length - 1) > 1 && endsWith(w, "ll"))
            return replace(w, "ll", "l");
        return w;
    }
}
