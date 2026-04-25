package com.bibleapp.difficulty;

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
                difficulty = new Difficulty() {
                    @Override
                    public int getNumBlanks() {
                        return 0;
                    }

                    @Override
                    public String getDisplayVerse() {
                        return verse;
                    }
                };
                break;

            case MemorizedVerse.DIFFICULTY_EVERY_OTHER_A:
                difficulty = new EveryOtherDiffA(verse);
                break;

            case MemorizedVerse.DIFFICULTY_EVERY_OTHER_B:
                difficulty = new EveryOtherDiffB(verse);
                break;

            case MemorizedVerse.DIFFICULTY_FULL_MEMORY:
                difficulty = new FullMemory(verse);
                break;

            default:
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
