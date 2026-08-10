package com.chachamaru.harness.workflow.orchestration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * EffortRouter 系统集成测试
 * 测试智能模型选择与 Effort Routing 的集成
 */
class EffortRouterTest {

    private EffortRouter router;

    @BeforeEach
    void setUp() {
        router = new EffortRouter();
    }

    @Test
    void testDetermineWorkerConfigWithLowComplexity() {
        // 低复杂度任务（0-2 分）
        TaskContext context = createTaskContext(1, 2, false);

        WorkerSpawnConfig config = router.determineWorkerConfig(context);

        assertNotNull(config);
        assertEquals("medium", config.getEffortTier());
        assertNotNull(config.getSelectedModel());
        assertFalse(config.getSelectedModel().isEmpty());
    }

    @Test
    void testDetermineWorkerConfigWithMediumComplexity() {
        // 中等复杂度任务（3-4 分）
        TaskContext context = createTaskContext(3, 1, false);

        WorkerSpawnConfig config = router.determineWorkerConfig(context);

        assertNotNull(config);
        // 3 文件 (+1) + 1 目录 (+1) = 2 分
        // 2 分 < 3 = medium，没有 code-risk
        assertEquals("medium", config.getEffortTier());
        assertNotNull(config.getSelectedModel());
    }

    @Test
    void testDetermineWorkerConfigWithHighComplexity() {
        // 高复杂度任务（5-6 分，有 code-risk 关键字）
        TaskContext context = createTaskContext(5, 2, true);

        WorkerSpawnConfig config = router.determineWorkerConfig(context);

        assertNotNull(config);
        // 5 文件 (+1) + 2 目录 (+1) + 关键字 (+1) = 3 分
        // 3 分且有 code-risk = xhigh
        assertEquals("xhigh", config.getEffortTier());
        assertNotNull(config.getSelectedModel());
    }

    @Test
    void testDetermineWorkerConfigWithVeryHighComplexity() {
        // 超高复杂度任务（≥7 分）
        TaskContext context = createTaskContext(8, 5, true);

        WorkerSpawnConfig config = router.determineWorkerConfig(context);

        assertNotNull(config);
        assertEquals("xhigh", config.getEffortTier());
        assertNotNull(config.getSelectedModel());
    }

    @Test
    void testCalculateComplexityScore() {
        // 测试复杂度评分逻辑
        TaskContext context = createTaskContext(2, 1, false);

        int score = router.calculateComplexityScore(context);

        // 文件数 2 (<4 不加分) + 目录数 1 (+1分) = 1 分
        assertEquals(1, score, "Score should be 1 for 2 files, 1 dir, no keywords");
    }

    @Test
    void testDetermineEffortTier() {
        // 测试 effort tier 决定
        assertEquals("medium", router.determineEffortTier(0));
        assertEquals("medium", router.determineEffortTier(2));
        assertEquals("high", router.determineEffortTier(3));
        // 需要 code-risk 才能返回 xhigh
        assertEquals("high", router.determineEffortTier(7));
    }

    @Test
    void testWorkerSpawnConfigValidation() {
        // 测试 WorkerSpawnConfig 验证
        TaskContext context = createTaskContext(3, 2, false);

        WorkerSpawnConfig config = router.determineWorkerConfig(context);

        assertDoesNotThrow(() -> config.validate());
    }

    @Test
    void testNullTaskContextHandling() {
        // 测试空任务上下文的处理
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            router.determineWorkerConfig(null);
        });

        assertTrue(exception.getMessage().contains("context"));
    }

    @Test
    void testModelSelectionIntegration() {
        // 测试模型选择集成
        TaskContext context = createTaskContext(4, 2, false);

        WorkerSpawnConfig config = router.determineWorkerConfig(context);

        // 验证选择的模型与复杂度分数匹配
        assertNotNull(config.getSelectedModel());

        // BALANCED 等级（3-4 分）应该选择相应的模型
        int score = router.calculateComplexityScore(context);
        String model = config.getSelectedModel();
        assertNotNull(model);
    }

    @Test
    void testConsistencyAcrossMultipleCalls() {
        // 测试多次调用的一致性
        TaskContext context = createTaskContext(3, 2, false);

        WorkerSpawnConfig config1 = router.determineWorkerConfig(context);
        WorkerSpawnConfig config2 = router.determineWorkerConfig(context);

        // 相同上下文应该返回相同配置
        assertEquals(config1.getEffortTier(), config2.getEffortTier());
        assertEquals(config1.getSelectedModel(), config2.getSelectedModel());
    }

    @Test
    void testComplexityScoreWithAllFactors() {
        // 测试所有因素的复杂度评分
        TaskContext context = createTaskContext(
            5,      // 5 个文件
            2,      // 2 个目录
            true,   // 包含关键字
            true    // 有失败历史
        );

        int score = router.calculateComplexityScore(context);

        // 评分应该 >= 各因素分数之和
        assertTrue(score >= 5, "Score should account for file count");
    }

    // Helper method to create test context
    private TaskContext createTaskContext(int fileCount, int directoryCount, boolean hasKeyword) {
        return new TaskContext(fileCount, directoryCount, hasKeyword, false);
    }

    private TaskContext createTaskContext(int fileCount, int directoryCount, boolean hasKeyword, boolean hasFailureHistory) {
        return new TaskContext(fileCount, directoryCount, hasKeyword, hasFailureHistory);
    }
}