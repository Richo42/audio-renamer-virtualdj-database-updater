package com.renamer.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

public class SmartNameParser {

    // =====================================================================
    // PATRONES DE EXPRESIONES REGULARES (SIN ESPACIOS BASURA)
    // =====================================================================

    // 1. Busca DJ/Editor dentro de corchetes [ ] o paréntesis ( )
    private static final Pattern DJ_PATTERN = Pattern.compile(
            "\\[([^\\]]*DJ[^\\]]*)\\]|\\(([^)]*DJ[^)]*)\\)", Pattern.CASE_INSENSITIVE);

    // 2. Busca "DJ" o "Deejay" FUERA de [] o () (ej: "dj pokra - ...")
    private static final Pattern DJ_STANDALONE_PATTERN = Pattern.compile(
            "(?i)\\b((?:DJ|Deejay)\\s+[^\\s\\-\\[\\(\\)]+)(?=\\s*-|\\s*$)");

    // 3. Limpieza de basura: @spam, $, VIRUS, PUTEAR, puntos residuales
    private static final Pattern JUNK_PATTERN = Pattern.compile(
            "@.*?(?=\\s-|\\)|$)|\\$+|VIRUS|PUTEAR|#\\S+|\\.(?=\\s|$)", Pattern.CASE_INSENSITIVE);

    // =====================================================================
    // LISTA DE ARTISTAS CONOCIDOS (Para inferencia automática)
    // =====================================================================
    private static final Set<String> KNOWN_ARTISTS = Set.of(
            "karol g", "bad bunny", "j balvin", "ozuna", "anuel aa",
            "daddy yankee", "maluma", "sech", "rauw alejandro", "myke towers",
            "feid", "shakira", "jennifer lopez", "selena", "alacranes musical",
            "grupo niche", "el gran combo", "willie colon", "hector lavoe",
            "marc anthony", "victor manuelle", "gilberto santa rosa",
            "los angeles azules", "sonora dinamita", "rafaga", "hermanos yaipen",
            "mdo", "alpha academy", "kevo dj", "rihanna", "calvin harris",
            "elvis crespo", "tono rosario", "nene malo", "clotta", "tom boxer",
            "jeyki", "edusx", "clarion", "flori", "andy", "donzio", "micky beat",
            "hermanos silva", "hermanos", "yaipen", "jason derulo", "nicky minaj",
            "swalla", "chantaje", "grupo 5"
    );

    // =====================================================================
    // RESULTADO DEL PARSER
    // =====================================================================
    public static class ParseResult {
        public String artist;
        public String song;
        public String editor;
        public boolean needsReview;
    }

    // =====================================================================
    // MÉTODO PRINCIPAL DE ANÁLISIS
    // =====================================================================
    public static ParseResult analyze(String rawFileName) {
        ParseResult result = new ParseResult();

        // =====================================================================
        // FASE 1: EXTRAER DJ (ANTES DE LIMPIAR BASURA)
        // =====================================================================

        // 1. DJ dentro de [] o ()
        Matcher djMatcher = DJ_PATTERN.matcher(rawFileName);
        String editor = "";
        if (djMatcher.find()) {
            editor = djMatcher.group(1) != null ? djMatcher.group(1) : djMatcher.group(2);
            rawFileName = djMatcher.replaceAll("").trim();
        }

        // 2. DJ Standalone (ej: "dj pokra - ...") - SOLO si editor está vacío
        if (editor.isEmpty()) {
            Matcher djStandalone = DJ_STANDALONE_PATTERN.matcher(rawFileName);
            if (djStandalone.find()) {
                editor = djStandalone.group(1).trim();
                rawFileName = djStandalone.replaceAll("").trim();
            }
        }

        // 3. DJ al final (ej: "... - DJ Mix")
        Pattern djEndPattern = Pattern.compile("\\s*-\\s*((?:DJ|Deejay)\\s+[^-]+)$", Pattern.CASE_INSENSITIVE);
        Matcher djEndMatcher = djEndPattern.matcher(rawFileName);
        if (djEndMatcher.find()) {
            String potentialDJ = djEndMatcher.group(1).trim();
            if (potentialDJ.split("\\s+").length >= 2 || potentialDJ.contains("DJ") || potentialDJ.contains("Deejay")) {
                editor = potentialDJ;
                rawFileName = djEndPattern.matcher(rawFileName).replaceAll("").trim();
            }
        }

        // =====================================================================
        // FASE 2: LIMPIAR BASURA Y ARTEFACTOS
        // =====================================================================

        // Eliminar basura (@, #, $, etc.)
        String name = JUNK_PATTERN.matcher(rawFileName).replaceAll("").trim();

        // Limpiar artefactos finales huérfanos (ej: " - (", " - [")
        name = name.replaceAll("\\s*-\\s*[\\(\\[\\{]\\s*$", "").trim();

        // =====================================================================
        // FASE 3: SEPARAR ARTISTA Y CANCIÓN
        // =====================================================================

        int guionCount = name.split(" - ", -1).length - 1;

        if (guionCount > 1) {
            // 🚨 MÁS DE 1 GUION → Ambiguo → Ventana modal
            result.artist = "";
            result.song = name;
            result.needsReview = true;

        } else if (guionCount == 1 && name.contains(" - ")) {

            String[] parts = name.split(" - ", 2);
            String left = parts[0].trim();
            String right = parts[1].trim();

            boolean leftHasTags = left.contains("(") || left.contains("[");
            boolean rightHasTags = right.contains("(") || right.contains("[");

            // ✅ CASO A: Izquierda SIN tags, Derecha CON tags
            if (!leftHasTags && rightHasTags) {
                result.artist = left;
                result.song = right;
                result.needsReview = false;

                // ✅ CASO B: Izquierda CON tags, Derecha SIN tags
            } else if (leftHasTags && !rightHasTags) {
                result.song = left;
                result.artist = right;
                result.needsReview = false;

                // ⚠️ CASO C: Ambos tienen tags O ninguno tiene → HEURÍSTICAS
            } else {
                boolean leftIsArtist = isProbablyArtist(left);
                boolean rightIsArtist = isProbablyArtist(right);
                boolean leftIsSong = isProbablySong(left);
                boolean rightIsSong = isProbablySong(right);

                if (leftIsArtist && rightIsSong && !rightIsArtist) {
                    result.artist = left;
                    result.song = right;
                    result.needsReview = false;
                } else if (rightIsArtist && leftIsSong && !leftIsArtist) {
                    result.artist = right;
                    result.song = left;
                    result.needsReview = false;
                } else if (leftIsArtist && !rightIsArtist) {
                    result.artist = left;
                    result.song = right;
                    result.needsReview = false;
                } else if (rightIsArtist && !leftIsArtist) {
                    result.artist = right;
                    result.song = left;
                    result.needsReview = false;
                } else {
                    // Aún ambiguo → Ventana manual
                    result.artist = left;
                    result.song = right;
                    result.needsReview = true;
                }
            }

        } else {
            // SIN GUIONES → Muy ambiguo → Ventana modal
            result.artist = "";
            result.song = name;
            result.needsReview = true;
        }

        result.editor = editor;
        return result;
    }

    // =====================================================================
    // MÉTODOS AUXILIARES DE HEURÍSTICA
    // =====================================================================

    /**
     * Determina si un texto es probablemente un ARTISTA.
     */
    private static boolean isProbablyArtist(String text) {
        if (text == null || text.isEmpty()) return false;
        String lowerText = text.toLowerCase().trim();
        int score = 0;

        // Regla 1: Coincide con artista conocido
        for (String artist : KNOWN_ARTISTS) {
            if (lowerText.contains(artist)) {
                score += 5;
                break;
            }
        }

        // Regla 2: Indicadores de colaboración (Ft, Feat, Vs, X)
        if (lowerText.matches("(?i).*\\b(ft|feat|featuring|vs|versus)\\b.*") || lowerText.contains(" x ")) {
            score += 3;
        }

        // Regla 3: Texto corto y sin tags de versión
        if (text.trim().split("\\s+").length <= 4) score += 1;
        if (!lowerText.matches(".*[\\(\\[].*(remix|edit|intro|vip|mix|version|clean|extended|short|original|afro|moombahton|salsa|merengue|transition)[\\)\\]].*")) {
            score += 2;
        }

        // Regla 4: Comienza con DJ/Deejay
        if (lowerText.matches("(?i)^(dj|deejay)\\s+.*")) score += 1;

        return score >= 3;
    }

    /**
     * Determina si un texto es probablemente una CANCIÓN.
     */
    private static boolean isProbablySong(String text) {
        if (text == null || text.isEmpty()) return false;
        String lowerText = text.toLowerCase().trim();
        int score = 0;

        // Regla 1: Contiene tags de versión/remix
        if (lowerText.matches(".*[\\(\\[].*(remix|edit|intro|vip|mix|version|clean|extended|short|original|afro|moombahton|salsa|merengue|transition|acapella|drums|starter)[\\)\\]].*")) {
            score += 3;
        }

        // Regla 2: Termina con guion huérfano
        if (text.trim().endsWith("-")) score += 1;

        // Regla 3: Texto más largo
        if (text.trim().split("\\s+").length >= 5) score += 1;

        // Regla 4: No es un artista conocido
        boolean matchesKnownArtist = false;
        for (String artist : KNOWN_ARTISTS) {
            if (lowerText.contains(artist)) {
                matchesKnownArtist = true;
                break;
            }
        }
        if (!matchesKnownArtist) score += 1;

        return score >= 2;
    }
}