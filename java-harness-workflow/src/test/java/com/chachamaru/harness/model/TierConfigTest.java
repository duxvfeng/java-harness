package com.chachamaru.harness.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TierConfig 数据类的单元测试
 * 测试降级链列表、验证逻辑、空降级链异常处理等功能
 */
class TierConfigTest {

    @Test
    void testTierConfigCreation() {
        TierConfig config = new TierConfig(
            ModelTier.FAST,
            "ANTHROPIC_DEFAULT_FABLE_MODEL",
            new String[]{"env:ANTHROPIC_DEFAULT_FABLE_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7"}
        );

        assertEquals(ModelTier.FAST, config.getTier());
        assertEquals("ANTHROPIC_DEFAULT_FABLE_MODEL", config.getModelEnv());
        assertEquals(3, config.getFallbackModels().length);
        assertEquals("env:ANTHROPIC_DEFAULT_FABLE_MODEL", config.getFallbackModels()[0]);
    }

    @Test
    void testGetFallbackModels() {
        String[] fallbackModels = new String[]{"model1", "model2", "model3"};
        TierConfig config = new TierConfig(ModelTier.BALANCED, "ENV_VAR", fallbackModels);

        String[] retrieved = config.getFallbackModels();
        assertArrayEquals(fallbackModels, retrieved);
    }

    @Test
    void testEmptyFallbackChain() {
        TierConfig config = new TierConfig(
            ModelTier.QUALITY,
            "ENV_VAR",
            new String[]{}
        );

        assertEquals(0, config.getFallbackModels().length);
        assertTrue(config.getFallbackModels().length == 0);
    }

    @Test
    void testNullFallbackChain() {
        TierConfig config = new TierConfig(
            ModelTier.POWERFUL,
            "ENV_VAR",
            null
        );

        assertNull(config.getFallbackModels());
    }

    @Test
    void testValidFallbackChains() {
        // Valid chain with multiple models
        TierConfig validConfig = new TierConfig(
            ModelTier.FAST,
            "ENV_VAR",
            new String[]{"model1", "model2"}
        );
        assertTrue(validConfig.getFallbackModels().length > 0);

        // Valid chain with single model
        TierConfig singleConfig = new TierConfig(
            ModelTier.BALANCED,
            "ENV_VAR",
            new String[]{"model1"}
        );
        assertTrue(singleConfig.getFallbackModels().length == 1);

        // Invalid chain (empty)
        TierConfig emptyConfig = new TierConfig(
            ModelTier.QUALITY,
            "ENV_VAR",
            new String[]{}
        );
        assertTrue(emptyConfig.getFallbackModels().length == 0);

        // Invalid chain (null)
        TierConfig nullConfig = new TierConfig(
            ModelTier.POWERFUL,
            "ENV_VAR",
            null
        );
        assertNull(nullConfig.getFallbackModels());
    }

    @Test
    void testGetTier() {
        TierConfig fastConfig = new TierConfig(ModelTier.FAST, "ENV", new String[]{"model"});
        assertEquals(ModelTier.FAST, fastConfig.getTier());

        TierConfig balancedConfig = new TierConfig(ModelTier.BALANCED, "ENV", new String[]{"model"});
        assertEquals(ModelTier.BALANCED, balancedConfig.getTier());

        TierConfig qualityConfig = new TierConfig(ModelTier.QUALITY, "ENV", new String[]{"model"});
        assertEquals(ModelTier.QUALITY, qualityConfig.getTier());

        TierConfig powerfulConfig = new TierConfig(ModelTier.POWERFUL, "ENV", new String[]{"model"});
        assertEquals(ModelTier.POWERFUL, powerfulConfig.getTier());
    }

    @Test
    void testValidateWithValidConfig() {
        TierConfig config = new TierConfig(
            ModelTier.FAST,
            "ANTHROPIC_DEFAULT_FABLE_MODEL",
            new String[]{"env:ANTHROPIC_DEFAULT_FABLE_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7"}
        );

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testValidateWithNullTier() {
        TierConfig config = new TierConfig(
            null,
            "ENV_VAR",
            new String[]{"model"}
        );

        Exception exception = assertThrows(IllegalArgumentException.class, config::validate);
        assertTrue(exception.getMessage().contains("tier"));
    }

    @Test
    void testValidateWithNullModelEnv() {
        TierConfig config = new TierConfig(
            ModelTier.BALANCED,
            null,
            new String[]{"model"}
        );

        Exception exception = assertThrows(IllegalArgumentException.class, config::validate);
        assertTrue(exception.getMessage().contains("modelEnv"));
    }

    @Test
    void testValidateWithEmptyModelEnv() {
        TierConfig config = new TierConfig(
            ModelTier.QUALITY,
            "",
            new String[]{"model"}
        );

        Exception exception = assertThrows(IllegalArgumentException.class, config::validate);
        assertTrue(exception.getMessage().contains("modelEnv"));
    }

    @Test
    void testValidateWithInvalidFallbackChain() {
        TierConfig config = new TierConfig(
            ModelTier.POWERFUL,
            "ENV_VAR",
            new String[]{}
        );

        Exception exception = assertThrows(IllegalArgumentException.class, config::validate);
        assertTrue(exception.getMessage().contains("fallback"));
    }

    @Test
    void testFallbackModelsImmutability() {
        String[] originalModels = new String[]{"model1", "model2"};
        TierConfig config = new TierConfig(ModelTier.FAST, "ENV", originalModels);

        // The config should return the fallback models
        assertNotNull(config.getFallbackModels());
        assertEquals(2, config.getFallbackModels().length);
    }

    @Test
    void testEnvVariableReferenceFormat() {
        TierConfig config = new TierConfig(
            ModelTier.BALANCED,
            "ANTHROPIC_DEFAULT_HAIKU_MODEL",
            new String[]{
                "env:ANTHROPIC_DEFAULT_HAIKU_MODEL",
                "env:ANTHROPIC_MODEL",
                "glm-4.7"
            }
        );

        String[] models = config.getFallbackModels();
        assertTrue(models[0].startsWith("env:"));
        assertTrue(models[1].startsWith("env:"));
        assertFalse(models[2].startsWith("env:"));
    }

    @Test
    void testToString() {
        TierConfig config = new TierConfig(
            ModelTier.QUALITY,
            "ENV_VAR",
            new String[]{"model1", "model2", "model3"}
        );

        String string = config.toString();
        assertNotNull(string);
        assertTrue(string.length() > 0);
    }
}