package com.renamer.util;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.*;
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
                    .anyMatch(p -> p.info().command().orElse("").toLowerCase().contains("virtualdj"));
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
            // Parsear PRESERVANDO TODO
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

                for (int i = 0; i < songs.getLength(); i++) {
                    Element song = (Element) songs.item(i);
                    String currentPath = song.getAttribute("FilePath");

                    // Buscar por ruta EXACTA
                    if (currentPath.equals(rec.oldPath)) {
                        // MODIFICACIÓN MÍNIMA: Solo 3 atributos
                        song.setAttribute("FilePath", rec.newPath);
                        song.setAttribute("FileSize", String.valueOf(rec.newSize));

                        // Actualizar Tags SOLO si existe
                        NodeList tagsList = song.getElementsByTagName("Tags");
                        if (tagsList.getLength() > 0) {
                            Element tags = (Element) tagsList.item(0);

                            // SOLO actualizar Title y Author, PRESERVAR todo lo demás
                            tags.setAttribute("Title", rec.bpm + " " + rec.song.toUpperCase().trim());
                            tags.setAttribute("Author", rec.bpm + " " + rec.artist.toUpperCase().trim());

                            // Eliminar SOLO Remix (si existe)
                            if (tags.hasAttribute("Remix")) {
                                tags.removeAttribute("Remix");
                            }
                        }

                        // Actualizar LastModified en Infos SOLO si existe
                        NodeList infosList = song.getElementsByTagName("Infos");
                        if (infosList.getLength() > 0) {
                            Element infos = (Element) infosList.item(0);
                            infos.setAttribute("LastModified", String.valueOf(System.currentTimeMillis() / 1000));
                        }

                        found = true;
                        updatedCount++;
                        System.out.println("✅ " + new File(rec.oldPath).getName());
                        break;
                    }
                }

                if (!found) {
                    System.out.println("⚠️ No encontrado: " + new File(rec.oldPath).getName());
                }
            }

            // Guardar SIN cambiar formato
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "no"); // VirtualDJ prefiere sin indentación
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");

            // Guardar en temporal
            File tempFile = new File(databaseFile.getParent(), "database.xml.tmp");
            StreamResult result = new StreamResult(tempFile);
            transformer.transform(new DOMSource(doc), result);

            // Validar
            try {
                factory.newDocumentBuilder().parse(tempFile);
            } catch (Exception e) {
                throw new Exception("XML inválido: " + e.getMessage());
            }

            // Reemplazar
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