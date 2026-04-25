package com.bibleapp.difficulty;

import java.util.ArrayList;
import java.util.List;

/**
 * EveryOtherA difficulty: the user is shown every odd-positioned word
 * (1st, 3rd, 5th, ...) and must fill in the even-positioned words.
 *
 * Example for "For God so loved the world":
 *   prompt:  "For ____ so ____ the ____"
 *   answers: ["God", "loved", "world"]
 */
public class EveryOtherDiffA implements Difficulty {

    public static final int DIFFICULTY_ID = 2;

    private static final String BLANK = "____";

    private final String[] words;

    public EveryOtherDiffA(String verseText) {
        this.words = verseText == null || verseText.isBlank()
                ? new String[0]
                : verseText.trim().split("\\s+");
    }

    /** Verse with even-positioned (1-based) words replaced by blanks. */
    public String getPrompt() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(isShown(i) ? words[i] : BLANK);
        }
        return sb.toString();
    }

    /** The original even-positioned words, in order. */
    public List<String> getAnswers() {
        List<String> answers = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            if (!isShown(i)) answers.add(words[i]);
        }
        return answers;
    }

    private static boolean isShown(int zeroBasedIndex) {
        return zeroBasedIndex % 2 == 0;
    }

    @Override
    public int getNumBlanks() {
        return getAnswers().size();
    }

    @Override
    public String getDisplayVerse() {
        return getPrompt();
    }
}
