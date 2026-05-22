package com.renamer.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;

public class AmbiguousDialogController {

    // Referencias a los campos de texto del FXML
    @FXML
    private TextField txtOriginalName;

    @FXML
    private TextField txtArtist;

    @FXML
    private TextField txtSong;

    // Variables para controlar el estado de la ventana
    private Stage dialogStage;
    private boolean okClicked = false;
    private boolean cancelAll = false;

    // Este método lo llamará el servicio principal para poder cerrar la ventana luego
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    // Método para poner el nombre del archivo en el campo de solo lectura
    public void setFileName(String fileName) {
        // El nombre ya viene limpio (sin extensión) desde FileRenameService
        txtOriginalName.setText(fileName);

    }

    // Botón Aceptar
    @FXML
    private void handleAccept() {
        // Validamos que no estén vacíos (opcional, pero recomendado)
        if (txtArtist.getText().trim().isEmpty() || txtSong.getText().trim().isEmpty()) {
            // Por ahora permitimos continuar, pero podrías mostrar una alerta aquí
        }
        okClicked = true;
        dialogStage.close();
    }

    // Botón Omitir
    @FXML
    private void handleSkip() {
        // okClicked se mantiene en false, indicando que se salte este archivo
        dialogStage.close();
    }

    // Botón Cancelar Todo
    @FXML
    private void handleCancel() {
        cancelAll= true;
        dialogStage.close();
    }

    // Pre-llena el campo de Artista con la sugerencia del parser.
    public void setArtistSuggestion(String artist) {
        if (artist != null && !artist.isEmpty()) {
            txtArtist.setText(artist);
        }
    }

    // Pre-llena el campo de Canción con la sugerencia del parser.
    public void setSongSuggestion(String song) {
        if (song != null && !song.isEmpty()) {
            txtSong.setText(song);
        }
    }

    // =====================================================================
    // MINI REPRODUCTOR DE AUDIO CON TIMELINE
    // =====================================================================

    @FXML private Slider sliderProgress;
    @FXML private Label lblTime;
    @FXML private Button btnPlay;

    private MediaPlayer mediaPlayer;
    private boolean isDraggingSlider = false; // Bandera para evitar conflictos

    /**
     * Inicializa el reproductor con el archivo.
     * Se llama desde FileRenameService ANTES de mostrar el modal.
     */
    public void loadAudio(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists()) return;

        try {
            Media media = new Media(sourceFile.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            // Configurar Slider cuando la duración esté lista
            mediaPlayer.totalDurationProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    sliderProgress.setMax(newVal.toSeconds());
                    lblTime.setText(formatTime(0) + " / " + formatTime(newVal.toSeconds()));
                }
            });

            // Sincronizar Slider con el tiempo actual (CORREGIDO)
            mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                // ✅ FIX: Verificar que mediaPlayer y su duración existan
                if (!isDraggingSlider && newVal != null && mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
                    sliderProgress.setValue(newVal.toSeconds());
                    updateLabelTime(newVal.toSeconds(), mediaPlayer.getTotalDuration().toSeconds());
                }
            });

            // Manejar cuando el usuario arrastra la barra
            sliderProgress.valueChangingProperty().addListener((obs, wasChanging, isNowChanging) -> {
                isDraggingSlider = isNowChanging;
                if (!isNowChanging && mediaPlayer != null) {
                    mediaPlayer.seek(javafx.util.Duration.seconds(sliderProgress.getValue()));
                }
            });

            // Establecer volumen inicial al 30% (como solicitaste)
            mediaPlayer.setVolume(0.3);

            //System.out.println("Audio cargado para preview: " + sourceFile.getName());

        } catch (Exception e) {
            System.err.println(" Error cargando audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onPlayPause() {
        if (mediaPlayer == null) return;

        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            btnPlay.setText("▶ Reproducir");
        } else {
            mediaPlayer.play();
            btnPlay.setText("⏸ Pausar");
        }
    }

    @FXML
    private void onStop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            sliderProgress.setValue(0);
            btnPlay.setText("▶ Reproducir");
        }
    }

    // Formato de tiempo MM:SS
    private String formatTime(double seconds) {
        int min = (int) (seconds / 60);
        int sec = (int) (seconds % 60);
        return String.format("%02d:%02d", min, sec);
    }

    private void updateLabelTime(double current, double total) {
        lblTime.setText(formatTime(current) + " / " + formatTime(total));
    }

    /**
     * Limpieza al cerrar
     */
    public void disposeAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    // --- GETTERS para que el Servicio pueda leer los datos ingresados ---

    public boolean isOkClicked() {
        return okClicked;
    }

    public boolean isCancelAll() {
        return cancelAll;
    }

    public String getArtist() {
        // .trim() elimina espacios accidentales al inicio o final
        return txtArtist.getText().trim();
    }

    public String getSong() {
        return txtSong.getText().trim();
    }
}