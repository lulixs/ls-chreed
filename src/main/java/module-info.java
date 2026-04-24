module com.bibleapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires json.simple;
    requires java.net.http;

    opens com.bibleapp to javafx.fxml;
    exports com.bibleapp;
    exports com.bibleapp.pages;
    exports com.bibleapp.data;
    exports com.bibleapp.services;
}
