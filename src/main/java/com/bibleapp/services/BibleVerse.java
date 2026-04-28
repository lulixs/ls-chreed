package com.bibleapp.services;

/** One verse returned by bible-api.com. */
public class BibleVerse {

    private final String bookId;
    private final String bookName;
    private final int chapter;
    private final int verse;
    private final String text;

    public BibleVerse(String bookId, String bookName, int chapter, int verse, String text) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.chapter = chapter;
        this.verse = verse;
        this.text = text;
    }

    public String getBookId()   { return bookId; }
    public String getBookName() { return bookName; }
    public int    getChapter()  { return chapter; }
    public int    getVerse()    { return verse; }
    public String getText()     { return text; }

    @Override
    public String toString() {
        return String.format("%s %d:%d  %s", bookId, chapter, verse, text.trim());
    }
}
