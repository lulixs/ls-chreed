package com.bibleapp.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bibleapp.badges.Badge;
import com.bibleapp.badges.BadgeRegistry;
import com.bibleapp.badges.BadgeService;
import com.bibleapp.badges.BadgeView;
import com.bibleapp.badges.BadgesDialog;
import com.bibleapp.data.DataStore;
import com.bibleapp.data.MemorizedVerse;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.bibleapp.data.DataStore;
import com.bibleapp.data.MemorizedVerse;

import java.util.*;

/**
 * Statistics page with badges, streak tracking, and reading progress.
 * Two-column layout with badges on the left, streak and progress on the right.
 */
public class StatisticsPage extends VBox {

    private static final int RECENT_BADGE_PREVIEW_COUNT = 6;

    private final StackPane appRoot;

    public StatisticsPage(StackPane appRoot) {
        this.appRoot = appRoot;
        getStyleClass().add("page");
        setSpacing(20);

        Label title = new Label("Stats");
        title.getStyleClass().add("page-title");

        HBox mainContainer = new HBox(20);
        mainContainer.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(mainContainer, Priority.ALWAYS);
        VBox.setVgrow(mainContainer, Priority.ALWAYS);

        VBox leftColumn = buildBadgesColumn();
        VBox rightColumn = buildRightColumn();

        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        HBox.setHgrow(rightColumn, Priority.ALWAYS);
        leftColumn.setPrefWidth(300);
        rightColumn.setPrefWidth(300);

        mainContainer.getChildren().addAll(leftColumn, rightColumn);
        VBox.setVgrow(mainContainer, Priority.ALWAYS);

        getChildren().addAll(title, mainContainer);
    }

    // =========================================================================
    // Badges column
    // =========================================================================

    private VBox buildBadgesColumn() {
        VBox column = new VBox(10);
        column.getStyleClass().add("stats-left-column");

        Label header = new Label("Badges");
        header.getStyleClass().add("column-header");
        column.getChildren().add(header);

        VBox content = new VBox(10);
        content.getStyleClass().add("stats-scroll-content");
        content.setPadding(new Insets(10));

        List<Badge> earned = BadgeService.earned();
        int total = BadgeRegistry.all().size();

        Label summary = new Label(earned.size() + " of " + total + " earned");
        summary.getStyleClass().add("badges-summary");
        content.getChildren().add(summary);

        Label preamble = new Label(earned.isEmpty() ? "Earn your first badge by completing a verse." : "Recently earned");
        preamble.getStyleClass().add("badges-preamble");
        content.getChildren().add(preamble);

        if (!earned.isEmpty()) {
            FlowPane preview = new FlowPane(10, 10);
            int show = Math.min(RECENT_BADGE_PREVIEW_COUNT, earned.size());
            for (int i = earned.size() - show; i < earned.size(); i++) {
                Badge b = earned.get(i);
                VBox cell = new VBox(4);
                cell.setAlignment(Pos.TOP_CENTER);
                cell.setPrefWidth(80);
                BadgeView view = new BadgeView(b, true, 56);
                Label name = new Label(b.getTitle());
                name.getStyleClass().add("badge-cell-name");
                name.setWrapText(true);
                cell.getChildren().addAll(view, name);
                preview.getChildren().add(cell);
            }
            content.getChildren().add(preview);
        }

        Button viewAll = new Button("View all badges");
        viewAll.getStyleClass().add("badges-view-all-btn");
        viewAll.setMaxWidth(Double.MAX_VALUE);
        viewAll.setOnAction(e -> {
            if (appRoot != null) BadgesDialog.show(appRoot);
        });
        content.getChildren().add(viewAll);

        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("stats-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        column.getChildren().add(scroll);
        return column;
    }

    // =========================================================================
    // Right column (unchanged scaffold for streak + progress)
    // =========================================================================

    private VBox buildRightColumn() {
        VBox rightColumn = new VBox(10);
        rightColumn.getStyleClass().add("stats-right-column");

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

        VBox progressSection = new VBox(10);
        progressSection.getStyleClass().add("stats-progress-section");

        Label progressLabel = new Label("Progress");
        progressLabel.getStyleClass().add("column-header");
        progressSection.getChildren().add(progressLabel);

        VBox progressContent = buildProgressContent();
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
        return rightColumn;
    }

    // =========================================================================
    // Progress section: per-book bars sorted by recency of activity
    // =========================================================================

    /**
     * One bar per book the user has touched. Books are ordered by recency of
     * activity — DataStore appends/moves the most recently changed entry to
     * the tail of the memorization list, so the index of the LATEST entry
     * belonging to each book is a "last activity" signal.
     */
    private VBox buildProgressContent() {
        VBox content = new VBox(10);

        List<MemorizedVerse> verses = DataStore.getMemorizationList();
        if (verses.isEmpty()) {
            Label empty = new Label("Add verses on the Read tab to start tracking progress.");
            empty.getStyleClass().add("badges-preamble");
            empty.setWrapText(true);
            content.getChildren().add(empty);
            return content;
        }

        // book -> most recent index, fully-memorized count
        Map<String, Integer> lastIndexByBook = new HashMap<>();
        Map<String, Integer> fullyMemorizedByBook = new HashMap<>();
        for (int i = 0; i < verses.size(); i++) {
            MemorizedVerse v = verses.get(i);
            lastIndexByBook.put(v.getBook(), i);
            if (v.getNextDifficulty() >= 4) {
                fullyMemorizedByBook.merge(v.getBook(), 1, Integer::sum);
            }
        }

        List<String> books = new ArrayList<>(lastIndexByBook.keySet());
        books.sort((a, b) -> Integer.compare(lastIndexByBook.get(b), lastIndexByBook.get(a)));

        for (String book : books) {
            int memorized = fullyMemorizedByBook.getOrDefault(book, 0);
            int total = BadgeRegistry.totalVersesIn(book);
            content.getChildren().add(buildBookProgressRow(book, memorized, total));
        }

        return content;
    }

    private VBox buildBookProgressRow(String book, int memorized, int total) {
        VBox row = new VBox(4);
        row.getStyleClass().add("progress-row");

        Label name = new Label(book);
        name.getStyleClass().add("progress-row-name");

        Label count = new Label(memorized + " / " + total + " fully memorized");
        count.getStyleClass().add("progress-row-count");

        HBox header = new HBox(name, new Region(), count);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        ProgressBar bar = new ProgressBar();
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setProgress(total == 0 ? 0 : Math.min(1.0, (double) memorized / total));
        // Tint the fill with the same per-book color used by the badges.
        bar.setStyle("-fx-accent: " + BadgeRegistry.colorForBook(book) + ";");

        row.getChildren().addAll(header, bar);
        return row;
    }

    private VBox buildProgressContent() {

        VBox container = new VBox(10);
    
        List<MemorizedVerse> verses = DataStore.getMemorizationList();
    
        if (verses.isEmpty()) {
            Label empty = new Label("No memorized verses yet.");
            empty.getStyleClass().add("popup-placeholder");
            container.getChildren().add(empty);
            return container;
        }
    
        // Group by book
        Map<String, List<MemorizedVerse>> byBook = new HashMap<>();
    
        for (MemorizedVerse v : verses) {
            byBook.computeIfAbsent(v.getBook(), k -> new ArrayList<>()).add(v);
        }
    
        for (String book : byBook.keySet()) {
    
            List<MemorizedVerse> bookVerses = byBook.get(book);
    
            int total = bookVerses.size();
    
            // 🔥 IMPORTANT: define "fully memorized"
            int mastered = (int) bookVerses.stream()
                    .filter(v -> v.getNextDifficulty() >= 4) // adjust if needed
                    .count();
    
            double percent = (double) mastered / total;
    
            VBox card = new VBox(6);
            card.getStyleClass().add("stats-book-card");
            card.setPadding(new Insets(8));
    
            Label title = new Label(book);
            title.getStyleClass().add("stats-book-title");
    
            Label countLabel = new Label(
                    mastered + " / " + total + " memorized"
            );
    
            ProgressBar bar = new ProgressBar(percent);
            bar.setPrefWidth(250);
    
            card.getChildren().addAll(title, countLabel, bar);
            container.getChildren().add(card);
        }
    
        return container;
    }
}
