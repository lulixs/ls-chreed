package com.bibleapp.badges;

import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Bottom-right corner toast that announces newly-earned badges. Self-removes
 * after a fade-in/hold/fade-out sequence. Multiple badges in one batch stack
 * vertically and share a single toast.
 */
public final class BadgeToast {

    private static final Duration FADE_IN  = Duration.millis(220);
    private static final Duration HOLD     = Duration.millis(3200);
    private static final Duration FADE_OUT = Duration.millis(420);

    private BadgeToast() {}

    public static void show(StackPane appRoot, List<Badge> badges) {
        if (badges == null || badges.isEmpty()) return;

        VBox content = new VBox(10);
        content.getStyleClass().add("badge-toast");
        content.setPadding(new Insets(14, 18, 14, 18));
        // Without an explicit max size, a StackPane child gets stretched to
        // fill the parent — the toast would span the full page.
        content.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label heading = new Label(badges.size() == 1 ? "Badge earned!" : badges.size() + " badges earned!");
        heading.getStyleClass().add("badge-toast-heading");
        content.getChildren().add(heading);

        for (Badge b : badges) {
            BadgeView view = new BadgeView(b, true, 44);
            Label name = new Label(b.getTitle());
            name.getStyleClass().add("badge-toast-name");
            HBox row = new HBox(12, view, name);
            row.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().add(row);
        }

        StackPane positioner = new StackPane(content);
        positioner.setMouseTransparent(true);
        positioner.setPickOnBounds(false);
        StackPane.setAlignment(content, Pos.TOP_RIGHT);
        StackPane.setMargin(content, new Insets(28, 24, 0, 0));
        HBox.setHgrow(positioner, Priority.ALWAYS);

        appRoot.getChildren().add(positioner);

        content.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(FADE_IN, content);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition hold = new PauseTransition(HOLD);

        FadeTransition fadeOut = new FadeTransition(FADE_OUT, content);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(fadeIn, hold, fadeOut);
        seq.setOnFinished(e -> Platform.runLater(() -> appRoot.getChildren().remove(positioner)));
        seq.play();
    }
}
