package com.renamer.service;

import com.renamer.controller.AmbiguousDialogController;
import com.renamer.util.BPMParser;
import com.renamer.util.SmartNameParser;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import com.renamer.util.VirtualDJDatabaseUpdater;
import com.renamer.util.VirtualDJDatabaseUpdater.VDJRecord;
import lombok.Setter;


public class FileRenameService {

    private final Stage primaryStage;

    // CAMPO NUEVO: Aquí se guardará la ruta del database.xml seleccionada
    @Setter
    private File databaseXmlFile;

    public FileRenameService(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public Task<Void> createRenameTask(File sourceFolder, File databaseXml) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Escaneando...");
                File[] allFiles = sourceFolder.listFiles();
                if (allFiles == null) { updateMessage("Error."); return null; }

                List<File> allFilesList = new ArrayList<>();
                for (File f : allFiles) if (f.isFile()) allFilesList.add(f);
                if (allFilesList.isEmpty()) { updateMessage("Vacía."); return null; }

                String destFolderName = sourceFolder.getName() + "_RENOMBRADO";
                File destFolder = new File(sourceFolder.getParentFile(), destFolderName);
                int suffix = 1;
                while (destFolder.exists()) {
                    destFolder = new File(sourceFolder.getParentFile(), sourceFolder.getName() + "_RENOMBRADO_" + suffix);
                    suffix++;
                }
                destFolder.mkdirs();

                updateMessage("Iniciando con " + allFilesList.size() + " archivos...");
                int totalFiles = allFilesList.size();
                int processedCount = 0;

                // 🔑 LISTA PARA ACUMULAR CAMBIOS DE VIRTUALDJ
                List<VDJRecord> dbChanges = new ArrayList<>();

                for (File sourceFile : allFilesList) {
                    if (isCancelled()) break;
                    try {
                        // 🔑 PASAR dbChanges AL PROCESO
                        int result = processFile(sourceFile, destFolder, dbChanges);

                        if (result == -1) { updateMessage("Cancelado."); break; }
                        else if (result == 0) updateMessage("Omitido: " + sourceFile.getName());
                        else processedCount++;

                        updateProgress(processedCount, totalFiles);
                        updateMessage("Procesados: " + processedCount + "/" + totalFiles);
                    } catch (Exception e) {
                        System.err.println("Error: " + sourceFile.getName());
                    }
                }

                // 🔑 ACTUALIZAR DATABASE.XML AL FINAL
                if (databaseXml != null && !dbChanges.isEmpty()) {
                    updateMessage("Actualizando VirtualDJ...");
                    VirtualDJDatabaseUpdater.updateDatabase(databaseXml, dbChanges);
                }

                updateMessage("¡Completado! " + processedCount);
                return null;
            }
        };
    }

    private int processFile(File sourceFile, File destFolder, List<VDJRecord> dbChanges) throws Exception {
        String originalName = sourceFile.getName();
        String lowerName = originalName.toLowerCase().trim();

        boolean isAudio = lowerName.contains(".mp3") || lowerName.contains(".m4a") ||
                lowerName.endsWith(".wav") || lowerName.endsWith(".flac") ||
                lowerName.endsWith(".ogg") || lowerName.endsWith(".aac") ||
                lowerName.endsWith(".wma") || lowerName.endsWith(".mp4") ||
                lowerName.endsWith(".mpeg") || lowerName.endsWith(".aif") ||
                lowerName.endsWith(".aiff");

        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
        String nameWithoutExt = originalName.contains(".") ? originalName.substring(0, originalName.lastIndexOf('.')) : originalName;
        nameWithoutExt = stripResidualExtensions(nameWithoutExt);

        String finalName;

        if (!isAudio) {
            finalName = "---- " + nameWithoutExt + " ----";
        } else {
            String bpm = BPMParser.extractBPM(nameWithoutExt);
            String nameClean = BPMParser.removeBPMPrefix(nameWithoutExt);
            SmartNameParser.ParseResult parsed = SmartNameParser.analyze(nameClean);

            // 🔑 PASAR sourceFile AL MODAL PARA AUDIO
            if (parsed.needsReview || parsed.artist.isEmpty()) {
                SmartNameParser.ParseResult manual = showAmbiguousDialog(nameClean, parsed, sourceFile);
                if (manual == null) return 0;
                if (manual.artist == null) return -1;
                parsed = manual;
            }

            finalName = buildName(bpm, parsed, extension);

            File destFile = new File(destFolder, finalName);
            if (destFile.exists()) {
                int counter = 1;
                String base = finalName.contains(".") ? finalName.substring(0, finalName.lastIndexOf('.')) : finalName;
                while (destFile.exists()) {
                    destFile = new File(destFolder, base + " (" + counter + ")" + extension);
                    counter++;
                }
            }
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 🔑 REGISTRAR CAMBIO PARA VIRTUALDJ
            VDJRecord record = new VDJRecord();
            record.oldPath = sourceFile.getAbsolutePath();
            record.newPath = destFile.getAbsolutePath();
            record.newSize = destFile.length();
            record.bpm = bpm;
            record.artist = parsed.artist;
            record.song = parsed.song;
            record.editor = parsed.editor;
            dbChanges.add(record);
        }

        return 1;
    }

    private String buildName(String bpm, SmartNameParser.ParseResult parsed, String originalExt) {
        String ext = originalExt;

        // Limpiar artista: eliminar guiones al inicio/final
        String artist = parsed.artist.toUpperCase().trim();
        artist = artist.replaceAll("^-\\s*|\\s*-$", ""); // Elimina "-" al inicio o final

        if (parsed.editor != null && !parsed.editor.isEmpty()) {
            artist += " [" + parsed.editor.toUpperCase() + "]";
        }

        // Limpiar canción: eliminar guiones al inicio/final
        String song = parsed.song.toUpperCase().trim();
        song = song.replaceAll("^-\\s*|\\s*-$", ""); // Elimina "-" al inicio o final

        // Formato final: BPM + CANCIÓN + " - " + ARTISTA + EXTENSIÓN
        return bpm + " " + song + " - " + artist + ext;
    }

    /**
     * Usa Platform.runLater()
     */
    private SmartNameParser.ParseResult showAmbiguousDialog(String fileName, SmartNameParser.ParseResult suggestion, File sourceFile) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<SmartNameParser.ParseResult> resultRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ambiguous_dialog.fxml"));
                loader.load();
                AmbiguousDialogController controller = loader.getController();

                // 🔑 INICIALIZAR REPRODUCTOR DE AUDIO
                controller.loadAudio(sourceFile);

                Stage dialogStage = new Stage();
                dialogStage.initModality(Modality.APPLICATION_MODAL);
                dialogStage.initStyle(StageStyle.UTILITY);
                dialogStage.setTitle("Verificar Separación");
                dialogStage.setScene(new Scene(loader.getRoot()));
                dialogStage.setAlwaysOnTop(true);

                controller.setDialogStage(dialogStage);
                controller.setFileName(fileName);
                if (suggestion != null) {
                    controller.setArtistSuggestion(suggestion.artist);
                    controller.setSongSuggestion(suggestion.song);
                }

                dialogStage.setOnHidden(e -> {
                    // 🔑 LIMPIAR REPRODUCTOR AL CERRAR
                    controller.disposeAudio();

                    if (controller.isCancelAll()) {
                        SmartNameParser.ParseResult r = new SmartNameParser.ParseResult(); r.artist = null; resultRef.set(r);
                    } else if (controller.isOkClicked()) {
                        SmartNameParser.ParseResult r = new SmartNameParser.ParseResult();
                        r.artist = controller.getArtist();
                        r.song = controller.getSong();
                        r.editor = suggestion != null ? suggestion.editor : "";
                        resultRef.set(r);
                    } else {
                        resultRef.set(null);
                    }
                    latch.countDown();
                });
                dialogStage.show();
            } catch (Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });

        try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return resultRef.get();
    }

    /**
     * Elimina extensiones de audio residuales del nombre (ej: .mp3, .wav en medio del texto).
     * Esto evita que queden "basura" como '.mp3' pegado al título después de quitar la extensión real.
     */
    private String stripResidualExtensions(String name) {
        // Lista de extensiones de audio conocidas que queremos eliminar si aparecen en medio
        String[] audioExts = {".mp3", ".m4a", ".wav", ".flac", ".ogg", ".aac", ".wma", ".mp4", ".mpeg", ".aif", ".aiff"};

        String cleanName = name;
        for (String ext : audioExts) {
            // Reemplaza TODAS las ocurrencias de esa extensión en el string (case-insensitive)
            cleanName = cleanName.replaceAll("(?i)" + Pattern.quote(ext), "");
        }
        // Limpieza final de espacios o puntos huérfanos dejados por el borrado
        return cleanName.replaceAll("\\s{2,}", " ").replaceAll("\\s*-\\s*$", "").trim();
    }

}