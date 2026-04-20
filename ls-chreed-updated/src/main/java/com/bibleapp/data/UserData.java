package com.bibleapp.data;

/**
 * UserData.java
 * -------------
 * A simple model representing a verse on the user's memorization list,
 * including which translation to display and the difficulty level to practice at.
 *
 * Difficulty levels:
 *   1 = Beginner   – full verse shown, user reads along
 *   2 = Intermediate – first letter of each word shown
 *   3 = Advanced   – blank, user types from memory
 */
public class UserData {

    public static final int DIFFICULTY_BEGINNER     = 1;
    public static final int DIFFICULTY_INTERMEDIATE = 2;
    public static final int DIFFICULTY_ADVANCED     = 3;

    /** The verse reference, e.g. "John 3:16" */
    private final String reference;

    /** The full text of the verse */
    private final String text;

    /**
     * Practice difficulty (1 = Beginner, 2 = Intermediate, 3 = Advanced).
     * Stored per-verse so each verse can be at a different stage.
     */
    private int difficulty;

    public UserData(String reference, String text, int difficulty) {
        this.reference = reference;
        this.text = text;
        this.difficulty = Math.max(DIFFICULTY_BEGINNER,
                          Math.min(DIFFICULTY_ADVANCED, difficulty)); // clamp 1-3
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getReference() { return reference; }
    public String getText()      { return text; }
    public int    getDifficulty(){ return difficulty; }

    /** Human-readable label for the current difficulty level. */
    public String getDifficultyLabel() {
        return switch (difficulty) {
            case DIFFICULTY_BEGINNER     -> "Beginner";
            case DIFFICULTY_INTERMEDIATE -> "Intermediate";
            case DIFFICULTY_ADVANCED     -> "Advanced";
            default                      -> "Unknown";
        };
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setDifficulty(int difficulty) {
        this.difficulty = Math.max(DIFFICULTY_BEGINNER,
                          Math.min(DIFFICULTY_ADVANCED, difficulty));
    }

    @Override
    public String toString() {
        return reference + " [" + getDifficultyLabel() + "]";
    }
}
