package com.renamer.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BPMParser {

    // Patrón para detectar rangos: "60 - 120"
    private static final Pattern BPM_RANGE_PATTERN = Pattern.compile("\\b(\\d{2,3})\\s*-\\s*(\\d{2,3})\\b");

    // Patrón para detectar número con contexto: "(92 bpm)", "128"
    private static final Pattern BPM_CONTEXT_PATTERN = Pattern.compile("\\(?\\s*(\\d{2,3})\\s*\\)?\\s*bpm", Pattern.CASE_INSENSITIVE);

    // Patrón para limpiar BPM al INICIO del string
    private static final Pattern BPM_PREFIX_PATTERN = Pattern.compile(
            "^\\s*(?:\\d{2,3}\\s*-\\s*)?[\\(\\[]?\\s*\\d{2,3}(?:\\s*-\\s*\\d{2,3})?\\s*(?:bpm)?\\s*[\\)\\]]?\\s*-?\\s*",
            Pattern.CASE_INSENSITIVE
    );

    // Patrón para limpiar BPM al FINAL del string
    // Elimina espacios + número + "bpm" opcional al final exacto de la cadena
    private static final Pattern BPM_SUFFIX_PATTERN = Pattern.compile(
            "\\s+\\d{2,3}\\s*(?:bpm)?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Extrae el BPM válido (60-180) del nombre del archivo.
     */
    public static String extractBPM(String fileName) {
        String nameToAnalyze = fileName.trim();
        String bpm = "000";

        // 1. Buscar rango primero
        Matcher rangeMatcher = BPM_RANGE_PATTERN.matcher(nameToAnalyze);
        if (rangeMatcher.find()) {
            int low = Integer.parseInt(rangeMatcher.group(1));
            int high = Integer.parseInt(rangeMatcher.group(2));
            if (low >= 60 && high <= 180 && low < high) {
                bpm = low + " - " + high;
            }
        }

        // 2. Buscar con contexto "bpm" o paréntesis
        if (bpm.equals("000")) {
            Matcher contextMatcher = BPM_CONTEXT_PATTERN.matcher(nameToAnalyze);
            if (contextMatcher.find()) {
                int val = Integer.parseInt(contextMatcher.group(1));
                if (val >= 60 && val <= 180) bpm = String.valueOf(val);
            }
        }

        // 3. Buscar cualquier número válido de 2-3 dígitos
        if (bpm.equals("000")) {
            Matcher numberMatcher = Pattern.compile("\\b(\\d{2,3})\\b").matcher(nameToAnalyze);
            while (numberMatcher.find()) {
                int val = Integer.parseInt(numberMatcher.group(1));
                if (val >= 60 && val <= 180) {
                    bpm = String.valueOf(val);
                    break;
                }
            }
        }
        return bpm;
    }

    /**
     * Elimina el prefijo y sufijo de BPM del nombre, dejando solo el texto limpio.
     */
    public static String removeBPMPrefix(String fileName) {
        String nameToAnalyze = fileName.trim();

        // 1. Eliminar BPM al inicio
        String rest = BPM_PREFIX_PATTERN.matcher(nameToAnalyze).replaceAll("").trim();
        if (rest.startsWith("- ")) rest = rest.substring(2);

        // 2. Eliminar BPM al final (NUEVO: para casos como "... ] 85")
        rest = BPM_SUFFIX_PATTERN.matcher(rest).replaceAll("").trim();
        if (rest.endsWith(" -")) rest = rest.substring(0, rest.length() - 2).trim();

        return rest;
    }
}