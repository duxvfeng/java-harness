package com.chachamaru.harness.config;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class HarnessTomlParserTest {
    @Test
    void testParseSimpleConfig() {
        String toml = """
                [harness]
                version = "5.0.0-java"
                backend = "codex"
                """;

        Map<String, Object> config = HarnessTomlParser.parse(toml);
        assertNotNull(config);
        assertTrue(config.containsKey("harness"));

        Map<String, Object> harness = (Map<String, Object>) config.get("harness");
        assertEquals("5.0.0-java", harness.get("version"));
        assertEquals("codex", harness.get("backend"));
    }

    @Test
    void testParseNestedSections() {
        String toml = """
                [harness]
                version = "5.0.0-java"

                [plan]
                enable = true
                auto_save = true

                [work]
                enable = true
                default_effort = "medium"
                """;

        Map<String, Object> config = HarnessTomlParser.parse(toml);

        Map<String, Object> plan = (Map<String, Object>) config.get("plan");
        assertTrue((Boolean) plan.get("enable"));
        assertTrue((Boolean) plan.get("auto_save"));

        Map<String, Object> work = (Map<String, Object>) config.get("work");
        assertEquals("medium", work.get("default_effort"));
    }

    @Test
    void testParseEmptyConfig() {
        String toml = "";
        Map<String, Object> config = HarnessTomlParser.parse(toml);
        assertNotNull(config);
        assertTrue(config.isEmpty());
    }

    @Test
    void testGetStringValue() {
        String toml = """
                [harness]
                version = "5.0.0-java"
                backend = "codex"
                """;

        Map<String, Object> config = HarnessTomlParser.parse(toml);
        assertEquals("5.0.0-java", HarnessTomlParser.getString(config, "harness.version"));
        assertEquals("codex", HarnessTomlParser.getString(config, "harness.backend"));
    }

    @Test
    void testGetBooleanValue() {
        String toml = """
                [plan]
                enable = true
                auto_save = false
                """;

        Map<String, Object> config = HarnessTomlParser.parse(toml);
        assertTrue(HarnessTomlParser.getBoolean(config, "plan.enable"));
        assertFalse(HarnessTomlParser.getBoolean(config, "plan.auto_save"));
    }

    @Test
    void testGetOptionalValue() {
        String toml = """
                [harness]
                version = "5.0.0-java"
                """;

        Map<String, Object> config = HarnessTomlParser.parse(toml);
        assertEquals(Optional.of("5.0.0-java"), HarnessTomlParser.getOptionalString(config, "harness.version"));
        assertEquals(Optional.empty(), HarnessTomlParser.getOptionalString(config, "harness.nonexistent"));
    }
}
