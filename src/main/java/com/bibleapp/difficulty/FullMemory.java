package com.bibleapp.difficulty;

/**
 * Extends the difficulty class and requires the user to fill in each
 *  word from memory starting with the first word (index 0).
 *
 * Holds data representing:
 *   - the verse to be displayed
 *   - the answer key
 *
 * Example:
 *   Input:   "For God so loved the world"
 *   Display: "___ ___ ___ ___ ___ ___"
 *   Answer:  "For God so loved the world"
 */

public class FullMemory extends Difficulty {

    public FullMemory(String verseText) {
        // Word will always have blank applied, no matter the index
        super(verseText, n -> true);
    }
}