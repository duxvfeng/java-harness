package com.chachamaru.harness.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigTemplateTest {
    @Test
    void testGenerateDefaultConfig() {
        String config = ConfigTemplate.generateDefault();
        assertNotNull(config);
        assertTrue(config.contains("[harness]"));
        assertTrue(config.contains("version"));
    }

    @Test
    void testGenerateWithCustomValues() {
        String config = ConfigTemplate.generateCustom("5.0.0", "codex", "/test/path");
        assertNotNull(config);
        assertTrue(config.contains("5.0.0"));
        assertTrue(config.contains("codex"));
        assertTrue(config.contains("/test/path"));
    }

    @Test
    void testGenerateMinimalConfig() {
        String config = ConfigTemplate.generateMinimal();
        assertNotNull(config);
        assertTrue(config.contains("[harness]"));
        // Minimal config should have fewer sections
    }
}
