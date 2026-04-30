package com.bibleapp.badges;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bibleapp.data.DataStore;
import com.bibleapp.data.MemorizedVerse;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Full-screen modal listing every badge in the catalog, grouped by category
 * and (for mastery) by book. Locked badges are desaturated and show progress
 * toward the next threshold.
 */
public final class BadgesDialog {

    private BadgesDialog() {}

    public static void show(StackPane appRoot) {
        List<MemorizedVerse> verses = DataStore.getMemorizationList();
        Set<String> earnedIds = new HashSet<>();
        for (Badge b : BadgeRegistry.all()) {
            if (b.isEarned(verses)) earnedIds.add(b.getId());
        }

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("popup-overlay");

        VBox panel = new VBox(16);
        panel.getStyleClass().add("badges-dialog");
        panel.setPadding(new Insets(20, 24, 20, 24));
        panel.setMaxWidth(820);
        panel.setMaxHeight(640);
        panel.setMinWidth(560);

        Label title = new Label("Badges");
        title.getStyleClass().add("badges-dialog-title");

        Label summary = new Label(earnedIds.size() + " of " + BadgeRegistry.all().size() + " earned");
        summary.getStyleClass().add("badges-dialog-summary");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("badges-dialog-close");

        HBox header = new HBox(12, title, summary, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(20);
        body.setFillWidth(true);
        body.getChildren().add(buildSection("Milestones", milestones(), earnedIds, verses));
        for (Map.Entry<String, List<Badge>> e : masteryByBook().entrySet()) {
            body.getChildren().add(buildSection(e.getKey(), e.getValue(), earnedIds, verses));
        }

        ScrollPane scroll = new ScrollPane(body);
        scroll.getStyleClass().add("badges-dialog-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.getChildren().addAll(header, scroll);

        StackPane wrapper = new StackPane(panel);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Runnable close = () -> appRoot.getChildren().removeAll(overlay, wrapper);
        overlay.setOnMouseClicked(e -> close.run());
        closeBtn.setOnAction(e -> close.run());
        // Block clicks on the panel itself from closing the dialog.
        panel.setOnMouseClicked(e -> e.consume());

        appRoot.getChildren().addAll(overlay, wrapper);
    }

    private static VBox buildSection(String heading, List<Badge> badges, Set<String> earnedIds, List<MemorizedVerse> verses) {
        VBox section = new VBox(8);
        Label sectionLabel = new Label(heading);
        sectionLabel.getStyleClass().add("badges-section-heading");

        FlowPane grid = new FlowPane(14, 14);
        grid.setPrefWrapLength(760);
        for (Badge b : badges) {
            grid.getChildren().add(buildCell(b, earnedIds.contains(b.getId()), verses));
        }
        section.getChildren().addAll(sectionLabel, grid);
        return section;
    }

    private static VBox buildCell(Badge badge, boolean earned, List<MemorizedVerse> verses) {
        BadgeView view = new BadgeView(badge, earned, 72);

        Label name = new Label(badge.getTitle());
        name.getStyleClass().add("badge-cell-name");
        name.setWrapText(true);

        Label sub;
        if (earned) {
            sub = new Label("Earned");
            sub.getStyleClass().addAll("badge-cell-sub", "badge-cell-sub--earned");
        } else {
            Badge.Progress p = badge.getProgress(verses);
            sub = new Label(p.current() + " / " + p.target());
            sub.getStyleClass().addAll("badge-cell-sub", "badge-cell-sub--locked");
        }

        VBox cell = new VBox(4, view, name, sub);
        cell.setAlignment(Pos.TOP_CENTER);
        cell.setPrefWidth(110);
        cell.setMaxWidth(110);
        return cell;
    }

    private static List<Badge> milestones() {
        List<Badge> out = new ArrayList<>();
        for (Badge b : BadgeRegistry.all()) {
            if (b.getCategory() == BadgeCategory.MILESTONE) out.add(b);
        }
        return out;
    }

    /** Mastery badges grouped by book, preserving canonical order. */
    private static Map<String, List<Badge>> masteryByBook() {
        Map<String, List<Badge>> out = new LinkedHashMap<>();
        for (String book : BadgeRegistry.BIBLE_BOOK_ORDER) out.put(book, new ArrayList<>());
        for (Badge b : BadgeRegistry.all()) {
            if (b.getCategory() == BadgeCategory.MASTERY) {
                out.get(b.getBook()).add(b);
            }
        }
        // Drop empty buckets (shouldn't happen given current registry, but defensive).
        out.entrySet().removeIf(e -> e.getValue().isEmpty());
        return out;
    }
}
