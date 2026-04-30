package com.bibleapp.pages;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;

public class MemorizationPage extends VBox {

    private static final String WORD_STYLE_BASE =
        "-fx-font-size: 16px; -fx-font-family: \"Consolas\", \"Courier New\", monospace; -fx-padding: 1 3 1 3; -fx-background-radius: 3px;";
    private static final String WORD_STYLE_DEFAULT =
        WORD_STYLE_BASE + "-fx-text-fill: #333333; -fx-background-color: transparent;";
    private static final String WORD_STYLE_PAST =
        WORD_STYLE_BASE + "-fx-text-fill: #888888; -fx-background-color: transparent;";
    private static final String WORD_STYLE_ACTIVE =
        WORD_STYLE_BASE + "-fx-font-weight: bold; -fx-text-fill: #000000; -fx-background-color: #FFF3CD;";

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
        leftColumn.setMaxWidth(200);

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
        rightColumn.setMinWidth(0);
        rightColumn.setMaxWidth(Double.MAX_VALUE);

        Label difficultyLabel = new Label("Select a difficulty to begin practice");
        difficultyLabel.getStyleClass().add("column-header");

        showDifficultySelector();

        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        HBox.setHgrow(leftColumn, Priority.NEVER);

        rightColumn.setMaxWidth(Double.MAX_VALUE);

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

        // Sets difficulty for the currently selected verse
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

        // Guard: do nothing if no verse is selected
        if (currentVerse == null) {
            showDifficultySelector();
            return;
        }

        // Sets difficulty for the currently selected verse
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

        String[] words = difficulty.getDisplayVerse();

        TextFlow verseFlow = new TextFlow();
        verseFlow.setLineSpacing(6);
        verseFlow.getStyleClass().add("verse-text-flow");
        for (int i = 0; i < words.length; i++) {
            Label wordLabel = new Label(words[i]);
            wordLabel.setStyle(WORD_STYLE_DEFAULT);
            wordLabels.add(wordLabel);

            verseFlow.getChildren().add(wordLabel);

            if (i < words.length - 1) {
                Label space = new Label(" ");
                space.setStyle("-fx-font-size: 16px;");
                verseFlow.getChildren().add(space);
            }
        }

        wordCorrect = new boolean[words.length];

        // Input field (Purely to listen for input, nothing will be displayed)
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
                lbl.setStyle(WORD_STYLE_PAST);
            } else if (i == index) {
                lbl.setStyle(WORD_STYLE_ACTIVE);
            } else {
                lbl.setStyle(WORD_STYLE_DEFAULT);
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
            wordLabel.setStyle(WORD_STYLE_BASE + "-fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-background-color: transparent;");
            verseReview.getChildren().add(wordLabel);

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
        Region spacerBottom = new Region();
        VBox.setVgrow(spacerBottom, Priority.ALWAYS);

        // If no verse is selected, show a placeholder prompt
        if (currentVerse == null) {
            Label placeholder = new Label("Select a verse to begin.");
            placeholder.setStyle("-fx-font-size: 16px; -fx-text-fill: #888888;");
            placeholder.setAlignment(Pos.CENTER);
            placeholder.setMaxWidth(Double.MAX_VALUE);
            rightColumn.getChildren().addAll(spacer, placeholder, spacerBottom);
            return;
        }

        // If the verse text is missing (e.g. saved before API fetch was fixed),
        // fetch it now on a background thread then refresh.
        String verseText = currentVerse.getText();
        if (verseText == null || verseText.trim().isEmpty()) {
            Label loadingLabel = new Label("Loading verse text...");
            loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #888888;");
            loadingLabel.setAlignment(Pos.CENTER);
            loadingLabel.setMaxWidth(Double.MAX_VALUE);
            rightColumn.getChildren().addAll(spacer, loadingLabel, spacerBottom);

            // Parse book/chapter/verse back out of the reference string (e.g. "John 3:16")
            String ref = currentVerse.getReference(); // e.g. "John 3:16" or "1 Samuel 2:3"
            String fetchedBook = ref.contains(" ") ? ref.substring(0, ref.lastIndexOf(' ')) : ref;
            String chapterVerse = ref.contains(" ") ? ref.substring(ref.lastIndexOf(' ') + 1) : "1:1";
            int fetchedChapter = 1, fetchedVerse = 1;
            try {
                String[] parts = chapterVerse.split(":");
                fetchedChapter = Integer.parseInt(parts[0].trim());
                fetchedVerse = Integer.parseInt(parts[1].trim());
            } catch (Exception ignored) {}
            final String fb = fetchedBook;
            final int fc = fetchedChapter, fv = fetchedVerse;

            Thread fetchThread = new Thread(() -> {
                String fetched = fetchVerseTextFromApi(fb, fc, fv);
                javafx.application.Platform.runLater(() -> {
                    if (fetched != null && !fetched.trim().isEmpty()) {
                        // Replace the verse in DataStore with one that has the fetched text
                        int savedDifficulty = currentVerse.getNextDifficulty();
                        DataStore.removeVerse(currentVerse.getId());
                        MemorizedVerse updated = new MemorizedVerse(fb, fc, fv, fetched, savedDifficulty);
                        DataStore.addVerse(updated);
                        // Re-select the newly saved verse
                        List<MemorizedVerse> all = DataStore.getMemorizationList();
                        currentVerse = all.isEmpty() ? null : all.get(all.size() - 1);
                        loadVerseList();
                        showDifficultySelector(); // retry now that text is loaded
                    } else {
                        rightColumn.getChildren().clear();
                        Label errLabel = new Label("Could not load verse text. Check your connection.");
                        errLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #D85A30;");
                        errLabel.setAlignment(Pos.CENTER);
                        errLabel.setMaxWidth(Double.MAX_VALUE);
                        errLabel.setWrapText(true);
                        Region s = new Region(); VBox.setVgrow(s, Priority.ALWAYS);
                        Region sb = new Region(); VBox.setVgrow(sb, Priority.ALWAYS);
                        rightColumn.getChildren().addAll(s, errLabel, sb);
                    }
                });
            });
            fetchThread.setDaemon(true);
            fetchThread.start();
            return;
        }

        // Show the selected verse reference in large text above the difficulty selector
        Label verseRefLabel = new Label(currentVerse.getReference());
        verseRefLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        verseRefLabel.setAlignment(Pos.CENTER);
        verseRefLabel.setMaxWidth(Double.MAX_VALUE);
        verseRefLabel.setWrapText(true);

        rightColumn.getChildren().addAll(spacer, verseRefLabel, buildDifficultySelector());
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
        refLabel.setMaxWidth(Double.MAX_VALUE);

        String preview = verse.getText();
        if (preview != null && preview.length() > 60) {
            preview = preview.substring(0, 60).stripTrailing() + "…";
        }
        Label previewLabel = new Label(preview);
        previewLabel.getStyleClass().add("verse-card-preview");
        previewLabel.setWrapText(true);
        previewLabel.setMaxWidth(Double.MAX_VALUE);

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

        // Explicit Practice button — avoids all JavaFX event-bubbling issues
        // with ComboBox/Button consuming mouse clicks on the card container.
        Button practiceBtn = new Button("Practice");
        practiceBtn.getStyleClass().add("add-verse-btn");
        practiceBtn.setMaxWidth(Double.MAX_VALUE);
        practiceBtn.setOnAction(e -> {
            currentVerse = verse;
            currentDifficulty = Math.min(currentDifficulty, Math.max(0, verse.getNextDifficulty()));
            showDifficultySelector();
            leftScrollContent.getChildren().forEach(node -> node.getStyleClass().remove("verse-card-selected"));
            card.getStyleClass().add("verse-card-selected");
        });

        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("remove-verse-btn");
        removeBtn.setOnAction(e -> {
            DataStore.removeVerse(verse.getId());
            if (currentVerse != null && currentVerse.getId().equals(verse.getId())) {
                currentVerse = null;
                showDifficultySelector();
            }
            loadVerseList();
            showDifficultySelector();
        });
        removeBtn.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);

        card.getChildren().addAll(refLabel, previewLabel, diffBox, practiceBtn, removeBtn);
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
        title.getStyleClass().add("popup-title");

        HBox header = new HBox(title);
        header.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> bookBox = new ComboBox<>();
        for (String book : BIBLE_BOOK_ORDER) {
            if (BIBLE.containsKey(book)) {
                bookBox.getItems().add(book);
            }
        }
        bookBox.setPromptText("Book");

        Spinner<Integer> chapterSpinner = new Spinner<>(1, 150, 1);
        chapterSpinner.setEditable(true);
        Spinner<Integer> verseSpinner = new Spinner<>(1, 200, 1);
        verseSpinner.setEditable(true);

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

        currentClosePopupHandler = () -> {
            appRoot.getChildren().removeAll(popupOverlay, wrapper);
            appRoot.setOnKeyPressed(null);
        };

        appRoot.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                closePopup();
            }
        });
        appRoot.requestFocus();
    }

    // Fetches verse text from bible-api.com for the given reference
    private String fetchVerseTextFromApi(String book, int chapter, int verse) {
        try {
            String reference = book + " " + chapter + ":" + verse;
            String encodedReference = URLEncoder.encode(reference, StandardCharsets.UTF_8).replace("+", "%20");

            String apiUrl = "https://bible-api.com/" + encodedReference;

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(response.body());

            Object textObj = json.get("text");

            if (textObj == null) {
                return null;
            }

            return textObj.toString().replace("\n", " ").trim();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void closePopup() {
        if (currentClosePopupHandler != null) {
            currentClosePopupHandler.run();
            currentClosePopupHandler = null;
        }
    }
}