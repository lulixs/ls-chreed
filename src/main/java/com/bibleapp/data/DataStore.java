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
 * Central data layer – readable and writable from anywhere in the program.
 *
 * Saved to: [user home directory]/bible-app-data.json
 *
 * JSON structure
 * ──────────────
 * {
 *   "preferred_translation": "NIV",
 *   "memorization_list": [
 *     {
 *       "reference": "John 3:16",
 *       "text":      "For God so loved the world…",
 *       "difficulty": 1
 *     },
 *     ...
 *   ],
 *   "reading_plans": [...],
 *   "statistics": {
 *     "chapters_read": 0,
 *     "current_streak": 0
 *   }
 * }
 *
 * How to use from any page
 * ────────────────────────
 *   // Read
 *   String translation = DataStore.getPreferredTranslation();
 *   List<UserData> verses = DataStore.getMemorizationList();
 *
 *   // Write
 *   DataStore.setPreferredTranslation("ESV");
 *   DataStore.addVerse(new UserData("John 3:16", "For God so loved…", 1));
 *   DataStore.updateVerseDifficulty("John 3:16", 2);
 *   DataStore.removeVerse("John 3:16");
 */
public class DataStore {

    // ── File path ─────────────────────────────────────────────────────────────

    private static final Path DATA_FILE = Paths.get(
        System.getProperty("user.home"), "bible-app-data.json"
    );

    // ── JSON keys ─────────────────────────────────────────────────────────────

    private static final String KEY_TRANSLATION      = "preferred_translation";
    private static final String KEY_MEMORIZATION     = "memorization_list";
    private static final String KEY_READING_PLANS    = "reading_plans";
    private static final String KEY_STATISTICS       = "statistics";
    private static final String KEY_VERSE_REFERENCE  = "reference";
    private static final String KEY_VERSE_TEXT       = "text";
    private static final String KEY_VERSE_DIFFICULTY = "difficulty";

    // ── Default values ────────────────────────────────────────────────────────

    private static final String DEFAULT_TRANSLATION = "NIV";

    // =========================================================================
    // Low-level load / save
    // =========================================================================

    /** Load the full JSON object from disk. Returns a fresh default if absent. */
    public static JSONObject load() {
        if (!Files.exists(DATA_FILE)) {
            return defaultData();
        }
        try (Reader reader = new FileReader(DATA_FILE.toFile())) {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(reader);
        } catch (Exception e) {
            System.err.println("DataStore: failed to load – " + e.getMessage());
            return defaultData();
        }
    }

    /** Write the full JSON object back to disk. */
    public static void save(JSONObject data) {
        try (Writer writer = new FileWriter(DATA_FILE.toFile())) {
            writer.write(data.toJSONString());
        } catch (IOException e) {
            System.err.println("DataStore: failed to save – " + e.getMessage());
        }
    }

    // =========================================================================
    // Preferred Translation
    // =========================================================================

    /**
     * Returns the user's preferred Bible translation (e.g. "NIV", "ESV").
     * Defaults to "NIV" if not yet set.
     */
    public static String getPreferredTranslation() {
        JSONObject data = load();
        Object val = data.get(KEY_TRANSLATION);
        return (val instanceof String s && !s.isBlank()) ? s : DEFAULT_TRANSLATION;
    }

    /**
     * Saves the user's preferred Bible translation.
     *
     * @param translation e.g. "NIV", "ESV", "KJV"
     */
    @SuppressWarnings("unchecked")
    public static void setPreferredTranslation(String translation) {
        JSONObject data = load();
        data.put(KEY_TRANSLATION, translation);
        save(data);
    }

    // =========================================================================
    // Memorization List
    // =========================================================================

    /**
     * Returns the full memorization list as Java objects.
     * Each {@link UserData} carries the reference, verse text, and difficulty.
     */
    public static List<UserData> getMemorizationList() {
        JSONObject data = load();
        JSONArray arr = getOrCreateArray(data, KEY_MEMORIZATION);
        List<UserData> result = new ArrayList<>();
        for (Object obj : arr) {
            if (obj instanceof JSONObject entry) {
                String ref  = getString(entry, KEY_VERSE_REFERENCE, "");
                String text = getString(entry, KEY_VERSE_TEXT, "");
                int diff    = getInt(entry, KEY_VERSE_DIFFICULTY,
                                    UserData.DIFFICULTY_BEGINNER);
                if (!ref.isBlank()) {
                    result.add(new UserData(ref, text, diff));
                }
            }
        }
        return result;
    }

    /**
     * Adds a verse to the memorization list.
     * If a verse with the same reference already exists it is replaced.
     *
     * @param verse {@link UserData} to add
     */
    @SuppressWarnings("unchecked")
    public static void addVerse(UserData verse) {
        JSONObject data = load();
        JSONArray arr = getOrCreateArray(data, KEY_MEMORIZATION);

        // Remove existing entry with the same reference (avoid duplicates)
        arr.removeIf(obj ->
            obj instanceof JSONObject entry &&
            verse.getReference().equalsIgnoreCase(getString(entry, KEY_VERSE_REFERENCE, ""))
        );

        // Append new entry
        JSONObject entry = new JSONObject();
        entry.put(KEY_VERSE_REFERENCE,  verse.getReference());
        entry.put(KEY_VERSE_TEXT,       verse.getText());
        entry.put(KEY_VERSE_DIFFICULTY, (long) verse.getDifficulty());
        arr.add(entry);

        data.put(KEY_MEMORIZATION, arr);
        save(data);
    }

    /**
     * Removes a verse from the memorization list by its reference.
     *
     * @param reference e.g. "John 3:16"
     */
    public static void removeVerse(String reference) {
        JSONObject data = load();
        JSONArray arr = getOrCreateArray(data, KEY_MEMORIZATION);
        arr.removeIf(obj ->
            obj instanceof JSONObject entry &&
            reference.equalsIgnoreCase(getString(entry, KEY_VERSE_REFERENCE, ""))
        );
        data.put(KEY_MEMORIZATION, arr);
        save(data);
    }

    /**
     * Updates the difficulty level for an existing verse.
     * Does nothing if the reference is not found.
     *
     * @param reference  e.g. "John 3:16"
     * @param difficulty one of {@link UserData#DIFFICULTY_BEGINNER},
     *                   {@link UserData#DIFFICULTY_INTERMEDIATE}, or
     *                   {@link UserData#DIFFICULTY_ADVANCED}
     */
    @SuppressWarnings("unchecked")
    public static void updateVerseDifficulty(String reference, int difficulty) {
        JSONObject data = load();
        JSONArray arr = getOrCreateArray(data, KEY_MEMORIZATION);
        for (Object obj : arr) {
            if (obj instanceof JSONObject entry &&
                reference.equalsIgnoreCase(getString(entry, KEY_VERSE_REFERENCE, ""))) {
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

    /** Returns the default data structure for a brand-new user. */
    @SuppressWarnings("unchecked")
    private static JSONObject defaultData() {
        JSONObject data = new JSONObject();
        data.put(KEY_TRANSLATION,   DEFAULT_TRANSLATION);
        data.put(KEY_MEMORIZATION,  new JSONArray());
        data.put(KEY_READING_PLANS, new JSONArray());

        JSONObject stats = new JSONObject();
        stats.put("chapters_read",  0L);
        stats.put("current_streak", 0L);
        data.put(KEY_STATISTICS, stats);

        return data;
    }

    /** Gets (or creates) a JSONArray for the given key, mutating {@code data}. */
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
