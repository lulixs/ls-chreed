package com.bibleapp.pages;

import com.bibleapp.data.DataStore;
import com.bibleapp.data.MemorizedVerse;
import com.bibleapp.services.BibleApiClient;
import com.bibleapp.services.BibleApiException;
import com.bibleapp.services.BiblePassage;
import com.bibleapp.services.BibleVerse;
import com.bibleapp.services.BibleTranslation;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;

/**
 * Bible reading page with selectors for translation, book, and chapter.
 * Verses are displayed as individual clickable rows — clicking a verse
 * adds it to the memorization list.
 */
public class ReadingPage extends VBox {

    private static final List<String> TRANSLATION_ORDER = List.of(
            "web", "kjv", "bbe", "darby", "asv", "dra"
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

    private static final List<BibleTranslation> FALLBACK_TRANSLATIONS = List.of(
            new BibleTranslation("asv", "American Standard Version (1901)", "English", "Public Domain", BOOKS),
            new BibleTranslation("bbe", "Bible in Basic English", "English", "Public Domain", BOOKS),
            new BibleTranslation("darby", "Darby Bible", "English", "Public Domain", BOOKS),
            new BibleTranslation("dra", "Douay-Rheims 1899 American Edition", "English", "Public Domain", BOOKS),
            new BibleTranslation("kjv", "King James Version", "English", "Public Domain", BOOKS),
            new BibleTranslation("web", "World English Bible", "English", "Public Domain", BOOKS)
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

    private final ComboBox<BibleTranslation> translationCombo;
    private final ComboBox<String> bookCombo;
    private final Spinner<Integer> chapterSpinner;

    // Verse rows are rendered here instead of a plain TextArea so they can be clicked
    private final VBox verseListContainer;

    private final BibleApiClient apiClient = new BibleApiClient();
    private final List<BibleTranslation> allTranslations = new ArrayList<>();
    private final PauseTransition fetchPause = new PauseTransition(Duration.millis(300));
    private final PauseTransition translationSearchPause = new PauseTransition(Duration.millis(900));
    private final StringBuilder translationSearchBuffer = new StringBuilder();
    private long fetchRequestId;
    private String selectedTranslationId = TRANSLATION_ORDER.get(0);

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
        translationCombo.setPrefWidth(340);
        translationCombo.setEditable(false);
        translationCombo.getStyleClass().add("selector-combo");
        translationCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(BibleTranslation item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayLabel());
            }
        });
        translationCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(BibleTranslation item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayLabel());
            }
        });

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

        // Hint label so users know they can click verses
        Label hint = new Label("Click a verse to add it to your memorization list.");
        hint.getStyleClass().add("verse-hint-label");

        // Each verse gets its own row inside this container
        verseListContainer = new VBox(4);
        verseListContainer.getStyleClass().add("verse-list-container");

        ScrollPane scrollPane = new ScrollPane(verseListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("chapter-display");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(title, selectorRow, hint, scrollPane);

        configureTranslationSearch();
        setTranslations(FALLBACK_TRANSLATIONS);
        fetchPause.setOnFinished(event -> fetchAndDisplayVerses());

        translationCombo.valueProperty().addListener((obs, oldV, newV) -> scheduleFetch());
        bookCombo.valueProperty().addListener((obs, oldV, newV) -> scheduleFetch());
        chapterSpinner.valueProperty().addListener((obs, oldV, newV) -> scheduleFetch());

        loadTranslations();
    }

    private void scheduleFetch() {
        fetchPause.playFromStart();
    }

    /**
     * Fetches the selected chapter from the API and renders each verse
     * as a clickable row in the verse list.
     */
    private void fetchAndDisplayVerses() {
        BibleTranslation translation = translationCombo.getValue();
        String book = bookCombo.getValue();
        Integer chapter = chapterSpinner.getValue();
        if (translation == null || book == null || chapter == null) return;

        if (!TRANSLATION_ORDER.contains(translation.identifier())) {
            showMessage("That translation is no longer available. Please choose a supported translation.");
            return;
        }

        String reference = book + " " + chapter;
        long requestId = ++fetchRequestId;
        showMessage("Loading " + reference + " in " + translation.displayLabel() + "...");

        Thread t = new Thread(() -> {
            try {
                BiblePassage passage = apiClient.getPassage(reference, translation.identifier());
                if (requestId != fetchRequestId) return;

                Platform.runLater(() -> {
                    if (requestId != fetchRequestId) return;
                    renderVerses(passage, book, chapter);
                });
            } catch (BibleApiException e) {
                if (requestId != fetchRequestId) return;
                Platform.runLater(() -> {
                    if (requestId != fetchRequestId) return;
                    showMessage(buildErrorText(e));
                });
            }
        }, "bible-api-fetch");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Clears the verse list and builds one clickable row per verse.
     * If the passage has no individual verse objects, falls back to
     * showing the raw passage text as a single unclickable block.
     */
    private void renderVerses(BiblePassage passage, String book, int chapter) {
        verseListContainer.getChildren().clear();

        // Header row: reference + translation name
        String header = passage.getReference();
        if (passage.getTranslationName() != null && !passage.getTranslationName().isBlank()) {
            header += " — " + passage.getTranslationName();
        }
        Label headerLabel = new Label(header);
        headerLabel.getStyleClass().add("verse-chapter-header");
        headerLabel.setWrapText(true);
        verseListContainer.getChildren().add(headerLabel);

        if (passage.getVerses().isEmpty()) {
            // API returned a text blob with no individual verses — show it as plain text
            Label fallback = new Label(passage.getText() == null ? "" : passage.getText().trim());
            fallback.setWrapText(true);
            fallback.getStyleClass().add("verse-row-text");
            verseListContainer.getChildren().add(fallback);
            return;
        }

        // Build one clickable row for every verse in the chapter
        for (BibleVerse verse : passage.getVerses()) {
            verseListContainer.getChildren().add(buildVerseRow(verse, book, chapter));
        }
    }

    /**
     * Builds a single verse row. Clicking it saves the verse to the
     * memorization list and briefly highlights the row as confirmation.
     */
    private HBox buildVerseRow(BibleVerse verse, String book, int chapter) {
        // Verse number badge on the left
        Label numberLabel = new Label(String.valueOf(verse.getVerse()));
        numberLabel.getStyleClass().add("verse-number-label");
        numberLabel.setMinWidth(30);
        numberLabel.setAlignment(Pos.TOP_RIGHT);

        // Verse text on the right
        Label textLabel = new Label(verse.getText() == null ? "" : verse.getText().trim());
        textLabel.setWrapText(true);
        textLabel.getStyleClass().add("verse-row-text");
        HBox.setHgrow(textLabel, Priority.ALWAYS);

        HBox row = new HBox(8, numberLabel, textLabel);
        row.setPadding(new Insets(6, 8, 6, 8));
        row.getStyleClass().add("verse-row");
        row.setAlignment(Pos.TOP_LEFT);

        // Clicking a verse row adds it to the memorization list
        row.setOnMouseClicked(e -> addVerseToMemorization(verse, book, chapter, row));

        return row;
    }

    /**
     * Saves the clicked verse to the memorization list via DataStore.
     * Skips duplicates (DataStore deduplicates by book.chapter.verse ID).
     * Applies a brief CSS highlight so the user gets visual feedback.
     */
    private void addVerseToMemorization(BibleVerse verse, String book, int chapter, HBox row) {
        MemorizedVerse memorizedVerse = new MemorizedVerse(
                book,
                chapter,
                verse.getVerse(),
                verse.getText() == null ? "" : verse.getText().trim(),
                MemorizedVerse.DIFFICULTY_COPY_DOWN  // default difficulty; user can change it later
        );

        DataStore.addVerse(memorizedVerse);

        // Flash the row green so the user knows it was added
        row.getStyleClass().add("verse-row-added");
        PauseTransition highlight = new PauseTransition(Duration.millis(1200));
        highlight.setOnFinished(e -> row.getStyleClass().remove("verse-row-added"));
        highlight.play();
    }

    /**
     * Replaces the verse list with a single status/error message.
     * Used while loading or when something goes wrong.
     */
    private void showMessage(String message) {
        verseListContainer.getChildren().clear();
        Label label = new Label(message);
        label.setWrapText(true);
        label.getStyleClass().add("verse-row-text");
        verseListContainer.getChildren().add(label);
    }

    private void loadTranslations() {
        Thread loader = new Thread(() -> {
            try {
                List<BibleTranslation> translations = apiClient.getTranslationsSupportingBooks(BOOKS);
                Platform.runLater(() -> setTranslations(translations.isEmpty() ? FALLBACK_TRANSLATIONS : translations));
            } catch (BibleApiException e) {
                Platform.runLater(() -> showMessage(
                        "Using built-in translation list because live translation metadata could not be loaded.\n\n"
                                + e.getMessage()
                ));
            }
        }, "translation-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void configureTranslationSearch() {
        translationSearchPause.setOnFinished(event -> translationSearchBuffer.setLength(0));

        translationCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) selectedTranslationId = newValue.identifier();
        });

        translationCombo.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            String character = event.getCharacter();
            if (character == null || character.isBlank() || Character.isISOControl(character.charAt(0))) return;

            translationSearchBuffer.append(character.toLowerCase());
            translationSearchPause.playFromStart();
            findTranslation(translationSearchBuffer.toString()).ifPresent(translationCombo::setValue);
            event.consume();
        });
    }

    private void setTranslations(List<BibleTranslation> translations) {
        allTranslations.clear();
        allTranslations.addAll(translations.stream()
                .filter(t -> t.identifier() != null && TRANSLATION_ORDER.contains(t.identifier()))
                .toList());
        allTranslations.sort((a, b) -> Integer.compare(orderIndex(a), orderIndex(b)));
        translationCombo.setItems(FXCollections.observableArrayList(allTranslations));
        if (!allTranslations.isEmpty()) {
            BibleTranslation saved = findTranslation(selectedTranslationId).orElse(allTranslations.get(0));
            translationCombo.setValue(saved);
        }
    }

    private Optional<BibleTranslation> findTranslation(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        String normalized = text.trim();
        return allTranslations.stream()
                .filter(t -> t.displayLabel().equalsIgnoreCase(normalized)
                        || (t.name() != null && t.name().equalsIgnoreCase(normalized))
                        || (t.identifier() != null && t.identifier().equalsIgnoreCase(normalized)))
                .findFirst()
                .or(() -> allTranslations.stream()
                        .filter(t -> startsWithIgnoreCase(t.displayLabel(), normalized)
                                || startsWithIgnoreCase(t.name(), normalized)
                                || startsWithIgnoreCase(t.identifier(), normalized))
                        .findFirst())
                .or(() -> allTranslations.stream()
                        .filter(t -> t.matchesQuery(normalized))
                        .findFirst());
    }

    private String buildErrorText(BibleApiException error) {
        String message = error.getMessage();
        if (message != null && message.contains("HTTP 429")) {
            return "Unable to load passage.\n\nThe Bible API is rate-limiting requests right now. Please wait a moment and try again.";
        }
        return "Unable to load passage.\n\n" + message;
    }

    private int orderIndex(BibleTranslation translation) {
        int index = TRANSLATION_ORDER.indexOf(translation.identifier());
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private boolean startsWithIgnoreCase(String value, String prefix) {
        return value != null && value.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}