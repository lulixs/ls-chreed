package com.bibleapp.services;

import java.util.Collections;
import java.util.List;

/** A parsed bible-api.com response: a reference plus one or more verses. */
public class BiblePassage {

    private final String reference;
    private final List<BibleVerse> verses;
    private final String text;
    private final String translationId;
    private final String translationName;
    private final String translationNote;

    public BiblePassage(String reference,
                        List<BibleVerse> verses,
                        String text,
                        String translationId,
                        String translationName,
                        String translationNote) {
        this.reference = reference;
        this.verses = verses == null ? Collections.emptyList() : List.copyOf(verses);
        this.text = text;
        this.translationId = translationId;
        this.translationName = translationName;
        this.translationNote = translationNote;
    }

    public String           getReference()       { return reference; }
    public List<BibleVerse> getVerses()          { return verses; }
    public String           getText()            { return text; }
    public String           getTranslationId()   { return translationId; }
    public String           getTranslationName() { return translationName; }
    public String           getTranslationNote() { return translationNote; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Reference: ").append(reference)
          .append("  |  Translation: ").append(translationName)
          .append(" (").append(translationId).append(")")
          .append(System.lineSeparator());
        for (BibleVerse v : verses) {
            sb.append("  ").append(v).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
