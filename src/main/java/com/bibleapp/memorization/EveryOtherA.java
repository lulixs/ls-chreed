package com.bibleapp.memorization;

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
public class EveryOtherA {

    public static final int DIFFICULTY_ID = 2;

    private static final String BLANK = "____";

    private final String[] words;

    public EveryOtherA(String verseText) {
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

    /** The original even-positioned words — the answer key, in order. */
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

    /** Standalone demo so the class can be sanity-checked from the terminal. */
    public static void main(String[] args) {
        String verse = "For God so loved the world that he gave his only Son";
        EveryOtherA mode = new EveryOtherA(verse);

        System.out.println("=== Every-other A difficulty demo ===");
        System.out.println("Verse:   " + verse);
        System.out.println("Prompt:  " + mode.getPrompt());
        System.out.println("Answers: " + mode.getAnswers());
        System.out.println();

        System.out.println("Iterating answers one at a time:");
        List<String> answers = mode.getAnswers();
        for (int i = 0; i < answers.size(); i++) {
            System.out.println("  blank " + (i + 1) + " -> " + answers.get(i));
        }
    }
}
