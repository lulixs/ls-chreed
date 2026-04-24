package com.bibleapp.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.bibleapp.data.DataStore;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

        // Right column - Practice area
        rightColumn = new VBox(10);
        rightColumn.getStyleClass().add("memorize-right-column");

        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        HBox.setHgrow(leftColumn, Priority.NEVER);

        columnsContainer.getChildren().addAll(leftColumn, rightColumn);
        VBox.setVgrow(columnsContainer, Priority.ALWAYS);

        getChildren().addAll(title, columnsContainer);
    }

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
    
        // ── CHANGED: form fields instead of placeholder label ──────────────────
    
        Label bookLabel = new Label("Book");
        TextField bookField = new TextField();
        bookField.setPromptText("e.g. John");
    
        Label chapterLabel = new Label("Chapter");
        TextField chapterField = new TextField();
        chapterField.setPromptText("e.g. 3");
    
        Label verseLabel = new Label("Verse");
        TextField verseField = new TextField();
        verseField.setPromptText("e.g. 16");
    
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: red;");
    
        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("add-verse-btn");
        saveBtn.setOnAction(e -> {
            String book = bookField.getText().trim();
            String chapterText = chapterField.getText().trim();
            String verseText = verseField.getText().trim();
    
            // Validate
            if (book.isEmpty() || chapterText.isEmpty() || verseText.isEmpty()) {
                errorLabel.setText("All fields are required.");
                return;
            }
            int chapter, verse;
            try {
                chapter = Integer.parseInt(chapterText);
                verse   = Integer.parseInt(verseText);
            } catch (NumberFormatException ex) {
                errorLabel.setText("Chapter and Verse must be whole numbers.");
                return;
            }
            if (chapter < 1 || verse < 1) {
                errorLabel.setText("Chapter and Verse must be positive.");
                return;
            }
    
            // Build JSON entry
            JSONObject entry = new JSONObject();
            entry.put("book",    book);
            entry.put("chapter", (long) chapter);   // json-simple uses Long
            entry.put("verse",   (long) verse);
    
            // Load → append → save
            JSONObject data = DataStore.load();
            JSONArray verses = (JSONArray) data.get("memorized_verses");
            if (verses == null) {
                verses = new JSONArray();
                data.put("memorized_verses", verses);
            }
            verses.add(entry);
            DataStore.save(data);
    
            closePopup();
        });
    
        popupContentArea.getChildren().addAll(
            bookLabel, bookField,
            chapterLabel, chapterField,
            verseLabel, verseField,
            errorLabel,
            saveBtn
        );
        // ── END CHANGED ────────────────────────────────────────────────────────
    
        ScrollPane popupScroll = new ScrollPane(popupContentArea);
        popupScroll.getStyleClass().add("popup-scroll-pane");
        popupScroll.setFitToWidth(true);
        popupScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        popupScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(popupScroll, Priority.ALWAYS);
    
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
