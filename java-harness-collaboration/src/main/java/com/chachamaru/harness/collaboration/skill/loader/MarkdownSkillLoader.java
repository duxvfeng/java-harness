package com.chachamaru.harness.collaboration.skill.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loader for Markdown-based skills.
 *
 * <p>The MarkdownSkillLoader is responsible for:
 * <ul>
 *   <li>Loading .SKILL.md files from the filesystem</li>
 *   <li>Parsing YAML frontmatter for skill metadata</li>
 *   <li>Extracting skill implementation from markdown content</li>
 *   <li>Creating MarkdownSkill instances</li>
 * </ul>
 *
 * <p>Expected .SKILL.md format:
 * <pre>
 * ---
 * id: my-skill
 * name: My Skill
 * description: Description of what this skill does
 * version: 1.0.0
 * tags: [tag1, tag2]
 * ---
 *
 * # Implementation
 *
 * The skill implementation goes here...
 * </pre>
 * </p>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public class MarkdownSkillLoader {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownSkillLoader.class);
    private static final String SKILL_FILE_EXTENSION = ".SKILL.md";

    /**
     * Loads a skill from a file.
     *
     * @param skillFile the path to the .SKILL.md file
     * @return the loaded MarkdownSkill
     * @throws SkillLoadingException if loading fails
     */
    public MarkdownSkill loadSkill(Path skillFile) throws SkillLoadingException {
        Objects.requireNonNull(skillFile, "skillFile cannot be null");

        logger.info("Loading skill from: {}", skillFile);

        try {
            String content = Files.readString(skillFile);
            return parseSkill(content, skillFile.toString());

        } catch (IOException e) {
            throw new SkillLoadingException("Failed to read skill file: " + skillFile, e);
        }
    }

    /**
     * Loads a skill from a string content.
     *
     * @param content the skill markdown content
     * @param sourcePath the source path for error reporting
     * @return the parsed MarkdownSkill
     * @throws SkillLoadingException if parsing fails
     */
    public MarkdownSkill loadSkillFromString(String content, String sourcePath) throws SkillLoadingException {
        Objects.requireNonNull(content, "content cannot be null");
        return parseSkill(content, sourcePath);
    }

    /**
     * Parses skill content into a MarkdownSkill.
     *
     * @param content the skill content
     * @param sourcePath the source path for error reporting
     * @return the parsed MarkdownSkill
     * @throws SkillLoadingException if parsing fails
     */
    private MarkdownSkill parseSkill(String content, String sourcePath) throws SkillLoadingException {
        // Parse YAML frontmatter
        Map<String, Object> frontmatter = parseFrontmatter(content);

        // Extract body content (after frontmatter)
        String body = extractBody(content);

        // Validate required fields
        String id = getRequiredField(frontmatter, "id", sourcePath);
        String name = getRequiredField(frontmatter, "name", sourcePath);

        // Optional fields
        String description = getField(frontmatter, "description", String.class, "Skill: " + name);
        String version = getField(frontmatter, "version", String.class, "1.0.0");

        logger.info("Parsed skill: {} ({})", id, name);

        return new MarkdownSkill(
            id,
            name,
            description,
            version,
            frontmatter,
            body
        );
    }

    /**
     * Parses YAML frontmatter from content.
     *
     * @param content the content to parse
     * @return the frontmatter metadata
     * @throws SkillLoadingException if frontmatter parsing fails
     */
    private Map<String, Object> parseFrontmatter(String content) throws SkillLoadingException {
        Map<String, Object> frontmatter = new HashMap<>();

        // Check for frontmatter markers
        if (!content.startsWith("---")) {
            logger.warn("No YAML frontmatter found, using default metadata");
            return frontmatter;
        }

        // Find end of frontmatter
        int endOfFrontmatter = content.indexOf("\n---", 3);
        if (endOfFrontmatter == -1) {
            throw new SkillLoadingException("Invalid YAML frontmatter: missing closing ---");
        }

        String frontmatterText = content.substring(3, endOfFrontmatter).trim();

        // Parse simple YAML-like key-value pairs
        // Note: This is a simplified parser. For production, use a proper YAML library
        parseSimpleYaml(frontmatterText, frontmatter);

        return frontmatter;
    }

    /**
     * Parses simple YAML key-value pairs.
     *
     * @param yamlText the YAML text
     * @param result the map to populate
     */
    private void parseSimpleYaml(String yamlText, Map<String, Object> result) {
        if (yamlText == null || yamlText.isBlank()) {
            return;
        }

        String[] lines = yamlText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }

            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();

                // Handle array values like [tag1, tag2]
                if (value.startsWith("[") && value.endsWith("]")) {
                    String arrayContent = value.substring(1, value.length() - 1);
                    List<String> items = parseArray(arrayContent);
                    result.put(key, items);
                } else {
                    // Handle quoted strings
                    value = unquote(value);
                    result.put(key, value);
                }
            }
        }
    }

    /**
     * Parses array values from comma-separated string.
     *
     * @param arrayContent the array content
     * @return list of items
     */
    private List<String> parseArray(String arrayContent) {
        List<String> items = new ArrayList<>();
        if (arrayContent.isBlank()) {
            return items;
        }

        String[] parts = arrayContent.split(",");
        for (String part : parts) {
            String item = part.trim();
            if (!item.isBlank()) {
                items.add(unquote(item));
            }
        }
        return items;
    }

    /**
     * Removes quotes from a string if present.
     *
     * @param value the value to unquote
     * @return the unquoted value
     */
    private String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Extracts body content after frontmatter.
     *
     * @param content the full content
     * @return the body content
     */
    private String extractBody(String content) {
        int endOfFrontmatter = content.indexOf("\n---", 3);
        if (endOfFrontmatter == -1) {
            return content;
        }
        return content.substring(endOfFrontmatter + 4).trim();
    }

    /**
     * Gets a required field from frontmatter.
     *
     * @param frontmatter the frontmatter map
     * @param key the field key
     * @param sourcePath the source path for error reporting
     * @return the field value
     * @throws SkillLoadingException if field is missing
     */
    private String getRequiredField(Map<String, Object> frontmatter, String key, String sourcePath)
            throws SkillLoadingException {
        String value = getField(frontmatter, key, String.class, null);
        if (value == null || value.isBlank()) {
            throw new SkillLoadingException("Missing required field '" + key + "' in " + sourcePath);
        }
        return value;
    }

    /**
     * Gets a field from frontmatter.
     *
     * @param frontmatter the frontmatter map
     * @param key the field key
     * @param type the expected type
     * @param defaultValue the default value if not found
     * @return the field value or default
     */
    @SuppressWarnings("unchecked")
    private <T> T getField(Map<String, Object> frontmatter, String key, Class<T> type, T defaultValue) {
        Object value = frontmatter.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (type.isInstance(value)) {
            return (T) value;
        }

        // Try to convert to string
        if (type == String.class) {
            return (T) value.toString();
        }

        return defaultValue;
    }

    /**
     * Checks if a file is a skill file.
     *
     * @param path the path to check
     * @return true if the file ends with .SKILL.md
     */
    public boolean isSkillFile(Path path) {
        if (path == null) {
            return false;
        }
        String fileName = path.getFileName().toString();
        return fileName.endsWith(SKILL_FILE_EXTENSION);
    }

    /**
     * Exception thrown when skill loading fails.
     */
    public static class SkillLoadingException extends Exception {
        public SkillLoadingException(String message) {
            super(message);
        }

        public SkillLoadingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
