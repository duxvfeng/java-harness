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
        assertEquals(3, config.getFallbackChain().length);
        assertEquals("env:ANTHROPIC_DEFAULT_FABLE_MODEL", config.getFallbackChain()[0]);
    }

    @Test
    void testGetFallbackChain() {
        String[] fallbackModels = new String[]{"model1", "model2", "model3"};
        TierConfig config = new TierConfig(ModelTier.BALANCED, "ENV_VAR", fallbackModels);

        String[] retrieved = config.getFallbackChain();
        assertArrayEquals(fallbackModels, retrieved);
        // Note: The returned array is a defensive copy, not the same reference
        assertNotSame(fallbackModels, retrieved);
    }

    @Test
    void testEmptyFallbackChain() {
        TierConfig config = new TierConfig(
            ModelTier.QUALITY,
            "ENV_VAR",
            new String[]{}
        );

        assertEquals(0, config.getFallbackChain().length);
        assertFalse(config.hasValidFallbackChain());
    }

    @Test
    void testNullFallbackChain() {
        TierConfig config = new TierConfig(
            ModelTier.POWERFUL,
            "ENV_VAR",
            null
        );

        assertNull(config.getFallbackChain());
        assertFalse(config.hasValidFallbackChain());
    }

    @Test
    void testHasValidFallbackChain() {
        // Valid chain with multiple models
        TierConfig validConfig = new TierConfig(
            ModelTier.FAST,
            "ENV_VAR",
            new String[]{"model1", "model2"}
        );
        assertTrue(validConfig.hasValidFallbackChain());

        // Valid chain with single model
        TierConfig singleConfig = new TierConfig(
            ModelTier.BALANCED,
            "ENV_VAR",
            new String[]{"model1"}
        );
        assertTrue(singleConfig.hasValidFallbackChain());

        // Invalid chain (empty)
        TierConfig emptyConfig = new TierConfig(
            ModelTier.QUALITY,
            "ENV_VAR",
            new String[]{}
        );
        assertFalse(emptyConfig.hasValidFallbackChain());

        // Invalid chain (null)
        TierConfig nullConfig = new TierConfig(
            ModelTier.POWERFUL,
            "ENV_VAR",
            null
        );
        assertFalse(nullConfig.hasValidFallbackChain());
    }

    @Test
    void testGetTierName() {
        TierConfig fastConfig = new TierConfig(ModelTier.FAST, "ENV", new String[]{"model"});
        assertEquals("FAST", fastConfig.getTierName());

        TierConfig balancedConfig = new TierConfig(ModelTier.BALANCED, "ENV", new String[]{"model"});
        assertEquals("BALANCED", balancedConfig.getTierName());

        TierConfig qualityConfig = new TierConfig(ModelTier.QUALITY, "ENV", new String[]{"model"});
        assertEquals("QUALITY", qualityConfig.getTierName());

        TierConfig powerfulConfig = new TierConfig(ModelTier.POWERFUL, "ENV", new String[]{"model"});
        assertEquals("POWERFUL", powerfulConfig.getTierName());
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
    void testFallbackChainImmutability() {
        String[] originalChain = new String[]{"model1", "model2"};
        TierConfig config = new TierConfig(ModelTier.FAST, "ENV", originalChain);

        // Modify the original array
        originalChain[0] = "modified";

        // The config should still have the original values
        assertEquals("model1", config.getFallbackChain()[0]);
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

        String[] chain = config.getFallbackChain();
        assertTrue(chain[0].startsWith("env:"));
        assertTrue(chain[1].startsWith("env:"));
        assertFalse(chain[2].startsWith("env:"));
    }

    @Test
    void testGetDisplayName() {
        TierConfig config = new TierConfig(
            ModelTier.FAST,
            "ANTHROPIC_DEFAULT_FABLE_MODEL",
            new String[]{"model1", "model2"}
        );

        String displayName = config.getDisplayName();
        assertTrue(displayName.contains("FAST"));
        assertTrue(displayName.contains("ANTHROPIC_DEFAULT_FABLE_MODEL"));
    }

    @Test
    void testToString() {
        TierConfig config = new TierConfig(
            ModelTier.QUALITY,
            "ENV_VAR",
            new String[]{"model1", "model2", "model3"}
        );

        String string = config.toString();
        assertTrue(string.contains("QUALITY"));
        assertTrue(string.contains("ENV_VAR"));
        assertTrue(string.contains("3")); // number of fallback models
    }
}