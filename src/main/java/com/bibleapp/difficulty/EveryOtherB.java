package com.bibleapp.difficulty;
 
/**
 * Extends the difficulty class and requires the user to copy down every
 *  other word starting with the second word (index 1), while requiring the rest
 *  of the words be filled in from memory.
 *
 * Holds data representing:
 *   - the verse to be displayed
 *   - the answer key
 *
 * Example:
 *   Input:   "For God so loved the world"
 *   Display: "___ God ___ loved ___ world"
 *   Answer:  "For God so loved the world"
 */

public class EveryOtherB extends Difficulty {
 
    public EveryOtherB(String verseText) {
        // Word will have blank applied if its index is even
        super(verseText, n -> n % 2 == 0);
    }
    
}