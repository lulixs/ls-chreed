package com.bibleapp.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.*;

/**
 * DataStore.java
 * --------------
 * Handles saving and loading data to a local JSON file on the user's machine.
 * This replaces the server + database from the web version.
 *
 * Data is saved to: [user home directory]/bible-app-data.json
 *
 * TODO: students can expand the data structure and add helper methods
 *       for each feature they build.
 */
public class DataStore {

    private static final Path DATA_FILE = Paths.get(
        System.getProperty("user.home"), "bible-app-data.json"
    );

    /** Load all data. Returns a new default object if the file doesn't exist. */
    public static JSONObject load() {
        if (!Files.exists(DATA_FILE)) {
            return defaultData();
        }
        try (Reader reader = new FileReader(DATA_FILE.toFile())) {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(reader);
        } catch (Exception e) {
            System.err.println("Failed to load data: " + e.getMessage());
            return defaultData();
        }
    }

    /** Save all data back to the JSON file. */
    public static void save(JSONObject data) {
        try (Writer writer = new FileWriter(DATA_FILE.toFile())) {
            writer.write(data.toJSONString());
        } catch (IOException e) {
            System.err.println("Failed to save data: " + e.getMessage());
        }
    }

    private static JSONObject defaultData() {
        JSONObject data = new JSONObject();
        data.put("memorized_verses", new JSONArray());
        data.put("reading_plans", new JSONArray());
        JSONObject stats = new JSONObject();
        stats.put("chapters_read", 0);
        stats.put("current_streak", 0);
        data.put("statistics", stats);
        return data;
    }
}
