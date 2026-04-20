package com.bibleapp.pages;

import com.bibleapp.data.DataStore;
import com.bibleapp.data.UserData;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

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

        // ── Right column – Practice area ──────────────────────────────────────
        VBox rightColumn = new VBox(10);
        rightColumn.getStyleClass().add("memorize-right-column");
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        HBox.setHgrow(leftColumn, Priority.NEVER);

        columnsContainer.getChildren().addAll(leftColumn, rightColumn);
        VBox.setVgrow(columnsContainer, Priority.ALWAYS);

        getChildren().addAll(title, columnsContainer);

        // Load saved verses from disk
        loadVerseList();
    }

    // =========================================================================
    // Data helpers
    // =========================================================================

    /** Reads all verses from DataStore and rebuilds the card list. */
    private void loadVerseList() {
        leftScrollContent.getChildren().clear();
        List<UserData> verses = DataStore.getMemorizationList();
        if (verses.isEmpty()) {
            Label empty = new Label("No verses yet. Tap '+ Add Verse'.");
            empty.getStyleClass().add("popup-placeholder");
            leftScrollContent.getChildren().add(empty);
        } else {
            for (UserData verse : verses) {
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
    @SuppressWarnings("unchecked")
    private VBox buildVerseCard(UserData verse) {
        VBox card = new VBox(4);
        card.getStyleClass().add("verse-card");
        card.setPadding(new Insets(8));

        Label refLabel = new Label(verse.getReference());
        refLabel.getStyleClass().add("verse-card-reference");
        refLabel.setWrapText(true);

        // Difficulty selector
        ComboBox<String> diffBox = new ComboBox<>();
        diffBox.getItems().addAll("Beginner", "Intermediate", "Advanced");
        diffBox.setValue(verse.getDifficultyLabel());
        diffBox.setMaxWidth(Double.MAX_VALUE);
        diffBox.setOnAction(e -> {
            int newDiff = switch (diffBox.getValue()) {
                case "Intermediate" -> UserData.DIFFICULTY_INTERMEDIATE;
                case "Advanced"     -> UserData.DIFFICULTY_ADVANCED;
                default             -> UserData.DIFFICULTY_BEGINNER;
            };
            DataStore.updateVerseDifficulty(verse.getReference(), newDiff);
        });

        // Remove button
        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("remove-verse-btn");
        removeBtn.setOnAction(e -> {
            DataStore.removeVerse(verse.getReference());
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
        diffBox.getItems().addAll("Beginner", "Intermediate", "Advanced");
        diffBox.setValue("Beginner");
        diffBox.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        Button saveBtn = new Button("Save Verse");
        saveBtn.getStyleClass().add("add-verse-btn");
        saveBtn.setOnAction(e -> {
            String ref  = refField.getText().trim();
            String text = textArea.getText().trim();
            if (ref.isEmpty() || text.isEmpty()) {
                errorLabel.setText("Please fill in both the reference and the verse text.");
                return;
            }
            int diff = switch (diffBox.getValue()) {
                case "Intermediate" -> UserData.DIFFICULTY_INTERMEDIATE;
                case "Advanced"     -> UserData.DIFFICULTY_ADVANCED;
                default             -> UserData.DIFFICULTY_BEGINNER;
            };
            DataStore.addVerse(new UserData(ref, text, diff));
            closePopup();
            loadVerseList();  // refresh list
        });

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
