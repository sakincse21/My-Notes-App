package com.sakin.mynotesapp;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SummarizeController {

    @FXML
    private TextArea promptBox;
    @FXML
    private Button sendBtn;

    private NoteItem noteItem;

    private static final String GEMINI_API_URL = Secrets.get_GEMINI_API_URL();
    private static final String API_KEY = Secrets.get_API_KEY();

    public void setNoteItem(NoteItem noteItem) {
        this.noteItem = noteItem;
    }

    @FXML
    public void initialize() {
        sendBtn.setOnAction(event -> summarizeNoteWithGemini());
    }

    private void summarizeNoteWithGemini() {
        String userPrompt = promptBox.getText();
        String defaultPrompt = "Elaborate the note precisely.";
        String prompt = userPrompt.isEmpty() ? defaultPrompt : userPrompt;
        String requestContent = prompt + "\n\nNote: " + noteItem.getContent();

        try {
            String response = sendToGeminiAPI(requestContent);

            if (response != null) {
                noteItem.setContent(response);
                updateNoteInDatabase(noteItem);
                AllNotesController.getInstance().refreshNotes();
                Stage stage = (Stage) sendBtn.getScene().getWindow();
                stage.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String sendToGeminiAPI(String content) throws Exception {
        URL url = new URL(GEMINI_API_URL + "?key=" + API_KEY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        JSONObject payload = new JSONObject();
        JSONObject textPart = new JSONObject();
        textPart.put("text", content);

        JSONObject contentPart = new JSONObject();
        contentPart.put("parts", new JSONObject[]{textPart});

        payload.put("contents", new JSONObject[]{contentPart});

        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.toString().getBytes());
            os.flush();
        }

        if (connection.getResponseCode() == 200) {
            StringBuilder response = new StringBuilder();
            try (var scanner = new java.util.Scanner(connection.getInputStream())) {
                while (scanner.hasNextLine()) {
                    response.append(scanner.nextLine());
                }
            }
            JSONObject jsonResponse = new JSONObject(response.toString());

            return jsonResponse
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
        } else {
            System.err.println("Error: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
            return null;
        }
    }

    private void updateNoteInDatabase(NoteItem note) {
        final String DB_URL = Secrets.get_DB_URL(); //sql database url
        final String DB_USER = Secrets.get_DB_USER(); //sql username
        final String DB_PASSWORD = Secrets.get_DB_PASSWORD(); //sql password

        try (java.sql.Connection connection = java.sql.DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "UPDATE notes SET content = ? WHERE id = ?";
            try (java.sql.PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, note.getContent());
                statement.setInt(2, note.getId());
                statement.executeUpdate();
                System.out.println("Note updated in database: " + note.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
