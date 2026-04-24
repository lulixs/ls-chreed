package com.bibleapp.pages;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;

/**
 * Verse memorization page with a list of verses and practice mode.
 * Contains a scrollable verse list and an add verse popup.
 */
public class MemorizationPage extends VBox {

    private final VBox leftColumn;
    private final VBox rightColumn;
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

        // Left column - My Verses list
        leftColumn = new VBox(10);
        leftColumn.getStyleClass().add("memorize-left-column");
        leftColumn.setPrefWidth(200);
        leftColumn.setMinWidth(150);

        // Scrollable content for verse cards
        Label leftLabel = new Label("My Verses");
        leftLabel.getStyleClass().add("column-header");
        leftColumn.getChildren().add(leftLabel);

        // Scrollable content for verse cards
        VBox leftScrollContent = new VBox(10);
        leftScrollContent.getStyleClass().add("memorize-scroll-content");
        leftScrollContent.setPadding(new Insets(10));

        ScrollPane leftScrollPane = new ScrollPane(leftScrollContent);
        leftScrollPane.getStyleClass().add("memorize-scroll");
        leftScrollPane.setFitToWidth(true);
        leftScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(leftScrollPane, Priority.ALWAYS);
        leftColumn.getChildren().add(leftScrollPane);

        // Add button
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

      Button startBtn = new Button("Start");
      startBtn.getStyleClass().add("diff-start-btn");
      startBtn.setOnAction(e -> startTask("For God so loved the world that he gave his one and only Son, that whoever believes in him shall not perish but have eternal life."));

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

    private void startTask(String verse) {
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

        String[] words = verse.split("\\s+");
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

        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                advanceWord(inputField, words);
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

    private void advanceWord(TextField inputField, String[] words) {
        String typed = inputField.getText();
        if (!typed.isEmpty()) {
            wordCorrect[currentWordIndex] = Character.toLowerCase(typed.charAt(0)) == Character.toLowerCase(words[currentWordIndex].charAt(0));
        }
        inputField.clear();
        currentWordIndex++;

        if (currentWordIndex < wordLabels.size()) {
            highlightWord(currentWordIndex);
        } else {
            onVerseComplete(words);
        }
    }

    private void onVerseComplete(String[] words) {
        rightColumn.getChildren().clear();

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

        Label scoreLabel = new Label(numCorrect + "/" + words.length);
        scoreLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: " + getScoreColor(numCorrect, words.length) + ";");

        Label scoreSubtitle = new Label(getScoreMessage(numCorrect, words.length));
        scoreSubtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #888888;");

        VBox scoreBox = new VBox(8, scoreTitle, scoreLabel, scoreSubtitle);
        scoreBox.setAlignment(Pos.CENTER);

        TextFlow verseReview = new TextFlow();
        verseReview.setLineSpacing(6);
        for (int i = 0; i < words.length; i++) {
            Label wordLabel = new Label(words[i]);
            String color = wordCorrect[i] ? "#1D9E75" : "#D85A30";
            wordLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
            verseReview.getChildren().add(wordLabel);
            if (i < words.length - 1) {
                Label space = new Label(" ");
                space.setStyle("-fx-font-size: 16px;");
                verseReview.getChildren().add(space);
            }
        }

        Button actionBtn;
        if (numCorrect == words.length) {
            actionBtn = new Button("Continue");
            actionBtn.setOnAction(e -> showDifficultySelector());
            if(currentDifficulty == nextDifficulty) {
                unlockNextDifficulty();
                currentDifficulty++;
            }
        } else {
            actionBtn = new Button("Retry");
            actionBtn.setOnAction(e -> startTask(String.join(" ", words)));
        }

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

    private void showAddVersePopup() {
        popupOverlay = new StackPane();
        popupOverlay.getStyleClass().add("popup-overlay");
        popupOverlay.setOnMouseClicked(e -> closePopup());

        popupContainer = new VBox();
        popupContainer.getStyleClass().add("memorize-popup");
        popupContainer.setMaxWidth(500);
        popupContainer.setMaxHeight(600);
        popupContainer.setMinWidth(400);
        popupContainer.setMinHeight(400);

        Label popupTitle = new Label("Add Verse");
        popupTitle.getStyleClass().add("popup-title");

        Button closeBtn = new Button("X");
        closeBtn.getStyleClass().add("popup-close-btn");
        closeBtn.setOnAction(e -> closePopup());

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(popupTitle, Priority.ALWAYS);
        header.getChildren().addAll(popupTitle, closeBtn);

        popupContentArea = new VBox(10);
        popupContentArea.getStyleClass().add("popup-scroll-content");
        popupContentArea.setPadding(new Insets(10));

        ScrollPane popupScroll = new ScrollPane(popupContentArea);
        popupScroll.getStyleClass().add("popup-scroll-pane");
        popupScroll.setFitToWidth(true);
        popupScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        popupScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(popupScroll, Priority.ALWAYS);

        Label placeholder = new Label("Available verses will appear here");
        placeholder.getStyleClass().add("popup-placeholder");
        popupContentArea.getChildren().add(placeholder);

        popupContainer.getChildren().addAll(header, popupScroll);

        StackPane popupWrapper = new StackPane(popupContainer);
        popupWrapper.getStyleClass().add("popup-wrapper");
        popupWrapper.setAlignment(Pos.CENTER);
        popupContainer.setOnMouseClicked(e -> e.consume());

        if (getScene() != null && getScene().getRoot() instanceof StackPane) {
            StackPane root = (StackPane) getScene().getRoot();
            root.getChildren().addAll(popupOverlay, popupWrapper);

            currentClosePopupHandler = () -> {
                root.getChildren().removeAll(popupOverlay, popupWrapper);
            };

            root.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    closePopup();
                }
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
