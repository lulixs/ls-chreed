package com.bibleapp.services;

import java.util.List;
import java.util.Locale;

/**
 * Metadata for a Bible translation exposed by bible-api.com.
 */
public record BibleTranslation(
        String identifier,
        String name,
        String language,
        String license,
        List<String> books
) {

    public BibleTranslation {
        books = books == null ? List.of() : List.copyOf(books);
    }

    public String displayLabel() {
        if (name == null || name.isBlank()) {
            return identifier == null ? "" : identifier.toUpperCase(Locale.ROOT);
        }
        if (identifier == null || identifier.isBlank()) {
            return name;
        }
        return name + " (" + identifier.toUpperCase(Locale.ROOT) + ")";
    }

    public boolean matchesQuery(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(name, normalized)
                || containsIgnoreCase(identifier, normalized)
                || containsIgnoreCase(displayLabel(), normalized);
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
}
