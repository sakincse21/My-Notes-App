package com.sakin.mynotesapp;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ForgetPasswordController {

    @FXML
    private TextField usernameBox;
    @FXML
    private Label resultLabel;

    private static final String DB_URL = Secrets.get_DB_URL(); //sql database url
    private static final String DB_USER = Secrets.get_DB_USER(); //sql username
    private static final String DB_PASSWORD = Secrets.get_DB_PASSWORD(); //sql password

    @FXML
    protected void onClickSendEmail() {
        String username = usernameBox.getText();

        if (username.isEmpty()) {
            resultLabel.setTextFill(Paint.valueOf("red"));
            resultLabel.setText("Please enter a username.");
            return;
        }

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT email, password FROM login WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String email = resultSet.getString("email");
                        String password = resultSet.getString("password");
                        sendEmail(email, password);
                        resultLabel.setTextFill(Paint.valueOf("green"));
                        resultLabel.setText("Password sent to your email.");
                    } else {
                        resultLabel.setTextFill(Paint.valueOf("red"));
                        resultLabel.setText("User not found.");
                    }
                }
            }
        } catch (Exception e) {
            resultLabel.setTextFill(Paint.valueOf("red"));
            resultLabel.setText("Database error. Please try again.");
            e.printStackTrace();
        }
    }

    private void sendEmail(String email, String password) {
        System.out.println("Sending email to " + email + " with password: " + password);
    }
}
