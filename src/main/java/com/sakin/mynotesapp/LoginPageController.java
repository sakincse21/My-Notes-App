package com.sakin.mynotesapp;

import javafx.application.Platform;
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
import javafx.stage.StageStyle;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginPageController {

    @FXML private TextField usernameBox;
    @FXML private TextField passwordBox;
    @FXML private Label resultLabel;
    @FXML private Button loginBtn;
    @FXML private Button registerBtn;

    private static final String DB_URL = Secrets.get_DB_URL(); //sql database url
    private static final String DB_USER = Secrets.get_DB_USER(); //sql username
    private static final String DB_PASSWORD = Secrets.get_DB_PASSWORD(); //sql password

    private static String getAppDataDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return System.getenv("APPDATA") + File.separator + "MyNotesApp";
        } else if (os.contains("mac")) {
            return System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support" + File.separator + "MyNotesApp";
        } else {
            return System.getProperty("user.home") + File.separator + ".config" + File.separator + "MyNotesApp";
        }
    }

    private void ensureAppDataDirectoryExists() {
        File directory = new File(getAppDataDirectory());
        if (!directory.exists()) {
            directory.mkdirs(); // Create the directory if it doesn't exist
        }
    }

    private static final String LOGIN_FILE_PATH = getAppDataDirectory() + File.separator + "login.txt";

    public static Boolean loggedIn = false;

    @FXML
    public void initialize() {
        Platform.runLater(this::autoLogin);
    }

    private void autoLogin() {
        ensureAppDataDirectoryExists();
        String[] credentials = readLoginDetails();
        if (credentials != null) {
            String savedUsername = credentials[0];
            String savedPassword = credentials[1];

            if (isAuthenticated(savedUsername, savedPassword)) {
                loginSuccess(savedUsername);
            }
        }
    }

    @FXML
    protected void onClickLoginBtn() {
        String username = usernameBox.getText();
        String password = passwordBox.getText();

        if (username.isEmpty() || password.isEmpty()) {
            resultLabel.setTextFill(Paint.valueOf("red"));
            resultLabel.setText("Please fill out all fields.");
            return;
        }

        validateUserFromDatabase(username, password);
    }

    private void validateUserFromDatabase(String username, String password) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            if (validateUser(connection, username, password)) {
                saveLoginDetails(username, password);
                loginSuccess(username);
            } else {
                resultLabel.setTextFill(Paint.valueOf("red"));
                resultLabel.setText("Invalid username or password. Please try again.");
                loggedIn = false;
            }
        } catch (Exception e) {
            resultLabel.setTextFill(Paint.valueOf("red"));
            resultLabel.setText("Database connection error. Please try again later.");
            e.printStackTrace();
        }
    }

    private boolean validateUser(Connection connection, String username, String password) throws Exception {
        String query = "SELECT password FROM login WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return password.equals(resultSet.getString("password"));
                }
            }
        }
        return false;
    }

    private void saveLoginDetails(String username, String password) {
        ensureAppDataDirectoryExists();
        File loginFile = new File(LOGIN_FILE_PATH);
        try (Writer writer = new FileWriter(loginFile)) {
            writer.write(EncryptionUtil.encrypt(username) + System.lineSeparator());
            writer.write(EncryptionUtil.encrypt(password) + System.lineSeparator());
            System.out.println("Login credentials saved securely.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] readLoginDetails() {
        ensureAppDataDirectoryExists();
        File loginFile = new File(LOGIN_FILE_PATH);
        if (loginFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(loginFile))) {
                String username = EncryptionUtil.decrypt(reader.readLine());
                String password = EncryptionUtil.decrypt(reader.readLine());
                return new String[]{username, password};
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private boolean isAuthenticated(String username, String password) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            return validateUser(connection, username, password);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void loginSuccess(String username) {
        try {
            resultLabel.setTextFill(Paint.valueOf("green"));
            resultLabel.setText("Login successful! Welcome " + username + ".");
            loggedIn = true;

            Platform.runLater(() -> {
                try {
                    Stage stage = (Stage) loginBtn.getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("AllNotes.fxml"));
                    Scene scene = new Scene(loader.load(), 960, 720);

                    stage.setResizable(true);
                    stage.setMaximized(false);
                    scene.getStylesheets().add(getClass().getResource("styles/style_listView.css").toExternalForm());
                    stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/sakin/mynotesapp/imgs/icon.png")));
                    stage.setTitle("My Notes");
                    stage.setScene(scene);
                    stage.show();
                } catch (IOException e) {
                    resultLabel.setTextFill(Paint.valueOf("red"));
                    resultLabel.setText("Error loading the next page.");
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() {
        try {
            Stage stage = (Stage) registerBtn.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("RegisterPage.fxml"));
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.setTitle("Sign Up");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void setForgetBtn() {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ForgetPassword.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setTitle("Forget Password");
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/sakin/mynotesapp/imgs/icon.png")));
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
