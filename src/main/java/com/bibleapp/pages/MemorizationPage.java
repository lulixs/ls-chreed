package com.bibleapp.pages;

import java.util.ArrayList;
import java.util.List;

import com.bibleapp.badges.Badge;
import com.bibleapp.badges.BadgeService;
import com.bibleapp.badges.BadgeToast;
import com.bibleapp.data.DataStore;
import com.bibleapp.data.MemorizedVerse;
import com.bibleapp.difficulty.VerseDifficultyServer;
import com.bibleapp.services.BibleApiClient;
import com.bibleapp.services.BiblePassage;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;

import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class MemorizationPage extends VBox {

    private static final String[] DIFFICULTY_LABELS = {
        "Copy-down",
        "Every-other A",
        "Every-other B",
        "Full-memory"
    };

    private static final String[] BIBLE_BOOK_ORDER = {
        "Genesis","Exodus","Leviticus","Numbers","Deuteronomy",
        "Joshua","Judges","Ruth","1 Samuel","2 Samuel",
        "1 Kings","2 Kings","1 Chronicles","2 Chronicles",
        "Ezra","Nehemiah","Esther","Job","Psalms","Proverbs",
        "Ecclesiastes","Song of Songs","Isaiah","Jeremiah",
        "Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos",
        "Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah",
        "Haggai","Zechariah","Malachi","Matthew","Mark","Luke",
        "John","Acts","Romans","1 Corinthians","2 Corinthians",
        "Galatians","Ephesians","Philippians","Colossians",
        "1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy",
        "Titus","Philemon","Hebrews","James","1 Peter","2 Peter",
        "1 John","2 John","3 John","Jude","Revelation"
    };

    private static final Map<String, Map<Integer, Integer>> BIBLE = loadBibleStructure();

    private static Map<String, Map<Integer, Integer>> loadBibleStructure() {
        try (InputStream is = openBibleStructureStream()) {
            if (is == null) {
                throw new IOException("BibleStructure.json was not found on the classpath or in project resources.");
            }

            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(new InputStreamReader(is, StandardCharsets.UTF_8));
            Map<String, Map<Integer, Integer>> bible = new HashMap<>();

            for (Object bookKey : root.keySet()) {
                String book = (String) bookKey;
                JSONObject chapters = (JSONObject) root.get(book);

                Map<Integer, Integer> chapterMap = new HashMap<>();

                for (Object chapterKey : chapters.keySet()) {
                    int chapter = Integer.parseInt((String) chapterKey);
                    int verses = ((Long) chapters.get(chapterKey)).intValue();
                    chapterMap.put(chapter, verses);
                }

                bible.put(book, chapterMap);
            }

            return bible;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Bible structure", e);
        }
    }

    private static InputStream openBibleStructureStream() throws IOException {
        InputStream classResource = MemorizationPage.class.getResourceAsStream("/com/bibleapp/BibleStructure.json");
        if (classResource != null) {
            return classResource;
        }

        ClassLoader classLoader = MemorizationPage.class.getClassLoader();
        if (classLoader != null) {
            InputStream loaderResource = classLoader.getResourceAsStream("com/bibleapp/BibleStructure.json");
            if (loaderResource != null) {
                return loaderResource;
            }
        }

        Path[] fallbackPaths = new Path[] {
            Path.of("src", "main", "resources", "com", "bibleapp", "BibleStructure.json"),
            Path.of("resources", "com", "bibleapp", "BibleStructure.json"),
            Path.of("target", "classes", "com", "bibleapp", "BibleStructure.json")
        };

        for (Path path : fallbackPaths) {
            if (Files.exists(path)) {
                return Files.newInputStream(path);
            }
        }

        return null;
    }

    private final VBox leftScrollContent;
    private final StackPane appRoot;
    private Runnable currentClosePopupHandler;
    private VerseDifficultyServer difficulty;

    private static final String[] DIFFICULTY_COLORS = { "#1D9E75", "#378ADD", "#BA7517", "#D85A30", "#555555" };

    private int currentDifficulty = 0;   // 0-indexed
    private Button prevBtn, nextBtn;
    private Label diffNameLabel;
    private Label diffLockedLabel;
    private VBox rightColumn;
    private MemorizedVerse currentVerse;
    private final Rectangle[] pips = new Rectangle[4];

    public MemorizationPage(StackPane appRoot) {
        this.appRoot = appRoot;
        getStyleClass().add("page");
        setSpacing(20);

        difficulty = new VerseDifficultyServer();

        Label title = new Label("Memorize");
        title.getStyleClass().add("page-title");

        HBox columnsContainer = new HBox(20);
        columnsContainer.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(columnsContainer, Priority.ALWAYS);
        VBox.setVgrow(columnsContainer, Priority.ALWAYS);

        VBox leftColumn = new VBox(10);
        leftColumn.getStyleClass().add("memorize-left-column");
        leftColumn.setPrefWidth(200);
        leftColumn.setMinWidth(150);

        // Scrollable content for verse cards
        Label leftLabel = new Label("My Verses");
        leftLabel.getStyleClass().add("column-header");
        leftColumn.getChildren().add(leftLabel);

        leftScrollContent = new VBox(10);
        leftScrollContent.getStyleClass().add("memorize-scroll-content");
        leftScrollContent.setPadding(new Insets(10));

        ScrollPane leftScrollPane = new ScrollPane(leftScrollContent);
        leftScrollPane.getStyleClass().add("memorize-scroll");
        leftScrollPane.setFitToWidth(true);
        leftScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(leftScrollPane, Priority.ALWAYS);
        leftColumn.getChildren().add(leftScrollPane);

        Button addButton = new Button("+ Add Verse");
        addButton.getStyleClass().add("add-verse-btn");
        addButton.setOnAction(e -> showAddVersePopup());
        HBox buttonContainer = new HBox(addButton);
        buttonContainer.setAlignment(Pos.CENTER);
        leftColumn.getChildren().add(buttonContainer);

        // Right column - Difficulty selection + practice area
        rightColumn = new VBox(10);
        rightColumn.getStyleClass().add("memorize-right-column");
        rightColumn.setPadding(new Insets(10));

        Label difficultyLabel = new Label("Select a difficulty to begin practice");
        difficultyLabel.getStyleClass().add("column-header");

        showDifficultySelector();

        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        HBox.setHgrow(leftColumn, Priority.NEVER);

        columnsContainer.getChildren().addAll(leftColumn, rightColumn);
        VBox.setVgrow(columnsContainer, Priority.ALWAYS);

        getChildren().addAll(title, columnsContainer);
        loadVerseList();
    }


    private MemorizedVerse getSelectedVerse() {
        return currentVerse;
    }

    /**
     * Marks the given card as the active selection and re-renders the right
     * column. Clears the highlight from any previously-selected card.
     */
    private void selectVerse(MemorizedVerse verse, VBox card) {
        currentVerse = verse;
        currentDifficulty = Math.min(verse.getNextDifficulty(), 3);

        for (javafx.scene.Node child : leftScrollContent.getChildren()) {
            child.getStyleClass().remove("verse-card--selected");
        }
        card.getStyleClass().add("verse-card--selected");

        showDifficultySelector();
    }

    private HBox buildDifficultySelector() {
        // --- Pip column ---
        VBox pipColumn = new VBox(6);
        pipColumn.setAlignment(Pos.BOTTOM_CENTER);
        pipColumn.getStyleClass().add("difficulty-pip-column");

        for (int i = 3; i >= 0; i--) {
            Rectangle pip = new Rectangle(18, 18);
            pip.setArcWidth(6);
            pip.setArcHeight(6);

            pips[i] = pip;

            StackPane pipWrapper = new StackPane(pip);
            pipWrapper.setMinSize(18, 18);
            pipWrapper.setMaxSize(18, 18);
            pipColumn.getChildren().add(pipWrapper);
        }

        // --- Selector bar ---
        prevBtn = new Button("\u2190");
        prevBtn.getStyleClass().add("diff-arrow-btn");
        prevBtn.setOnAction(e -> cycleLeft());

        nextBtn = new Button("\u2192");
        nextBtn.getStyleClass().add("diff-arrow-btn");
        nextBtn.setOnAction(e -> cycleRight());

        diffNameLabel = new Label();
        diffNameLabel.getStyleClass().add("diff-name-label");

        diffLockedLabel = new Label();
        diffLockedLabel.getStyleClass().add("diff-locked-label");

        VBox textStack = new VBox(2, diffNameLabel, diffLockedLabel);
        textStack.setAlignment(Pos.CENTER);
        HBox.setHgrow(textStack, Priority.ALWAYS);

        HBox selectorBar = new HBox(10, prevBtn, textStack, nextBtn);
        selectorBar.setAlignment(Pos.CENTER);
        selectorBar.getStyleClass().add("diff-selector-bar");
        HBox.setHgrow(selectorBar, Priority.ALWAYS);

        // Sets selected verse and difficulty
        currentVerse = getSelectedVerse();
        difficulty.setDifficulty(currentVerse, currentDifficulty);

        // Button used to start a memorization task
        Button startBtn = new Button("Start");
        startBtn.getStyleClass().add("add-verse-btn");
        startBtn.setOnAction(e -> startTask());

        // --- Outer row ---
        Label sectionLabel = new Label("Difficulty");
        sectionLabel.getStyleClass().add("diff-section-label");

        VBox selectorColumn = new VBox(8, sectionLabel, selectorBar, startBtn);
        selectorColumn.setAlignment(Pos.CENTER);
        HBox.setHgrow(selectorColumn, Priority.ALWAYS);

        HBox row = new HBox(16, selectorColumn, pipColumn);
        row.setAlignment(Pos.BOTTOM_LEFT);
        row.getStyleClass().add("diff-selector-row");

        refreshDifficultyUI();

        return row;
    }

    // Refreshes Difficulty UI
    private void refreshDifficultyUI() {
        for (int i = 0; i < 4; i++) {

            String color;
            // If difficulty is completed, color its pip in
            if(i < currentVerse.getNextDifficulty()) {
                color = DIFFICULTY_COLORS[i];
            }
            else {
                // If difficulty is yet to be completed, color its pip gray
                color = DIFFICULTY_COLORS[4];

                // If difficulty is locked, make its pip small
                if(i != currentVerse.getNextDifficulty()){
                    pips[i].setWidth(9);
                    pips[i].setHeight(9);
                }
            }
            
            pips[i].setFill(javafx.scene.paint.Color.web(color));

            // If difficulty is currently selected, highlight its pip
            if (i == currentDifficulty) {
                pips[i].setStroke(javafx.scene.paint.Color.web("#000000"));
                pips[i].setStrokeWidth(2.5);
            } else {
                pips[i].setStroke(javafx.scene.paint.Color.TRANSPARENT);
                pips[i].setStrokeWidth(2.5);
            }
        }

        diffNameLabel.setText(MemorizedVerse.getDifficultyLabel(currentDifficulty));
        diffLockedLabel.setText(currentVerse.getNextDifficulty() == currentDifficulty ? "Incomplete" : "Completed");

        prevBtn.setDisable(currentDifficulty == 0);
        nextBtn.setDisable(currentDifficulty == Math.min(currentVerse.getNextDifficulty(), 3));
    }

    private List<Label> wordLabels = new ArrayList<>();
    private int currentWordIndex = 0;
    private boolean[] wordCorrect;

    private void startTask() {
        // Resets right column
        rightColumn.getChildren().clear();

        // Sets selected verse and difficulty
        currentVerse = getSelectedVerse();
        difficulty.setDifficulty(currentVerse, currentDifficulty);
        
        // Adds a back button to return to difficulty selector
        Button backBtn = new Button("\u2190 Back");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> showDifficultySelector());

        HBox topBar = new HBox(backBtn);
        topBar.setAlignment(Pos.TOP_LEFT);

        // Build word labels
        wordLabels.clear();
        currentWordIndex = 0;

        // TextFlow displays verse
        TextFlow verseFlow = new TextFlow();
        verseFlow.setLineSpacing(6);
        verseFlow.getStyleClass().add("verse-text-flow");

        String[] words = difficulty.getDisplayVerse();
        for (int i = 0; i < words.length; i++) {

            // Adds word to verse display
            Label wordLabel = new Label(words[i]);
            wordLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333;");
            wordLabels.add(wordLabel);
            verseFlow.getChildren().add(wordLabel);

            // Add a space between words
            if (i < words.length - 1) {
                Label space = new Label(" ");
                space.setStyle("-fx-font-size: 16px;");
                verseFlow.getChildren().add(space);
            }
        }   

        wordCorrect = new boolean[words.length];

        // Input field (Purely to listen for input, nothing will be dispalyed)
        TextField inputField = new TextField();
        inputField.getStyleClass().add("verse-input");
        inputField.setPromptText("Type the first letter of each word...");

        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                // If advanceWord fails, then verse must be complete
                if(!advanceWord(inputField)) {
                    onVerseComplete();
                }
            }
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        rightColumn.getChildren().addAll(topBar, verseFlow, spacer, inputField);

        // Highlights first word
        highlightWord(0);

        // Focus the input after the scene has laid out
        javafx.application.Platform.runLater(inputField::requestFocus);
    }

    private void highlightWord(int index) {
        for (int i = 0; i < wordLabels.size(); i++) {
            Label lbl = wordLabels.get(i);
            if (i < index) {
                lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #888888;");
            } else if (i == index) {
                lbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #000000; -fx-background-color: #FFF3CD; -fx-background-radius: 3px; -fx-padding: 1 3 1 3;");
            } else {
                lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333;");
            }
        }
    }

    private boolean advanceWord(TextField inputField) {

        String[] key = difficulty.getAnswerKey();

        // Compare the first *letter or digit* in each string, case-insensitive.
        // Skipping leading punctuation lets users answer "L" for an answer
        // word like "Let or '(Let — they shouldn't have to type the quote.
        String typed = inputField.getText();
        if (!typed.isEmpty()) {
            Character typedFirst = firstAlnum(typed);
            Character keyFirst   = firstAlnum(key[currentWordIndex]);
            wordCorrect[currentWordIndex] = typedFirst != null && keyFirst != null
                    && Character.toLowerCase(typedFirst) == Character.toLowerCase(keyFirst);
        }
        inputField.clear();
        currentWordIndex++;

        // If there is a word after the current one, highlight it. Otherwise, report failure
        if (currentWordIndex < wordLabels.size()) {
            highlightWord(currentWordIndex);
            return true;
        } else {
            return false;
        }
    }

    private void onVerseComplete() {

        rightColumn.getChildren().clear();

        String[] key = difficulty.getAnswerKey();

        // Adds button to return to difficulty selector
        Button backBtn = new Button("\u2190 Back");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> showDifficultySelector());

        HBox topBar = new HBox(backBtn);
        topBar.setAlignment(Pos.TOP_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Region spacerBottom = new Region();
        VBox.setVgrow(spacerBottom, Priority.ALWAYS);

        // Displays "Complete!" because they completed it
        Label scoreTitle = new Label("Complete!");
        scoreTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333333;");

        int numCorrect = countCorrect(wordCorrect);

        Label scoreLabel = new Label(numCorrect + "/" + key.length);
        scoreLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: " + getScoreColor(numCorrect, key.length) + ";");

        Label scoreSubtitle = new Label(getScoreMessage(numCorrect, key.length));
        scoreSubtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #888888;");

        VBox scoreBox = new VBox(8, scoreTitle, scoreLabel, scoreSubtitle);
        scoreBox.setAlignment(Pos.CENTER);

        TextFlow verseReview = new TextFlow();
        verseReview.setLineSpacing(6);
        for (int i = 0; i < key.length; i++) {
            Label wordLabel = new Label(key[i]);

            // If correct, the word will be green, otherwise orange
            String color = wordCorrect[i] ? "#1D9E75" : "#D85A30";
            wordLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            verseReview.getChildren().add(wordLabel);

            // Adds spaces in between words
            if (i < key.length - 1) {
                Label space = new Label(" ");
                space.setStyle("-fx-font-size: 16px;");
                verseReview.getChildren().add(space);
            }
        }

        Button actionBtn;
        if (numCorrect == key.length) {
            // If the difficulty is passed, the button will return the user to the difficulty selector
            actionBtn = new Button("Continue");
            actionBtn.setOnAction(e -> showDifficultySelector());
            // If the current difficulty is the max difficulty, then the max difficulty is raised
            if(currentDifficulty == currentVerse.getNextDifficulty()) {
                unlockNextDifficulty(currentVerse);

                // Automatically selects the new max difficulty
                if(currentDifficulty < 3)
                    currentDifficulty++;
            }
        } else {
            // If the difficulty is failed, the button will allow the user to try again
            actionBtn = new Button("Retry");
            actionBtn.setOnAction(e -> startTask());
        }

        actionBtn.getStyleClass().add("add-verse-btn");

        HBox actionBar = new HBox(actionBtn);
        actionBar.setAlignment(Pos.CENTER);

        rightColumn.getChildren().addAll(topBar, spacer, scoreBox, verseReview, spacerBottom, actionBar);
    }


    /** First letter or digit in {@code s}, or null if there is none. */
    private static Character firstAlnum(String s) {
        if (s == null) return null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) return c;
        }
        return null;
    }

    // Counts the number of true (correct) values in the array
    private int countCorrect(boolean[] arr) {
        int count = 0;
        for(int i = 0; i < arr.length; i++) {
            if(arr[i]) {
                count++;
            }
        }
        return count;
    }

    // Changes the color of the score based on if it exeeds certain thresholds (100% -> 50% -> 0%)
    private String getScoreColor(int score, int total) {
        if (score >= total) return "#1D9E75";  // green
        if (score * 2 >= total) return "#BA7517";  // amber
        return "#D85A30";                   // red
    }

    // Changes the message on completiong if the score exeeds certain thresholds (100% -> 50% -> 0%)
    private String getScoreMessage(int score, int total) {
        if (score >= total) return "Great work!";
        if (score * 2 >= total) return "Keep practicing!";
        return "Don't give up!";
    }

    // Method used to return to the difficulty selector screen
    private void showDifficultySelector() {
        rightColumn.getChildren().clear();

        if (currentVerse == null) {
            Label placeholder = new Label("Select a verse from the left to begin practice.");
            placeholder.getStyleClass().add("memorize-placeholder");
            placeholder.setWrapText(true);
            VBox box = new VBox(placeholder);
            box.setAlignment(Pos.CENTER);
            VBox.setVgrow(box, Priority.ALWAYS);
            rightColumn.getChildren().add(box);
            return;
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        rightColumn.getChildren().addAll(spacer, buildDifficultySelector());
    }

    private void cycleLeft() {
        // Doesn't let the user go lower than the lowest difficulty
        if (currentDifficulty > 0) {
            currentDifficulty--;
            refreshDifficultyUI();
        }
    }

    private void cycleRight() {
        // Doesn't let the user go higher than the current or absolute max difficulty 
        if (currentDifficulty < Math.min(currentVerse.getNextDifficulty(), 3)) {
            currentDifficulty++;
            refreshDifficultyUI();
        }
    }

    // Call this to unlock the next difficulty at runtime
    public void unlockNextDifficulty(MemorizedVerse verse) {
        java.util.Set<String> earnedBefore = BadgeService.snapshotEarnedIds();

        if(currentVerse.getNextDifficulty() < 4) {
            verse.setNextDifficulty(verse.getNextDifficulty() + 1);
            // Persist so badge evaluation (and the next session's verse list) sees the new state.
            DataStore.updateVerseDifficulty(verse.getId(), verse.getNextDifficulty());
        }

        java.util.List<Badge> newlyEarned = BadgeService.diffEarnedSince(earnedBefore);
        if (!newlyEarned.isEmpty()) {
            BadgeToast.show(appRoot, newlyEarned);
        }

        // Rebuild the left-column cards so their dropdowns reflect the new difficulty.
        loadVerseList();
        refreshDifficultyUI();
    }

    // Returns the currently displayed difficulty (0-indexed)

    // --- Popup logic ---
    // =========================================================================
    // Data helpers
    // =========================================================================

    /** Reads all verses from DataStore and rebuilds the card list. */
    private void loadVerseList() {
        leftScrollContent.getChildren().clear();
        List<MemorizedVerse> verses = DataStore.getMemorizationList();
        if (verses.isEmpty()) {
            Label empty = new Label("No verses yet. Tap '+ Add Verse'.");
            empty.getStyleClass().add("popup-placeholder");
            leftScrollContent.getChildren().add(empty);
        } else {
            for (MemorizedVerse verse : verses) {
                leftScrollContent.getChildren().add(buildVerseCard(verse));
            }
        }
    }

    private VBox buildVerseCard(MemorizedVerse verse) {
        VBox card = new VBox(4);
        card.getStyleClass().add("verse-card");
        card.setPadding(new Insets(8));
        if (currentVerse != null && currentVerse.getId().equals(verse.getId())) {
            card.getStyleClass().add("verse-card--selected");
        }
        card.setOnMouseClicked(e -> selectVerse(verse, card));

        Label refLabel = new Label(verse.getReference());
        refLabel.getStyleClass().add("verse-card-reference");
        refLabel.setWrapText(true);

        String preview = verse.getText();
        if (preview != null && preview.length() > 60) {
            preview = preview.substring(0, 60).stripTrailing() + "…";
        }
        Label previewLabel = new Label(preview);
        previewLabel.getStyleClass().add("verse-card-preview");
        previewLabel.setWrapText(true);

        ComboBox<String> diffBox = new ComboBox<>();
        diffBox.getItems().addAll(DIFFICULTY_LABELS);
        diffBox.setMaxWidth(Double.MAX_VALUE);
        int diffIndex = Math.max(0, Math.min(verse.getNextDifficulty(), DIFFICULTY_LABELS.length - 1));
        diffBox.getSelectionModel().select(diffIndex);
        diffBox.setOnAction(e -> {
            int selectedIndex = diffBox.getSelectionModel().getSelectedIndex();
            DataStore.updateVerseDifficulty(verse.getId(), selectedIndex + 1);
        });
        // Don't let interacting with the dropdown also "select" the card.
        diffBox.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);

        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("remove-verse-btn");
        removeBtn.setOnAction(e -> {
            DataStore.removeVerse(verse.getId());
            if (currentVerse != null && currentVerse.getId().equals(verse.getId())) {
                currentVerse = null;
                showDifficultySelector();
            }
            loadVerseList();
        });
        removeBtn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);

        card.getChildren().addAll(refLabel, previewLabel, diffBox, removeBtn);
        return card;
    }

    private void showAddVersePopup() {

        StackPane popupOverlay = new StackPane();
        popupOverlay.getStyleClass().add("popup-overlay");
        popupOverlay.setOnMouseClicked(e -> closePopup());
    
        VBox popupContainer = new VBox(8);
        popupContainer.getStyleClass().add("memorize-popup");
        popupContainer.setPadding(new Insets(10));
        popupContainer.setMaxWidth(380);
        popupContainer.setMinWidth(320);
        popupContainer.setMaxHeight(200);   // 👈 controls height
        popupContainer.setPrefHeight(180);  // optional fine-tune
    
        Label title = new Label("Add Verse");
        Button closeBtn = new Button("X");
        closeBtn.setOnAction(e -> closePopup());
    
        HBox header = new HBox(title, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
    
        ComboBox<String> bookBox = new ComboBox<>();
        for (String book : BIBLE_BOOK_ORDER) {
            if (BIBLE.containsKey(book)) {
                bookBox.getItems().add(book);
            }
        }
        bookBox.setPromptText("Book");
    
        Spinner<Integer> chapterSpinner = new Spinner<>(1, 150, 1);
        Spinner<Integer> verseSpinner = new Spinner<>(1, 200, 1);
    
        chapterSpinner.setPrefWidth(90);
        verseSpinner.setPrefWidth(90);
    
        Label error = new Label();
        error.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
    
        Button saveBtn = new Button("Save");
        saveBtn.setOnAction(e -> {

            error.setText("");

            String book = bookBox.getValue();
            int chapter = chapterSpinner.getValue();
            int verse = verseSpinner.getValue();

            if (book == null) {
                error.setText("Select a book.");
                return;
            }

            Map<Integer, Integer> chapters = BIBLE.get(book);

            if (chapters == null || !chapters.containsKey(chapter)) {
                error.setText("Invalid chapter for " + book);
                return;
            }

            int maxVerse = chapters.get(chapter);

            if (verse < 1 || verse > maxVerse) {
                error.setText("Chapter " + chapter + " has 1-" + maxVerse + " verses.");
                return;
            }

            // Fetch the verse text from the Bible API on a background thread
            // so the UI stays responsive. Without text, a verse can't be
            // practiced — the difficulty server has nothing to test against.
            saveBtn.setDisable(true);
            saveBtn.setText("Fetching…");

            String reference = book + " " + chapter + ":" + verse;
            String translation = DataStore.getPreferredTranslation();
            final String chosenTranslation = (translation == null || translation.isBlank()) ? "web" : translation;
            final String bookFinal = book;
            final int chapterFinal = chapter;
            final int verseFinal = verse;

            javafx.concurrent.Task<String> fetchTask = new javafx.concurrent.Task<>() {
                @Override
                protected String call() throws Exception {
                    BiblePassage passage = new BibleApiClient().getPassage(reference, chosenTranslation);
                    String text = passage.getText();
                    return text == null ? "" : text.trim();
                }
            };

            fetchTask.setOnSucceeded(ev -> {
                String text = fetchTask.getValue();
                if (text == null || text.isBlank()) {
                    error.setText("Couldn't fetch verse text. Try again.");
                    saveBtn.setDisable(false);
                    saveBtn.setText("Save");
                    return;
                }
                MemorizedVerse verseObj = new MemorizedVerse(bookFinal, chapterFinal, verseFinal, text, 0);
                DataStore.addVerse(verseObj);
                loadVerseList();
                closePopup();
            });

            fetchTask.setOnFailed(ev -> {
                Throwable ex = fetchTask.getException();
                error.setText("Fetch failed: " + (ex == null ? "unknown error" : ex.getMessage()));
                saveBtn.setDisable(false);
                saveBtn.setText("Save");
            });

            Thread t = new Thread(fetchTask, "verse-fetch");
            t.setDaemon(true);
            t.start();
        });
    
        VBox content = new VBox(8,
            bookBox,
            new HBox(8, new Label("Ch"), chapterSpinner,
                          new Label("V"), verseSpinner),
            error,
            saveBtn
        );
    
        popupContainer.getChildren().addAll(header, content);
    
        StackPane wrapper = new StackPane(popupContainer);
        wrapper.setAlignment(Pos.CENTER);

        // prevents full-screen stretching
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    
        appRoot.getChildren().addAll(popupOverlay, wrapper);
    
        currentClosePopupHandler = () ->
            appRoot.getChildren().removeAll(popupOverlay, wrapper);
    }

    private void closePopup() {
        if (currentClosePopupHandler != null) {
            currentClosePopupHandler.run();
            currentClosePopupHandler = null;
        }
    }
}
