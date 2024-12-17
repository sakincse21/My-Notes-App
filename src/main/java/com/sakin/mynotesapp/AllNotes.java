package com.sakin.mynotesapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class AllNotes extends Application {

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("AllNotes.fxml"));
        Scene scene = new Scene(loader.load(), 960, 720);
        stage.setResizable(true);
        stage.setMaximized(false);
        scene.getStylesheets().add(getClass().getResource("styles/style_listView.css").toExternalForm());
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/sakin/mynotesapp/imgs/icon.png")));
        stage.setTitle("My Notes");
        stage.setScene(scene);
        stage.show();
    }
}
