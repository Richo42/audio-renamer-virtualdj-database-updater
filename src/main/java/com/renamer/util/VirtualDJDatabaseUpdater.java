package com.renamer.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class VirtualDJDatabaseUpdater {

    public static class VDJRecord {
        public String oldPath;
        public String newPath;
        public long newSize;
        public String bpm;
        public String artist;
        public String song;
        public String editor;
    }

    public static boolean isVirtualDJRunning() {
        try {
            return ProcessHandle.allProcesses()
                    .anyMatch(p -> p.info().command().orElse(" ").toLowerCase().contains("virtualdj"));
        } catch (Exception e) {
            return false;
        }
    }

    public static void updateDatabase(File databaseFile, List<VDJRecord> records) throws Exception {
        if (isVirtualDJRunning()) {
            throw new IllegalStateException("VirtualDJ está ABIERTO. Ciérralo completamente.");
        }
        if (!databaseFile.exists()) {
            throw new IllegalArgumentException("Database.xml no encontrado");
        }
        if (records == null || records.isEmpty()) {
            System.out.println("No hay cambios para aplicar.");
            return;
        }

        // Backup
        File backupFile = new File(databaseFile.getParent(), "database.xml.backup");
        if (backupFile.exists()) backupFile.delete();
        Files.copy(databaseFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(false);
            factory.setIgnoringElementContentWhitespace(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(databaseFile);
            doc.getDocumentElement().normalize();

            int updatedCount = 0;

            for (VDJRecord rec : records) {
                NodeList songs = doc.getElementsByTagName("Song");
                boolean found = false;

                // Comparar SOLO por nombre de archivo (ignora carpetas y mayúsculas)
                String searchName = new File(rec.oldPath).getName().toLowerCase();

                for (int i = 0; i < songs.getLength(); i++) {
                    Element song = (Element) songs.item(i);
                    String currentPath = song.getAttribute("FilePath");
                    String currentName = new File(currentPath).getName().toLowerCase();

                    if (currentName.equals(searchName)) {
                        // ✅ Actualizar ruta y tamaño
                        song.setAttribute("FilePath", rec.newPath);
                        song.setAttribute("FileSize", String.valueOf(rec.newSize));

                        // ✅ Gestionar nodo <Tags>
                        NodeList tagsList = song.getElementsByTagName("Tags");
                        Element tags;
                        if (tagsList.getLength() == 0) {
                            tags = doc.createElement("Tags");
                            song.appendChild(tags);
                        } else {
                            tags = (Element) tagsList.item(0);
                        }

                        // ✅ Limpiar BPM
                        String cleanBpm = (rec.bpm != null && !rec.bpm.equals("000")) ? rec.bpm : "000";

                        // ✅ Garantizar artista (Fallback si está vacío)
                        String cleanArtist = (rec.artist != null && !rec.artist.trim().isEmpty())
                                ? rec.artist.trim()
                                : "";

                        if (cleanArtist.isEmpty()) {
                            // Extraer del nuevo nombre: "BPM CANCION - ARTISTA.ext"
                            String newName = new File(rec.newPath).getName().replaceAll("\\.[^.]+$", "").trim();
                            int lastDash = newName.lastIndexOf(" - ");
                            if (lastDash > 0) {
                                cleanArtist = newName.substring(lastDash + 3).trim();
                            }
                        }

                        // ✅ FORZAR FORMATO SOLICITADO: "BPM ARTISTA" y "BPM CANCION"
                        if (rec.song != null && !rec.song.trim().isEmpty()) {
                            tags.setAttribute("Title", cleanBpm + " " + rec.song.trim().toUpperCase());
                        }
                        if (!cleanArtist.isEmpty()) {
                            tags.setAttribute("Author", cleanBpm + " " + cleanArtist.toUpperCase());
                        }

                        // Eliminar Remix si existe
                        if (tags.hasAttribute("Remix")) {
                            tags.removeAttribute("Remix");
                        }

                        // Actualizar LastModified
                        NodeList infos = song.getElementsByTagName("Infos");
                        if (infos.getLength() > 0) {
                            ((Element) infos.item(0)).setAttribute("LastModified", String.valueOf(System.currentTimeMillis() / 1000));
                        }

                        found = true;
                        updatedCount++;
                        System.out.println("✅ Actualizado: " + currentName + " → Author: " + tags.getAttribute("Author"));
                        break;
                    }
                }

                if (!found) {
                    System.out.println("⚠️ No encontrado: " + searchName);
                }
            }

            // Guardar XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");

            File tempFile = new File(databaseFile.getParent(), "database.xml.tmp");
            StreamResult result = new StreamResult(tempFile);
            transformer.transform(new DOMSource(doc), result);

            // Validar
            try {
                factory.newDocumentBuilder().parse(tempFile);
            } catch (Exception e) {
                throw new Exception("XML inválido: " + e.getMessage());
            }

            Files.move(tempFile.toPath(), databaseFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ DB actualizada: " + updatedCount + " canciones");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            if (backupFile.exists()) {
                Files.move(backupFile.toPath(), databaseFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.err.println("🔄 Backup restaurado");
            }
            throw e;
        }
    }
}