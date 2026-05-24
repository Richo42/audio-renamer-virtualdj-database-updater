package com.renamer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.kordamp.ikonli.javafx.FontIcon;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {

            // Cargar el diseño visual (FXML)
            // El "/" al inicio busca en la carpeta resources
            Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));

            // Título de ventana
            primaryStage.setTitle("Audio Renamer - VirtualDJ Database Updater v1.0");

            primaryStage.setScene(new Scene(root));
            primaryStage.show();

            // 🎨 Banner de bienvenida
            String welcomeBanner = """
          █████╗ ██╗   ██╗██████╗ ██╗ ██████╗     ██████╗ ███╗   ██╗███╗   ███╗███████╗██████╗ 
         ██╔══██╗██║   ██║██╔══██╗██║██╔═══██╗    ██╔══██╗████╗  ██║████╗ ████║██╔════╝██╔══██╗
         ███████║██║   ██║██║  ██║██║██║   ██║    ██████╔╝██╔██╗ ██║██╔████╔██║█████╗  ██████╔╝
         ██╔══██║██║   ██║██║  ██║██║██║   ██║    ██╔══██╗██║╚██╗██║██║╚██╔╝██║██╔══╝  ██╔══██╗
         ██║  ██║╚██████╔╝██████╔╝██║╚██████╔╝    ██║  ██║██║ ╚████║██║ ╚═╝ ██║███████╗██║  ██║
         ╚═╝  ╚═╝ ╚═════╝ ╚═════╝ ╚═╝ ╚═════╝     ╚═╝  ╚═╝╚═╝  ╚══╝╚══╝     ╚═╝╚══════╝╚═╝  ╚═╝
         ─────────────────────────────────[ DATABASE UPDATER ]─────────────────────────────────
             █ ▄ █ ▄ ▄ █ ▄ █ ▄ █   |   Developed by: Ricardo Castillo   |   ▄ █ ▄ ▄ █ ▄ █  █
        """;

            System.out.println(welcomeBanner);

        } catch (IOException e) {
            System.err.println("❌ ERROR CRÍTICO AL CARGAR LA INTERFAZ:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Fuerza a la consola a interpretar caracteres UTF-8
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        launch(args);
    }
}