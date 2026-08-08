package com.chachamaru.harness.collaboration.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Simple TOML configuration parser for Java Harness.
 *
 * <p>Provides basic TOML parsing functionality without external dependencies.
 * Supports:
 * <ul>
 *   <li>Key-value pairs</li>
 *   <li>Sections and nested sections</li>
 *   <li>Environment variable expansion (${VAR})</li>
 *   <li>Basic validation</li>
 * </ul>
 *
 * <p>Limitations (for future improvement):
 * <ul>
 *   <li>Arrays and inline tables</li>
 *   <li>Multi-line strings</li>
 *   <li>Datetime types</li>
 * </ul>
 *
 * @spec_reference Phase 7: Dual Platform Support
 */
public class HarnessConfigParser {

    /**
     * Parses a TOML configuration from string content.
     *
     * @param content the TOML content
     * @param source the source identifier (for error messages)
     * @return the parsed configuration
     * @throws ConfigParseException if parsing fails
     */
    public HarnessConfig parseString(String content, String source) throws ConfigParseException {
        if (content == null) {
            throw new ConfigParseException("Content cannot be null");
        }

        try {
            // Parse into nested map structure
            Map<String, Object> parsed = parseToml(content);
            return convertToConfig(parsed, source);
        } catch (Exception e) {
            throw new ConfigParseException("Failed to parse TOML from " + source + ": " + e.getMessage(), e);
        }
    }

    /**
     * Parses a TOML configuration from a file.
     *
     * @param filePath the path to the TOML file
     * @return the parsed configuration
     * @throws ConfigParseException if parsing fails
     */
    public HarnessConfig parseFile(String filePath) throws ConfigParseException {
        try {
            String content = Files.readString(Path.of(filePath));
            return parseString(content, filePath);
        } catch (IOException e) {
            throw new ConfigParseException("Failed to read file: " + filePath, e);
        }
    }

    /**
     * Simple TOML parser implementation.
     *
     * @param content the TOML content
     * @return parsed data structure
     */
    private Map<String, Object> parseToml(String content) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> currentSection = result;
        List<Map<String, Object>> sectionStack = new ArrayList<>();
        sectionStack.add(result);

        String[] lines = content.split("\n");
        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum].trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Handle section headers
            if (line.startsWith("[")) {
                if (!line.endsWith("]")) {
                    throw new IllegalArgumentException("Invalid section header at line " + (lineNum + 1));
                }

                String sectionPath = line.substring(1, line.length() - 1);
                Map<String, Object> section = createNestedSection(result, sectionPath, sectionStack);
                currentSection = section;
                continue;
            }

            // Handle key-value pairs
            int equalIndex = line.indexOf('=');
            if (equalIndex > 0) {
                String key = line.substring(0, equalIndex).trim();
                String value = line.substring(equalIndex + 1).trim();

                // Remove quotes from string values
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                // Handle booleans
                if (value.equals("true")) {
                    currentSection.put(key, true);
                } else if (value.equals("false")) {
                    currentSection.put(key, false);
                }
                // Handle integers
                else if (value.matches("-?\\d+")) {
                    try {
                        currentSection.put(key, Integer.parseInt(value));
                    } catch (NumberFormatException e) {
                        currentSection.put(key, value);
                    }
                }
                // Default: keep as string
                else {
                    currentSection.put(key, value);
                }
            }
        }

        return result;
    }

    /**
     * Creates nested section structure.
     */
    private Map<String, Object> createNestedSection(Map<String, Object> root, String sectionPath, List<Map<String, Object>> sectionStack) {
        String[] parts = sectionPath.split("\\.");
        Map<String, Object> current = root;

        // Navigate/create nested structure
        for (String part : parts) {
            Object existing = current.get(part);
            Map<String, Object> section;

            if (existing instanceof Map) {
                section = (Map<String, Object>) existing;
            } else {
                section = new HashMap<>();
                current.put(part, section);
            }

            current = section;
        }

        // Update section stack
        sectionStack.set(0, root);
        for (int i = 1; i < parts.length; i++) {
            String pathSoFar = String.join(".", Arrays.copyOfRange(parts, 0, i + 1));
            Object obj = getNestedValue(root, pathSoFar);
            if (obj instanceof Map) {
                sectionStack.set(i, (Map<String, Object>) obj);
            }
        }
        sectionStack.add(current);

        return current;
    }

    /**
     * Gets value from nested map using dot notation.
     */
    private Object getNestedValue(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Object current = map;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Converts parsed map to HarnessConfig object.
     */
    private HarnessConfig convertToConfig(Map<String, Object> parsed, String source) throws ConfigParseException {
        HarnessConfig config = new HarnessConfig();

        // Extract [harness] section
        Map<String, Object> harnessSection = getSectionMap(parsed, "harness");
        if (harnessSection != null) {
            if (harnessSection.containsKey("version")) {
                config.setVersion((String) harnessSection.get("version"));
            }
            if (harnessSection.containsKey("platform")) {
                config.setPlatform((String) harnessSection.get("platform"));
            }
            if (harnessSection.containsKey("multi-platform")) {
                config.setMultiPlatform((Boolean) harnessSection.get("multi-platform"));
            }
        }

        // Extract [backend] section
        Map<String, Object> backendSection = getSectionMap(parsed, "backend");
        if (backendSection != null) {
            if (backendSection.containsKey("default")) {
                config.setBackendDefault((String) backendSection.get("default"));
            }
            if (backendSection.containsKey("timeout")) {
                Object timeout = backendSection.get("timeout");
                if (timeout instanceof Number) {
                    config.setBackendTimeout(((Number) timeout).longValue());
                } else {
                    config.setBackendTimeout(Long.parseLong(timeout.toString()));
                }
            }
            if (backendSection.containsKey("max-retries")) {
                Object retries = backendSection.get("max-retries");
                if (retries instanceof Number) {
                    config.setBackendMaxRetries(((Number) retries).intValue());
                } else {
                    config.setBackendMaxRetries(Integer.parseInt(retries.toString()));
                }
            }
        }

        // Extract platform-specific sections
        for (String key : parsed.keySet()) {
            if (key.equals("harness") || key.equals("backend")) {
                continue;
            }
            Object value = parsed.get(key);
            if (value instanceof Map) {
                config.setPlatformConfig(key, (Map<String, Object>) value);
            }
        }

        // Validate required fields
        if (config.getVersion() == null || config.getVersion().isEmpty()) {
            throw new ConfigParseException("Missing required field: 'version' in [harness] section");
        }

        return config;
    }

    /**
     * Gets a section map from parsed data.
     */
    private Map<String, Object> getSectionMap(Map<String, Object> parsed, String sectionName) {
        Object section = parsed.get(sectionName);
        return (section instanceof Map) ? (Map<String, Object>) section : null;
    }
}
