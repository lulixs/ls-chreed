package com.bibleapp.difficulty;
 
/**
 * Extends the difficulty class and requires the user to copy down each
 *  word starting with the first word (index 0).
 *
 * Holds data representing:
 *   - the verse to be displayed
 *   - the answer key
 *
 * Example:
 *   Input:   "For God so loved the world"
 *   Display: "For God so loved the world"
 *   Answer:  "For God so loved the world"
 */

public class CopyDown extends Difficulty {
 
    public CopyDown(String verseText) {
        // Word will never have blank applied, no matter the index
        super(verseText, n -> false);
    }
}