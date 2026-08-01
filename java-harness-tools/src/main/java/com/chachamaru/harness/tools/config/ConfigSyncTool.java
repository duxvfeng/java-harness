package com.chachamaru.harness.tools.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Configuration synchronization tool for Claude Code Harness.
 *
 * <p>Generates and synchronizes configuration files between Java Harness
 * and Claude Code's configuration system. Handles:
 * <ul>
 *   <li>Claude Code settings.json generation</li>
 *   <li>Harness-specific configuration files</li>
 *   <li>Configuration validation and migration</li>
 * </ul>
 *
 * @spec_reference spec.md#Configuration Management
 */
public class ConfigSyncTool {

    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;
    private final String projectName;

    /**
     * Creates a config sync tool with default project name detection.
     */
    public ConfigSyncTool() {
        this(detectProjectName());
    }

    /**
     * Creates a config sync tool with explicit project name.
     *
     * @param projectName Project name
     */
    public ConfigSyncTool(String projectName) {
        this.projectName = projectName != null ? projectName : "java-harness";
        YAMLFactory yamlFactory = YAMLFactory.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .build();
        this.yamlMapper = new ObjectMapper(yamlFactory)
            .enable(SerializationFeature.INDENT_OUTPUT);
        this.jsonMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    /**
     * Detects the project name from the current directory.
     *
     * @return Detected project name, or "java-harness" if unable to detect
     */
    private static String detectProjectName() {
        Path currentPath = Paths.get("").toAbsolutePath();
        String path = currentPath.toString();
        // Extract project name from path (last directory name)
        String[] parts = path.split(File.separator.equals("\\") ? "\\\\" : File.separator);
        return parts.length > 0 ? parts[parts.length - 1] : "java-harness";
    }

    /**
     * Generates Claude Code settings.json configuration.
     *
     * @param targetDir Target directory for settings.json
     * @return Path to generated settings.json
     * @throws ConfigSyncException if generation fails
     */
    public Path generateClaudeCodeSettings(Path targetDir) throws ConfigSyncException {
        if (targetDir == null) {
            throw new IllegalArgumentException("targetDir cannot be null");
        }

        try {
            Files.createDirectories(targetDir);
            Path settingsPath = targetDir.resolve("settings.json");

            Map<String, Object> settings = createClaudeCodeSettings();
            jsonMapper.writeValue(settingsPath.toFile(), settings);

            System.out.println("[ConfigSync] Generated Claude Code settings: " + settingsPath);
            return settingsPath;

        } catch (IOException e) {
            throw new ConfigSyncException("Failed to generate Claude Code settings", e);
        }
    }

    /**
     * Generates harness.yaml configuration template.
     *
     * @param targetDir Target directory for harness.yaml
     * @return Path to generated harness.yaml
     * @throws ConfigSyncException if generation fails
     */
    public Path generateHarnessConfig(Path targetDir) throws ConfigSyncException {
        if (targetDir == null) {
            throw new IllegalArgumentException("targetDir cannot be null");
        }

        try {
            Files.createDirectories(targetDir);
            Path configPath = targetDir.resolve("harness.yaml");

            Map<String, Object> config = createHarnessConfig();
            yamlMapper.writeValue(configPath.toFile(), config);

            System.out.println("[ConfigSync] Generated harness config: " + configPath);
            return configPath;

        } catch (IOException e) {
            throw new ConfigSyncException("Failed to generate harness config", e);
        }
    }

    /**
     * Synchronizes configuration from Java source to Claude Code.
     *
     * @param claudeConfigDir Claude Code .claude directory
     * @return Synchronization result
     * @throws ConfigSyncException if sync fails
     */
    public ConfigSyncResult syncToClaudeCode(Path claudeConfigDir) throws ConfigSyncException {
        if (claudeConfigDir == null) {
            throw new IllegalArgumentException("claudeConfigDir cannot be null");
        }

        try {
            Path settingsPath = generateClaudeCodeSettings(claudeConfigDir);
            Path harnessConfigPath = generateHarnessConfig(claudeConfigDir);

            return new ConfigSyncResult(
                true,
                List.of(settingsPath, harnessConfigPath),
                "Configuration synchronized successfully"
            );

        } catch (Exception e) {
            return new ConfigSyncResult(
                false,
                List.of(),
                "Sync failed: " + e.getMessage()
            );
        }
    }

    /**
     * Validates an existing configuration.
     *
     * @param configPath Path to configuration file
     * @return Validation result
     */
    public ValidationResult validateConfig(Path configPath) {
        if (configPath == null || !Files.exists(configPath)) {
            return new ValidationResult(
                false,
                List.of("Configuration file does not exist: " + configPath)
            );
        }

        try {
            String filename = configPath.getFileName().toString();
            Map<String, Object> config;

            if (filename.endsWith(".json")) {
                config = jsonMapper.readValue(configPath.toFile(), Map.class);
            } else if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
                config = yamlMapper.readValue(configPath.toFile(), Map.class);
            } else {
                return new ValidationResult(
                    false,
                    List.of("Unsupported config file type: " + filename)
                );
            }

            List<String> errors = validateConfigStructure(config);
            return new ValidationResult(errors.isEmpty(), errors);

        } catch (Exception e) {
            return new ValidationResult(
                false,
                List.of("Failed to parse configuration: " + e.getMessage())
            );
        }
    }

    /**
     * Validates the structure of a configuration map.
     *
     * @param config Configuration map
     * @return List of validation errors (empty if valid)
     */
    private List<String> validateConfigStructure(Map<String, Object> config) {
        List<String> errors = new ArrayList<>();

        // Validate required top-level keys
        // For Claude Code settings, check for common required fields
        if (config.containsKey("permissions")) {
            Object permissions = config.get("permissions");
            if (!(permissions instanceof Map)) {
                errors.add("'permissions' must be a map");
            }
        }

        if (config.containsKey("skills")) {
            Object skills = config.get("skills");
            if (!(skills instanceof Map)) {
                errors.add("'skills' must be a map");
            }
        }

        return errors;
    }

    /**
     * Creates Claude Code settings structure.
     *
     * @return Settings map
     */
    private Map<String, Object> createClaudeCodeSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();

        // Permissions configuration
        Map<String, Object> permissions = new LinkedHashMap<>();
        permissions.put("bash", "allow");
        permissions.put("read", "allow");
        permissions.put("write", "prompt");
        settings.put("permissions", permissions);

        // Skills configuration
        Map<String, Object> skills = new LinkedHashMap<>();
        skills.put("enabled", true);
        skills.put("autoUpdate", true);
        settings.put("skills", skills);

        // Metadata
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("project", projectName);
        meta.put("version", "4.1.0-SNAPSHOT");
        meta.put("generatedBy", "ConfigSyncTool");
        meta.put("generatedAt", java.time.LocalDateTime.now().toString());
        settings.put("meta", meta);

        return settings;
    }

    /**
     * Creates harness.yaml configuration structure.
     *
     * @return Config map
     */
    private Map<String, Object> createHarnessConfig() {
        Map<String, Object> config = new LinkedHashMap<>();

        // Project configuration
        config.put("project", projectName);
        config.put("version", "4.1.0-SNAPSHOT");

        // Feature flags
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("stateRecovery", true);
        features.put("parallelExecution", true);
        features.put("nativeImage", false);
        config.put("features", features);

        // Recovery configuration
        Map<String, Object> recovery = new LinkedHashMap<>();
        recovery.put("maxAttempts", 3);
        recovery.put("backoffBase", 1000);
        recovery.put("backoffMax", 10000);
        config.put("recovery", recovery);

        // Worker configuration
        Map<String, Object> workers = new LinkedHashMap<>();
        workers.put("default", "claude");
        workers.put("maxParallel", 4);
        config.put("workers", workers);

        return config;
    }

    /**
     * Gets the current project name.
     *
     * @return Project name
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Exception thrown during config sync operations.
     */
    public static class ConfigSyncException extends Exception {
        public ConfigSyncException(String message) {
            super(message);
        }

        public ConfigSyncException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Result of a configuration sync operation.
     */
    public record ConfigSyncResult(
        boolean success,
        List<Path> generatedFiles,
        String message
    ) {
        public ConfigSyncResult {
            if (generatedFiles == null) {
                generatedFiles = List.of();
            }
        }

        /**
         * Gets the number of generated files.
         */
        public int fileCount() {
            return generatedFiles.size();
        }
    }

    /**
     * Result of a configuration validation operation.
     */
    public record ValidationResult(
        boolean valid,
        List<String> errors
    ) {
        public ValidationResult {
            if (errors == null) {
                errors = List.of();
            }
        }

        /**
         * Checks if validation passed.
         */
        public boolean isValid() {
            return valid && errors.isEmpty();
        }

        /**
         * Gets the number of validation errors.
         */
        public int errorCount() {
            return errors.size();
        }
    }
}
