package com.bibleapp.pages;

import java.util.ArrayList;
import java.util.List;

import com.bibleapp.data.DataStore;
import com.bibleapp.data.MemorizedVerse;
import com.bibleapp.difficulty.VerseDifficultyServer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;

public class MemorizationPage extends VBox {

    private static final String[] DIFFICULTY_LABELS = {
        "Copy-down",
        "Every-other A",
        "Every-other B",
        "Full-memory"
    };

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


    //  TODO: Wire to left-column verse selection
    private MemorizedVerse getSelectedVerse() {
        // Debug code. Replace with actual code to get the current verse selected
        MemorizedVerse verse = new MemorizedVerse("John", 3, 16, "For God so loved the world that he gave his one and only Son, that whoever believes in him shall not perish but have eternal life.", 0);
        return currentVerse == null ? verse : currentVerse;
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

        // If first typed letter matches first letter of key word (case-insensitive), then mark it correct
        String typed = inputField.getText();
        if (!typed.isEmpty()) {
            wordCorrect[currentWordIndex] = Character.toLowerCase(typed.charAt(0)) == Character.toLowerCase(key[currentWordIndex].charAt(0));
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
        if(currentVerse.getNextDifficulty() < 4) {
            verse.setNextDifficulty(verse.getNextDifficulty() + 1);
        }

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

        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("remove-verse-btn");
        removeBtn.setOnAction(e -> {
            DataStore.removeVerse(verse.getId());
            loadVerseList();
        });

        card.getChildren().addAll(refLabel, previewLabel, diffBox, removeBtn);
        return card;
    }

    private void showAddVersePopup() {

        StackPane popupOverlay = new StackPane();
        popupOverlay.getStyleClass().add("popup-overlay");
        popupOverlay.setOnMouseClicked(e -> closePopup());
    
        VBox popupContainer = new VBox(8); // reduced spacing
        popupContainer.getStyleClass().add("memorize-popup");
        popupContainer.setPadding(new Insets(10)); // smaller padding
        popupContainer.setMaxWidth(380); // smaller popup
        popupContainer.setMinWidth(320);
    
        Label popupTitle = new Label("Add Verse");
        popupTitle.getStyleClass().add("popup-title");
    
        Button closeBtn = new Button("X");
        closeBtn.getStyleClass().add("popup-close-btn");
        closeBtn.setOnAction(e -> closePopup());
    
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(popupTitle, Priority.ALWAYS);
        header.getChildren().addAll(popupTitle, closeBtn);
    
        // ---------------- BOOK ----------------
        ComboBox<String> bookBox = new ComboBox<>();
        bookBox.getItems().addAll(
            "Genesis","Exodus","Leviticus","Numbers","Deuteronomy",
            "Joshua","Judges","Ruth","1 Samuel","2 Samuel",
            "1 Kings","2 Kings","1 Chronicles","2 Chronicles",
            "Ezra","Nehemiah","Esther","Job","Psalms","Proverbs",
            "Ecclesiastes","Song of Solomon","Isaiah","Jeremiah",
            "Lamentations","Ezekiel","Daniel","Hosea","Joel","Amos",
            "Obadiah","Jonah","Micah","Nahum","Habakkuk","Zephaniah",
            "Haggai","Zechariah","Malachi","Matthew","Mark","Luke",
            "John","Acts","Romans","1 Corinthians","2 Corinthians",
            "Galatians","Ephesians","Philippians","Colossians",
            "1 Thessalonians","2 Thessalonians","1 Timothy","2 Timothy",
            "Titus","Philemon","Hebrews","James","1 Peter","2 Peter",
            "1 John","2 John","3 John","Jude","Revelation"
        );
        bookBox.setPromptText("Book");
    
        // ---------------- CHAPTER / VERSE ----------------
        Spinner<Integer> chapterSpinner = new Spinner<>(1, 150, 1);
        Spinner<Integer> verseSpinner = new Spinner<>(1, 200, 1);
    
        chapterSpinner.setPrefWidth(90);
        verseSpinner.setPrefWidth(90);
    
        HBox row = new HBox(8,
            new Label("Ch"), chapterSpinner,
            new Label("V"), verseSpinner
        );
    
        row.setAlignment(Pos.CENTER_LEFT);
    
        // ---------------- VALIDATION DATA (simplified real bounds) ----------------
        java.util.Map<String, Integer> maxChapters = new java.util.HashMap<>();
        maxChapters.put("Genesis", 50);
        maxChapters.put("Exodus", 40);
        maxChapters.put("Psalms", 150);
        maxChapters.put("Matthew", 28);
        maxChapters.put("John", 21);
        maxChapters.put("Revelation", 22);
        // (you can expand later)
    
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
    
        // ---------------- SAVE ----------------
        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("add-verse-btn");
    
        saveBtn.setOnAction(e -> {
    
            errorLabel.setText("");
    
            String book = bookBox.getValue();
            int chapter = chapterSpinner.getValue();
            int verse = verseSpinner.getValue();
    
            if (book == null) {
                errorLabel.setText("Select a book.");
                return;
            }
    
            int maxChapter = maxChapters.getOrDefault(book, 150);
    
            if (chapter < 1 || chapter > maxChapter) {
                errorLabel.setText(book + " has 1-" + maxChapter + " chapters.");
                return;
            }
    
            if (verse < 1) {
                errorLabel.setText("Verse must be at least 1.");
                return;
            }
    
            // soft safety cap (since we don’t have per-chapter verse data)
            if (verse > 200) {
                errorLabel.setText("Verse number seems too high.");
                return;
            }
    
            MemorizedVerse newVerse =
                new MemorizedVerse(book, chapter, verse, "", 0);
    
            DataStore.addVerse(newVerse);
            loadVerseList();
            closePopup();
        });
    
        // ---------------- LAYOUT (tightened) ----------------
        VBox content = new VBox(8,
            bookBox,
            row,
            errorLabel,
            saveBtn
        );
    
        popupContainer.getChildren().addAll(header, content);
    
        StackPane popupWrapper = new StackPane(popupContainer);
        popupWrapper.getStyleClass().add("popup-wrapper");
        popupWrapper.setAlignment(Pos.CENTER);
    
        popupContainer.setOnMouseClicked(e -> e.consume());
    
        appRoot.getChildren().addAll(popupOverlay, popupWrapper);
    
        currentClosePopupHandler = () ->
            appRoot.getChildren().removeAll(popupOverlay, popupWrapper);
    
        appRoot.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) closePopup();
        });
    
        appRoot.requestFocus();
    }

    private void closePopup() {
        if (currentClosePopupHandler != null) {
            currentClosePopupHandler.run();
            currentClosePopupHandler = null;
        }
    }
}