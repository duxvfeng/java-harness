package com.chachamaru.harness.collaboration.skill.loader;

import com.chachamaru.harness.collaboration.skill.SkillExecutionException;
import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MarkdownSkillLoader.
 */
class MarkdownSkillLoaderTest {

    private final MarkdownSkillLoader loader = new MarkdownSkillLoader();

    @Test
    void testLoadSkillFromString() throws MarkdownSkillLoader.SkillLoadingException, SkillExecutionException {
        String skillContent = """
            ---
            id: test-skill
            name: Test Skill
            description: A test skill for validation
            version: 1.0.0
            tags: [test, validation]
            ---

            # Implementation

            This is the implementation of the test skill.
            """;

        MarkdownSkill skill = loader.loadSkillFromString(skillContent, "test-content");

        assertNotNull(skill);
        assertEquals("test-skill", skill.getId());
        assertEquals("Test Skill", skill.getName());
        assertEquals("A test skill for validation", skill.getDescription());
        assertEquals("1.0.0", skill.getVersion());
        assertFalse(skill.getMetadata().isEmpty());

        // Test execution
        SkillContext context = SkillContext.createForTest("test-skill", "Test Skill");
        var result = skill.execute(context);
        assertNotNull(result);
        assertTrue(result.isSuccess());
    }

    @Test
    void testLoadSkillFromFile(@TempDir Path tempDir) throws Exception {
        Path skillFile = tempDir.resolve("TEST.SKILL.md");
        String skillContent = """
            ---
            id: file-skill
            name: File Skill
            description: A skill loaded from file
            version: 2.0.0
            ---

            Implementation loaded from file.
            """;

        Files.writeString(skillFile, skillContent);

        assertTrue(loader.isSkillFile(skillFile));

        MarkdownSkill skill = loader.loadSkill(skillFile);

        assertNotNull(skill);
        assertEquals("file-skill", skill.getId());
        assertEquals("File Skill", skill.getName());
        assertEquals("2.0.0", skill.getVersion());
    }

    @Test
    void testLoadSkillWithoutFrontmatter() throws MarkdownSkillLoader.SkillLoadingException {
        // Should handle content without frontmatter gracefully
        String content = "# Simple Skill\n\nImplementation without frontmatter";

        assertThrows(MarkdownSkillLoader.SkillLoadingException.class, () ->
            loader.loadSkillFromString(content, "no-frontmatter")
        );
    }

    @Test
    void testLoadSkillMissingRequiredField() {
        String content = """
            ---
            name: Incomplete Skill
            description: Missing id field
            ---

            Implementation
            """;

        assertThrows(MarkdownSkillLoader.SkillLoadingException.class, () ->
            loader.loadSkillFromString(content, "incomplete-skill")
        );
    }

    @Test
    void testIsSkillFile() {
        assertTrue(loader.isSkillFile(Path.of("test.SKILL.md")));
        assertTrue(loader.isSkillFile(Path.of("MY-SKILL.SKILL.md")));
        assertFalse(loader.isSkillFile(Path.of("test.md")));
        assertFalse(loader.isSkillFile(Path.of("README.md")));
        assertFalse(loader.isSkillFile(null));
    }

    @Test
    void testLoadSkillWithArrays() throws MarkdownSkillLoader.SkillLoadingException {
        String content = """
            ---
            id: array-skill
            name: Array Skill
            description: Test array parsing
            tags: [tag1, tag2, tag3]
            categories: [cat1, cat2]
            ---

            Implementation
            """;

        MarkdownSkill skill = loader.loadSkillFromString(content, "array-test");

        assertNotNull(skill);
        assertEquals("array-skill", skill.getId());
        assertTrue(skill.getMetadata().containsKey("tags"));
        assertTrue(skill.getMetadata().containsKey("categories"));
    }

    @Test
    void testLoadSkillWithMinimalFrontmatter() throws MarkdownSkillLoader.SkillLoadingException, SkillExecutionException {
        String content = """
            ---
            id: minimal-skill
            name: Minimal Skill
            ---

            Minimal implementation
            """;

        MarkdownSkill skill = loader.loadSkillFromString(content, "minimal-test");

        assertNotNull(skill);
        assertEquals("minimal-skill", skill.getId());
        assertEquals("Minimal Skill", skill.getName());
        assertEquals("Skill: Minimal Skill", skill.getDescription()); // Default
        assertEquals("1.0.0", skill.getVersion()); // Default

        // Should execute successfully
        SkillContext context = SkillContext.createForTest("minimal-skill", "Minimal Skill");
        var result = skill.execute(context);
        assertTrue(result.isSuccess());
    }

    @Test
    void testSkillExecutionWithoutImplementation() {
        // Create a skill with empty implementation
        MarkdownSkill skill = new MarkdownSkill(
            "no-impl",
            "No Implementation",
            "Test skill without implementation",
            "1.0.0",
            java.util.Map.of(),
            ""
        );

        SkillContext context = SkillContext.createForTest("no-impl", "No Implementation");

        assertThrows(SkillExecutionException.class, () -> skill.execute(context));
    }

    @Test
    void testGetMetadata() throws MarkdownSkillLoader.SkillLoadingException {
        String content = """
            ---
            id: metadata-skill
            name: Metadata Skill
            customField: customValue
            numericField: 42
            ---

            Implementation
            """;

        MarkdownSkill skill = loader.loadSkillFromString(content, "metadata-test");

        var metadata = skill.getMetadata();
        assertNotNull(metadata);
        assertTrue(metadata.containsKey("customField"));
        assertEquals("customValue", metadata.get("customField"));
        assertTrue(metadata.containsKey("numericField"));
    }

    @Test
    void testLoadSkillFromNonExistentFile(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("NON-EXISTENT.SKILL.md");

        assertThrows(MarkdownSkillLoader.SkillLoadingException.class, () ->
            loader.loadSkill(nonExistent)
        );
    }

    @Test
    void testSkillToString() throws MarkdownSkillLoader.SkillLoadingException {
        String content = """
            ---
            id: tostring-skill
            name: To String Skill
            version: 3.0.0
            ---

            Implementation
            """;

        MarkdownSkill skill = loader.loadSkillFromString(content, "tostring-test");

        String toString = skill.toString();
        assertTrue(toString.contains("tostring-skill"));
        assertTrue(toString.contains("To String Skill"));
        assertTrue(toString.contains("3.0.0"));
    }
}
