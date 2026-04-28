package com.bibleapp.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Statistics page with badges, streak tracking, and reading progress.
 * Two-column layout with badges on the left, streak and progress on the right.
 */
public class StatisticsPage extends VBox {

    public StatisticsPage() {
        getStyleClass().add("page");
        setSpacing(20);

        Label title = new Label("Stats");
        title.getStyleClass().add("page-title");

        HBox mainContainer = new HBox(20);
        mainContainer.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(mainContainer, Priority.ALWAYS);
        VBox.setVgrow(mainContainer, Priority.ALWAYS);

        // Left column - Badges
        VBox leftColumn = new VBox(10);
        leftColumn.getStyleClass().add("stats-left-column");

        Label badgesLabel = new Label("Badges");
        badgesLabel.getStyleClass().add("column-header");
        leftColumn.getChildren().add(badgesLabel);

        VBox badgesContent = new VBox(10);
        badgesContent.getStyleClass().add("stats-scroll-content");
        badgesContent.setPadding(new Insets(10));

        ScrollPane badgesScroll = new ScrollPane(badgesContent);
        badgesScroll.getStyleClass().add("stats-scroll");
        badgesScroll.setFitToWidth(true);
        badgesScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        badgesScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(badgesScroll, Priority.ALWAYS);
        leftColumn.getChildren().add(badgesScroll);

        // Right column - Streak (top) and Progress (bottom)
        VBox rightColumn = new VBox(10);
        rightColumn.getStyleClass().add("stats-right-column");

        // Top - Streak section
        VBox streakSection = new VBox(10);
        streakSection.getStyleClass().add("stats-streak-section");

        Label streakLabel = new Label("Streak");
        streakLabel.getStyleClass().add("column-header");
        streakSection.getChildren().add(streakLabel);

        VBox streakContent = new VBox(10);
        streakContent.getStyleClass().add("stats-streak-content");
        streakContent.setPadding(new Insets(10));
        VBox.setVgrow(streakContent, Priority.ALWAYS);
        streakSection.getChildren().add(streakContent);

        VBox.setVgrow(streakSection, Priority.ALWAYS);

        // Bottom - Progress section
        VBox progressSection = new VBox(10);
        progressSection.getStyleClass().add("stats-progress-section");

        Label progressLabel = new Label("Progress");
        progressLabel.getStyleClass().add("column-header");
        progressSection.getChildren().add(progressLabel);

        VBox progressContent = new VBox(10);
        progressContent.getStyleClass().add("stats-progress-content");
        progressContent.setPadding(new Insets(10));

        ScrollPane progressScroll = new ScrollPane(progressContent);
        progressScroll.getStyleClass().add("stats-scroll");
        progressScroll.setFitToWidth(true);
        progressScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        progressScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(progressScroll, Priority.ALWAYS);
        progressSection.getChildren().add(progressScroll);

        VBox.setVgrow(progressSection, Priority.ALWAYS);

        rightColumn.getChildren().addAll(streakSection, progressSection);

        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        leftColumn.setPrefWidth(300);
        rightColumn.setPrefWidth(300);

        mainContainer.getChildren().addAll(leftColumn, rightColumn);
        VBox.setVgrow(mainContainer, Priority.ALWAYS);

        getChildren().addAll(title, mainContainer);
    }
}
