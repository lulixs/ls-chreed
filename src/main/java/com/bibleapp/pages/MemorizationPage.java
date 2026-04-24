package com.bibleapp.pages;

import java.util.ArrayList;
import java.util.List;

import com.bibleapp.data.DataStore;
import com.bibleapp.data.MemorizedVerse;
import com.bibleapp.difficulty.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;

/**
 * Verse memorization page with a list of verses and practice mode.
 * Verse data (reference, text, difficulty) is loaded from and saved to
 * DataStore so it persists between sessions.
 */
public class MemorizationPage extends VBox {

    private final VBox leftScrollContent;   // holds the verse cards
    private StackPane popupOverlay;
    private VBox popupContainer;
    private VBox popupContentArea;
    private Runnable currentClosePopupHandler;

    private static final String[] DIFFICULTY_NAMES = { "Copy-Down", "Every-Other A", "Every-Other B", "Full-Memory" };
    private static final String[] DIFFICULTY_COLORS = { "#1D9E75", "#378ADD", "#BA7517", "#D85A30", "#555555" };
    private int nextDifficulty = 0;

    private int currentDifficulty = 0;   // 0-indexed
    private Button prevBtn, nextBtn;
    private Label diffNameLabel;
    private Label diffLockedLabel;
    private VBox rightColumn;
    private final Rectangle[] pips = new Rectangle[4];

    public MemorizationPage() {
        getStyleClass().add("page");
        setSpacing(20);

        Label title = new Label("Memorize");
        title.getStyleClass().add("page-title");

        HBox columnsContainer = new HBox(20);
        columnsContainer.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(columnsContainer, Priority.ALWAYS);
        VBox.setVgrow(columnsContainer, Priority.ALWAYS);

        // ── Left column – My Verses ───────────────────────────────────────────
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

        // Load saved verses from disk
        loadVerseList();
    }


    //  TODO: Wire to left column verse selection
    private MemorizedVerse getSelectedVerse() {
        // Debug code. Replace with actual code to get the current verse selected
        MemorizedVerse verse = new MemorizedVerse("John", 3, 16, "For God so loved the world that he gave his one and only Son, that whoever believes in him shall not perish but have eternal life.", 0);
        return verse;
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

            String color;
            if(i < nextDifficulty)
                color = DIFFICULTY_COLORS[i];
            else {
                color = DIFFICULTY_COLORS[4];
                if(i != nextDifficulty)
                    pip = new Rectangle(9, 9);
            }
            pip.setFill(javafx.scene.paint.Color.web(color));
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

        String verse = getSelectedVerse().getText();

        Difficulty diff = switch(currentDifficulty) {
            case 0 -> new CopyDown(verse);
            case 1 -> new EveryOtherA(verse);
            case 2 -> new EveryOtherB(verse);
            case 3 -> new FullMemory(verse);
            default -> null;
        };
        Button startBtn = new Button("Start");
        startBtn.getStyleClass().add("add-verse-btn");
        startBtn.setOnAction(e -> startTask(diff));

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

    private void refreshDifficultyUI() {
        for (int i = 0; i < 4; i++) {
            if (i == currentDifficulty) {
                pips[i].setStroke(javafx.scene.paint.Color.web("#000000"));
                pips[i].setStrokeWidth(2.5);
            } else {
                pips[i].setStroke(javafx.scene.paint.Color.TRANSPARENT);
                pips[i].setStrokeWidth(2.5);
            }
        }

        diffNameLabel.setText(DIFFICULTY_NAMES[currentDifficulty]);
        diffLockedLabel.setText(nextDifficulty == currentDifficulty ? "Incomplete" : "Completed");

        prevBtn.setDisable(currentDifficulty == 0);
        nextBtn.setDisable(currentDifficulty == Math.min(nextDifficulty, 3));
    }

    private List<Label> wordLabels = new ArrayList<>();
    private int currentWordIndex = 0;
    private boolean[] wordCorrect;

    private void startTask(Difficulty diff) {
        rightColumn.getChildren().clear();

        Button backBtn = new Button("\u2190 Back");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> showDifficultySelector());

        HBox topBar = new HBox(backBtn);
        topBar.setAlignment(Pos.TOP_LEFT);

        // Build word labels
        wordLabels.clear();
        currentWordIndex = 0;

        TextFlow verseFlow = new TextFlow();
        verseFlow.setLineSpacing(6);
        verseFlow.getStyleClass().add("verse-text-flow");

        String[] words = diff.getDisplayVerse();
        for (int i = 0; i < words.length; i++) {
            Label wordLabel = new Label(words[i]);
            wordLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333;");
            wordLabels.add(wordLabel);
            verseFlow.getChildren().add(wordLabel);

            if (i < words.length - 1) {
                Label space = new Label(" ");
                space.setStyle("-fx-font-size: 16px;");
                verseFlow.getChildren().add(space);
            }
        }   

        wordCorrect = new boolean[words.length];

        // Input field
        TextField inputField = new TextField();
        inputField.getStyleClass().add("verse-input");
        inputField.setPromptText("Type the first letter of each word...");

        String[] key = diff.getAnswerKey();

        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                if(!advanceWord(inputField, key)) {
                    onVerseComplete(diff);
                }
            }
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        rightColumn.getChildren().addAll(topBar, verseFlow, spacer, inputField);

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

    private boolean advanceWord(TextField inputField, String[] key) {
        String typed = inputField.getText();
        if (!typed.isEmpty()) {
            wordCorrect[currentWordIndex] = Character.toLowerCase(typed.charAt(0)) == Character.toLowerCase(key[currentWordIndex].charAt(0));
        }
        inputField.clear();
        currentWordIndex++;

        if (currentWordIndex < wordLabels.size()) {
            highlightWord(currentWordIndex);
            return true;
        } else {
            return false;
        }
    }

    private void onVerseComplete(Difficulty diff) {
        rightColumn.getChildren().clear();

        String[] key = diff.getAnswerKey();

        Button backBtn = new Button("\u2190 Back");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> showDifficultySelector());

        HBox topBar = new HBox(backBtn);
        topBar.setAlignment(Pos.TOP_LEFT);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Region spacerBottom = new Region();
        VBox.setVgrow(spacerBottom, Priority.ALWAYS);

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
            String color = wordCorrect[i] ? "#1D9E75" : "#D85A30";
            wordLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            verseReview.getChildren().add(wordLabel);
            if (i < key.length - 1) {
                Label space = new Label(" ");
                space.setStyle("-fx-font-size: 16px;");
                verseReview.getChildren().add(space);
            }
        }

        Button actionBtn;
        if (numCorrect == key.length) {
            actionBtn = new Button("Continue");
            actionBtn.setOnAction(e -> showDifficultySelector());
            if(currentDifficulty == nextDifficulty) {
                unlockNextDifficulty();
                if(currentDifficulty < 3)
                    currentDifficulty++;
            }
        } else {
            actionBtn = new Button("Retry");
            actionBtn.setOnAction(e -> startTask(diff));
        }

        actionBtn.getStyleClass().add("add-verse-btn");

        HBox actionBar = new HBox(actionBtn);
        actionBar.setAlignment(Pos.CENTER);

        rightColumn.getChildren().addAll(topBar, spacer, scoreBox, verseReview, spacerBottom, actionBar);
    }

    private int countCorrect(boolean[] arr) {
        int count = 0;
        for(int i = 0; i < arr.length; i++) {
            if(arr[i]) {
                count++;
            }
        }
        return count;
    }

    private String getScoreColor(int score, int total) {
        if (score >= total) return "#1D9E75";  // green
        if (score * 2 >= total) return "#BA7517";  // amber
        return "#D85A30";                   // red
    }

    private String getScoreMessage(int score, int total) {
        if (score >= total) return "Great work!";
        if (score * 2 >= total) return "Keep practicing!";
        return "Don't give up!";
    }

    private void showDifficultySelector() {
        rightColumn.getChildren().clear();

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        rightColumn.getChildren().addAll(spacer, buildDifficultySelector());
    }       

    private void cycleLeft() {
        if (currentDifficulty > 0) {
            currentDifficulty--;
            refreshDifficultyUI();
        }
    }

    private void cycleRight() {
        if (currentDifficulty < Math.min(nextDifficulty, 3)) {
            currentDifficulty++;
            refreshDifficultyUI();
        }
    }

    // Call this to unlock the next difficulty at runtime
    public void unlockNextDifficulty() {
        if(nextDifficulty < 4) {
            pips[nextDifficulty].setFill(javafx.scene.paint.Color.web(DIFFICULTY_COLORS[nextDifficulty]));
            nextDifficulty++;
            if(nextDifficulty < 4) {
            pips[nextDifficulty].setWidth(18);
            pips[nextDifficulty].setHeight(18);
            }
        }

        refreshDifficultyUI();
    }

    // Call this when a to reset progress at runtime
    public void resetProgress() {
        nextDifficulty = 0;
        refreshDifficultyUI();
    }

    // Returns the currently displayed difficulty (0-indexed)
    public int getSelectedDifficulty() {
        return currentDifficulty;
    }

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

    /**
     * Builds a small card for one verse with:
     *  - Reference label
     *  - Difficulty level label
     *  - A ComboBox to change difficulty (auto-saved)
     *  - A Remove button
     */
    private VBox buildVerseCard(MemorizedVerse verse) {
        VBox card = new VBox(4);
        card.getStyleClass().add("verse-card");
        card.setPadding(new Insets(8));

        Label refLabel = new Label(verse.getReference());
        refLabel.getStyleClass().add("verse-card-reference");
        refLabel.setWrapText(true);

        // Difficulty selector.
        // TODO(ui): wire this ComboBox to the new 4-level difficulty scheme
        // on MemorizedVerse (Copy-down / Every-other A / Every-other B /
        // Full-memory). Pre-populate from verse.getDifficulty() and persist
        // via DataStore.updateVerseDifficulty(verse.getId(), ...). Labels
        // and mapping left for the UI redesign.
        ComboBox<String> diffBox = new ComboBox<>();
        diffBox.setMaxWidth(Double.MAX_VALUE);

        // Remove button
        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("remove-verse-btn");
        removeBtn.setOnAction(e -> {
            DataStore.removeVerse(verse.getId());
            loadVerseList();   // refresh UI
        });

        card.getChildren().addAll(refLabel, diffBox, removeBtn);
        return card;
    }

    // =========================================================================
    // Add-Verse popup
    // =========================================================================

    private void showAddVersePopup() {
        popupOverlay = new StackPane();
        popupOverlay.getStyleClass().add("popup-overlay");
        popupOverlay.setOnMouseClicked(e -> closePopup());

        popupContainer = new VBox(10);
        popupContainer.getStyleClass().add("memorize-popup");
        popupContainer.setPadding(new Insets(16));
        popupContainer.setMaxWidth(500);
        popupContainer.setMinWidth(350);

        Label popupTitle = new Label("Add Verse");
        popupTitle.getStyleClass().add("popup-title");

        Button closeBtn = new Button("X");
        closeBtn.getStyleClass().add("popup-close-btn");
        closeBtn.setOnAction(e -> closePopup());

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(popupTitle, Priority.ALWAYS);
        header.getChildren().addAll(popupTitle, closeBtn);

        // Input fields
        TextField refField = new TextField();
        refField.setPromptText("Reference (e.g. John 3:16)");

        TextArea textArea = new TextArea();
        textArea.setPromptText("Verse text…");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(4);

        ComboBox<String> diffBox = new ComboBox<>();
        diffBox.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        Button saveBtn = new Button("Save Verse");
        saveBtn.getStyleClass().add("add-verse-btn");
        // TODO(ui): collect book, chapter, verse, text, and difficulty from
        // the form, construct a MemorizedVerse, and persist via
        // DataStore.addVerse(...). The new entry shape requires separate
        // book / chapter / verse fields rather than a single reference
        // string, so the input widgets above will need to be redesigned.
        saveBtn.setOnAction(e ->
            errorLabel.setText("Add-verse flow is being redesigned and is not yet wired."));

        popupContentArea = new VBox(10);
        popupContentArea.getChildren().addAll(
            new Label("Reference:"), refField,
            new Label("Verse text:"), textArea,
            new Label("Difficulty:"), diffBox,
            errorLabel, saveBtn
        );

        popupContainer.getChildren().addAll(header, popupContentArea);

        StackPane popupWrapper = new StackPane(popupContainer);
        popupWrapper.getStyleClass().add("popup-wrapper");
        popupWrapper.setAlignment(Pos.CENTER);
        popupContainer.setOnMouseClicked(e -> e.consume());

        if (getScene() != null && getScene().getRoot() instanceof StackPane root) {
            root.getChildren().addAll(popupOverlay, popupWrapper);
            currentClosePopupHandler = () ->
                root.getChildren().removeAll(popupOverlay, popupWrapper);
            root.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) closePopup();
            });
            root.requestFocus();
        }
    }

    private void closePopup() {
        if (currentClosePopupHandler != null) {
            currentClosePopupHandler.run();
            currentClosePopupHandler = null;
        }
    }
}
