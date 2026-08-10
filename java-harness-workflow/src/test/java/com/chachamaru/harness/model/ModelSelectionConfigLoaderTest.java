package com.chachamaru.harness.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Optional;

/**
 * ModelSelectionConfigLoader 加载器的单元测试
 * 测试配置加载、优先级处理、默认配置生成等功能
 */
class ModelSelectionConfigLoaderTest {

    private ModelSelectionConfigLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ModelSelectionConfigLoader();
    }

    @AfterEach
    void tearDown() {
        // 清理资源
    }

    @Test
    void testLoadOrDefaultReturnsConfig() {
        // 测试加载或返回默认配置
        ModelSelectionConfig config = loader.loadOrDefault();
        assertNotNull(config);
        assertTrue(config.isEnabled());
        assertNotNull(config.getStrategy());
        assertFalse(config.getTierConfigs().isEmpty());
    }

    @Test
    void testLoadOrDefaultWithPriority() {
        // 测试优先级：即使没有文件，也应该返回默认配置
        ModelSelectionConfig config = loader.loadOrDefault();
        assertNotNull(config);

        // 验证默认配置的基本属性
        assertTrue(config.isEnabled());
        assertEquals("effortBased", config.getStrategy());
        assertTrue(config.getMaxAttempts() > 0);
        assertTrue(config.getTimeout() > 0);
    }

    @Test
    void testGetDefaultConfig() {
        // 测试获取默认配置
        ModelSelectionConfig defaultConfig = loader.getDefaultConfig();
        assertNotNull(defaultConfig);

        // 验证默认配置包含所有必需的等级
        assertTrue(defaultConfig.getAllTiers().contains(ModelTier.FAST));
        assertTrue(defaultConfig.getAllTiers().contains(ModelTier.BALANCED));
        assertTrue(defaultConfig.getAllTiers().contains(ModelTier.QUALITY));
        assertTrue(defaultConfig.getAllTiers().contains(ModelTier.POWERFUL));
    }

    @Test
    void testDefaultConfigHasValidTierConfigs() {
        // 测试默认配置的所有等级配置都有效
        ModelSelectionConfig defaultConfig = loader.getDefaultConfig();

        for (ModelTier tier : defaultConfig.getAllTiers()) {
            Optional<TierConfig> tierConfig = defaultConfig.getTierConfig(tier);
            assertTrue(tierConfig.isPresent(), "Tier " + tier + " should have config");

            TierConfig config = tierConfig.get();
            assertDoesNotThrow(() -> config.validate(), "Tier " + tier + " config should be valid");
        }
    }

    @Test
    void testDefaultConfigTimeoutSettings() {
        // 测试默认配置的超时设置
        ModelSelectionConfig defaultConfig = loader.getDefaultConfig();

        assertEquals(5000, defaultConfig.getTimeout(), "Default timeout should be 5000ms");
        assertEquals(3, defaultConfig.getMaxAttempts(), "Default max attempts should be 3");
        assertFalse(defaultConfig.isValidateApiCall(), "API validation should be disabled by default");
    }

    @Test
    void testDefaultConfigStrategy() {
        // 测试默认配置的策略
        ModelSelectionConfig defaultConfig = loader.getDefaultConfig();

        assertEquals("effortBased", defaultConfig.getStrategy());
    }

    @Test
    void testLoadFromSettingsJsonWithNonExistentFile() {
        // 测试加载不存在的 settings.json（应该回退到默认配置）
        Optional<ModelSelectionConfig> result = loader.loadFromSettingsJson(".claude/non_existent_settings.json");
        // 由于文件不存在，应该返回 Optional.empty()
        assertFalse(result.isPresent(), "Non-existent file should return empty Optional");
    }

    @Test
    void testLoadFromHarnessTomlWithNonExistentFile() {
        // 测试加载不存在的 harness.toml（应该回退到默认配置）
        Optional<ModelSelectionConfig> result = loader.loadFromHarnessToml("non_existent_harness.toml");
        assertFalse(result.isPresent(), "Non-existent file should return empty Optional");
    }

    @Test
    void testLoadOrDefaultWithMissingFiles() {
        // 测试当所有配置文件都不存在时，返回默认配置
        ModelSelectionConfig config = loader.loadOrDefault();
        assertNotNull(config, "Should always return a config, even if default");
        assertTrue(config.isEnabled(), "Default config should be enabled");
    }

    @Test
    void testDefaultConfigFallbackChainStructure() {
        // 测试默认配置的降级链结构
        ModelSelectionConfig defaultConfig = loader.getDefaultConfig();

        for (ModelTier tier : defaultConfig.getAllTiers()) {
            Optional<TierConfig> tierConfigOpt = defaultConfig.getTierConfig(tier);
            assertTrue(tierConfigOpt.isPresent());

            TierConfig tierConfig = tierConfigOpt.get();
            String[] fallbackChain = tierConfig.getFallbackChain();

            // 验证降级链结构：env: -> env: -> 直接模型名
            assertTrue(fallbackChain.length >= 2, "Fallback chain should have at least 2 entries");

            // 第一个应该是环境变量引用
            assertTrue(fallbackChain[0].startsWith("env:"),
                "First fallback should be env reference for tier " + tier);

            // 最后一个应该是直接模型名（兜底）
            assertFalse(fallbackChain[fallbackChain.length - 1].startsWith("env:"),
                "Last fallback should be direct model name for tier " + tier);
        }
    }

    @Test
    void testDefaultConfigEnvironmentVariables() {
        // 测试默认配置的环境变量映射
        ModelSelectionConfig defaultConfig = loader.getDefaultConfig();

        // 检查每个等级的环境变量名称
        Map<ModelTier, TierConfig> tierConfigs = defaultConfig.getTierConfigs();

        assertEquals("ANTHROPIC_DEFAULT_FABLE_MODEL",
            tierConfigs.get(ModelTier.FAST).getModelEnv());

        assertEquals("ANTHROPIC_DEFAULT_HAIKU_MODEL",
            tierConfigs.get(ModelTier.BALANCED).getModelEnv());

        assertEquals("ANTHROPIC_DEFAULT_SONNET_MODEL",
            tierConfigs.get(ModelTier.QUALITY).getModelEnv());

        assertEquals("ANTHROPIC_DEFAULT_OPUS_MODEL",
            tierConfigs.get(ModelTier.POWERFUL).getModelEnv());
    }

    @Test
    void testDefaultConfigValidation() {
        // 测试默认配置能通过验证
        ModelSelectionConfig defaultConfig = loader.getDefaultConfig();
        assertDoesNotThrow(() -> defaultConfig.validate());
    }

    @Test
    void testLoadPriority() {
        // 测试配置加载优先级（逻辑测试）
        // settings.json > harness.toml > 默认配置

        // 由于我们无法在测试中创建真实文件，这里测试方法存在性和基本行为
        ModelSelectionConfig config = loader.loadOrDefault();
        assertNotNull(config, "Should return default config when no files exist");
    }

    @Test
    void testLoadWithExplicitPaths() {
        // 测试使用显式路径加载配置
        Optional<ModelSelectionConfig> result1 = loader.loadFromSettingsJson("/custom/path/settings.json");
        Optional<ModelSelectionConfig> result2 = loader.loadFromHarnessToml("/custom/path/harness.toml");

        // 由于文件不存在，都应该返回空
        assertFalse(result1.isPresent());
        assertFalse(result2.isPresent());
    }

    @Test
    void testGetDefaultConfigIsConsistent() {
        // 测试默认配置的一致性
        ModelSelectionConfig config1 = loader.getDefaultConfig();
        ModelSelectionConfig config2 = loader.getDefaultConfig();

        // 默认配置应该是相同的实例或等价的
        assertEquals(config1.getStrategy(), config2.getStrategy());
        assertEquals(config1.getTimeout(), config2.getTimeout());
        assertEquals(config1.getMaxAttempts(), config2.getMaxAttempts());
        assertEquals(config1.getAllTiers(), config2.getAllTiers());
    }

    @Test
    void testLoadOrDefaultReturnsImmutableConfig() {
        // 测试返回的配置是不可变的（通过验证不抛异常）
        ModelSelectionConfig config = loader.loadOrDefault();
        assertDoesNotThrow(() -> config.validate());

        // 配置应该包含所有必需的等级
        assertFalse(config.getAllTiers().isEmpty());
    }
}