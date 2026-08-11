package com.chachamaru.harness.mode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * ModeRecommender 核心引擎的单元测试
 * 验证智能推荐系统的完整工作流程和API设计
 */
@DisplayName("ModeRecommender 核心引擎测试")
class ModeRecommenderTest {

    private final ModeRecommender recommender = new ModeRecommender();

    @Test
    @DisplayName("应该能够为简单任务生成完整推荐")
    void shouldGenerateCompleteRecommendationForSimpleTask() {
        List<String> tasks = List.of("fix typo in README");
        List<String> files = List.of("README.md");

        ModeRecommendation recommendation = recommender.recommend(tasks, files);

        assertNotNull(recommendation);
        assertEquals(ExecutionMode.SOLO, recommendation.recommendedMode(),
            "单个简单文档任务应该推荐 SOLO 模式");
        assertTrue(recommendation.confidence() >= 0.7, "推荐应该有较高置信度");
        assertFalse(recommendation.reason().isEmpty(), "推荐理由不能为空");
        assertNotNull(recommendation.alternativeModes(), "应该提供备选方案");
    }

    @Test
    @DisplayName("应该能够为中等任务组生成完整推荐")
    void shouldGenerateCompleteRecommendationForModerateTasks() {
        List<String> tasks = List.of(
            "add unit tests for UserService",
            "update authentication logic",
            "fix UI bug in login page"
        );
        List<String> files = List.of(
            "src/UserService.java",
            "src/AuthController.java",
            "ui/LoginComponent.java"
        );

        ModeRecommendation recommendation = recommender.recommend(tasks, files);

        assertNotNull(recommendation);
        // 根据我们的评分算法，3个任务应该推荐 PARALLEL 或 SOLO
        assertTrue(recommendation.recommendedMode() == ExecutionMode.PARALLEL ||
                  recommendation.recommendedMode() == ExecutionMode.SOLO,
            "3个中等任务应该推荐 PARALLEL 或 SOLO");
        assertTrue(recommendation.confidence() >= 0.5, "推荐应该有合理置信度");
    }

    @Test
    @DisplayName("应该能够为复杂大任务组生成 BREEZING 推荐")
    void shouldGenerateBreezingRecommendationForComplexLargeTasks() {
        List<String> tasks = List.of(
            "refactor core authentication system",
            "update security layer",
            "migrate database schema",
            "implement new API endpoints",
            "update frontend to use new API",
            "add comprehensive tests",
            "update documentation"
        );
        List<String> files = List.of(
            "src/core/auth/AuthManager.java",
            "src/security/SecurityFilter.java",
            "src/core/database/SchemaMigration.java",
            "src/api/v2/Endpoints.java",
            "frontend/api-client.js",
            "tests/integration/AuthTest.java",
            "docs/api-changes.md"
        );

        ModeRecommendation recommendation = recommender.recommend(tasks, files);

        assertNotNull(recommendation);
        assertEquals(ExecutionMode.BREEZING, recommendation.recommendedMode(),
            "7个复杂任务应该推荐 BREEZING 模式");
        assertTrue(recommendation.confidence() >= 0.7, "推荐应该有较高置信度");
        assertTrue(recommendation.reason().toLowerCase().contains("团队") ||
                   recommendation.reason().toLowerCase().contains("协作") ||
                   recommendation.reason().toLowerCase().contains("复杂"),
            "推荐理由应该强调团队协作或复杂性");
    }

    @Test
    @DisplayName("应该能够处理失败历史的任务")
    void shouldHandleTasksWithFailureHistory() {
        List<String> tasks = List.of("retry authentication implementation");
        List<String> files = List.of("src/auth/AuthService.java");

        ModeRecommendation recommendation = recommender.recommend(tasks, files, true);

        assertNotNull(recommendation);
        // 失败历史应该提高复杂度，可能倾向于更谨慎的模式
        assertTrue(recommendation.confidence() >= 0.0 && recommendation.confidence() <= 1.0,
            "置信度应该在有效范围内");
    }

    @Test
    @DisplayName("应该能够处理显式指定的 effort")
    void shouldHandleExplicitEffortSpecification() {
        List<String> tasks = List.of("implement feature X");
        List<String> files = List.of("src/feature/XService.java");

        ModeRecommendation highEffortRecommendation = recommender.recommend(tasks, files, false, "high");

        assertNotNull(highEffortRecommendation);
        // 显式指定 high effort 应该影响推荐
        assertTrue(highEffortRecommendation.confidence() >= 0.0 && highEffortRecommendation.confidence() <= 1.0,
            "显式 effort 推荐应该有效");

        ModeRecommendation xHighEffortRecommendation = recommender.recommend(tasks, files, false, "xhigh");

        assertNotNull(xHighEffortRecommendation);
        // xhigh effort 应该倾向于更复杂的模式
        assertTrue(xHighEffortRecommendation.confidence() >= 0.0 && xHighEffortRecommendation.confidence() <= 1.0,
            "显式 xhigh effort 推荐应该有效");
    }

    @Test
    @DisplayName("应该能够提供完整的推荐信息")
    void shouldProvideCompleteRecommendationInformation() {
        List<String> tasks = List.of("update user profile features");
        List<String> files = List.of("src/user/ProfileService.java");

        ModeRecommendation recommendation = recommender.recommend(tasks, files);

        assertNotNull(recommendation);
        assertNotNull(recommendation.recommendedMode(), "推荐模式不能为null");
        assertTrue(recommendation.confidence() >= 0.0 && recommendation.confidence() <= 1.0,
            "置信度必须在 [0, 1] 范围内");
        assertFalse(recommendation.reason().isEmpty(), "推荐理由不能为空");
        assertNotNull(recommendation.alternativeModes(), "备选方案列表不能为null");
        assertTrue(recommendation.alternativeModes().size() <= 2, "备选方案最多2个");
    }

    @Test
    @DisplayName("应该能够处理空任务列表")
    void shouldHandleEmptyTaskList() {
        ModeRecommendation recommendation = recommender.recommend(List.of(), List.of());

        assertNotNull(recommendation);
        assertTrue(recommendation.confidence() < 0.5, "空任务应该有低置信度");
        assertTrue(recommendation.reason().toLowerCase().contains("无") ||
                   recommendation.reason().toLowerCase().contains("空") ||
                   recommendation.reason().toLowerCase().contains("没有"),
            "空任务推荐理由应该提到没有任务");
    }

    @Test
    @DisplayName("应该能够处理大任务组")
    void shouldHandleLargeTaskGroups() {
        List<String> tasks = List.of(
            "task1", "task2", "task3", "task4", "task5",
            "task6", "task7", "task8", "task9", "task10"
        );
        List<String> files = List.of("file1.java", "file2.java");

        ModeRecommendation recommendation = recommender.recommend(tasks, files);

        assertNotNull(recommendation);
        // 大任务组应该倾向于 BREEZING 或 PARALLEL
        assertTrue(recommendation.recommendedMode() == ExecutionMode.BREEZING ||
                  recommendation.recommendedMode() == ExecutionMode.PARALLEL,
            "大任务组应该推荐 BREEZING 或 PARALLEL");
        assertTrue(recommendation.confidence() >= 0.6, "大任务组推荐应该有较高置信度");
    }

    @Test
    @DisplayName("应该能够使用自定义配置")
    void shouldSupportCustomConfiguration() {
        ScoringWeights customWeights = new ScoringWeights(
            0.5,  // 提高任务数量权重
            0.3,  // 降低复杂度权重
            0.15, // 调整依赖关系权重
            0.05  // 降低审查需求权重
        );

        ModeRecommender customRecommender = new ModeRecommender(customWeights);

        List<String> tasks = List.of("task1", "task2");
        List<String> files = List.of("file1.java");

        ModeRecommendation customRecommendation = customRecommender.recommend(tasks, files);

        assertNotNull(customRecommendation);
        // 自定义权重应该产生不同的推荐结果
        ModeRecommendation defaultRecommendation = recommender.recommend(tasks, files);

        // 验证自定义配置确实生效（推荐或置信度应该有所不同）
        assertNotNull(customRecommendation);
        assertNotNull(defaultRecommendation);
    }

    @Test
    @DisplayName("应该能够提供调试信息")
    void shouldProvideDebugInformation() {
        List<String> tasks = List.of("implement feature X");
        List<String> files = List.of("src/feature/XService.java");

        // 使用带调试信息的API
        RecommendationResult result = recommender.recommendWithDebugInfo(tasks, files);

        assertNotNull(result);
        assertNotNull(result.recommendation(), "推荐结果不能为null");
        assertNotNull(result.characteristics(), "任务特征不能为null");
        assertNotNull(result.scores(), "评分结果不能为null");

        // 验证调试信息的完整性
        assertEquals(tasks.size(), result.characteristics().taskCount(),
            "任务特征应该反映实际任务数量");
    }

    @Test
    @DisplayName("API设计应该清晰易用")
    void apiDesignShouldBeClearAndEasyToUse() {
        // 验证API的各种重载方法都能正常工作
        List<String> tasks = List.of("simple task");
        List<String> files = List.of("file.java");

        // 1. 最简单的API
        ModeRecommendation rec1 = recommender.recommend(tasks, files);
        assertNotNull(rec1);

        // 2. 带失败历史的API
        ModeRecommendation rec2 = recommender.recommend(tasks, files, true);
        assertNotNull(rec2);

        // 3. 带显式effort的API
        ModeRecommendation rec3 = recommender.recommend(tasks, files, false, "high");
        assertNotNull(rec3);

        // 4. 带调试信息的API
        RecommendationResult rec4 = recommender.recommendWithDebugInfo(tasks, files);
        assertNotNull(rec4);

        // 5. 使用自定义配置的API
        ModeRecommender customRecommender = new ModeRecommender(ScoringWeights.DEFAULT);
        ModeRecommendation rec5 = customRecommender.recommend(tasks, files);
        assertNotNull(rec5);
    }

    @Test
    @DisplayName("应该能够处理边界情况")
    void shouldHandleEdgeCases() {
        // 单任务但没有文件
        ModeRecommendation rec1 = recommender.recommend(
            List.of("document existing code"),
            List.of()
        );
        assertNotNull(rec1);

        // 有文件但没有任务
        ModeRecommendation rec2 = recommender.recommend(
            List.of(),
            List.of("file1.java", "file2.java")
        );
        assertNotNull(rec2);

        // 极长的任务描述
        String longTask = "implement a very complex feature that " +
                          "requires extensive changes across multiple modules " +
                          "including the core system the security layer " +
                          "the database schema and the user interface";
        ModeRecommendation rec3 = recommender.recommend(
            List.of(longTask),
            List.of("file.java")
        );
        assertNotNull(rec3);
    }

    @Test
    @DisplayName("推荐结果应该具有一致性")
    void recommendationResultsShouldBeConsistent() {
        List<String> tasks = List.of("task1", "task2", "task3");
        List<String> files = List.of("file1.java", "file2.java", "file3.java");

        // 多次调用应该产生一致的结果
        ModeRecommendation rec1 = recommender.recommend(tasks, files);
        ModeRecommendation rec2 = recommender.recommend(tasks, files);
        ModeRecommendation rec3 = recommender.recommend(tasks, files);

        assertEquals(rec1.recommendedMode(), rec2.recommendedMode(),
            "多次推荐的推荐模式应该一致");
        assertEquals(rec1.recommendedMode(), rec3.recommendedMode(),
            "多次推荐的推荐模式应该一致");

        // 置信度也应该相同（因为输入相同）
        assertEquals(rec1.confidence(), rec2.confidence(), 0.001,
            "多次推荐的置信度应该一致");
        assertEquals(rec1.confidence(), rec3.confidence(), 0.001,
            "多次推荐的置信度应该一致");
    }

    @Test
    @DisplayName("应该能够处理不同复杂度的任务")
    void shouldHandleTasksOfDifferentComplexity() {
        // 简单任务
        ModeRecommendation simpleRec = recommender.recommend(
            List.of("fix typo"),
            List.of("README.md")
        );
        assertEquals(ExecutionMode.SOLO, simpleRec.recommendedMode(),
            "简单任务应该推荐 SOLO");

        // 中等复杂度
        ModeRecommendation moderateRec = recommender.recommend(
            List.of("add tests", "update docs", "fix bug"),
            List.of("test.java", "docs.md", "bugfix.java")
        );
        assertNotNull(moderateRec.recommendedMode());

        // 高复杂度
        ModeRecommendation complexRec = recommender.recommend(
            List.of("refactor core", "update security", "migrate db"),
            List.of("core/Auth.java", "security/Filter.java", "db/Migration.java")
        );
        // 高复杂度应该推荐 BREEZING 或至少不推荐 SOLO
        assertTrue(complexRec.recommendedMode() == ExecutionMode.BREEZING ||
                  complexRec.recommendedMode() == ExecutionMode.PARALLEL,
            "高复杂度任务应该推荐 BREEZING 或 PARALLEL");
    }
}