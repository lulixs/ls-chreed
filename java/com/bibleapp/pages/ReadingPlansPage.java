package com.bibleapp.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Reading plans page with a scrollable list of active plans and an add button.
 * Opens a popup for selecting and adding new reading plans.
 */
public class ReadingPlansPage extends VBox {

    private final VBox leftColumn;
    private final VBox rightColumn;
    private StackPane popupOverlay;
    private VBox popupContainer;
    private VBox popupContentArea;
    private Runnable currentClosePopupHandler;

    public ReadingPlansPage() {
        getStyleClass().add("page");
        setSpacing(20);

        Label title = new Label("Plans");
        title.getStyleClass().add("page-title");

        // Two-column layout
        HBox columnsContainer = new HBox(20);
        columnsContainer.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(columnsContainer, Priority.ALWAYS);
        VBox.setVgrow(columnsContainer, Priority.ALWAYS);

        // Left column - Active Plans
        leftColumn = new VBox(10);
        leftColumn.getStyleClass().add("reading-plans-left-column");
        leftColumn.setPrefWidth(200);
        leftColumn.setMinWidth(150);

        Label leftLabel = new Label("Active Plans");
        leftLabel.getStyleClass().add("column-header");
        leftColumn.getChildren().add(leftLabel);

        // Scrollable content area for plan items
        VBox leftScrollContent = new VBox(10);
        leftScrollContent.getStyleClass().add("reading-plans-scroll-content");
        leftScrollContent.setPadding(new Insets(10));

        ScrollPane leftScrollPane = new ScrollPane(leftScrollContent);
        leftScrollPane.getStyleClass().add("reading-plans-scroll");
        leftScrollPane.setFitToWidth(true);
        leftScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(leftScrollPane, Priority.ALWAYS);
        leftColumn.getChildren().add(leftScrollPane);

        // Add button at bottom of left column
        Button addButton = new Button("+ Add Plan");
        addButton.getStyleClass().add("add-plan-btn");
        addButton.setOnAction(e -> showAddPlanPopup());
        HBox buttonContainer = new HBox(addButton);
        buttonContainer.setAlignment(Pos.CENTER);
        leftColumn.getChildren().add(buttonContainer);

        // Right column - Progress area
        rightColumn = new VBox(10);
        rightColumn.getStyleClass().add("reading-plans-right-column");

        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        HBox.setHgrow(leftColumn, Priority.NEVER);

        columnsContainer.getChildren().addAll(leftColumn, rightColumn);
        VBox.setVgrow(columnsContainer, Priority.ALWAYS);

        getChildren().addAll(title, columnsContainer);
    }

    private void showAddPlanPopup() {
        popupOverlay = new StackPane();
        popupOverlay.getStyleClass().add("popup-overlay");
        popupOverlay.setOnMouseClicked(e -> closePopup());

        popupContainer = new VBox();
        popupContainer.getStyleClass().add("reading-plans-popup");
        popupContainer.setMaxWidth(500);
        popupContainer.setMaxHeight(600);
        popupContainer.setMinWidth(400);
        popupContainer.setMinHeight(400);

        Label popupTitle = new Label("Add Reading Plan");
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

        Label placeholder = new Label("Available reading plans will appear here");
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
