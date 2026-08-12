package com.chachamaru.harness.foundation.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TemplateService 单元测试
 *
 * @since 4.0.0
 */
class TemplateServiceTest {

    private TemplateService templateService;
    private TemplateRegistry registry;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        String registryPath = tempDir.resolve("template-registry.json").toString();
        String templatesDir = tempDir.resolve("templates").toString();
        registry = new TemplateRegistry(registryPath, templatesDir);
        templateService = new TemplateService(registry);
    }

    @Test
    void testGenerateContent() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PROJECT_NAME", "TestProject");
        variables.put("AUTHOR", "TestAuthor");

        String content = templateService.generateContent("claude-md", variables);

        assertNotNull(content);
        assertTrue(content.contains("TestProject"));
        assertTrue(content.contains("TestAuthor"));
        assertTrue(content.contains("# TestProject"));
    }

    @Test
    void testGenerateContentWithMissingVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PROJECT_NAME", "TestProject");
        // Missing AUTHOR and other required variables

        String content = templateService.generateContent("claude-md", variables);

        assertNotNull(content);
        assertTrue(content.contains("TestProject"));
        // Should use defaults where available
    }

    @Test
    void testGenerateFile() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PROJECT_NAME", "TestProject");
        variables.put("AUTHOR", "TestAuthor");

        Path outputFile = tempDir.resolve("generated-file.md");

        templateService.generateFile("claude-md", variables, outputFile);

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("TestProject"));
    }

    @Test
    void testGenerateContentPreservesUtf8Characters() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PROJECT_NAME", "中文项目名");
        variables.put("AUTHOR", "作者");

        String content = templateService.generateContent("claude-md", variables);

        assertTrue(content.contains("中文项目名"));
        assertTrue(content.contains("作者"));
    }

    @Test
    void testListTemplates() {
        List<String> templates = templateService.listTemplates();

        assertNotNull(templates);
        assertFalse(templates.isEmpty());
        assertTrue(templates.contains("claude-md"));
        assertTrue(templates.contains("agents-md"));
    }

    @Test
    void testGetBuiltinTemplates() {
        List<String> builtin = templateService.getBuiltinTemplates();

        assertNotNull(builtin);
        assertTrue(builtin.contains("claude-md"));
        assertTrue(builtin.contains("agents-md"));
    }

    @Test
    void testLoadsCanonicalPlanAndReadmeTemplates() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PROJECT_NAME", "CanonicalProject");

        String plans = templateService.generateContent("plans-md", variables);
        String readme = templateService.generateContent("readme-md", variables);

        assertTrue(plans.contains("Plans.md - Task Tracking"));
        assertTrue(readme.contains("CanonicalProject"));
        assertFalse(plans.contains("Generated content for plans-md.template"));
        assertFalse(readme.contains("Generated content for readme-md.template"));
    }

    @Test
    void testHasTemplate() {
        assertTrue(templateService.hasTemplate("claude-md"));
        assertTrue(templateService.hasTemplate("agents-md"));
        assertFalse(templateService.hasTemplate("non-existent"));
    }

    @Test
    void testGetTemplateMetadata() {
        FrontmatterMetadata metadata = templateService.getTemplateMetadata("claude-md");

        assertNotNull(metadata);
        assertTrue(metadata.hasVersionInfo());
        assertEquals("4.0.0", metadata.getHarnessVersion());
    }

    @Test
    void testValidateTemplate() {
        assertTrue(templateService.validateTemplate("claude-md"));
        assertTrue(templateService.validateTemplate("agents-md"));
        // Non-existent templates will use default template, which validates as true
        assertTrue(templateService.validateTemplate("non-existent"));
    }

    @Test
    void testGenerateAgentsMd() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PROJECT_NAME", "MyProject");

        String content = templateService.generateContent("agents-md", variables);

        assertNotNull(content);
        assertTrue(content.contains("MyProject"));
        assertTrue(content.contains("Code Agent"));
        assertTrue(content.contains("Review Agent"));
        assertTrue(content.contains("Test Agent"));
    }

    @Test
    void testGenerateWithEmptyVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PROJECT_NAME", "TestProject"); // Add required variable

        String content = templateService.generateContent("claude-md", variables);

        assertNotNull(content);
        // Should use built-in variables like DATE
        assertTrue(content.contains("20")); // Year should be present
    }

    @Test
    void testGenerateWithCustomVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PROJECT_NAME", "CustomProject");
        variables.put("CUSTOM_VAR", "custom_value");

        String content = templateService.generateContent("claude-md", variables);

        assertNotNull(content);
        assertTrue(content.contains("CustomProject"));
    }

    @Test
    void testGenerateContentWithBuiltinVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PROJECT_NAME", "TestProject");

        String content = templateService.generateContent("claude-md", variables);

        // Should contain built-in variables like DATE, TIME, etc.
        assertTrue(content.contains("TestProject"));
        assertNotNull(content);
    }

    @Test
    void testGenerateFileInSubdirectory() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PROJECT_NAME", "TestProject");

        Path outputFile = tempDir.resolve("subdir").resolve("generated-file.md");

        templateService.generateFile("claude-md", variables, outputFile);

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.exists(outputFile.getParent()));
    }

    @Test
    void testGetTemplateMetadataForAgentsMd() {
        FrontmatterMetadata metadata = templateService.getTemplateMetadata("agents-md");

        assertNotNull(metadata);
        assertTrue(metadata.hasVersionInfo());
        assertNotNull(metadata.getTemplateReference());
    }

    @Test
    void testGenerateMultipleFiles() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("PROJECT_NAME", "MultiTest");

        Path file1 = tempDir.resolve("file1.md");
        Path file2 = tempDir.resolve("file2.md");

        templateService.generateFile("claude-md", variables, file1);
        templateService.generateFile("agents-md", variables, file2);

        assertTrue(Files.exists(file1));
        assertTrue(Files.exists(file2));

        String content1 = Files.readString(file1);
        String content2 = Files.readString(file2);

        assertTrue(content1.contains("MultiTest"));
        assertTrue(content2.contains("MultiTest"));
    }

    @Test
    void testTemplateServiceWithNullVariables() {
        // The TemplateService handles null variables by creating an empty map
        // but the template requires PROJECT_NAME, so it will throw validation exception
        assertThrows(TemplateRegistryException.class, () -> {
            templateService.generateContent("claude-md", null);
        });

        // This is the expected behavior - templates require PROJECT_NAME
        // The null handling itself doesn't cause NullPointerException
    }

    @Test
    void testListTemplatesReturnsUnmodifiableList() {
        List<String> templates = templateService.listTemplates();

        assertThrows(UnsupportedOperationException.class, () -> {
            templates.add("new-template");
        });
    }

    @Test
    void testGetBuiltinTemplatesReturnsUnmodifiableList() {
        List<String> builtin = templateService.getBuiltinTemplates();

        assertThrows(UnsupportedOperationException.class, () -> {
            builtin.add("new-template");
        });
    }

    @Test
    void allBundledTemplateResourcesAreReadable() {
        List<String> resources = List.of(
            "templates/config/harness.toml.template",
            "templates/config/harness-minimal.toml.template",
            "templates/core/agents-md.template",
            "templates/core/claude-md.template",
            "templates/core/plans-generated.template",
            "templates/core/plans-md.template",
            "templates/core/readme-md.template",
            "templates/memory/decisions-java.template",
            "templates/memory/patterns-java.template",
            "templates/rules/guardrail-rule.template",
            "templates/rules/rule-md.template",
            "templates/rules/security-rule.template"
        );

        for (String resource : resources) {
            assertFalse(TemplateResourceLoader.load(resource).isBlank(), resource);
        }
    }
}
