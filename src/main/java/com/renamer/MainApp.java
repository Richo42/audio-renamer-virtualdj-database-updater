package com.renamer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Cargar el diseño visual (FXML)
            // El "/" al inicio busca en la carpeta resources
            Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));

            primaryStage.setTitle("Renamer - Audio File Organizer");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}