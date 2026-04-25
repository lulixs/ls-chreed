package com.bibleapp;

import com.bibleapp.pages.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

/**
 * Main application container with sidebar navigation and page content area.
 * Contains the CHREED logo, navigation tabs, user profile section, and popup.
 */
public class App extends StackPane {

    private final BorderPane contentArea;
    private final HBox mainView;
    private final VBox sidebar;
    private StackPane userIcon;
    private Runnable currentClosePopupHandler;

    private static final String[] TABS = {"Stats", "Read", "Plans", "Memorize"};

    public App() {
        getStyleClass().add("app");

        sidebar = buildSidebar();

        contentArea = new BorderPane();
        contentArea.getStyleClass().add("content-area");
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        mainView = new HBox();
        mainView.getStyleClass().add("main-view");
        mainView.getChildren().addAll(sidebar, contentArea);

        getChildren().add(mainView);

        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && currentClosePopupHandler != null) {
                currentClosePopupHandler.run();
            }
        });
        setFocusTraversable(true);

        showPage("Stats");
        highlightTab(0);
    }

    private VBox buildSidebar() {
        VBox sb = new VBox(0);
        sb.getStyleClass().add("sidebar");
        sb.setPrefWidth(240);

        // Logo display
        ImageView chreedLogo = new ImageView(new Image(getClass().getResourceAsStream("/com/bibleapp/chreed.png")));
        chreedLogo.setPreserveRatio(true);
        chreedLogo.setFitWidth(220);
        StackPane logoBox = new StackPane(chreedLogo);
        logoBox.setPadding(new Insets(30, 10, 20, 10));
        logoBox.setPrefWidth(240);
        sb.getChildren().add(logoBox);

        // Navigation tabs
        VBox navContainer = new VBox(0);
        VBox.setVgrow(navContainer, Priority.ALWAYS);

        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            final String label = TABS[i];

            Label btn = new Label(label);
            btn.getStyleClass().add("nav-btn");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setId("nav-" + i);
            btn.setOnMouseClicked(e -> {
                showPage(label);
                highlightTab(index);
            });

            navContainer.getChildren().add(btn);
        }
        sb.getChildren().add(navContainer);

        // User profile section at bottom
        VBox userSection = new VBox(10);
        userSection.getStyleClass().add("user-section");
        userSection.setPadding(new Insets(15, 20, 15, 20));
        userSection.setOnMouseClicked(e -> showUserPopup());

        userIcon = createUserIcon();
        userIcon.getStyleClass().add("user-icon-container");

        Label userName = new Label("Profile");
        userName.getStyleClass().add("user-name-label");

        HBox userInfoContainer = new HBox(10);
        userInfoContainer.setAlignment(Pos.CENTER_LEFT);
        userInfoContainer.getChildren().addAll(userIcon, userName);

        userSection.getChildren().add(userInfoContainer);

        HBox iconContainer = new HBox(userSection);
        iconContainer.setAlignment(Pos.BOTTOM_LEFT);
        sb.getChildren().add(iconContainer);

        return sb;
    }

    private StackPane createUserIcon() {
        Circle circle = new Circle(20);
        circle.getStyleClass().add("user-icon-circle");

        Text text = new Text("U");
        text.getStyleClass().add("user-icon-text");

        return new StackPane(circle, text);
    }

    private void showUserPopup() {
        currentClosePopupHandler = this::closePopup;

        // Full-screen overlay that closes popup when clicked
        Pane overlay = new Pane();
        overlay.getStyleClass().add("popup-overlay");
        overlay.setPrefSize(3000, 3000);
        overlay.setPickOnBounds(true);
        overlay.setOnMouseClicked(e -> {
            closePopup();
            e.consume();
        });

        // Popup content container
        VBox popupContent = new VBox();
        popupContent.getStyleClass().add("user-popup");
        popupContent.setAlignment(Pos.CENTER);
        popupContent.setMaxWidth(200);
        popupContent.setMaxHeight(100);
        popupContent.setMouseTransparent(false);
        popupContent.setOnMouseClicked(e -> e.consume());

        StackPane popupWrapper = new StackPane(popupContent);
        popupWrapper.getStyleClass().add("popup-wrapper");
        popupWrapper.setAlignment(Pos.CENTER);
        popupWrapper.setPickOnBounds(false);
        popupWrapper.setOnMouseClicked(e -> closePopup());

        getChildren().addAll(overlay, popupWrapper);
        requestFocus();
    }

    private void closePopup() {
        getChildren().removeIf(node ->
            node.getStyleClass().contains("popup-overlay") ||
            node.getStyleClass().contains("popup-wrapper")
        );
        currentClosePopupHandler = null;
    }

    private void showPage(String name) {
        switch (name) {
            case "Read" -> contentArea.setCenter(new ReadingPage());
            case "Plans" -> contentArea.setCenter(new ReadingPlansPage());
            case "Stats" -> contentArea.setCenter(new StatisticsPage());
            case "Memorize" -> contentArea.setCenter(new MemorizationPage(new StackPane()));
        }
    }

    private void highlightTab(int activeIndex) {
        for (int i = 0; i < TABS.length; i++) {
            Label btn = (Label) sidebar.lookup("#nav-" + i);
            if (btn != null) {
                btn.getStyleClass().remove("nav-btn--active");
                if (i == activeIndex) btn.getStyleClass().add("nav-btn--active");
            }
        }
    }
}
