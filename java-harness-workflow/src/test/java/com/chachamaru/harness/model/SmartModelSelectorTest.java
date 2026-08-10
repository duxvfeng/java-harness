package com.chachamaru.harness.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Optional;

/**
 * SmartModelSelector 核心选择器的单元测试
 * 测试模型选择逻辑、降级链执行、异常处理等功能
 */
class SmartModelSelectorTest {

    private SmartModelSelector selector;
    private ModelSelectionConfig testConfig;

    @BeforeEach
    void setUp() {
        // 创建测试配置
        testConfig = createTestConfig();
        selector = new SmartModelSelector(testConfig);
    }

    @AfterEach
    void tearDown() {
        // 清理资源
    }

    @Test
    void testSelectModelForFastTier() {
        // 低复杂度任务（0-2 分）应该选择 FAST 等级
        String model = selector.selectModel(0);
        assertNotNull(model);
        assertFalse(model.trim().isEmpty(), "Model should not be empty");
        // 模型可以是降级链中的任何一个（环境变量或直接模型名）

        String model2 = selector.selectModel(2);
        assertNotNull(model2);
        assertFalse(model2.trim().isEmpty());
    }

    @Test
    void testSelectModelForBalancedTier() {
        // 中等复杂度任务（3-4 分）应该选择 BALANCED 等级
        String model = selector.selectModel(3);
        assertNotNull(model);
        assertFalse(model.trim().isEmpty(), "Model should not be empty");

        String model2 = selector.selectModel(4);
        assertNotNull(model2);
        assertFalse(model2.trim().isEmpty());
    }

    @Test
    void testSelectModelForQualityTier() {
        // 高复杂度任务（5-6 分）应该选择 QUALITY 等级
        String model = selector.selectModel(5);
        assertNotNull(model);
        assertFalse(model.trim().isEmpty(), "Model should not be empty");

        String model2 = selector.selectModel(6);
        assertNotNull(model2);
        assertFalse(model2.trim().isEmpty());
    }

    @Test
    void testSelectModelForPowerfulTier() {
        // 超高复杂度任务（≥7 分）应该选择 POWERFUL 等级
        String model = selector.selectModel(7);
        assertNotNull(model);
        assertFalse(model.trim().isEmpty(), "Model should not be empty");

        String model2 = selector.selectModel(10);
        assertNotNull(model2);
        assertFalse(model2.trim().isEmpty());

        String model3 = selector.selectModel(100);
        assertNotNull(model3);
        assertFalse(model3.trim().isEmpty());
    }

    @Test
    void testSelectModelWithNegativeScore() {
        // 负数分数应该默认使用 FAST 等级
        String model = selector.selectModel(-1);
        assertNotNull(model);
    }

    @Test
    void testSelectModelWithAllTiersUnavailable() {
        // 创建所有降级链都无效的配置
        ModelSelectionConfig invalidConfig = createInvalidConfig();
        SmartModelSelector invalidSelector = new SmartModelSelector(invalidConfig);

        // 应该抛出异常或返回兜底模型
        assertThrows(ModelUnavailableException.class, () -> {
            invalidSelector.selectModel(5);
        });
    }

    @Test
    void testSelectModelWithEmptyTierConfig() {
        // 创建缺少某个等级的配置
        ModelSelectionConfig incompleteConfig = createIncompleteConfig();
        SmartModelSelector incompleteSelector = new SmartModelSelector(incompleteConfig);

        // 当请求缺失的等级时应该抛出异常
        assertThrows(ModelUnavailableException.class, () -> {
            incompleteSelector.selectModel(7); // POWERFUL 等级缺失
        });
    }

    @Test
    void testFallbackChainExecution() {
        // 测试降级链执行逻辑
        String model = selector.selectModel(3);

        // 应该成功选择一个模型（无论是环境变量还是兜底）
        assertNotNull(model);
        assertFalse(model.trim().isEmpty(), "Should return a valid model from fallback chain");

        // 验证模型是有效的（不为空且合理长度）
        assertTrue(model.length() > 0 && model.length() <= 100, "Model name should have valid length");
    }

    @Test
    void testSelectModelConsistency() {
        // 测试相同复杂度分数的选择一致性
        String model1 = selector.selectModel(3);
        String model2 = selector.selectModel(3);

        // 应该返回相同的模型（假设环境状态不变）
        assertEquals(model1, model2, "Same score should return same model");
    }

    @Test
    void testSelectModelWithBoundaryScores() {
        // 测试边界值
        String fastModel = selector.selectModel(2);  // FAST 上界
        String balancedModel = selector.selectModel(3);  // BALANCED 下界

        assertNotNull(fastModel);
        assertNotNull(balancedModel);
    }

    @Test
    void testGetConfiguration() {
        // 测试获取配置
        ModelSelectionConfig config = selector.getConfiguration();
        assertNotNull(config);
        assertEquals(testConfig, config);
    }

    @Test
    void testModelUnavailableException() {
        // 测试模型不可用异常
        ModelSelectionConfig invalidConfig = createInvalidConfig();
        SmartModelSelector invalidSelector = new SmartModelSelector(invalidConfig);

        try {
            invalidSelector.selectModel(5);
            fail("Should throw ModelUnavailableException");
        } catch (ModelUnavailableException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("No models available"));
        }
    }

    @Test
    void testSelectModelWithZeroScore() {
        // 零分应该选择 FAST 等级
        String model = selector.selectModel(0);
        assertNotNull(model);
    }

    @Test
    void testSelectModelWithHighScore() {
        // 高分应该选择 POWERFUL 等级
        String model = selector.selectModel(999);
        assertNotNull(model);
    }

    @Test
    void testAllTiersReturnValidModels() {
        // 测试所有等级都能返回有效的模型
        for (int score = 0; score <= 10; score++) {
            String model = selector.selectModel(score);
            assertNotNull(model, "Score " + score + " should return a model");
            assertFalse(model.trim().isEmpty(), "Model should not be empty for score " + score);
        }
    }

    @Test
    void testSelectModelWithConfigValidation() {
        // 测试配置验证集成
        ModelSelectionConfig validConfig = createTestConfig();
        assertDoesNotThrow(() -> {
            SmartModelSelector validSelector = new SmartModelSelector(validConfig);
            validSelector.selectModel(5);
        });
    }

    // Helper methods to create test configurations

    private ModelSelectionConfig createTestConfig() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, new TierConfig(
                ModelTier.FAST,
                "ANTHROPIC_DEFAULT_FABLE_MODEL",
                new String[]{"env:ANTHROPIC_DEFAULT_FABLE_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7"}
            ),
            ModelTier.BALANCED, new TierConfig(
                ModelTier.BALANCED,
                "ANTHROPIC_DEFAULT_HAIKU_MODEL",
                new String[]{"env:ANTHROPIC_DEFAULT_HAIKU_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7"}
            ),
            ModelTier.QUALITY, new TierConfig(
                ModelTier.QUALITY,
                "ANTHROPIC_DEFAULT_SONNET_MODEL",
                new String[]{"env:ANTHROPIC_DEFAULT_SONNET_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7"}
            ),
            ModelTier.POWERFUL, new TierConfig(
                ModelTier.POWERFUL,
                "ANTHROPIC_DEFAULT_OPUS_MODEL",
                new String[]{"env:ANTHROPIC_DEFAULT_OPUS_MODEL", "env:ANTHROPIC_MODEL", "glm-4.7"}
            )
        );

        return new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );
    }

    private ModelSelectionConfig createInvalidConfig() {
        // 创建所有降级链都无效的配置
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.QUALITY, new TierConfig(
                ModelTier.QUALITY,
                "NON_EXISTENT_VAR",
                new String[]{"env:NON_EXISTENT_VAR_1", "env:NON_EXISTENT_VAR_2"}
            )
        );

        return new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );
    }

    private ModelSelectionConfig createIncompleteConfig() {
        // 创建缺少 POWERFUL 等级的配置
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.FAST, new TierConfig(
                ModelTier.FAST,
                "ANTHROPIC_DEFAULT_FABLE_MODEL",
                new String[]{"glm-4.7"}
            ),
            ModelTier.BALANCED, new TierConfig(
                ModelTier.BALANCED,
                "ANTHROPIC_DEFAULT_HAIKU_MODEL",
                new String[]{"glm-4.7"}
            ),
            ModelTier.QUALITY, new TierConfig(
                ModelTier.QUALITY,
                "ANTHROPIC_DEFAULT_SONNET_MODEL",
                new String[]{"glm-4.7"}
            )
            // 缺少 POWERFUL 等级
        );

        return new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs,
            5000,
            3,
            false
        );
    }
}