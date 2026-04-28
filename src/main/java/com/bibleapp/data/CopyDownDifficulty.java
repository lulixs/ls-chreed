package com.bibleapp.data;

public class CopyDownDifficulty implements Difficulty {
    private String verseText;
    private int numberOfBlanks;

    public CopyDownDifficulty(String verseText, int numberOfBlanks) {
        this.verseText = verseText;
        this.numberOfBlanks = numberOfBlanks;
    }

    @Override
    public String getVerse() {
        return verseText;
    }

    @Override
    public int getBlanks() {
        return numberOfBlanks;
    }
}
