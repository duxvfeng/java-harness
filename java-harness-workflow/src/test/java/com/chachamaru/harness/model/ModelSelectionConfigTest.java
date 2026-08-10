package com.chachamaru.harness.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Optional;

/**
 * ModelSelectionConfig 总配置类的单元测试
 * 测试所有等级配置管理、全局设置、启用/禁用功能等
 */
class ModelSelectionConfigTest {

    @Test
    void testConfigCreation() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig(),
            ModelTier.BALANCED, createBalancedTierConfig()
        );

        ModelSelectionConfig config = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        assertTrue(config.isEnabled());
        assertEquals("effortBased", config.getStrategy());
        assertEquals(2, config.getTierConfigs().size());
        assertEquals(5000, config.getTimeout());
        assertEquals(3, config.getMaxAttempts());
        assertFalse(config.isValidateApiCall());
    }

    @Test
    void testGetTierConfig() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig(),
            ModelTier.BALANCED, createBalancedTierConfig(),
            ModelTier.QUALITY, createQualityTierConfig(),
            ModelTier.POWERFUL, createPowerfulTierConfig()
        );

        ModelSelectionConfig config = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        Optional<TierConfig> fastConfig = config.getTierConfig(ModelTier.FAST);
        assertTrue(fastConfig.isPresent());
        assertEquals("ANTHROPIC_DEFAULT_FABLE_MODEL", fastConfig.get().getModelEnv());

        Optional<TierConfig> balancedConfig = config.getTierConfig(ModelTier.BALANCED);
        assertTrue(balancedConfig.isPresent());
        assertEquals("ANTHROPIC_DEFAULT_HAIKU_MODEL", balancedConfig.get().getModelEnv());
    }

    @Test
    void testGetTierConfigNotFound() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig()
        );

        ModelSelectionConfig config = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        Optional<TierConfig> result = config.getTierConfig(ModelTier.POWERFUL);
        assertFalse(result.isPresent());
    }

    @Test
    void testEnabledDisabled() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig()
        );

        ModelSelectionConfig enabledConfig = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        ModelSelectionConfig disabledConfig = new ModelSelectionConfig(
            false,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        assertTrue(enabledConfig.isEnabled());
        assertFalse(disabledConfig.isEnabled());
    }

    @Test
    void testStrategy() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig()
        );

        ModelSelectionConfig effortBasedConfig = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        ModelSelectionConfig costBasedConfig = new ModelSelectionConfig(
            true,
            "costBased",
            tierConfigs,
            5000,
            3,
            false
        );

        assertEquals("effortBased", effortBasedConfig.getStrategy());
        assertEquals("costBased", costBasedConfig.getStrategy());
    }

    @Test
    void testTimeoutSettings() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig()
        );

        ModelSelectionConfig config = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            10000,
            3,
            false
        );

        assertEquals(10000, config.getTimeout());
    }

    @Test
    void testMaxAttemptsSettings() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig()
        );

        ModelSelectionConfig config = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            5,
            false
        );

        assertEquals(5, config.getMaxAttempts());
    }

    @Test
    void testValidateApiCallSettings() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig()
        );

        ModelSelectionConfig configWithValidation = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            true
        );

        ModelSelectionConfig configWithoutValidation = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        assertTrue(configWithValidation.isValidateApiCall());
        assertFalse(configWithoutValidation.isValidateApiCall());
    }

    @Test
    void testValidateWithValidConfig() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig(),
            ModelTier.BALANCED, createBalancedTierConfig()
        );

        ModelSelectionConfig config = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateWithEmptyTierConfigs() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of();

        ModelSelectionConfig config = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        Exception exception = assertThrows(IllegalArgumentException.class, config::validate);
        assertTrue(exception.getMessage().contains("tier configs"));
    }

    @Test
    void testValidateWithNullTierConfigs() {
        ModelSelectionConfig config = new ModelSelectionConfig(
            true,
            "effortBased",
            null,
            5000,
            3,
            false
        );

        Exception exception = assertThrows(IllegalArgumentException.class, config::validate);
        assertTrue(exception.getMessage().contains("tier configs"));
    }

    @Test
    void testValidateWithInvalidTimeout() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig()
        );

        ModelSelectionConfig config = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            -1,
            3,
            false
        );

        Exception exception = assertThrows(IllegalArgumentException.class, config::validate);
        assertTrue(exception.getMessage().contains("timeout"));
    }

    @Test
    void testValidateWithInvalidMaxAttempts() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig()
        );

        ModelSelectionConfig config = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            0,
            false
        );

        Exception exception = assertThrows(IllegalArgumentException.class, config::validate);
        assertTrue(exception.getMessage().contains("max attempts"));
    }

    @Test
    void testGetAllTiers() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig(),
            ModelTier.BALANCED, createBalancedTierConfig(),
            ModelTier.QUALITY, createQualityTierConfig()
        );

        ModelSelectionConfig config = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        assertEquals(3, config.getAllTiers().size());
        assertTrue(config.getAllTiers().contains(ModelTier.FAST));
        assertTrue(config.getAllTiers().contains(ModelTier.BALANCED));
        assertTrue(config.getAllTiers().contains(ModelTier.QUALITY));
    }

    @Test
    void testToString() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig()
        );

        ModelSelectionConfig config = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        String string = config.toString();
        assertTrue(string.contains("enabled"));
        assertTrue(string.contains("effortBased"));
        assertTrue(string.contains("1")); // number of tier configs
    }

    @Test
    void testEqualsAndHashCode() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, createFastTierConfig()
        );

        ModelSelectionConfig config1 = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        ModelSelectionConfig config2 = new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    // Helper methods to create tier configs
    private TierConfig createFastTierConfig() {
        return new TierConfig(
            ModelTier.FAST,
            "ANTHROPIC_DEFAULT_FABLE_MODEL",
            new String[]{"env:ANTHROPIC_DEFAULT_FABLE_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7"}
        );
    }

    private TierConfig createBalancedTierConfig() {
        return new TierConfig(
            ModelTier.BALANCED,
            "ANTHROPIC_DEFAULT_HAIKU_MODEL",
            new String[]{"env:ANTHROPIC_DEFAULT_HAIKU_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7"}
        );
    }

    private TierConfig createQualityTierConfig() {
        return new TierConfig(
            ModelTier.QUALITY,
            "ANTHROPIC_DEFAULT_SONNET_MODEL",
            new String[]{"env:ANTHROPIC_DEFAULT_SONNET_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7"}
        );
    }

    private TierConfig createPowerfulTierConfig() {
        return new TierConfig(
            ModelTier.POWERFUL,
            "ANTHROPIC_DEFAULT_OPUS_MODEL",
            new String[]{"env:ANTHROPIC_DEFAULT_OPUS_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7"}
        );
    }
}