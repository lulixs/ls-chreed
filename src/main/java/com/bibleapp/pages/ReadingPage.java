package com.bibleapp.pages;

import com.bibleapp.services.BibleApiClient;
import com.bibleapp.services.BibleApiException;
import com.bibleapp.services.BiblePassage;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

/**
 * Bible reading page with selectors for translation, book, and chapter.
 * Contains a text area for displaying scripture content.
 */
public class ReadingPage extends VBox {

    private static final List<String> TRANSLATIONS = List.of(
            "web", "kjv", "asv", "bbe", "darby", "dra", "ylt", "webbe", "oeb-cw", "oeb-us"
    );

    private static final List<String> BOOKS = List.of(
            "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy",
            "Joshua", "Judges", "Ruth", "1 Samuel", "2 Samuel",
            "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
            "Nehemiah", "Esther", "Job", "Psalms", "Proverbs",
            "Ecclesiastes", "Song of Solomon", "Isaiah", "Jeremiah", "Lamentations",
            "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
            "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk",
            "Zephaniah", "Haggai", "Zechariah", "Malachi",
            "Matthew", "Mark", "Luke", "John", "Acts",
            "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians",
            "Philippians", "Colossians", "1 Thessalonians", "2 Thessalonians", "1 Timothy",
            "2 Timothy", "Titus", "Philemon", "Hebrews", "James",
            "1 Peter", "2 Peter", "1 John", "2 John", "3 John",
            "Jude", "Revelation"
    );

    // Maps each book to its number of chapters so the spinner can't go out of bounds
    private static final Map<String, Integer> CHAPTER_COUNTS = Map.ofEntries(
            Map.entry("Genesis", 50), Map.entry("Exodus", 40), Map.entry("Leviticus", 27),
            Map.entry("Numbers", 36), Map.entry("Deuteronomy", 34), Map.entry("Joshua", 24),
            Map.entry("Judges", 21), Map.entry("Ruth", 4), Map.entry("1 Samuel", 31),
            Map.entry("2 Samuel", 24), Map.entry("1 Kings", 22), Map.entry("2 Kings", 25),
            Map.entry("1 Chronicles", 29), Map.entry("2 Chronicles", 36), Map.entry("Ezra", 10),
            Map.entry("Nehemiah", 13), Map.entry("Esther", 10), Map.entry("Job", 42),
            Map.entry("Psalms", 150), Map.entry("Proverbs", 31), Map.entry("Ecclesiastes", 12),
            Map.entry("Song of Solomon", 8), Map.entry("Isaiah", 66), Map.entry("Jeremiah", 52),
            Map.entry("Lamentations", 5), Map.entry("Ezekiel", 48), Map.entry("Daniel", 12),
            Map.entry("Hosea", 14), Map.entry("Joel", 3), Map.entry("Amos", 9),
            Map.entry("Obadiah", 1), Map.entry("Jonah", 4), Map.entry("Micah", 7),
            Map.entry("Nahum", 3), Map.entry("Habakkuk", 3), Map.entry("Zephaniah", 3),
            Map.entry("Haggai", 2), Map.entry("Zechariah", 14), Map.entry("Malachi", 4),
            Map.entry("Matthew", 28), Map.entry("Mark", 16), Map.entry("Luke", 24),
            Map.entry("John", 21), Map.entry("Acts", 28), Map.entry("Romans", 16),
            Map.entry("1 Corinthians", 16), Map.entry("2 Corinthians", 13), Map.entry("Galatians", 6),
            Map.entry("Ephesians", 6), Map.entry("Philippians", 4), Map.entry("Colossians", 4),
            Map.entry("1 Thessalonians", 5), Map.entry("2 Thessalonians", 3), Map.entry("1 Timothy", 6),
            Map.entry("2 Timothy", 4), Map.entry("Titus", 3), Map.entry("Philemon", 1),
            Map.entry("Hebrews", 13), Map.entry("James", 5), Map.entry("1 Peter", 5),
            Map.entry("2 Peter", 3), Map.entry("1 John", 5), Map.entry("2 John", 1),
            Map.entry("3 John", 1), Map.entry("Jude", 1), Map.entry("Revelation", 22)
    );

    private final ComboBox<String> translationCombo;
    private final ComboBox<String> bookCombo;
    private final Spinner<Integer> chapterSpinner;
    private final TextArea chapterDisplay;
    private final BibleApiClient apiClient = new BibleApiClient();

    public ReadingPage() {
        getStyleClass().add("page");
        setSpacing(20);

        Label title = new Label("Read");
        title.getStyleClass().add("page-title");

        // Selector row for translation, book, and chapter
        HBox selectorRow = new HBox(12);
        selectorRow.setAlignment(Pos.CENTER_LEFT);

        translationCombo = new ComboBox<>();
        translationCombo.setPromptText("Translation");
        translationCombo.setPrefWidth(140);
        translationCombo.getStyleClass().add("selector-combo");
        translationCombo.getItems().setAll(TRANSLATIONS);

        bookCombo = new ComboBox<>();
        bookCombo.setPromptText("Book");
        bookCombo.setPrefWidth(180);
        bookCombo.getStyleClass().add("selector-combo");
        bookCombo.getItems().setAll(BOOKS);

        // Keep a reference to the factory so we can update its max when the book changes
        SpinnerValueFactory.IntegerSpinnerValueFactory spinnerFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 150, 1);
        chapterSpinner = new Spinner<>(spinnerFactory);
        chapterSpinner.setPrefWidth(70);
        chapterSpinner.getStyleClass().add("chapter-spinner");

        // When the user picks a book, cap the spinner to that book's chapter count.
        // If the current chapter is now out of range, drop it down to the new max.
        bookCombo.valueProperty().addListener((obs, oldBook, newBook) -> {
            if (newBook == null) return;
            int maxChapters = CHAPTER_COUNTS.getOrDefault(newBook, 1);
            spinnerFactory.setMax(maxChapters);
            if (chapterSpinner.getValue() > maxChapters) {
                spinnerFactory.setValue(maxChapters);
            }
        });

        selectorRow.getChildren().addAll(translationCombo, bookCombo, chapterSpinner);

        // Text area for scripture display
        chapterDisplay = new TextArea();
        chapterDisplay.setEditable(false);
        chapterDisplay.setWrapText(true);
        chapterDisplay.setPrefRowCount(20);
        chapterDisplay.getStyleClass().add("chapter-display");

        VBox displayContainer = new VBox(chapterDisplay);
        VBox.setVgrow(displayContainer, Priority.ALWAYS);
        displayContainer.setFillWidth(true);

        getChildren().addAll(title, selectorRow, displayContainer);

        translationCombo.valueProperty().addListener((obs, oldV, newV) -> fetchAndPrint());
        bookCombo.valueProperty().addListener((obs, oldV, newV) -> fetchAndPrint());
        chapterSpinner.valueProperty().addListener((obs, oldV, newV) -> fetchAndPrint());
    }

    private void fetchAndPrint() {
        String translation = translationCombo.getValue();
        String book = bookCombo.getValue();
        Integer chapter = chapterSpinner.getValue();
        if (translation == null || book == null || chapter == null) {
            return;
        }
        String reference = book + " " + chapter;

        Thread t = new Thread(() -> {
            System.out.println("=== Reading: " + reference + " (" + translation + ") ===");
            try {
                BiblePassage passage = apiClient.getPassage(reference, translation);
                System.out.println(passage);
            } catch (BibleApiException e) {
                System.err.println("ERROR: " + e.getMessage());
            }
        }, "bible-api-fetch");
        t.setDaemon(true);
        t.start();
    }
}