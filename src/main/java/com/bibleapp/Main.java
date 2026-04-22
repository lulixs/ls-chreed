package com.bibleapp;

import com.bibleapp.data.DataStore;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // Print the user data JSON so it can be eyeballed in the terminal.
        // On first launch this also triggers creation of the file with empty
        // defaults; on subsequent launches it simply reads what's already there.
        DataStore.printSnapshot();

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
