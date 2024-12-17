package com.sakin.mynotesapp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class NoteController {

    @FXML
    private TextArea noteTextArea;
    @FXML
    private Button saveButton;


    private NoteItem noteItem;
    private String username;

    private static final String DB_URL = Secrets.get_DB_URL(); //sql database url
    private static final String DB_USER = Secrets.get_DB_USER(); //sql username
    private static final String DB_PASSWORD = Secrets.get_DB_PASSWORD(); //sql password

    public void setNoteItem(NoteItem noteItem) {
        this.noteItem = noteItem;
        if (noteItem != null) {
            noteTextArea.setText(noteItem.getContent());
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @FXML
    public void onSaveButtonClicked() {
        String updatedContent = noteTextArea.getText().trim();
        if (updatedContent.isEmpty()) {
            System.out.println("Note content is empty, not saving.");
            return;
        }

        if (noteItem == null) {
            saveNewNoteToDatabase(updatedContent);
        } else {
            updateNoteInDatabase(noteItem.getId(), updatedContent);
        }

        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private void saveNewNoteToDatabase(String content) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "INSERT INTO notes (username, content) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                statement.setString(2, content);
                statement.executeUpdate();
                System.out.println("New note saved.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateNoteInDatabase(int noteId, String content) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "UPDATE notes SET content = ? WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, content);
                statement.setInt(2, noteId);
                statement.executeUpdate();
                System.out.println("Note updated successfully.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onAIButtonClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UseAI.fxml"));
            Parent root = loader.load();

            UseAIController useAIController = loader.getController();
            useAIController.setNoteController(this);

            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/sakin/mynotesapp/imgs/icon.png")));
            stage.setScene(new Scene(root));
            stage.setTitle("Use AI");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendToNoteText(String aiContent) {
        String currentText = noteTextArea.getText();
        noteTextArea.setText(currentText + "\n\n" + aiContent);
    }
}
