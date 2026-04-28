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
