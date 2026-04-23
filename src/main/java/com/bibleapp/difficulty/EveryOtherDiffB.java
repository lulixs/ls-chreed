package com.bibleapp.difficulty;
 
/**
 * Implements the difficulty interface and requires the user to fill in
 * every other word starting with the first word (index 0).
 *
 * Holds data representing:
 *   - the number of blanks to be filled
 *   - the verse to be displayed with the appropriate words blanked out
 *
 * Example:
 *   Input:   "For God so loved the world"
 *   Display: "___  God  ___  loved  ___  world"
 *   Blanks:  3
 * 
 * NOTE!!!!!: No difficulty interface exists yet. Once it is created, this class should be updated to: 
 * public class EveryOtherDiffB implements _____
 * 
 * Additionally, when the interface is ready, ensure that getBlanksCount() and getDisplayVerse() 
 * match the method signatures defined in the interface.
 */
public class EveryOtherDiffB {
 
    private int blanksCount;
    private String displayVerse;
 
    public EveryOtherDiffB(String verseText) {
        blanksCount = 0;
        displayVerse = "";
 
        if (verseText == null || verseText.isBlank()) return;
 
        String[] words = verseText.trim().split("\\s+");
        String[] display = new String[words.length];
 
        for (int i = 0; i < words.length; i++) {
            if (i % 2 == 0) {
                display[i] = "___";
                blanksCount++;
            } else {
                display[i] = words[i];
            }
        }
 
        displayVerse = String.join("  ", display);
    }
 
    public int getBlanksCount() {
        return blanksCount;
    }
 
    public String getDisplayVerse() {
        return displayVerse;
    }
}