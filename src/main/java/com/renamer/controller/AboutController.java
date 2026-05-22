package com.renamer.controller;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.net.URI;

/**
 * Controller for the About dialog window.
 * Displays developer info and external links.
 */
public class AboutController {

    private Stage dialogStage;

    /**
     * Sets the dialog stage reference for closing.
     * @param stage The stage of this dialog.
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Closes the About dialog.
     */
    @FXML
    private void onClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Opens Ricardo Castillo's LinkedIn profile in default browser.
     */
    @FXML
    private void onOpenLinkedIn() {
        openUrl("https://www.linkedin.com/in/ricardo-castillo-perez/");
    }

    /**
     * Opens the GitHub repository in default browser.
     */
    @FXML
    private void onOpenRepo() {
        openUrl("https://github.com/Richo42/audio-renamer-virtualdj-database-updater");
    }

    /**
     * Helper method to open URL in system default browser.
     * @param url The URL to open.
     */
    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            System.err.println("❌ Error opening URL: " + url);
            e.printStackTrace();
        }
    }
}