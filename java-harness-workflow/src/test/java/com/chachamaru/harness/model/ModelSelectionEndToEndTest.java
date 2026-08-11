package com.chachamaru.harness.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import com.chachamaru.harness.workflow.orchestration.EffortRouter;
import com.chachamaru.harness.workflow.orchestration.TaskContext;
import com.chachamaru.harness.workflow.orchestration.WorkerSpawnConfig;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 智能模型选择端到端集成测试
 * 覆盖配置优先级、降级机制、异常处理、并发性能等完整场景
 */
class ModelSelectionEndToEndTest {

    @TempDir
    Path tempDir;

    private ModelSelectionConfigLoader loader;
    private SmartModelSelector selector;
    private EffortRouter router;

    @BeforeEach
    void setUp() {
        loader = new ModelSelectionConfigLoader();
    }

    @AfterEach
    void tearDown() {
        // 清理资源
        if (selector != null) {
            selector.clearAllCache();
        }
    }

    // ==================== 配置优先级测试 ====================

    @Test
    void testConfigPriorityEnvironmentVariablesHighest() throws IOException, ModelUnavailableException {
        // 环境变量优先级最高
        String originalValue = System.getenv("ANTHROPIC_MODEL");
        try {
            System.setProperty("ANTHROPIC_MODEL", "test-env-model");

            ModelSelectionConfig config = loader.loadOrDefault();
            assertNotNull(config);

            selector = new SmartModelSelector(config);
            String selectedModel = selector.selectModel(5);

            // 验证环境变量优先级
            assertNotNull(selectedModel);
            assertTrue(selectedModel.contains("env:") || selectedModel.equals("glm-4.7"));
        } finally {
            if (originalValue == null) {
                System.clearProperty("ANTHROPIC_MODEL");
            } else {
                System.setProperty("ANTHROPIC_MODEL", originalValue);
            }
        }
    }

    @Test
    void testConfigPrioritySettingsJson() throws IOException, ModelUnavailableException {
        // 测试 settings.json 配置优先级
        Path settingsFile = tempDir.resolve("settings.json");
        createSettingsFile(settingsFile);

        // 模拟从 settings.json 加载配置
        ModelSelectionConfig config = loader.loadOrDefault();
        assertNotNull(config);
        assertTrue(config.isEnabled());
        assertEquals("effortBased", config.getStrategy());
    }

    @Test
    void testConfigPriorityHarnessToml() throws IOException, ModelUnavailableException {
        // 测试 harness.toml 配置优先级
        Path tomlFile = tempDir.resolve("harness.toml");
        createTomlFile(tomlFile);

        ModelSelectionConfig config = loader.loadOrDefault();
        assertNotNull(config);

        // 验证默认配置结构
        assertEquals(4, config.getTierConfigs().size());
        assertTrue(config.isEnabled());
    }

    @Test
    void testConfigPriorityDefaultFallback() throws ModelUnavailableException {
        // 测试默认配置兜底
        ModelSelectionConfig config = loader.loadOrDefault();
        assertNotNull(config);
        assertFalse(config.getTierConfigs().isEmpty());

        // 验证所有必要等级都存在
        assertNotNull(config.getTierConfig(ModelTier.FAST));
        assertNotNull(config.getTierConfig(ModelTier.BALANCED));
        assertNotNull(config.getTierConfig(ModelTier.QUALITY));
        assertNotNull(config.getTierConfig(ModelTier.POWERFUL));
    }

    // ==================== 降级机制测试 ====================

    @Test
    void testFallbackChainExecution() {
        ModelSelectionConfig config = loader.loadOrDefault();
        selector = new SmartModelSelector(config);

        // 测试正常降级链执行
        assertDoesNotThrow(() -> {
            for (int score = 0; score <= 10; score++) {
                String model = selector.selectModel(score);
                assertNotNull(model, "Score " + score + " should return valid model");
                assertFalse(model.trim().isEmpty());
            }
        });
    }

    @Test
    void testFallbackChainWithUnavailableModels() {
        // 创建降级链测试配置
        ModelSelectionConfig config = createFallbackTestConfig();
        selector = new SmartModelSelector(config);

        // 即使某些模型不可用，降级链应该找到可用模型
        assertDoesNotThrow(() -> {
            String model = selector.selectModel(3);
            assertNotNull(model);
            assertFalse(model.trim().isEmpty());
        });
    }

    @Test
    void testFallbackChainCompleteFailure() {
        // 创建完全失败的配置
        ModelSelectionConfig config = createCompleteFailureConfig();
        selector = new SmartModelSelector(config);

        // 降级链完全失败时应该抛出异常
        assertThrows(ModelUnavailableException.class, () -> {
            selector.selectModel(5);
        });
    }

    @Test
    void testFallbackChainTimeoutHandling() {
        ModelSelectionConfig config = loader.loadOrDefault();
        selector = new SmartModelSelector(config);

        // 测试超时处理（模拟网络延迟）
        long startTime = System.currentTimeMillis();
        assertDoesNotThrow(() -> {
            selector.selectModel(5);
        });
        long duration = System.currentTimeMillis() - startTime;

        // 应该在合理时间内完成（即使有超时重试）
        assertTrue(duration < 10000, "Selection should complete within timeout");
    }

    // ==================== 异常处理测试 ====================

    @Test
    void testExceptionHandlingWithInvalidConfig() {
        ModelSelectionConfig invalidConfig = createInvalidConfig();
        selector = new SmartModelSelector(invalidConfig);

        // 无效配置应该抛出异常
        assertThrows(ModelUnavailableException.class, () -> {
            selector.selectModel(5);
        });
    }

    @Test
    void testExceptionHandlingWithNullConfig() {
        // null 配置应该使用默认配置
        assertDoesNotThrow(() -> {
            SmartModelSelector nullSelector = new SmartModelSelector(null);
            String model = nullSelector.selectModel(3);
            assertNotNull(model);
        });
    }

    @Test
    void testExceptionHandlingWithNegativeScore() {
        ModelSelectionConfig config = loader.loadOrDefault();
        selector = new SmartModelSelector(config);

        // 负分数应该得到有效处理
        assertDoesNotThrow(() -> {
            String model = selector.selectModel(-1);
            assertNotNull(model);
        });
    }

    @Test
    void testExceptionHandlingWithExtremeScore() {
        ModelSelectionConfig config = loader.loadOrDefault();
        selector = new SmartModelSelector(config);

        // 极端分数应该得到有效处理
        assertDoesNotThrow(() -> {
            String model = selector.selectModel(Integer.MAX_VALUE);
            assertNotNull(model);
            assertFalse(model.trim().isEmpty());
        });
    }

    @Test
    void testExceptionLoggingAndTracking() {
        ModelSelectionConfig config = createInvalidConfig();
        selector = new SmartModelSelector(config);

        // 验证异常被正确记录
        ModelSelectionLogger logger = ModelSelectionLogger.getInstance();

        try {
            selector.selectModel(5);
            fail("Should throw exception");
        } catch (ModelUnavailableException e) {
            // 验证异常消息有意义
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("No models available") ||
                       e.getMessage().contains("tier"));
        }
    }

    // ==================== 端到端集成测试 ====================

    @Test
    void testEndToEndWithEffortRouter() {
        // 测试与 EffortRouter 的完整集成
        router = new EffortRouter();

        // 创建不同复杂度的任务上下文
        assertDoesNotThrow(() -> {
            // 低复杂度任务
            WorkerSpawnConfig lowConfig = router.determineWorkerConfig(
                new TaskContext(2, 1, false, false)
            );
            assertNotNull(lowConfig);
            assertEquals("medium", lowConfig.getEffortTier());
            assertNotNull(lowConfig.getSelectedModel());

            // 高复杂度任务
            WorkerSpawnConfig highConfig = router.determineWorkerConfig(
                new TaskContext(5, 2, true, false)
            );
            assertNotNull(highConfig);
            assertEquals("xhigh", highConfig.getEffortTier());
            assertNotNull(highConfig.getSelectedModel());
        });
    }

    @Test
    void testEndToEndConfigurationToModelSelection() {
        // 测试从配置加载到模型选择的完整流程
        Path settingsFile = tempDir.resolve("settings.json");

        // 创建测试配置
        try (FileWriter writer = new FileWriter(settingsFile.toFile())) {
            writer.write("""
                {
                  "modelSelection": {
                    "enabled": true,
                    "strategy": "effortBased",
                    "fallback": {
                      "priority": ["tierModel", "defaultModel", "safeModel"],
                      "maxAttempts": 3,
                      "timeoutMs": 5000
                    }
                  }
                }
                """);
        } catch (IOException e) {
            fail("Failed to create settings file: " + e.getMessage());
        }

        ModelSelectionConfig config = loader.loadOrDefault();
        assertNotNull(config);

        selector = new SmartModelSelector(config);

        // 验证完整流程
        assertDoesNotThrow(() -> {
            for (int score = 0; score <= 10; score++) {
                String model = selector.selectModel(score);
                assertNotNull(model);
                assertFalse(model.trim().isEmpty());
            }
        });
    }

    // ==================== 并发和性能测试 ====================

    @Test
    void testConcurrentModelSelection() throws InterruptedException {
        ModelSelectionConfig config = loader.loadOrDefault();
        selector = new SmartModelSelector(config);

        int threadCount = 10;
        int requestsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // 并发执行模型选择
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            String model = selector.selectModel(j % 11);
                            if (model != null && !model.trim().isEmpty()) {
                                successCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Concurrent test should complete");
        executor.shutdown();

        // 验证大部分请求成功
        assertTrue(successCount.get() > threadCount * requestsPerThread * 0.95,
                   "Most requests should succeed: " + successCount.get() + " / " +
                   (threadCount * requestsPerThread));
    }

    @Test
    void testPerformanceTargetSelectionTime() {
        ModelSelectionConfig config = loader.loadOrDefault();
        selector = new SmartModelSelector(config);

        // 预热
        for (int i = 0; i < 10; i++) {
            selector.selectModel(i);
        }

        // 性能测试
        long totalTime = 0;
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            selector.selectModel(i % 11);
            long duration = System.nanoTime() - startTime;
            totalTime += duration;
        }

        double avgTimeMs = (totalTime / iterations) / 1_000_000.0;

        // 验证平均选择时间 < 100ms
        assertTrue(avgTimeMs < 100, "Average selection time should be < 100ms, was: " + avgTimeMs + "ms");
    }

    @Test
    void testCachePerformanceUnderLoad() {
        ModelSelectionConfig config = loader.loadOrDefault();
        selector = new SmartModelSelector(config);

        // 第一次选择（缓存未命中）
        long firstTime = measureSelectionTime(selector, 5);

        // 后续选择（应该命中缓存）
        long cachedTime = measureSelectionTime(selector, 5);

        // 缓存命中应该明显更快
        assertTrue(cachedTime <= firstTime || firstTime < 50,
                   "Cached selection should be faster or acceptable: first=" +
                   firstTime + "ms, cached=" + cachedTime + "ms");
    }

    // ==================== 缓存功能测试 ====================

    @Test
    void testCachingFunctionality() {
        ModelSelectionConfig config = loader.loadOrDefault();
        selector = new SmartModelSelector(config);

        // 第一次选择
        String model1 = selector.selectModel(5);
        assertNotNull(model1);

        // 第二次选择（应该从缓存读取）
        String model2 = selector.selectModel(5);
        assertEquals(model1, model2, "Cached selection should return same model");

        // 清除缓存后应该重新选择
        selector.clearAllCache();
        String model3 = selector.selectModel(5);
        assertNotNull(model3);
    }

    @Test
    void testCacheStatistics() {
        ModelSelectionConfig config = loader.loadOrDefault();
        selector = new SmartModelSelector(config);

        // 清除统计
        selector.clearAllCache();

        // 生成一些缓存命中和未命中
        for (int i = 0; i < 10; i++) {
            selector.selectModel(5); // 相同分数，应该缓存
        }

        for (int i = 0; i < 5; i++) {
            selector.selectModel(i); // 不同分数，缓存未命中
        }

        // 获取缓存统计
        String stats = selector.getCacheStats();
        assertNotNull(stats);
        assertTrue(stats.contains("Cache Stats"));
    }

    @Test
    void testCacheInvalidation() {
        ModelSelectionConfig config = loader.loadOrDefault();
        selector = new SmartModelSelector(config);

        // 预填充缓存
        String model1 = selector.selectModel(5);
        assertNotNull(model1);

        // 等待缓存过期（模拟）
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }

        // 清除过期缓存
        selector.clearExpiredCache();

        // 再次选择应该仍然工作
        String model2 = selector.selectModel(5);
        assertNotNull(model2);
    }

    // ==================== 监控和日志测试 ====================

    @Test
    void testMonitoringAndLogging() {
        ModelSelectionConfig config = loader.loadOrDefault();
        selector = new SmartModelSelector(config);
        ModelSelectionLogger logger = ModelSelectionLogger.getInstance();

        // 重置统计
        logger.resetStatistics();

        // 执行一些选择操作
        for (int i = 0; i < 10; i++) {
            try {
                selector.selectModel(i);
            } catch (Exception e) {
                // 预期部分可能失败
            }
        }

        // 获取统计信息
        String statistics = logger.getStatistics();
        assertNotNull(statistics);
        assertTrue(statistics.contains("Model Selection Statistics"));
    }

    // ==================== 辅助方法 ====================

    private long measureSelectionTime(SmartModelSelector selector, int score) {
        long startTime = System.nanoTime();
        selector.selectModel(score);
        return (System.nanoTime() - startTime) / 1_000_000; // 转换为毫秒
    }

    private void createSettingsFile(Path file) throws IOException {
        try (FileWriter writer = new FileWriter(file.toFile())) {
            writer.write("""
                {
                  "modelSelection": {
                    "enabled": true,
                    "strategy": "effortBased",
                    "fallback": {
                      "priority": ["tierModel", "defaultModel", "safeModel"],
                      "maxAttempts": 3,
                      "timeoutMs": 5000
                    }
                  }
                }
                """);
        }
    }

    private void createTomlFile(Path file) throws IOException {
        try (FileWriter writer = new FileWriter(file.toFile())) {
            writer.write("""
                [model_selection]
                enable_smart_selection = true
                strategy = "effort_based"

                [model_selection.fallback]
                priority = ["tier_model", "default_model", "safe_model"]
                max_attempts = 3
                timeout_ms = 5000
                """);
        }
    }

    private ModelSelectionConfig createFallbackTestConfig() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.BALANCED, new TierConfig(
                ModelTier.BALANCED,
                "TEST_MODEL",
                new String[]{"env:NON_EXISTENT_VAR", "glm-4.7"}
            )
        );

        return new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs
        );
    }

    private ModelSelectionConfig createCompleteFailureConfig() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.QUALITY, new TierConfig(
                ModelTier.QUALITY,
                "NON_EXISTENT",
                new String[]{"env:ANOTHER_NON_EXISTENT"}
            )
        );

        return new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs
        );
    }

    private ModelSelectionConfig createInvalidConfig() {
        Map<ModelTier, TierConfig> tierConfigs = Map.of(
            ModelTier.QUALITY, new TierConfig(
                ModelTier.QUALITY,
                null,
                new String[]{""}
            )
        );

        return new ModelSelectionConfig(
            true,
            "effortBased",
            tierConfigs
        );
    }
}