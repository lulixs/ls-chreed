package com.bibleapp.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DataStore.java
 * --------------
 * Central data layer for the app. Loads and saves a single JSON file that
 * holds all user-specific state. Callable from anywhere in the program.
 *
 * File location: [user home directory]/bible-app-data.json
 *
 * The file is created exactly once — on first launch, with empty defaults —
 * and persists across runs. Every subsequent launch reads the existing file
 * as-is; no values are filled in or overwritten behind the user's back.
 *
 * JSON structure on first run
 * ───────────────────────────
 * {
 *   "schema_version":        1,
 *   "preferred_translation": "",
 *   "memorization_list":     [],
 *   "reading_plans":         [],
 *   "statistics": {
 *     "chapters_read":  0,
 *     "current_streak": 0
 *   }
 * }
 *
 * The "schema_version" field tags the layout of the file. When load() reads
 * a file whose version does not match {@link #CURRENT_SCHEMA_VERSION} (for
 * example, a file written by a pre-versioning build), it is overwritten
 * with a fresh empty scaffold and a warning is printed to stderr. Bump the
 * constant any time this layout changes incompatibly.
 *
 * A populated memorization entry looks like:
 * {
 *   "id":         "John.3.16",
 *   "book":       "John",
 *   "chapter":    3,
 *   "verse":      16,
 *   "text":       "For God so loved the world…",
 *   "difficulty": 1
 * }
 *
 * The composite "id" field ("{book}.{chapter}.{verse}") uniquely identifies
 * a verse and is used as the dedup / lookup key — see {@link MemorizedVerse}.
 *
 * How to use from any page
 * ────────────────────────
 *   // Read
 *   String translation = DataStore.getPreferredTranslation();
 *   List<MemorizedVerse> verses = DataStore.getMemorizationList();
 *
 *   // Write
 *   DataStore.setPreferredTranslation("ESV");
 *   DataStore.addVerse(new MemorizedVerse("John", 3, 16, "For God so loved…", 1));
 *   DataStore.updateVerseDifficulty("John.3.16", 2);
 *   DataStore.removeVerse("John.3.16");
 */
public class DataStore {

    // ── File path ─────────────────────────────────────────────────────────────

    private static final Path DATA_FILE = Paths.get(
        System.getProperty("user.home"), "bible-app-data.json"
    );

    // ── Schema version ────────────────────────────────────────────────────────

    /** Layout version of the JSON file. Bump on any incompatible change. */
    private static final int CURRENT_SCHEMA_VERSION = 1;

    // ── Top-level JSON keys ───────────────────────────────────────────────────

    private static final String KEY_SCHEMA_VERSION = "schema_version";
    private static final String KEY_TRANSLATION    = "preferred_translation";
    private static final String KEY_MEMORIZATION   = "memorization_list";
    private static final String KEY_READING_PLANS  = "reading_plans";
    private static final String KEY_STATISTICS     = "statistics";

    // ── Memorization entry keys ───────────────────────────────────────────────

    private static final String KEY_VERSE_ID         = "id";
    private static final String KEY_VERSE_BOOK       = "book";
    private static final String KEY_VERSE_CHAPTER    = "chapter";
    private static final String KEY_VERSE_NUMBER     = "verse";
    private static final String KEY_VERSE_TEXT       = "text";
    private static final String KEY_VERSE_DIFFICULTY = "difficulty";

    // =========================================================================
    // Low-level load / save
    // =========================================================================

    /**
     * Load the full JSON object from disk.
     *
     * On first run (file missing), writes an empty scaffold and returns it.
     * On every subsequent run, reads the existing file — unless its
     * schema_version doesn't match {@link #CURRENT_SCHEMA_VERSION}, in which
     * case the file is replaced with a fresh empty scaffold.
     */
    public static JSONObject load() {
        if (!Files.exists(DATA_FILE)) {
            JSONObject scaffold = emptyUserData();
            save(scaffold);
            return scaffold;
        }
        try (Reader reader = new FileReader(DATA_FILE.toFile())) {
            JSONParser parser = new JSONParser();
            JSONObject data = (JSONObject) parser.parse(reader);
            if (!isCurrentSchema(data)) {
                System.err.println(
                    "DataStore: user data file at " + DATA_FILE
                    + " is from an older schema and has been reset.");
                JSONObject scaffold = emptyUserData();
                save(scaffold);
                return scaffold;
            }
            return data;
        } catch (Exception e) {
            System.err.println("DataStore: failed to load – " + e.getMessage());
            return emptyUserData();
        }
    }

    /** True if the given JSON carries the current schema version tag. */
    private static boolean isCurrentSchema(JSONObject data) {
        Object v = data.get(KEY_SCHEMA_VERSION);
        if (v instanceof Long l)    return l.intValue() == CURRENT_SCHEMA_VERSION;
        if (v instanceof Integer i) return i == CURRENT_SCHEMA_VERSION;
        return false;
    }

    /** Write the full JSON object back to disk. */
    public static void save(JSONObject data) {
        try (Writer writer = new FileWriter(DATA_FILE.toFile())) {
            writer.write(data.toJSONString());
        } catch (IOException e) {
            System.err.println("DataStore: failed to save – " + e.getMessage());
        }
    }

    /**
     * Prints the current contents of the user data file to stdout. Called at
     * startup so the JSON can be eyeballed in the terminal to confirm the
     * file was created / persisted correctly.
     */
    public static void printSnapshot() {
        JSONObject data = load();
        System.out.println("User data file: " + DATA_FILE);
        System.out.println(data.toJSONString());
    }

    // =========================================================================
    // Preferred Translation
    // =========================================================================

    /** Returns the user's preferred translation, or "" if not yet chosen. */
    public static String getPreferredTranslation() {
        JSONObject data = load();
        Object val = data.get(KEY_TRANSLATION);
        return (val instanceof String s) ? s : "";
    }

    @SuppressWarnings("unchecked")
    public static void setPreferredTranslation(String translation) {
        JSONObject data = load();
        data.put(KEY_TRANSLATION, translation);
        save(data);
    }

    // =========================================================================
    // Memorization List
    // =========================================================================

    /** Returns the full memorization list as {@link MemorizedVerse} objects. */
    public static List<MemorizedVerse> getMemorizationList() {
        JSONObject data = load();
        JSONArray arr = getOrCreateArray(data, KEY_MEMORIZATION);
        List<MemorizedVerse> result = new ArrayList<>();
        for (Object obj : arr) {
            if (obj instanceof JSONObject entry) {
                String book  = getString(entry, KEY_VERSE_BOOK, "");
                int chapter  = getInt(entry, KEY_VERSE_CHAPTER, 0);
                int verseNum = getInt(entry, KEY_VERSE_NUMBER, 0);
                String text  = getString(entry, KEY_VERSE_TEXT, "");
                int diff     = getInt(entry, KEY_VERSE_DIFFICULTY,
                                     MemorizedVerse.DIFFICULTY_COPY_DOWN);
                if (!book.isBlank() && chapter > 0 && verseNum > 0) {
                    result.add(new MemorizedVerse(book, chapter, verseNum, text, diff));
                }
            }
        }
        return result;
    }

    /**
     * Adds a verse to the memorization list. If an entry with the same ID
     * already exists, it is replaced.
     */
    @SuppressWarnings("unchecked")
    public static void addVerse(MemorizedVerse verse) {
        JSONObject data = load();
        JSONArray arr = getOrCreateArray(data, KEY_MEMORIZATION);

        arr.removeIf(obj ->
            obj instanceof JSONObject entry &&
            verse.getId().equalsIgnoreCase(getString(entry, KEY_VERSE_ID, ""))
        );

        JSONObject entry = new JSONObject();
        entry.put(KEY_VERSE_ID,         verse.getId());
        entry.put(KEY_VERSE_BOOK,       verse.getBook());
        entry.put(KEY_VERSE_CHAPTER,    (long) verse.getChapter());
        entry.put(KEY_VERSE_NUMBER,     (long) verse.getVerse());
        entry.put(KEY_VERSE_TEXT,       verse.getText());
        entry.put(KEY_VERSE_DIFFICULTY, (long) verse.getDifficulty());
        arr.add(entry);

        data.put(KEY_MEMORIZATION, arr);
        save(data);
    }

    /** Removes a verse from the memorization list by its composite ID. */
    public static void removeVerse(String id) {
        JSONObject data = load();
        JSONArray arr = getOrCreateArray(data, KEY_MEMORIZATION);
        arr.removeIf(obj ->
            obj instanceof JSONObject entry &&
            id.equalsIgnoreCase(getString(entry, KEY_VERSE_ID, ""))
        );
        data.put(KEY_MEMORIZATION, arr);
        save(data);
    }

    /**
     * Updates the difficulty level for an existing verse, identified by its
     * composite ID. Does nothing if the ID is not found.
     */
    @SuppressWarnings("unchecked")
    public static void updateVerseDifficulty(String id, int difficulty) {
        JSONObject data = load();
        JSONArray arr = getOrCreateArray(data, KEY_MEMORIZATION);
        for (Object obj : arr) {
            if (obj instanceof JSONObject entry &&
                id.equalsIgnoreCase(getString(entry, KEY_VERSE_ID, ""))) {
                entry.put(KEY_VERSE_DIFFICULTY, (long) difficulty);
                break;
            }
        }
        data.put(KEY_MEMORIZATION, arr);
        save(data);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /** Empty user-data scaffold — what a brand-new file looks like on first run. */
    @SuppressWarnings("unchecked")
    private static JSONObject emptyUserData() {
        JSONObject data = new JSONObject();
        data.put(KEY_SCHEMA_VERSION, (long) CURRENT_SCHEMA_VERSION);
        data.put(KEY_TRANSLATION,    "");
        data.put(KEY_MEMORIZATION,   new JSONArray());
        data.put(KEY_READING_PLANS,  new JSONArray());

        JSONObject stats = new JSONObject();
        stats.put("chapters_read",  0L);
        stats.put("current_streak", 0L);
        data.put(KEY_STATISTICS, stats);

        return data;
    }

    @SuppressWarnings("unchecked")
    private static JSONArray getOrCreateArray(JSONObject data, String key) {
        Object val = data.get(key);
        if (val instanceof JSONArray arr) return arr;
        JSONArray fresh = new JSONArray();
        data.put(key, fresh);
        return fresh;
    }

    private static String getString(JSONObject obj, String key, String fallback) {
        Object val = obj.get(key);
        return (val instanceof String s) ? s : fallback;
    }

    private static int getInt(JSONObject obj, String key, int fallback) {
        Object val = obj.get(key);
        if (val instanceof Long l)    return l.intValue();
        if (val instanceof Integer i) return i;
        return fallback;
    }
}
