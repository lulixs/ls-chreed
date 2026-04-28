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

    // Code is much easier to read 0-indexed
    public static final int DIFFICULTY_COPY_DOWN     = 0;
    public static final int DIFFICULTY_EVERY_OTHER_A = 1;
    public static final int DIFFICULTY_EVERY_OTHER_B = 2;
    public static final int DIFFICULTY_FULL_MEMORY   = 3;

    private final String book;
    private final int    chapter;
    private final int    verse;
    private final String text;
    private int          nextDifficulty; // Maximum difficulty that can be selected for the verse
    /* It doesn't work to fix a verse to a specific difficulty as the user may wish to go back to a lower difficulty.
     * It works better to set a maximum difficulty to represent the next difficulty in the progression.
     */

    public MemorizedVerse(String book, int chapter, int verse, String text, int nextDifficulty) {
        this.book           = book;
        this.chapter        = chapter;
        this.verse          = verse;
        this.text           = text;
        this.nextDifficulty = clampDifficulty(nextDifficulty);
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
    public int    getNextDifficulty() { return nextDifficulty; }

    /** Human-readable reference, e.g. "John 3:16". */
    public String getReference() {
        return book + " " + chapter + ":" + verse;
    }

    /** Human-readable label for the current difficulty level. */
    static public String getDifficultyLabel(int difficulty) {
        return switch (difficulty) {
            case DIFFICULTY_COPY_DOWN     -> "Copy-down";
            case DIFFICULTY_EVERY_OTHER_A -> "Every-other A";
            case DIFFICULTY_EVERY_OTHER_B -> "Every-other B";
            case DIFFICULTY_FULL_MEMORY   -> "Full-memory";
            default                       -> "Unknown";
        };
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setNextDifficulty(int nextDifficulty) {
        this.nextDifficulty = clampDifficulty(nextDifficulty);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private static int clampDifficulty(int d) {
        return Math.max(DIFFICULTY_COPY_DOWN, Math.min(DIFFICULTY_FULL_MEMORY + 1, d));
    }

    @Override
    public String toString() {
        return getReference() + " [" + MemorizedVerse.getDifficultyLabel(nextDifficulty) + "]";
    }
}
