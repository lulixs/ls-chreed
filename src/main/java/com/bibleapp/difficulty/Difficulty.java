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
                display[i] = buildBlank(words[i]);
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

    private String buildBlank(String word) {
        if (word == null || word.isEmpty()) {
            return BLANK;
        }

        StringBuilder blank = new StringBuilder(word.length());
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            blank.append(Character.isLetterOrDigit(c) ? '_' : c);
        }

        return blank.toString();
    }
}
