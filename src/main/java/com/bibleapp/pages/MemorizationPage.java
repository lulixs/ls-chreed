package com.bibleapp.pages;

import com.bibleapp.data.DataStore;
import com.bibleapp.data.MemorizedVerse;
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

    /** Labels matching MemorizedVerse difficulty constants 1–4. */
    private static final String[] DIFFICULTY_LABELS = {
        "Copy-down",       // index 0 → difficulty 1
        "Every-other A",   // index 1 → difficulty 2
        "Every-other B",   // index 2 → difficulty 3
        "Full-memory"      // index 3 → difficulty 4
    };

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
     *  - Reference label (e.g. "John 3:16")
     *  - Verse text preview (first 60 chars, ellipsised)
     *  - Difficulty ComboBox pre-populated from the stored value; changes
     *    are persisted immediately via DataStore.updateVerseDifficulty()
     *  - A Remove button
     */
    private VBox buildVerseCard(MemorizedVerse verse) {
        VBox card = new VBox(4);
        card.getStyleClass().add("verse-card");
        card.setPadding(new Insets(8));

        // Reference  (e.g. "John 3:16")
        Label refLabel = new Label(verse.getReference());
        refLabel.getStyleClass().add("verse-card-reference");
        refLabel.setWrapText(true);

        // Short text preview
        String preview = verse.getText();
        if (preview != null && preview.length() > 60) {
            preview = preview.substring(0, 60).stripTrailing() + "…";
        }
        Label previewLabel = new Label(preview);
        previewLabel.getStyleClass().add("verse-card-preview");
        previewLabel.setWrapText(true);

        // Difficulty selector — pre-populated from stored value, auto-saved on change
        ComboBox<String> diffBox = new ComboBox<>();
        diffBox.getItems().addAll(DIFFICULTY_LABELS);
        diffBox.setMaxWidth(Double.MAX_VALUE);
        // Difficulty is 1-based; convert to 0-based ComboBox index
        int diffIndex = Math.max(0, Math.min(verse.getDifficulty() - 1, DIFFICULTY_LABELS.length - 1));
        diffBox.getSelectionModel().select(diffIndex);
        diffBox.setOnAction(e -> {
            int selectedIndex = diffBox.getSelectionModel().getSelectedIndex();
            // Convert 0-based index back to 1-based difficulty constant
            DataStore.updateVerseDifficulty(verse.getId(), selectedIndex + 1);
        });

        // Remove button
        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("remove-verse-btn");
        removeBtn.setOnAction(e -> {
            DataStore.removeVerse(verse.getId());
            loadVerseList();   // refresh UI
        });

        card.getChildren().addAll(refLabel, previewLabel, diffBox, removeBtn);
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

        // ── Book / Chapter / Verse fields ─────────────────────────────────────
        TextField bookField = new TextField();
        bookField.setPromptText("Book (e.g. John)");

        TextField chapterField = new TextField();
        chapterField.setPromptText("Chapter (e.g. 3)");

        TextField verseNumField = new TextField();
        verseNumField.setPromptText("Verse (e.g. 16)");

        HBox locationRow = new HBox(8, bookField, chapterField, verseNumField);
        HBox.setHgrow(bookField, Priority.ALWAYS);
        locationRow.setAlignment(Pos.CENTER_LEFT);

        // ── Verse text ────────────────────────────────────────────────────────
        TextArea textArea = new TextArea();
        textArea.setPromptText("Verse text…");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(4);

        // ── Difficulty selector ───────────────────────────────────────────────
        ComboBox<String> diffBox = new ComboBox<>();
        diffBox.getItems().addAll(DIFFICULTY_LABELS);
        diffBox.getSelectionModel().selectFirst();   // default: Copy-down
        diffBox.setMaxWidth(Double.MAX_VALUE);

        // ── Error / feedback label ────────────────────────────────────────────
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        // ── Save button — fully wired ─────────────────────────────────────────
        Button saveBtn = new Button("Save Verse");
        saveBtn.getStyleClass().add("add-verse-btn");
        saveBtn.setOnAction(e -> {
            errorLabel.setText("");

            String book    = bookField.getText().trim();
            String chapStr = chapterField.getText().trim();
            String verseStr = verseNumField.getText().trim();
            String text    = textArea.getText().trim();

            // Basic validation
            if (book.isEmpty() || chapStr.isEmpty() || verseStr.isEmpty() || text.isEmpty()) {
                errorLabel.setText("Please fill in all fields.");
                return;
            }

            int chapter, verseNum;
            try {
                chapter  = Integer.parseInt(chapStr);
                verseNum = Integer.parseInt(verseStr);
            } catch (NumberFormatException ex) {
                errorLabel.setText("Chapter and verse must be numbers.");
                return;
            }

            if (chapter <= 0 || verseNum <= 0) {
                errorLabel.setText("Chapter and verse must be greater than 0.");
                return;
            }

            // Convert 0-based ComboBox selection to 1-based difficulty constant
            int difficulty = diffBox.getSelectionModel().getSelectedIndex() + 1;

            MemorizedVerse newVerse = new MemorizedVerse(book, chapter, verseNum, text, difficulty);
            DataStore.addVerse(newVerse);

            // Refresh the "My Verses" list and dismiss the popup
            loadVerseList();
            closePopup();
        });

        popupContentArea = new VBox(10);
        popupContentArea.getChildren().addAll(
            new Label("Location:"), locationRow,
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
