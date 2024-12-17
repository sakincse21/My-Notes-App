module com.sakin.mynotesapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.xml.dom;
    requires com.google.gson;
    requires java.sql;
    requires java.net.http;
    requires org.json;
    requires flexmark;
    requires javafx.web;
    requires jdk.compiler;
    requires org.apache.pdfbox;
    requires org.jsoup;


    opens com.sakin.mynotesapp to javafx.fxml;
    exports com.sakin.mynotesapp;
}