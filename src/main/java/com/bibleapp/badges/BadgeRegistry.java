package com.bibleapp.badges;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.bibleapp.data.MemorizedVerse;

/**
 * The catalog of every possible badge. Built once at class init and reused.
 * Adding a new badge type = one new entry here; no callsite changes.
 *
 * Two categories today:
 *   - MILESTONE  one-off achievements (first verse at each difficulty, first
 *                fully-memorized chapter)
 *   - MASTERY    per-book tiered counts of fully-memorized verses (4 tiers per
 *                book, lower tiers skipped if the book is too short to reach
 *                that count)
 */
public final class BadgeRegistry {

    private static final String SLATE = "#3d5a80";

    /** Verse count required for each mastery tier. */
    public static final int[] MASTERY_THRESHOLDS = { 10, 25, 50, 100 };

    public static final String[] BIBLE_BOOK_ORDER = {
        "Genesis","Exodus","Leviticus","Numbers","Deuteronomy",
        "Joshua","Judges","Ruth","1 Samuel","2 Samuel",
        "1 Kings","2 Kings","1 Chronicles","2 Chronicles",
        "Ezra","Nehemiah","Esther","Job","Psalms","Proverbs",
        "Ecclesiastes","Song of Songs","Isaiah","Jeremiah",
        "Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos",
        "Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah",
        "Haggai","Zechariah","Malachi","Matthew","Mark","Luke",
        "John","Acts","Romans","1 Corinthians","2 Corinthians",
        "Galatians","Ephesians","Philippians","Colossians",
        "1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy",
        "Titus","Philemon","Hebrews","James","1 Peter","2 Peter",
        "1 John","2 John","3 John","Jude","Revelation"
    };

    /** book -> chapter -> verse count. Loaded from the bundled JSON. */
    private static final Map<String, Map<Integer, Integer>> BIBLE_STRUCTURE = loadBibleStructure();

    /** Total verses per book — used to decide which mastery tiers are achievable. */
    private static final Map<String, Integer> BOOK_TOTAL_VERSES = computeBookTotals();

    /** Stable hex color per book, spread evenly across the hue wheel. */
    private static final Map<String, String> BOOK_COLORS = computeBookColors();

    private static final List<Badge> ALL_BADGES = buildAll();

    private BadgeRegistry() {}

    public static List<Badge> all() {
        return ALL_BADGES;
    }

    public static String colorForBook(String book) {
        return BOOK_COLORS.getOrDefault(book, SLATE);
    }

    public static int totalVersesIn(String book) {
        return BOOK_TOTAL_VERSES.getOrDefault(book, 0);
    }

    public static int versesIn(String book, int chapter) {
        Map<Integer, Integer> chapters = BIBLE_STRUCTURE.get(book);
        if (chapters == null) return 0;
        return chapters.getOrDefault(chapter, 0);
    }

    // =========================================================================
    // Build
    // =========================================================================

    private static List<Badge> buildAll() {
        List<Badge> out = new ArrayList<>();
        addMilestones(out);
        addPerBookMastery(out);
        return Collections.unmodifiableList(out);
    }

    /**
     * One-off milestones. The four difficulty-first badges fire when any verse
     * has progressed past that difficulty (i.e. nextDifficulty > level), since
     * `nextDifficulty` is the *upcoming* level — completing level N bumps it
     * to N+1.
     */
    private static void addMilestones(List<Badge> out) {
        out.add(new Badge(
                "milestone.first.copy_down",
                "First Copy-Down",
                "Complete a verse at the Copy-down level.",
                BadgeCategory.MILESTONE, null, 0, SLATE,
                verses -> hasAnyVerseAtOrAbove(verses, 1),
                verses -> new Badge.Progress(hasAnyVerseAtOrAbove(verses, 1) ? 1 : 0, 1)
        ));
        out.add(new Badge(
                "milestone.first.every_other_a",
                "First Every-Other A",
                "Complete a verse at the Every-other A level.",
                BadgeCategory.MILESTONE, null, 0, SLATE,
                verses -> hasAnyVerseAtOrAbove(verses, 2),
                verses -> new Badge.Progress(hasAnyVerseAtOrAbove(verses, 2) ? 1 : 0, 1)
        ));
        out.add(new Badge(
                "milestone.first.every_other_b",
                "First Every-Other B",
                "Complete a verse at the Every-other B level.",
                BadgeCategory.MILESTONE, null, 0, SLATE,
                verses -> hasAnyVerseAtOrAbove(verses, 3),
                verses -> new Badge.Progress(hasAnyVerseAtOrAbove(verses, 3) ? 1 : 0, 1)
        ));
        out.add(new Badge(
                "milestone.first.full_memory",
                "First Full-Memory",
                "Complete a verse at the Full-memory level.",
                BadgeCategory.MILESTONE, null, 0, SLATE,
                verses -> hasAnyVerseAtOrAbove(verses, 4),
                verses -> new Badge.Progress(hasAnyVerseAtOrAbove(verses, 4) ? 1 : 0, 1)
        ));
        out.add(new Badge(
                "milestone.first.full_chapter",
                "First Full Chapter",
                "Fully memorize every verse of any chapter.",
                BadgeCategory.MILESTONE, null, 0, SLATE,
                BadgeRegistry::hasAnyFullChapter,
                verses -> new Badge.Progress(hasAnyFullChapter(verses) ? 1 : 0, 1)
        ));
    }

    /**
     * Four mastery tiers per book — but only those tiers achievable given the
     * book's total verse count are added (e.g. Jude has 25 verses, so it gets
     * tiers 1 and 2 only).
     */
    private static void addPerBookMastery(List<Badge> out) {
        for (String book : BIBLE_BOOK_ORDER) {
            int totalInBook = totalVersesIn(book);
            String color = colorForBook(book);

            for (int tier = 1; tier <= MASTERY_THRESHOLDS.length; tier++) {
                int threshold = MASTERY_THRESHOLDS[tier - 1];
                // Skip tiers above what's possible. Tier 1 always shown — even
                // for books smaller than the tier-1 threshold — so users can
                // see "memorize the whole book" as a goal.
                if (tier > 1 && threshold > totalInBook) {
                    continue;
                }
                final int t = threshold;
                final String b = book;
                out.add(new Badge(
                        "mastery." + book.toLowerCase().replace(' ', '_') + ".t" + tier,
                        book + " Mastery " + tier,
                        "Fully memorize " + threshold + " verses in " + book + ".",
                        BadgeCategory.MASTERY, book, tier, color,
                        verses -> countFullyMemorizedIn(verses, b) >= t,
                        verses -> new Badge.Progress(countFullyMemorizedIn(verses, b), t)
                ));
            }
        }
    }

    // =========================================================================
    // Predicates
    // =========================================================================

    /**
     * `nextDifficulty` is the *next* level the user will attempt; completing
     * level N bumps it to N+1. So "user has completed at least level N" =
     * "any verse has nextDifficulty >= N+1", which we encode here as
     * "nextDifficulty >= level" where the caller passes level+1.
     */
    private static boolean hasAnyVerseAtOrAbove(List<MemorizedVerse> verses, int nextDifficultyAtOrAbove) {
        for (MemorizedVerse v : verses) {
            if (v.getNextDifficulty() >= nextDifficultyAtOrAbove) return true;
        }
        return false;
    }

    private static int countFullyMemorizedIn(List<MemorizedVerse> verses, String book) {
        int count = 0;
        for (MemorizedVerse v : verses) {
            if (v.getNextDifficulty() >= 4 && book.equals(v.getBook())) count++;
        }
        return count;
    }

    private static boolean hasAnyFullChapter(List<MemorizedVerse> verses) {
        Map<String, Integer> fullyMemorizedByChapter = new HashMap<>();
        for (MemorizedVerse v : verses) {
            if (v.getNextDifficulty() < 4) continue;
            String key = v.getBook() + "|" + v.getChapter();
            fullyMemorizedByChapter.merge(key, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : fullyMemorizedByChapter.entrySet()) {
            String[] parts = e.getKey().split("\\|");
            if (parts.length != 2) continue;
            String book = parts[0];
            int chapter = Integer.parseInt(parts[1]);
            int totalVersesInChapter = versesIn(book, chapter);
            if (totalVersesInChapter > 0 && e.getValue() >= totalVersesInChapter) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // Bible structure / colors
    // =========================================================================

    private static Map<String, Map<Integer, Integer>> loadBibleStructure() {
        try (InputStream is = BadgeRegistry.class.getResourceAsStream("/com/bibleapp/BibleStructure.json")) {
            if (is == null) return Collections.emptyMap();
            JSONObject root = (JSONObject) new JSONParser().parse(new InputStreamReader(is, StandardCharsets.UTF_8));
            Map<String, Map<Integer, Integer>> bible = new HashMap<>();
            for (Object bookKey : root.keySet()) {
                String book = (String) bookKey;
                JSONObject chapters = (JSONObject) root.get(book);
                Map<Integer, Integer> chapterMap = new HashMap<>();
                for (Object chapterKey : chapters.keySet()) {
                    int chapter = Integer.parseInt((String) chapterKey);
                    int versesInChapter = ((Long) chapters.get(chapterKey)).intValue();
                    chapterMap.put(chapter, versesInChapter);
                }
                bible.put(book, chapterMap);
            }
            return bible;
        } catch (Exception e) {
            System.err.println("BadgeRegistry: failed to load BibleStructure.json — " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    private static Map<String, Integer> computeBookTotals() {
        Map<String, Integer> totals = new HashMap<>();
        for (Map.Entry<String, Map<Integer, Integer>> bookEntry : BIBLE_STRUCTURE.entrySet()) {
            int sum = 0;
            for (int v : bookEntry.getValue().values()) sum += v;
            totals.put(bookEntry.getKey(), sum);
        }
        return totals;
    }

    /**
     * Spread book hues evenly around the wheel so adjacent books in canon
     * order get visibly different colors. Saturation/lightness held
     * constant so the badges read as a coherent set.
     */
    private static Map<String, String> computeBookColors() {
        Map<String, String> colors = new HashMap<>();
        int n = BIBLE_BOOK_ORDER.length;
        for (int i = 0; i < n; i++) {
            float hue = (i * 360f / n);
            colors.put(BIBLE_BOOK_ORDER[i], hsbToHex(hue, 0.55f, 0.78f));
        }
        return colors;
    }

    private static String hsbToHex(float hueDeg, float sat, float bright) {
        // Standard HSB → RGB conversion (HSV). hueDeg in [0,360), sat/bright in [0,1].
        float c = bright * sat;
        float h = ((hueDeg % 360f) + 360f) % 360f / 60f;
        float x = c * (1f - Math.abs(h % 2f - 1f));
        float r1 = 0, g1 = 0, b1 = 0;
        if      (h < 1) { r1 = c; g1 = x; }
        else if (h < 2) { r1 = x; g1 = c; }
        else if (h < 3) { g1 = c; b1 = x; }
        else if (h < 4) { g1 = x; b1 = c; }
        else if (h < 5) { r1 = x; b1 = c; }
        else            { r1 = c; b1 = x; }
        float m = bright - c;
        int r = Math.round((r1 + m) * 255f);
        int g = Math.round((g1 + m) * 255f);
        int b = Math.round((b1 + m) * 255f);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
