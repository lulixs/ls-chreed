package com.bibleapp.services;

public class FullMemory implements Difficulty {
    private int numBlanks;
    private String displayVerse;

    public FullMemory(String verse) {
        String[] words = verse.split(" ");

        this.numBlanks = words.length;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            sb.append("_____");
            if (i < words.length - 1) {
                sb.append(" ");
            }
        }
        this.displayVerse = sb.toString();
    }

    @Override
    public int getNumBlanks() {
        return numBlanks;
    }

    @Override
    public String getDisplayVerse() {
        return displayVerse;
    }
}