package com.bibleapp.difficulty;

/**
 * Extends the difficulty class and requires the user to copy down every
 *  other word starting with the first word (index 0), while requiring the rest
 *  of the words be filled in from memory.
 *
 * Holds data representing:
 *   - the verse to be displayed
 *   - the answer key
 *
 * Example:
 *   Input:   "For God so loved the world"
 *   Display: "For ___ so ___ the ___"
 *   Answer:  "For God so loved the world"
 */

public class EveryOtherA extends Difficulty {

    public EveryOtherA(String verseText) {
        // Word will have blank applied if its index is odd
        super(verseText, n -> n % 2 == 1);
    }
}