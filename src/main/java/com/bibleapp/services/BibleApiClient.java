package com.bibleapp.services;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HTTP client for bible-api.com. No auth required; free tier rate-limited
 * to 15 requests / 30 seconds per IP.
 */
public class BibleApiClient {

    private static final String BASE_URL = "https://bible-api.com";
    private static final String DEFAULT_TRANSLATION = "web";

    private final HttpClient http;

    public BibleApiClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public BiblePassage getPassage(String reference) throws BibleApiException {
        return getPassage(reference, DEFAULT_TRANSLATION);
    }

    public BiblePassage getPassage(String reference, String translation) throws BibleApiException {
        String encodedRef = URLEncoder.encode(reference.trim(), StandardCharsets.UTF_8)
                                      .replace("+", "%20");
        // single_chapter_book_matching=indifferent so "Jude 1" returns the whole
        // book instead of just verse 1 (same for Obadiah, Philemon, 2 John, 3 John).
        String url = BASE_URL + "/" + encodedRef
                + "?translation=" + translation
                + "&single_chapter_book_matching=indifferent";
        return parsePassage(sendRequest(url));
    }

    public BiblePassage getRandomVerse(String translation) throws BibleApiException {
        String url = BASE_URL + "/data/" + translation + "/random";
        return parsePassage(sendRequest(url));
    }

    public BiblePassage getRandomVerse(String translation, String bookIds) throws BibleApiException {
        String url = BASE_URL + "/data/" + translation + "/random/" + bookIds;
        return parsePassage(sendRequest(url));
    }

    public List<BibleTranslation> getTranslations() throws BibleApiException {
        try {
            JSONObject root = (JSONObject) new JSONParser().parse(sendRequest(BASE_URL + "/data"));
            JSONArray translationsJson = (JSONArray) root.get("translations");
            List<BibleTranslation> translations = new ArrayList<>();
            if (translationsJson != null) {
                for (Object item : translationsJson) {
                    JSONObject translation = (JSONObject) item;
                    translations.add(new BibleTranslation(
                            asString(translation.get("identifier")),
                            asString(translation.get("name")),
                            asString(translation.get("language")),
                            asString(translation.get("license")),
                            List.of()
                    ));
                }
            }
            return translations;
        } catch (ParseException e) {
            throw new BibleApiException("Failed to parse translation metadata: " + e.getMessage(), e);
        }
    }

    public BibleTranslation getTranslationDetails(String translationId) throws BibleApiException {
        try {
            JSONObject root = (JSONObject) new JSONParser().parse(sendRequest(BASE_URL + "/data/" + translationId));
            JSONObject translation = (JSONObject) root.get("translation");
            JSONArray booksJson = (JSONArray) root.get("books");

            List<String> books = new ArrayList<>();
            if (booksJson != null) {
                for (Object item : booksJson) {
                    JSONObject book = (JSONObject) item;
                    books.add(asString(book.get("name")));
                }
            }

            return new BibleTranslation(
                    translation == null ? translationId : asString(translation.get("identifier")),
                    translation == null ? null : asString(translation.get("name")),
                    translation == null ? null : asString(translation.get("language")),
                    translation == null ? null : asString(translation.get("license")),
                    books
            );
        } catch (ParseException e) {
            throw new BibleApiException("Failed to parse translation details for " + translationId + ": " + e.getMessage(), e);
        }
    }

    public List<BibleTranslation> getTranslationsSupportingBooks(List<String> requiredBooks) throws BibleApiException {
        Set<String> normalizedRequired = requiredBooks.stream()
                .map(BibleApiClient::normalizeBookName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<BibleTranslation> supported = new ArrayList<>();
        for (BibleTranslation translation : getTranslations()) {
            if (!"english".equalsIgnoreCase(translation.language())) {
                continue;
            }

            BibleTranslation detailed = getTranslationDetails(translation.identifier());
            Set<String> supportedBooks = detailed.books().stream()
                    .map(BibleApiClient::normalizeBookName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (supportedBooks.containsAll(normalizedRequired)) {
                supported.add(detailed);
            }
        }

        supported.sort(Comparator.comparing(
                translation -> translation.name() == null ? translation.identifier() : translation.name(),
                String.CASE_INSENSITIVE_ORDER
        ));
        return supported;
    }

    private String sendRequest(String url) throws BibleApiException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new BibleApiException("HTTP " + status + " from " + url + ": " + response.body());
            }
            return response.body();
        } catch (IOException e) {
            throw new BibleApiException("Network error calling " + url + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BibleApiException("Request interrupted: " + url, e);
        }
    }

    private BiblePassage parsePassage(String body) throws BibleApiException {
        try {
            JSONObject root = (JSONObject) new JSONParser().parse(body);

            // The random-verse endpoint wraps the payload under "random_verse".
            JSONObject random = (JSONObject) root.get("random_verse");
            if (random != null) {
                return parseRandomPassage(root, random);
            }

            List<BibleVerse> verses = new ArrayList<>();
            JSONArray versesJson = (JSONArray) root.get("verses");
            if (versesJson != null) {
                for (Object item : versesJson) {
                    verses.add(toVerse((JSONObject) item));
                }
            }

            return new BiblePassage(
                    asString(root.get("reference")),
                    verses,
                    asString(root.get("text")),
                    asString(root.get("translation_id")),
                    asString(root.get("translation_name")),
                    asString(root.get("translation_note"))
            );
        } catch (ParseException e) {
            throw new BibleApiException("Failed to parse API response: " + e.getMessage(), e);
        }
    }

    private BiblePassage parseRandomPassage(JSONObject root, JSONObject random) {
        BibleVerse verse = new BibleVerse(
                asString(random.get("book_id")),
                asString(random.get("book")),
                asInt(random.get("chapter")),
                asInt(random.get("verse")),
                asString(random.get("text"))
        );
        String reference = verse.getBookName() + " " + verse.getChapter() + ":" + verse.getVerse();

        JSONObject translation = (JSONObject) root.get("translation");
        String translationId   = translation == null ? null : asString(translation.get("identifier"));
        String translationName = translation == null ? null : asString(translation.get("name"));
        String translationNote = translation == null ? null : asString(translation.get("license"));

        return new BiblePassage(reference, List.of(verse), verse.getText(),
                translationId, translationName, translationNote);
    }

    private static BibleVerse toVerse(JSONObject v) {
        return new BibleVerse(
                asString(v.get("book_id")),
                asString(v.get("book_name")),
                asInt(v.get("chapter")),
                asInt(v.get("verse")),
                asString(v.get("text"))
        );
    }

    private static String asString(Object o) { return o == null ? null : o.toString(); }

    private static int asInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        if (o == null) return 0;
        return Integer.parseInt(o.toString());
    }

    private static String normalizeBookName(String bookName) {
        return bookName == null ? "" : bookName.trim().toLowerCase(Locale.ROOT);
    }
}
