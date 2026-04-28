package com.bibleapp.difficulty;
import java.util.function.Function;

public class Difficulty {
    private String[] displayVerse;
    private String[] answerKey;
    protected static final String BLANK = "___";  

    protected Difficulty(String verseText, Function<Integer, Boolean> blankCheck) {
        if (verseText == null || verseText.isBlank()) return;
 
        String[] words = verseText.trim().split("\\s+");
        String[] display = new String[words.length];
 
        for (int i = 0; i < words.length; i++) {
            if (blankCheck.apply(i)) {
                display[i] = BLANK;
            } else {
                display[i] = words[i];
            }
        }
 
        displayVerse = display;
        answerKey = words;
    }
    public String[] getDisplayVerse() {
        return displayVerse;
    }

    public String[] getAnswerKey() {
        return answerKey;
    }
}
