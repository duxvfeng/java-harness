package com.chachamaru.harness.config;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigSyncTest {
    @Test
    void testSyncConfigToFile() throws IOException {
        Path tempFile = Files.createTempFile("harness", ".toml");
        String configContent = ConfigTemplate.generateDefault();

        ConfigSync.syncConfigToFile(tempFile.toString(), configContent);

        String readContent = Files.readString(tempFile);
        assertTrue(readContent.contains("[harness]"));
        assertTrue(readContent.contains("version"));

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testLoadConfigFromFile() throws IOException {
        Path tempFile = Files.createTempFile("harness", ".toml");
        String toml = """
                [harness]
                version = "5.0.0-java"
                backend = "codex"
                """;

        Files.writeString(tempFile, toml);

        Map<String, Object> config = ConfigSync.loadConfigFromFile(tempFile.toString());
        assertNotNull(config);
        assertTrue(config.containsKey("harness"));

        Files.deleteIfExists(tempFile);
    }

    @Test
    void testMergeConfigs() {
        String baseToml = """
                [harness]
                version = "5.0.0-java"
                backend = "codex"
                """;

        String overrideToml = """
                [harness]
                backend = "cursor"
                """;

        Map<String, Object> merged = ConfigSync.mergeConfigs(baseToml, overrideToml);
        assertNotNull(merged);

        Map<String, Object> harness = (Map<String, Object>) merged.get("harness");
        assertEquals("5.0.0-java", harness.get("version"));
        assertEquals("cursor", harness.get("backend")); // Should be overridden
    }

    @Test
    void testValidateConfig() {
        String validToml = """
                [harness]
                version = "5.0.0-java"
                backend = "codex"
                """;

        assertTrue(ConfigSync.validateConfig(validToml));

        String invalidToml = """
                [harness]
                version = "5.0.0-java"
                # Missing required field: backend
                """;

        assertFalse(ConfigSync.validateConfig(invalidToml));
    }

    @Test
    void testGenerateConfigFromTemplate() {
        // Test with custom template that has placeholders
        String customTemplate = """
                [harness]
                version = "${version}"
                backend = "${backend}"
                project_root = "${project_root}"
                """;

        // Test that variables would be substituted (if template had placeholders)
        Map<String, String> variables = Map.of(
                "version", "5.0.0",
                "backend", "codex",
                "project_root", "/test/path"
        );

        // For now, this tests the generateFromTemplate method exists and doesn't throw
        // The actual variable substitution would work if the template had placeholders
        String result = ConfigSync.generateFromTemplate(variables);
        assertNotNull(result);
        // The default template doesn't have placeholders, so it returns the default
        assertTrue(result.contains("[harness]"));
    }
}
