package com.chachamaru.harness.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Configuration synchronization utilities.
 * Handles loading, saving, merging, and validating configuration files.
 */
public class ConfigSync {

    /**
     * Sync configuration content to file.
     *
     * @param filePath Path to the configuration file
     * @param content Configuration content to write
     * @throws IOException if file operation fails
     */
    public static void syncConfigToFile(String filePath, String content) throws IOException {
        Path path = Paths.get(filePath);
        Path parentDir = path.getParent();

        // Create parent directories if they don't exist
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Files.writeString(path, content);
    }

    /**
     * Load configuration from file.
     *
     * @param filePath Path to the configuration file
     * @return Parsed configuration map
     * @throws IOException if file operation fails
     */
    public static Map<String, Object> loadConfigFromFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return new HashMap<>();
        }

        String content = Files.readString(path);
        return HarnessTomlParser.parse(content);
    }

    /**
     * Merge two configuration strings (second overrides first).
     *
     * @param baseToml Base TOML configuration
     * @param overrideToml Override TOML configuration
     * @return Merged configuration map
     */
    public static Map<String, Object> mergeConfigs(String baseToml, String overrideToml) {
        Map<String, Object> base = HarnessTomlParser.parse(baseToml);
        Map<String, Object> override = HarnessTomlParser.parse(overrideToml);

        Map<String, Object> merged = new HashMap<>(base);

        // Override with values from second config
        for (Map.Entry<String, Object> entry : override.entrySet()) {
            if (entry.getValue() instanceof Map && merged.get(entry.getKey()) instanceof Map) {
                // Deep merge for nested sections
                Map<String, Object> baseSection = (Map<String, Object>) merged.get(entry.getKey());
                Map<String, Object> overrideSection = (Map<String, Object>) entry.getValue();
                Map<String, Object> mergedSection = new HashMap<>(baseSection);
                mergedSection.putAll(overrideSection);
                merged.put(entry.getKey(), mergedSection);
            } else {
                merged.put(entry.getKey(), entry.getValue());
            }
        }

        return merged;
    }

    /**
     * Validate configuration has required fields.
     *
     * @param toml TOML configuration string
     * @return true if configuration is valid
     */
    public static boolean validateConfig(String toml) {
        Map<String, Object> config = HarnessTomlParser.parse(toml);

        if (!config.containsKey("harness")) {
            return false;
        }

        Map<String, Object> harness = (Map<String, Object>) config.get("harness");

        // Required fields for [harness] section
        if (!harness.containsKey("version") || !harness.containsKey("backend")) {
            return false;
        }

        // Validate backend value
        String backend = (String) harness.get("backend");
        if (!Arrays.asList("codex", "cursor", "auto").contains(backend)) {
            return false;
        }

        return true;
    }

    /**
     * Generate configuration from template with variable substitution.
     *
     * @param variables Map of variable names to values
     * @return Generated configuration string
     */
    public static String generateFromTemplate(Map<String, String> variables) {
        String template = ConfigTemplate.generateDefault();

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            template = template.replace(placeholder, entry.getValue());
        }

        return template;
    }

    /**
     * Create default configuration file at specified path.
     *
     * @param filePath Path where to create the configuration
     * @throws IOException if file creation fails
     */
    public static void createDefaultConfig(String filePath) throws IOException {
        String content = ConfigTemplate.generateDefault();
        syncConfigToFile(filePath, content);
    }

    /**
     * Ensure configuration file exists, create if missing.
     *
     * @param filePath Path to the configuration file
     * @return true if file was created, false if it already existed
     * @throws IOException if file operation fails
     */
    public static boolean ensureConfigExists(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            createDefaultConfig(filePath);
            return true;
        }
        return false;
    }
}
