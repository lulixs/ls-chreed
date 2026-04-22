package com.bibleapp.services;
import com.bibleapp.data.MemorizedVerse;

// The serving system that decides between the correct difficulty
public class VerseDifficultyServer {
    private Difficulty difficulty;

    // The 4 types of difficulties and switch between them
    public void setDifficulty(MemorizedVerse memVerse) {
        int level = memVerse.getDifficulty();
        String verse = memVerse.getText();

        switch (level) {
            case MemorizedVerse.DIFFICULTY_COPY_DOWN:
                difficulty = new CopyDownDifficulty(verse);
                break;

            //For Ever Other A and B They are both considered Intermediate
            // because we only have 3 difficulty levels in the UserData.java
            // I think we would need 4 different levels though.
            case MemorizedVerse.DIFFICULTY_EVERY_OTHER_A: 
                difficulty = new EveryOtherDifficultyA(verse);
                break;

            case MemorizedVerse.DIFFICULTY_EVERY_OTHER_B:
                difficulty = new EveryOtherDifficultyB(verse);
                break;

            case MemorizedVerse.DIFFICULTY_FULL_MEMORY:
                difficulty = new FullMemory(verse);
                break;
        }
    }

    // Returns the neccessary difficulty for the choosen verse
    public String getDisplayVerse() {
        return difficulty.getDisplayVerse();
    }

    // Returns the difficulty and number of blanks
    public int getNumBlanks() {
        return difficulty.getNumBlanks();
    }
}