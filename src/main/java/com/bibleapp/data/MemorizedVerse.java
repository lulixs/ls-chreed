package com.bibleapp.data;

/**
 * MemorizedVerse.java
 * -------------------
 * A single verse on the user's memorization list.
 *
 * Each verse is identified by a composite ID in the form
 * "{book}.{chapter}.{verse}" (e.g. "John.3.16"). Because book/chapter/verse
 * uniquely identify a verse in the Bible, this ID doubles as the dedup key
 * for the memorization list — no separate counter or UUID is needed.
 *
 * Difficulty levels:
 *   1 = Copy-down      – full verse shown; user copies it
 *   2 = Every-other A  – odd-indexed words shown; user fills in the even ones
 *   3 = Every-other B  – even-indexed words shown; user fills in the odd ones
 *   4 = Full-memory    – no verse shown; user writes the full verse from memory
 */
public class MemorizedVerse {

    public static final int DIFFICULTY_COPY_DOWN     = 1;
    public static final int DIFFICULTY_EVERY_OTHER_A = 2;
    public static final int DIFFICULTY_EVERY_OTHER_B = 3;
    public static final int DIFFICULTY_FULL_MEMORY   = 4;

    private final String book;
    private final int    chapter;
    private final int    verse;
    private final String text;
    private int          difficulty;

    public MemorizedVerse(String book, int chapter, int verse, String text, int difficulty) {
        this.book       = book;
        this.chapter    = chapter;
        this.verse      = verse;
        this.text       = text;
        this.difficulty = clampDifficulty(difficulty);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    /** Composite ID: "{book}.{chapter}.{verse}" — e.g. "John.3.16". */
    public String getId() {
        return book + "." + chapter + "." + verse;
    }

    public String getBook()       { return book; }
    public int    getChapter()    { return chapter; }
    public int    getVerse()      { return verse; }
    public String getText()       { return text; }
    public int    getDifficulty() { return difficulty; }

    /** Human-readable reference, e.g. "John 3:16". */
    public String getReference() {
        return book + " " + chapter + ":" + verse;
    }

    /** Human-readable label for the current difficulty level. */
    public String getDifficultyLabel() {
        return switch (difficulty) {
            case DIFFICULTY_COPY_DOWN     -> "Copy-down";
            case DIFFICULTY_EVERY_OTHER_A -> "Every-other A";
            case DIFFICULTY_EVERY_OTHER_B -> "Every-other B";
            case DIFFICULTY_FULL_MEMORY   -> "Full-memory";
            default                       -> "Unknown";
        };
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setDifficulty(int difficulty) {
        this.difficulty = clampDifficulty(difficulty);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private static int clampDifficulty(int d) {
        return Math.max(DIFFICULTY_COPY_DOWN, Math.min(DIFFICULTY_FULL_MEMORY, d));
    }

    @Override
    public String toString() {
        return getReference() + " [" + getDifficultyLabel() + "]";
    }
}
