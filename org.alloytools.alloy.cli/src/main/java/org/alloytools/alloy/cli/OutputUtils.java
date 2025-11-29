package org.alloytools.alloy.cli;

/**
 * Utility class for string sanitization and escaping.
 * These methods are used for generating various output formats.
 * Extracted for better testability and mutation testing.
 */
public class OutputUtils {

    /**
     * Sanitize identifier for DOT format
     */
    public static String sanitizeId(String s) {
        if (s == null || s.isEmpty()) return "unknown";
        String result = s.replace("$", "_").replace("/", "_").replace("\"", "\\\"").trim();
        if (result.isEmpty()) return "unknown";
        return result;
    }

    /**
     * Sanitize identifier for RDF URI
     */
    public static String sanitizeRdfId(String s) {
        if (s == null || s.isEmpty()) return "unknown";
        String result = s.replace("$", "_")
                .replace("/", "_")
                .replace(" ", "_")
                .replace("\"", "")
                .replace("'", "")
                .replace("<", "")
                .replace(">", "")
                .replace(":", "_")
                .trim();
        if (result.isEmpty()) return "unknown";
        return result;
    }

    /**
     * Escape special characters for RDF literal
     */
    public static String escapeRdf(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * Escape special characters for YAML output
     */
    public static String escapeYaml(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
