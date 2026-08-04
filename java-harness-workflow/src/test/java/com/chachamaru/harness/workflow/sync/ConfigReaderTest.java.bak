package com.chachamaru.harness.workflow.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class ConfigReaderTest {

    @Test
    void testParseValidConfig(@TempDir Path tempDir) throws Exception {
        String toml = """
            [project]
            name = "test-plugin"
            version = "1.0.0"

            [agent]
            default = "claude-sonnet-5"
            """;

        Path tomlPath = tempDir.resolve("harness.toml");
        Files.writeString(tomlPath, toml);

        SyncConfig config = ConfigReader.parse(tomlPath.toFile());

        assertNotNull(config);
        assertEquals("test-plugin", config.getProject().getName());
        assertEquals("1.0.0", config.getProject().getVersion());
        assertEquals("claude-sonnet-5", config.getAgent().getDefaultAgent());
    }

    @Test
    void testParseNonExistentFile(@TempDir Path tempDir) {
        Path tomlPath = tempDir.resolve("nonexistent.toml");

        IOException exception = assertThrows(IOException.class, () -> {
            ConfigReader.parse(tomlPath.toFile());
        });

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void testParseEnvConfig(@TempDir Path tempDir) throws Exception {
        String toml = """
            [env]
            KEY1 = "value1"
            KEY2 = "value2"
            API_KEY = "secret123"
            """;

        Path tomlPath = tempDir.resolve("harness.toml");
        Files.writeString(tomlPath, toml);

        SyncConfig config = ConfigReader.parse(tomlPath.toFile());

        assertNotNull(config.getEnv());
        assertEquals(3, config.getEnv().size());
        assertEquals("value1", config.getEnv().get("KEY1"));
        assertEquals("value2", config.getEnv().get("KEY2"));
        assertEquals("secret123", config.getEnv().get("API_KEY"));
    }

    @Test
    void testParseSafetyPermissions(@TempDir Path tempDir) throws Exception {
        String toml = """
            [safety.permissions]
            allow = ["Bash(git status:*)"]
            deny = ["Bash(rm:*)", "Bash(rmdir:*)"]
            ask = ["Bash(npm install:*)"]
            """;

        Path tomlPath = tempDir.resolve("harness.toml");
        Files.writeString(tomlPath, toml);

        SyncConfig config = ConfigReader.parse(tomlPath.toFile());

        assertNotNull(config.getSafety());
        assertNotNull(config.getSafety().getPermissions());
        assertEquals(1, config.getSafety().getPermissions().getAllow().size());
        assertEquals("Bash(git status:*)", config.getSafety().getPermissions().getAllow().get(0));
        assertEquals(2, config.getSafety().getPermissions().getDeny().size());
        assertEquals("Bash(rm:*)", config.getSafety().getPermissions().getDeny().get(0));
        assertEquals("Bash(rmdir:*)", config.getSafety().getPermissions().getDeny().get(1));
        assertEquals(1, config.getSafety().getPermissions().getAsk().size());
        assertEquals("Bash(npm install:*)", config.getSafety().getPermissions().getAsk().get(0));
    }

    @Test
    void testParseSafetySandbox(@TempDir Path tempDir) throws Exception {
        String toml = """
            [safety.sandbox]
            fail_if_unavailable = true

            [safety.sandbox.network]
            denied_domains = ["169.254.169.254", "metadata.google.internal"]

            [safety.sandbox.filesystem]
            deny_read = [".env", "*.key"]
            allow_read = ["/usr/bin/git"]
            """;

        Path tomlPath = tempDir.resolve("harness.toml");
        Files.writeString(tomlPath, toml);

        SyncConfig config = ConfigReader.parse(tomlPath.toFile());

        assertNotNull(config.getSafety());
        assertNotNull(config.getSafety().getSandbox());
        assertTrue(config.getSafety().getSandbox().isFailIfUnavailable());

        // Verify network config
        assertNotNull(config.getSafety().getSandbox().getNetwork());
        assertEquals(2, config.getSafety().getSandbox().getNetwork().getDeniedDomains().size());
        assertEquals("169.254.169.254", config.getSafety().getSandbox().getNetwork().getDeniedDomains().get(0));
        assertEquals("metadata.google.internal", config.getSafety().getSandbox().getNetwork().getDeniedDomains().get(1));

        // Verify filesystem config
        assertNotNull(config.getSafety().getSandbox().getFilesystem());
        assertEquals(2, config.getSafety().getSandbox().getFilesystem().getDenyRead().size());
        assertEquals(".env", config.getSafety().getSandbox().getFilesystem().getDenyRead().get(0));
        assertEquals("*.key", config.getSafety().getSandbox().getFilesystem().getDenyRead().get(1));
        assertEquals(1, config.getSafety().getSandbox().getFilesystem().getAllowRead().size());
        assertEquals("/usr/bin/git", config.getSafety().getSandbox().getFilesystem().getAllowRead().get(0));
    }

    @Test
    void testParseCompleteConfig(@TempDir Path tempDir) throws Exception {
        String toml = """
            [project]
            name = "complete-plugin"
            version = "2.0.0"
            description = "A complete test plugin"
            author_name = "Test Author"
            author_url = "https://example.com"
            homepage = "https://example.com/home"
            repository = "https://github.com/test/repo"
            license = "MIT"
            keywords = ["test", "plugin", "complete"]

            [agent]
            default = "claude-opus-4"

            [env]
            NODE_ENV = "production"
            PORT = "8080"

            [safety.permissions]
            allow = ["Bash(git:*)"]
            deny = ["Bash(rm:*)"]
            ask = ["Bash(npm:*)"]

            [safety.sandbox]
            fail_if_unavailable = false

            [safety.sandbox.network]
            denied_domains = ["internal.api"]

            [safety.sandbox.filesystem]
            deny_read = [".env"]
            allow_read = ["/usr/bin/git", "/usr/bin/node"]
            """;

        Path tomlPath = tempDir.resolve("harness.toml");
        Files.writeString(tomlPath, toml);

        SyncConfig config = ConfigReader.parse(tomlPath.toFile());

        // Verify project
        assertNotNull(config.getProject());
        assertEquals("complete-plugin", config.getProject().getName());
        assertEquals("2.0.0", config.getProject().getVersion());
        assertEquals("A complete test plugin", config.getProject().getDescription());
        assertEquals("Test Author", config.getProject().getAuthorName());
        assertEquals("https://example.com", config.getProject().getAuthorUrl());
        assertEquals("https://example.com/home", config.getProject().getHomepage());
        assertEquals("https://github.com/test/repo", config.getProject().getRepository());
        assertEquals("MIT", config.getProject().getLicense());
        assertEquals(3, config.getProject().getKeywords().size());

        // Verify agent
        assertNotNull(config.getAgent());
        assertEquals("claude-opus-4", config.getAgent().getDefaultAgent());

        // Verify env
        assertNotNull(config.getEnv());
        assertEquals(2, config.getEnv().size());

        // Verify safety
        assertNotNull(config.getSafety());
        assertNotNull(config.getSafety().getPermissions());
        assertNotNull(config.getSafety().getSandbox());
        assertNotNull(config.getSafety().getSandbox().getNetwork());
        assertNotNull(config.getSafety().getSandbox().getFilesystem());
    }

    @Test
    void testParseEmptyToml(@TempDir Path tempDir) throws Exception {
        String toml = "";

        Path tomlPath = tempDir.resolve("harness.toml");
        Files.writeString(tomlPath, toml);

        SyncConfig config = ConfigReader.parse(tomlPath.toFile());

        // 所有配置节都应该为 null
        assertNull(config.getProject());
        assertNull(config.getAgent());
        assertNull(config.getEnv());
        assertNull(config.getSafety());
    }

    @Test
    void testParseMalformedToml(@TempDir Path tempDir) throws Exception {
        String malformed = """
            [project
            name = "test"
            """;

        Path tomlPath = tempDir.resolve("harness.toml");
        Files.writeString(tomlPath, malformed);

        IOException exception = assertThrows(IOException.class, () -> {
            ConfigReader.parse(tomlPath.toFile());
        });

        assertTrue(exception.getMessage().contains("parse errors"));
    }
}
