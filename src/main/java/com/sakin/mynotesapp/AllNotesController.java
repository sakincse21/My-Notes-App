package com.sakin.mynotesapp;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AllNotesController {

    private static AllNotesController instance;

    public AllNotesController() {
        instance = this;
    }

    public static AllNotesController getInstance() {
        return instance;
    }

    public void refreshNotes() {
        fetchNotesForUser();
    }


    @FXML
    private Label userName;
    @FXML
    private Button logoutBtn;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TilePane tilePane;

    private ObservableList<NoteItem> notes = FXCollections.observableArrayList();
    private Set<Integer> noteIds = new HashSet<>();
    private String username;

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


    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            ensureAppDataDirectoryExists();
            username = fetchUsernameFromFile();
            userName.setText("Hello, "+username+"!");

            if (username == null || username.isEmpty()) {
                redirectToLoginPage();
                return;
            }

            configureTilePane();
            configureScrollPane();
            fetchNotesForUser();
        });
    }

    private void configureTilePane() {
        tilePane.setAlignment(Pos.CENTER);
        tilePane.setPadding(new Insets(10));
        tilePane.setHgap(10);
        tilePane.setVgap(10);
        tilePane.setPrefColumns(3); //adds 3 notes in each row by default
    }

    private void configureScrollPane() {
        scrollPane.setContent(tilePane); // tilepane as the content of scrollpane
        scrollPane.setFitToWidth(true);  // dynamic width
        scrollPane.setPannable(true);    // enables mouse drag
    }

    private String fetchUsernameFromFile() {
        File loginFile = new File(LOGIN_FILE_PATH);
        if (loginFile.exists()) {
            try (Scanner scanner = new Scanner(loginFile)) {
                String username = scanner.nextLine();
                return EncryptionUtil.decrypt(username);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void fetchNotesForUser() {
        notes.clear();
        noteIds.clear();
        tilePane.getChildren().clear();

        if (username == null || username.isEmpty()) {
            System.err.println("Username is not set. Cannot fetch notes.");
            redirectToLoginPage();
            return;
        }

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT id, content FROM notes WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username); //safely inserts the username into the query

                try (var resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        int noteId = resultSet.getInt("id");
                        String content = resultSet.getString("content");

                        if (!noteIds.contains(noteId)) { //if id not present in notes then adds it
                            NoteItem noteItem = new NoteItem(noteId, content);
                            noteIds.add(noteId);
                            addNoteToTilePane(noteItem);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addNoteToTilePane(NoteItem note) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        VBox noteBox = new VBox();
        noteBox.setPadding(new Insets(10));
        noteBox.setStyle("-fx-border-color: #d3d3d3; -fx-border-radius: 5px; -fx-background-color: #f9f9f9;");
        noteBox.setSpacing(10);
        WebView webView = new WebView();
        String htmlContent = renderer.render(parser.parse(note.getContent()));


        //style added to hide scrollbar
        String styledContent = """
        <style>
            body {
                margin: 5px;
                padding: 0;
                overflow: hidden; 
                font-family: Arial, sans-serif;
                font-size: 14px;
            }
        </style>
        """ + htmlContent;

        webView.getEngine().loadContent(styledContent);
        webView.setPrefHeight(200);
        webView.setPrefWidth(240);

        
        ImageView editImage = createImageView("edit.png", 24, 24, () -> addNoteScene(note));
        ImageView deleteImage = createImageView("delete.png", 24, 24, () -> {
            deleteNoteFromDatabase(note.getId());
            fetchNotesForUser();
        });
        ImageView summarizeImage = createImageView("summarize.png", 24, 24, () -> openSummarizeScene(note));
        ImageView viewImage = createImageView("view.png", 24, 24, () -> openViewScene(note));

        HBox imageBox = new HBox(viewImage, editImage, summarizeImage, deleteImage);
        imageBox.setSpacing(8);
        imageBox.setAlignment(Pos.CENTER_RIGHT);

        noteBox.getChildren().addAll(webView, imageBox);
        tilePane.getChildren().add(noteBox);
    }


    private ImageView createImageView(String imagePath, double width, double height, Runnable action) {
        ImageView imageView = new ImageView(new Image(getClass().getResourceAsStream("/com/sakin/mynotesapp/imgs/" + imagePath)));
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setOnMouseClicked(event -> action.run());
        return imageView;
    }


    private void openViewScene(NoteItem note) {
        try {
            Parser parser = Parser.builder().build();
            HtmlRenderer renderer = HtmlRenderer.builder().build();

            WebView webView = new WebView();
            String htmlContent = renderer.render(parser.parse(note.getContent()));
            String styledContent = """
        <style>
            body {
                margin: 5px;
                padding: 5px 20px 5px 10px;
                overflow: scroll; 
                font-family: Arial, sans-serif;
                font-size: 14px;
            }
        </style>
        """ + htmlContent;
            Stage stage = new Stage();
            webView.getEngine().loadContent(styledContent);
            webView.setPrefHeight(500);
            ImageView printImage = createImageView("print.png", 24, 24, () -> {
//                String markdownContent =  renderer.render(parser.parse(note.getContent())); // Example HTML content
                executePrintJob(webView);

            });


            VBox root = new VBox(webView, printImage);
            root.setAlignment(Pos.CENTER_RIGHT);
            root.setSpacing(10);
            root.setPadding(new Insets(20));
            Scene scene = new Scene(root, 720, 540);

            stage.setScene(scene);
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/sakin/mynotesapp/imgs/icon.png")));
            stage.setTitle("View Note");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void executePrintJob(WebView webView) {
        Printer printer = Printer.getDefaultPrinter();
        if (printer == null) {
            System.err.println("No printer found.");
            return;
        }

        PrinterJob printerJob = PrinterJob.createPrinterJob(printer);
        if (printerJob == null) {
            System.err.println("Unable to create a PrinterJob.");
            return;
        }

        PageLayout pageLayout = printer.createPageLayout(
                javafx.print.Paper.A4,
                javafx.print.PageOrientation.PORTRAIT,
                Printer.MarginType.DEFAULT
        );

        boolean success = printerJob.printPage(pageLayout, webView);
        if (success) {
            printerJob.endJob();
            System.out.println("Note printed successfully.");
        } else {
            System.err.println("Failed to print the note.");
        }
    }

    private void redirectToLoginPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginPage.fxml"));
            Parent root = loader.load();
            Stage currentStage = (Stage) logoutBtn.getScene().getWindow();
            currentStage.close();

            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(root));
            loginStage.setResizable(false);
            loginStage.setMaximized(false);
            loginStage.getIcons().add(new Image(getClass().getResourceAsStream("/com/sakin/mynotesapp/imgs/icon.png")));
            loginStage.setTitle("Login");
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void addNoteAction() {
        addNoteScene(null);
    }

    private void addNoteScene(NoteItem note) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Note.fxml"));
            Parent root = loader.load();

            NoteController controller = loader.getController();
            controller.setNoteItem(note);
            controller.setUsername(username);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/sakin/mynotesapp/imgs/icon.png")));
            stage.setTitle("Note");
            stage.setOnHidden(event -> fetchNotesForUser());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void logoutAction() {
        ensureAppDataDirectoryExists();
        File loginFile = new File(LOGIN_FILE_PATH);
        if (loginFile.exists() && loginFile.delete()) {
            System.out.println("Login file deleted. Logging out...");
        } else {
            System.err.println("Failed to delete login file or file does not exist.");
        }
        redirectToLoginPage();
    }

    public void deleteNoteFromDatabase(int noteId) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "DELETE FROM notes WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, noteId);
                statement.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openSummarizeScene(NoteItem note) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SummarizeScene.fxml"));
            Parent root = loader.load();

            SummarizeController controller = loader.getController();
            controller.setNoteItem(note);

            Stage stage = new Stage();
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/com/sakin/mynotesapp/imgs/icon.png")));
            stage.setScene(new Scene(root));
            stage.setTitle("Summarize with AI");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
