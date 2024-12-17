package com.sakin.mynotesapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.regex.Pattern;

public class RegisterPageController {

    @FXML
    private TextField usernameBox;
    @FXML
    private TextField passwordBox;
    @FXML
    private TextField emailBox; // New field for email
    @FXML
    private Label resultLabel;
    @FXML
    private Button loginBtn;

    public static Boolean signedUp = false;


    private static final String DB_URL = Secrets.get_DB_URL(); //sql database url
    private static final String DB_USER = Secrets.get_DB_USER(); //sql username
    private static final String DB_PASSWORD = Secrets.get_DB_PASSWORD(); //sql password

    private void insertUser() {
        String username = usernameBox.getText();
        String password = passwordBox.getText();
        String email = emailBox.getText();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            resultLabel.setTextFill(Paint.valueOf("red"));
            resultLabel.setText("Please fill out all fields.");
            return;
        }

        if (!isPasswordValid(password)) {
            resultLabel.setTextFill(Paint.valueOf("red"));
            resultLabel.setText("Password must be at least 6 characters, including uppercase, lowercase, a number, and a special character.");
            return;
        }

        if (!isEmailValid(email)) {
            resultLabel.setTextFill(Paint.valueOf("red"));
            resultLabel.setText("Please enter a valid email address.");
            return;
        }

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            if (doesUserExist(connection, username)) {
                resultLabel.setTextFill(Paint.valueOf("orange"));
                resultLabel.setText("User exists, try to log in or use a different username.");
                return;
            }

            //inserting user to db if user does not exist
            String query = "INSERT INTO login (username, password, email) VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, email);

            int rows = statement.executeUpdate();
            if (rows > 0) {
                resultLabel.setTextFill(Paint.valueOf("green"));
                resultLabel.setText("Registration successful!");
                switchToLoginPage();
            }
        } catch (SQLException e) {
            resultLabel.setTextFill(Paint.valueOf("red"));
            resultLabel.setText("Database Error: " + e.getMessage());
        }
    }

    private boolean doesUserExist(Connection connection, String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM login WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0; // row counts check, if > 0 then user exists
                }
            }
        }
        return false;
    }


    //regex to check valid password
    private boolean isPasswordValid(String password) {
        String regex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(password).matches();
    }

    //regex to check valid email
    private boolean isEmailValid(String email) {
        String regex = "^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(email).matches();
    }

    @FXML
    protected void onClickRegisterBtn(ActionEvent event) {
        String username = usernameBox.getText();
        String password = passwordBox.getText();
        String email = emailBox.getText();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            resultLabel.setTextFill(Paint.valueOf("red"));
            resultLabel.setText("Please fill out all fields.");
        } else {
            insertUser();
        }
    }

    @FXML
    private void handleBackToLogin() {
        switchToLoginPage();
    }

    private void switchToLoginPage() {
        try {
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginPage.fxml"));
            Parent root = loader.load();
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.setScene(new Scene(root));
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/sakin/mynotesapp/imgs/icon.png")));
            stage.setTitle("Log In");
            stage.show();
        } catch (IOException e) {
            resultLabel.setTextFill(Paint.valueOf("red"));
            resultLabel.setText("Error loading LoginPage.fxml.");
            e.printStackTrace();
        }
    }
}
