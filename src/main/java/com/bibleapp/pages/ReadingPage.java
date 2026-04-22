package com.bibleapp.pages;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Bible reading page with selectors for translation, book, and chapter.
 * Contains a text area for displaying scripture content.
 */
public class ReadingPage extends VBox {

    private final ComboBox<String> translationCombo;
    private final ComboBox<String> bookCombo;
    private final Spinner<Integer> chapterSpinner;
    private final TextArea chapterDisplay;

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
        translationCombo.setPrefWidth(140);
        translationCombo.getStyleClass().add("selector-combo");

        bookCombo = new ComboBox<>();
        bookCombo.setPromptText("Book");
        bookCombo.setPrefWidth(180);
        bookCombo.getStyleClass().add("selector-combo");

        chapterSpinner = new Spinner<>(1, 150, 1);
        chapterSpinner.setPrefWidth(70);
        chapterSpinner.getStyleClass().add("chapter-spinner");

        // Start with a safe default range (1–150); it will be narrowed on selection.
        SpinnerValueFactory.IntegerSpinnerValueFactory spinnerFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 150, 1);
        chapterSpinner = new Spinner<>(spinnerFactory);
        chapterSpinner.setPrefWidth(70);
        chapterSpinner.getStyleClass().add("chapter-spinner");
        // Make the spinner editable so users can also type a number directly,
        // but clamp typed values on focus-lost.
        chapterSpinner.setEditable(true);

         // -------------------------------------------------------------------
        // BUG FIX: update the chapter spinner's max whenever the book changes
        // -------------------------------------------------------------------
        bookCombo.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldBook, newBook) -> {
                    if (newBook == null) return;

                    // Clamp the current chapter value so it stays in range
                    int currentChapter = chapterSpinner.getValue();
                    int clampedChapter = Math.min(currentChapter, maxChapters);

                    spinnerFactory.setMax(maxChapters);
                    spinnerFactory.setValue(clampedChapter);
                });


        selectorRow.getChildren().addAll(translationCombo, bookCombo, chapterSpinner);

        // Text area for scripture display
        chapterDisplay = new TextArea();
        chapterDisplay.setEditable(false);
        chapterDisplay.setWrapText(true);
        chapterDisplay.setPrefRowCount(20);
        chapterDisplay.getStyleClass().add("chapter-display");

        VBox displayContainer = new VBox(chapterDisplay);
        VBox.setVgrow(displayContainer, Priority.ALWAYS);
        displayContainer.setFillWidth(true);

        getChildren().addAll(title, selectorRow, displayContainer);
    }
}
