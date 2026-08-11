package com.chachamaru.harness.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        assertTrue(config.getTierConfigs().containsKey(ModelTier.FAST));
        assertTrue(config.getTierConfigs().containsKey(ModelTier.BALANCED));
        assertTrue(config.getTierConfigs().containsKey(ModelTier.QUALITY));
        assertTrue(config.getTierConfigs().containsKey(ModelTier.POWERFUL));
    }

    @Test
    void testAllTiersLoadedCorrectly(@TempDir Path tempDir) throws IOException {
        // 创建包含所有等级的配置
        Path settingsPath = tempDir.resolve(".claude");
        Files.createDirectories(settingsPath);

        Path settingsFile = settingsPath.resolve("settings.json");
        String jsonContent = """
            {
              "modelSelection": {
                "enabled": true,
                "strategy": "effortBased",
                "tierMapping": {
                  "fast": {
                    "modelEnv": "FAST_MODEL",
                    "fallbackModels": ["fast1", "fast2"]
                  },
                  "balanced": {
                    "modelEnv": "BALANCED_MODEL",
                    "fallbackModels": ["balanced1", "balanced2"]
                  },
                  "quality": {
                    "modelEnv": "QUALITY_MODEL",
                    "fallbackModels": ["quality1", "quality2"]
                  },
                  "powerful": {
                    "modelEnv": "POWERFUL_MODEL",
                    "fallbackModels": ["powerful1", "powerful2"]
                  }
                }
              }
            }
            """;
        Files.writeString(settingsFile, jsonContent);

        // 在临时目录中测试
        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        try {
            ModelSelectionConfigLoader testLoader = new ModelSelectionConfigLoader();
            ModelSelectionConfig config = testLoader.loadOrDefault();

            assertNotNull(config);
            assertEquals(4, config.getTierConfigs().size());
            assertNotNull(config.getTierConfig(ModelTier.FAST));
            assertNotNull(config.getTierConfig(ModelTier.BALANCED));
            assertNotNull(config.getTierConfig(ModelTier.QUALITY));
            assertNotNull(config.getTierConfig(ModelTier.POWERFUL));
        } finally {
            System.setProperty("user.dir", originalDir);
        }
    }

    @Test
    void testLoadFromSettingsJsonWithValidConfig(@TempDir Path tempDir) throws IOException {
        // 创建测试配置文件
        Path settingsPath = tempDir.resolve(".claude");
        Files.createDirectories(settingsPath);

        Path settingsFile = settingsPath.resolve("settings.json");
        String jsonContent = """
            {
              "modelSelection": {
                "enabled": true,
                "strategy": "effortBased",
                "tierMapping": {
                  "fast": {
                    "modelEnv": "ANTHROPIC_DEFAULT_FABLE_MODEL",
                    "fallbackModels": [
                      "env:ANTHROPIC_DEFAULT_FABLE_MODEL",
                      "env:ANTHROPIC_MODEL",
                      "glm-4.7"
                    ]
                  }
                }
              }
            }
            """;
        Files.writeString(settingsFile, jsonContent);

        // 在临时目录中测试
        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        try {
            ModelSelectionConfigLoader testLoader = new ModelSelectionConfigLoader();
            ModelSelectionConfig config = testLoader.loadOrDefault();

            assertNotNull(config);
            assertTrue(config.isEnabled());
            assertEquals("effortBased", config.getStrategy());
            assertNotNull(config.getTierConfig(ModelTier.FAST));
        } finally {
            System.setProperty("user.dir", originalDir);
        }
    }

    @Test
    void testLoadFromHarnessTomlWithValidConfig(@TempDir Path tempDir) throws IOException {
        // 创建测试配置文件
        Path tomlFile = tempDir.resolve("harness.toml");
        String tomlContent = """
            [model_selection]
            enable_smart_selection = true
            strategy = "effort_based"

            [model_selection.tiers.fast]
            model_env = "ANTHROPIC_DEFAULT_FABLE_MODEL"
            fallback_models = [
              "env:ANTHROPIC_DEFAULT_FABLE_MODEL",
              "env:ANTHROPIC_MODEL",
              "glm-4.7"
            ]
            """;
        Files.writeString(tomlFile, tomlContent);

        // 在临时目录中测试
        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        try {
            ModelSelectionConfigLoader testLoader = new ModelSelectionConfigLoader();
            ModelSelectionConfig config = testLoader.loadOrDefault();

            assertNotNull(config);
            assertTrue(config.isEnabled());
            assertEquals("effortBased", config.getStrategy());
            assertNotNull(config.getTierConfig(ModelTier.FAST));
        } finally {
            System.setProperty("user.dir", originalDir);
        }
    }

    @Test
    void testConfigPrioritySettingsJsonOverridesToml(@TempDir Path tempDir) throws IOException {
        // 创建两个配置文件
        Path settingsPath = tempDir.resolve(".claude");
        Files.createDirectories(settingsPath);

        Path settingsFile = settingsPath.resolve("settings.json");
        String jsonContent = """
            {
              "modelSelection": {
                "enabled": false,
                "strategy": "custom_strategy"
              }
            }
            """;
        Files.writeString(settingsFile, jsonContent);

        Path tomlFile = tempDir.resolve("harness.toml");
        String tomlContent = """
            [model_selection]
            enable_smart_selection = true
            strategy = "toml_strategy"
            """;
        Files.writeString(tomlFile, tomlContent);

        // 在临时目录中测试
        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        try {
            ModelSelectionConfigLoader testLoader = new ModelSelectionConfigLoader();
            ModelSelectionConfig config = testLoader.loadOrDefault();

            // JSON 配置应该覆盖 TOML 配置
            assertNotNull(config);
            assertFalse(config.isEnabled()); // JSON 的 false 应该覆盖 TOML 的 true
            assertEquals("custom_strategy", config.getStrategy()); // JSON 的 strategy 应该被使用
        } finally {
            System.setProperty("user.dir", originalDir);
        }
    }

    @Test
    void testLoadWithInvalidJsonReturnsDefaultConfig(@TempDir Path tempDir) throws IOException {
        // 创建无效的 JSON 文件
        Path settingsPath = tempDir.resolve(".claude");
        Files.createDirectories(settingsPath);

        Path settingsFile = settingsPath.resolve("settings.json");
        Files.writeString(settingsFile, "{ invalid json }");

        // 在临时目录中测试
        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        try {
            ModelSelectionConfigLoader testLoader = new ModelSelectionConfigLoader();
            ModelSelectionConfig config = testLoader.loadOrDefault();

            // 应该回退到默认配置
            assertNotNull(config);
            assertTrue(config.isEnabled());
        } finally {
            System.setProperty("user.dir", originalDir);
        }
    }

    @Test
    void testLoadWithInvalidTomlReturnsDefaultConfig(@TempDir Path tempDir) throws IOException {
        // 创建无效的 TOML 文件
        Path tomlFile = tempDir.resolve("harness.toml");
        Files.writeString(tomlFile, "invalid toml content [(");

        // 在临时目录中测试
        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        try {
            ModelSelectionConfigLoader testLoader = new ModelSelectionConfigLoader();
            ModelSelectionConfig config = testLoader.loadOrDefault();

            // 应该回退到默认配置
            assertNotNull(config);
            assertTrue(config.isEnabled());
        } finally {
            System.setProperty("user.dir", originalDir);
        }
    }

    @Test
    void testDefaultConfigFallbackChainStructure() {
        // 测试默认配置的降级链结构
        ModelSelectionConfig defaultConfig = loader.loadOrDefault();

        Map<ModelTier, TierConfig> tierConfigs = defaultConfig.getTierConfigs();

        for (ModelTier tier : tierConfigs.keySet()) {
            TierConfig tierConfig = tierConfigs.get(tier);
            String[] fallbackChain = tierConfig.getFallbackModels();

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
        ModelSelectionConfig defaultConfig = loader.loadOrDefault();

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
        ModelSelectionConfig defaultConfig = loader.loadOrDefault();
        assertDoesNotThrow(() -> defaultConfig.validate());
    }

    @Test
    void testGetDefaultConfigIsConsistent() {
        // 测试默认配置的一致性
        ModelSelectionConfig config1 = loader.loadOrDefault();
        ModelSelectionConfig config2 = loader.loadOrDefault();

        // 默认配置的基本属性应该是一致的
        assertEquals(config1.getStrategy(), config2.getStrategy());
        assertEquals(config1.isEnabled(), config2.isEnabled());
        assertEquals(config1.getTierConfigs().size(), config2.getTierConfigs().size());
    }
}