package com.bibleapp.pages;

import com.bibleapp.data.DataStore;
import com.bibleapp.data.MemorizedVerse;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

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

    public MemorizationPage(StackPane appRoot) {
        this.appRoot = appRoot;
        getStyleClass().add("page");
        setSpacing(20);

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

        VBox rightColumn = new VBox(10);
        rightColumn.getStyleClass().add("memorize-right-column");
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        HBox.setHgrow(leftColumn, Priority.NEVER);

        columnsContainer.getChildren().addAll(leftColumn, rightColumn);
        VBox.setVgrow(columnsContainer, Priority.ALWAYS);

        getChildren().addAll(title, columnsContainer);
        loadVerseList();
    }

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
        int diffIndex = Math.max(0, Math.min(verse.getDifficulty() - 1, DIFFICULTY_LABELS.length - 1));
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

        VBox popupContainer = new VBox(10);
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

        TextField bookField = new TextField();
        bookField.setPromptText("Book (e.g. John)");

        TextField chapterField = new TextField();
        chapterField.setPromptText("Chapter (e.g. 3)");

        TextField verseNumField = new TextField();
        verseNumField.setPromptText("Verse (e.g. 16)");

        HBox locationRow = new HBox(8, bookField, chapterField, verseNumField);
        HBox.setHgrow(bookField, Priority.ALWAYS);
        locationRow.setAlignment(Pos.CENTER_LEFT);

        TextArea textArea = new TextArea();
        textArea.setPromptText("Verse text…");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(4);

        ComboBox<String> diffBox = new ComboBox<>();
        diffBox.getItems().addAll(DIFFICULTY_LABELS);
        diffBox.getSelectionModel().selectFirst();
        diffBox.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        Button saveBtn = new Button("Save Verse");
        saveBtn.getStyleClass().add("add-verse-btn");
        saveBtn.setOnAction(e -> {
            errorLabel.setText("");

            String book     = bookField.getText().trim();
            String chapStr  = chapterField.getText().trim();
            String verseStr = verseNumField.getText().trim();
            String text     = textArea.getText().trim();

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

            int difficulty = diffBox.getSelectionModel().getSelectedIndex() + 1;
            MemorizedVerse newVerse = new MemorizedVerse(book, chapter, verseNum, text, difficulty);
            DataStore.addVerse(newVerse);
            loadVerseList();
            closePopup();
        });

        VBox popupContentArea = new VBox(10);
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