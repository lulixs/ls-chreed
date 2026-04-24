package com.bibleapp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        App app = new App();
        root.setCenter(app);

        Scene scene = new Scene(root, 900, 650);
        scene.getStylesheets().add(
            getClass().getResource("/com/bibleapp/styles.css").toExternalForm()
        );

        stage.setTitle("Scripture");
        stage.setScene(scene);
        stage.setMinWidth(700);
        stage.setMinHeight(500);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
