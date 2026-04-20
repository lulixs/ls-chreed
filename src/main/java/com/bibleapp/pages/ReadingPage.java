package com.bibleapp.pages;

import com.bibleapp.services.BibleApiClient;
import com.bibleapp.services.BibleApiException;
import com.bibleapp.services.BiblePassage;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

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

        chapterSpinner = new Spinner<>(1, 150, 1);
        chapterSpinner.setPrefWidth(70);
        chapterSpinner.getStyleClass().add("chapter-spinner");

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
