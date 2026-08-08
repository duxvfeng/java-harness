package com.chachamaru.harness.collaboration.config.parser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TOML parser for platform configuration files.
 *
 * <p>This is a standalone TOML parser for the collaboration layer to avoid
 * circular dependencies with the CLI module. It supports basic TOML features
 * needed for configuration files.</p>
 *
 * @spec_reference Phase 7: Dual Platform Support - Task 7.5
 */
public class TomlParser {

    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\[([^\\]]+)\\]$");
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^([^=]+)\\s*=\\s*(.+)$");
    private static final Pattern STRING_PATTERN = Pattern.compile("^\"([^\"]*)\"$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false)$", Pattern.CASE_INSENSITIVE);

    /**
     * Parse TOML string into nested map structure.
     *
     * @param toml TOML content string
     * @return Nested map representing the TOML structure
     */
    public static Map<String, Object> parse(String toml) {
        Map<String, Object> result = new HashMap<>();
        if (toml == null || toml.trim().isEmpty()) {
            return result;
        }

        String[] lines = toml.split("\n");
        String currentSection = null;

        for (String line : lines) {
            String trimmed = line.trim();

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            // Check for section header
            Matcher sectionMatcher = SECTION_PATTERN.matcher(trimmed);
            if (sectionMatcher.find()) {
                currentSection = sectionMatcher.group(1);
                continue;
            }

            // Check for key-value pair
            Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(trimmed);
            if (kvMatcher.find()) {
                String key = kvMatcher.group(1).trim();
                String value = kvMatcher.group(2).trim();
                Object parsedValue = parseValue(value);

                if (currentSection != null) {
                    result.computeIfAbsent(currentSection, k -> new HashMap<String, Object>());
                    ((Map<String, Object>) result.get(currentSection)).put(key, parsedValue);
                } else {
                    result.put(key, parsedValue);
                }
            }
        }

        return result;
    }

    /**
     * Parse a single value (string, boolean, number).
     */
    private static Object parseValue(String value) {
        // Try to parse as string
        Matcher stringMatcher = STRING_PATTERN.matcher(value);
        if (stringMatcher.find()) {
            return stringMatcher.group(1);
        }

        // Try to parse as boolean
        Matcher booleanMatcher = BOOLEAN_PATTERN.matcher(value);
        if (booleanMatcher.find()) {
            return Boolean.parseBoolean(booleanMatcher.group(1));
        }

        // Try to parse as number (integer or decimal)
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            // If all else fails, return as string
            return value;
        }
    }

    /**
     * Get string value from nested config using dot notation.
     *
     * @param config Config map
     * @param path Dot-separated path (e.g., "harness.version")
     * @return String value, or null if not found
     */
    public static String getString(Map<String, Object> config, String path) {
        Object value = getNestedValue(config, path);
        return value != null ? value.toString() : null;
    }

    /**
     * Get boolean value from nested config using dot notation.
     *
     * @param config Config map
     * @param path Dot-separated path (e.g., "plan.enable")
     * @return Boolean value, or false if not found
     */
    public static boolean getBoolean(Map<String, Object> config, String path) {
        Object value = getNestedValue(config, path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    /**
     * Get integer value from nested config using dot notation.
     *
     * @param config Config map
     * @param path Dot-separated path
     * @return Integer value, or 0 if not found
     */
    public static int getInt(Map<String, Object> config, String path) {
        Object value = getNestedValue(config, path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * Get optional string value from nested config using dot notation.
     *
     * @param config Config map
     * @param path Dot-separated path
     * @return Optional containing the value if found
     */
    public static Optional<String> getOptionalString(Map<String, Object> config, String path) {
        String value = getString(config, path);
        return value != null ? Optional.of(value) : Optional.empty();
    }

    /**
     * Get nested value using dot notation.
     */
    private static Object getNestedValue(Map<String, Object> config, String path) {
        String[] parts = path.split("\\.");
        Object current = config;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }
}
