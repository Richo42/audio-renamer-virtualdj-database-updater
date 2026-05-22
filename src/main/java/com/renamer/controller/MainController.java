package com.renamer.controller;

import com.renamer.service.FileRenameService;
import com.renamer.util.VirtualDJDatabaseUpdater;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.File;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

public class MainController {

    @FXML private Button btnSelectFolder;
    @FXML private Label lblFolderPath;
    @FXML private Button btnStart;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblStatus;
    @FXML private Button btnSelectDatabase;
    @FXML private Label lblDatabasePath;

    private File selectedFolder;
    private File databaseXmlFile;
    private FileRenameService renameService;

    @FXML
    private void handleSelectFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar Carpeta de Origen");

        selectedFolder = directoryChooser.showDialog(btnSelectFolder.getScene().getWindow());

        if (selectedFolder != null) {
            lblFolderPath.setText(selectedFolder.getAbsolutePath());
            btnStart.setDisable(false);
            lblStatus.setText("Carpeta seleccionada. Listo para iniciar.");
        }
    }

    @FXML
    private void handleSelectDatabase() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Seleccionar database.xml de VirtualDJ");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("VirtualDJ Database", "*.xml")
        );

        // Intentar abrir en Documents/VirtualDJ por defecto
        File defaultDir = new File(System.getProperty("user.home") + "/Documents/VirtualDJ");
        if (defaultDir.exists()) {
            fileChooser.setInitialDirectory(defaultDir);
        }

        File selected = fileChooser.showOpenDialog(btnSelectDatabase.getScene().getWindow());

        if (selected != null && selected.getName().equalsIgnoreCase("database.xml")) {
            databaseXmlFile = selected;
            lblDatabasePath.setText(selected.getAbsolutePath());
            lblStatus.setText("Database.xml seleccionado. Listo para iniciar.");
        } else if (selected != null) {
            // Si seleccionó otro archivo, mostrar alerta
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Archivo incorrecto");
            alert.setHeaderText(null);
            alert.setContentText("Debes seleccionar el archivo 'database.xml' de VirtualDJ.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleStart() {
        // VALIDACIÓN 1: Carpeta de origen
        if (selectedFolder == null) {
            showAlert("Carpeta no seleccionada", "Debes seleccionar una carpeta de origen primero.");
            return;
        }

        // VALIDACIÓN 2: Archivo database.xml (OBLIGATORIO)
        if (databaseXmlFile == null || !databaseXmlFile.exists()) {
            showAlert("Database.xml no seleccionado",
                    "Para actualizar los tags de VirtualDJ, debes seleccionar el archivo 'database.xml'.\n\n" +
                            "Sin este archivo, los nombres cambiarán en disco pero VirtualDJ no reconocerá los cambios.");
            return;
        }

        // VALIDACIÓN 3: VirtualDJ NO debe estar abierto
        if (VirtualDJDatabaseUpdater.isVirtualDJRunning()) {
            showAlert("VirtualDJ está abierto",
                    "VirtualDJ debe estar CERRADO para actualizar su base de datos.\n\n" +
                            "1. Cierra VirtualDJ completamente.\n" +
                            "2. Vuelve a hacer clic en 'Iniciar Renombrado'.");
            return;
        }

        // Todas las validaciones pasaron: proceder con el renombrado
        btnStart.setDisable(true);
        btnSelectFolder.setDisable(true);
        if (btnSelectDatabase != null) btnSelectDatabase.setDisable(true);

        Stage stage = (Stage) btnStart.getScene().getWindow();
        renameService = new FileRenameService(stage);

        // Pasar databaseXmlFile al servicio
        var task = renameService.createRenameTask(selectedFolder, databaseXmlFile);

        progressBar.progressProperty().bind(task.progressProperty());
        lblStatus.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            btnStart.setDisable(false);
            btnSelectFolder.setDisable(false);
            if (btnSelectDatabase != null) btnSelectDatabase.setDisable(false);
            progressBar.progressProperty().unbind();
            lblStatus.textProperty().unbind();

            // Mostrar resumen final
            showAlert("Proceso completado",
                    "Renombrado finalizado.\n" +
                            "Database.xml actualizado.\n\n" +
                            "Ahora puedes abrir VirtualDJ y verás los archivos con sus nuevos nombres y tags.");
        });

        task.setOnFailed(e -> {
            showAlert("Error en el proceso", "Error: " + task.getException().getMessage());
            btnStart.setDisable(false);
            btnSelectFolder.setDisable(false);
            if (btnSelectDatabase != null) btnSelectDatabase.setDisable(false);
        });

        new Thread(task).start();
    }

    // Método auxiliar para mostrar alertas
    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Opens the About dialog with developer info.
     */
    @FXML
    private void handleAbout() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/about_dialog.fxml"));
            loader.load();

            AboutController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.UTILITY);
            stage.setTitle("About Audio Renamer");
            stage.setScene(new Scene(loader.getRoot()));
            stage.setResizable(false);

            controller.setDialogStage(stage);
            stage.show();

        } catch (Exception e) {
            System.err.println("❌ Error opening About dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

}