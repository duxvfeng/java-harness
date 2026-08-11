package com.chachamaru.harness.integration;

import com.chachamaru.harness.mode.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * ModeRecommender 与 Harness Work 集成测试
 * 验证智能推荐系统与执行流程的正确集成
 */
@DisplayName("ModeRecommender Harness Work 集成测试")
class ModeRecommenderIntegrationTest {

    private final ModeRecommender recommender = new ModeRecommender();

    @Test
    @DisplayName("应该能够为简单的单任务推荐 SOLO 模式")
    void shouldRecommendSoloModeForSimpleSingleTask() {
        List<String> tasks = List.of("fix typo in README");
        List<String> files = List.of("README.md");

        ModeRecommendation recommendation = recommender.recommend(tasks, files);

        assertNotNull(recommendation);
        assertEquals(ExecutionMode.SOLO, recommendation.recommendedMode(),
            "单个简单文档任务应该推荐 SOLO 模式");
        assertTrue(recommendation.confidence() >= 0.7,
            "简单任务推荐应该有较高置信度");
    }

    @Test
    @DisplayName("应该能够为中等复杂度任务组推荐 PARALLEL 模式")
    void shouldRecommendParallelModeForModerateTasks() {
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
        assertTrue(
            recommendation.recommendedMode() == ExecutionMode.PARALLEL ||
            recommendation.recommendedMode() == ExecutionMode.SOLO,
            "3个中等任务应该推荐 PARALLEL 或 SOLO 模式"
        );
        assertTrue(recommendation.confidence() >= 0.5,
            "中等任务推荐应该有合理置信度");
    }

    @Test
    @DisplayName("应该能够为复杂大任务组推荐 BREEZING 模式")
    void shouldRecommendBreezingModeForComplexLargeTasks() {
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
        assertTrue(recommendation.confidence() >= 0.7,
            "复杂大任务推荐应该有较高置信度");
    }

    @Test
    @DisplayName("应该能够处理空任务列表的边界情况")
    void shouldHandleEmptyTaskListEdgeCase() {
        ModeRecommendation recommendation = recommender.recommend(List.of(), List.of());

        assertNotNull(recommendation);
        assertTrue(recommendation.confidence() < 0.5,
            "空任务应该产生低置信度推荐");
    }

    @Test
    @DisplayName("应该能够处理单个任务但无文件的情况")
    void shouldHandleSingleTaskWithoutFiles() {
        List<String> tasks = List.of("document existing code");

        ModeRecommendation recommendation = recommender.recommend(tasks, List.of());

        assertNotNull(recommendation);
        assertTrue(recommendation.confidence() >= 0.0 && recommendation.confidence() <= 1.0,
            "置信度应该在有效范围内");
    }

    @Test
    @DisplayName("应该能够提供调试信息用于分析推荐过程")
    void shouldProvideDebugInformationForRecommendationAnalysis() {
        List<String> tasks = List.of("implement feature X");
        List<String> files = List.of("src/feature/XService.java");

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
    @DisplayName("应该能够使用快速推荐API获取执行模式")
    void shouldProvideQuickRecommendationAPI() {
        List<String> tasks = List.of("simple task");
        List<String> files = List.of("file.java");

        ExecutionMode mode = recommender.quickRecommend(tasks, files);

        assertNotNull(mode, "快速推荐应该返回执行模式");
        assertTrue(mode == ExecutionMode.SOLO || mode == ExecutionMode.PARALLEL || mode == ExecutionMode.BREEZING,
            "返回的模式应该是有效的执行模式");
    }

    @Test
    @DisplayName("高置信度推荐应该可以自动应用")
    void shouldAutoApplyHighConfidenceRecommendations() {
        // 创建一个高置信度的推荐
        List<String> tasks = List.of("fix typo in README");
        List<String> files = List.of("README.md");

        ModeRecommendation recommendation = recommender.recommend(tasks, files);

        assertTrue(recommender.shouldAutoApply(recommendation),
            "高置信度推荐应该可以自动应用");
    }

    @Test
    @DisplayName("低置信度推荐应该需要用户确认")
    void shouldRequireUserConfirmationForLowConfidence() {
        List<String> tasks = List.of("complex ambiguous task");
        List<String> files = List.of("file.java", "file2.java");

        ModeRecommendation recommendation = recommender.recommend(tasks, files);

        // 如果置信度较低，应该需要用户确认
        boolean requiresConfirmation = recommender.requiresUserConfirmation(recommendation);

        assertTrue(
            recommendation.confidence() < 0.7 && requiresConfirmation ||
            recommendation.confidence() >= 0.7 && !requiresConfirmation,
            "置信度与确认需求应该一致"
        );
    }

    @Test
    @DisplayName("应该能够为失败历史的任务提供推荐")
    void shouldProvideRecommendationForTasksWithFailureHistory() {
        List<String> tasks = List.of("retry authentication implementation");
        List<String> files = List.of("src/auth/AuthService.java");

        ModeRecommendation recommendation = recommender.recommend(tasks, files, true);

        assertNotNull(recommendation);
        assertTrue(recommendation.confidence() >= 0.0 && recommendation.confidence() <= 1.0,
            "失败历史任务推荐应该在有效置信度范围内");
    }

    @Test
    @DisplayName("应该能够支持显式effort指定的推荐")
    void shouldSupportExplicitEffortRecommendation() {
        List<String> tasks = List.of("implement feature X");
        List<String> files = List.of("src/feature/XService.java");

        ModeRecommendation highEffortRec = recommender.recommend(tasks, files, false, "high");

        assertNotNull(highEffortRec);
        assertTrue(highEffortRec.confidence() >= 0.0 && highEffortRec.confidence() <= 1.0,
            "显式effort推荐应该在有效范围内");

        ModeRecommendation xHighEffortRec = recommender.recommend(tasks, files, false, "xhigh");

        assertNotNull(xHighEffortRec);
        assertTrue(xHighEffortRec.confidence() >= 0.0 && xHighEffortRec.confidence() <= 1.0,
            "显式xhigh effort推荐应该在有效范围内");
    }

    @Test
    @DisplayName("应该能够使用自定义权重配置")
    void shouldSupportCustomWeightConfiguration() {
        ScoringWeights customWeights = new ScoringWeights(
            0.5,  // 提高任务数量权重
            0.3,  // 降低复杂度权重
            0.15, // 调整依赖关系权重
            0.05  // 降低审查需求权重
        );

        ModeRecommender customRecommender = new ModeRecommender(customWeights);

        List<String> tasks = List.of("task1", "task2");
        List<String> files = List.of("file1.java");

        ModeRecommendation recommendation = customRecommender.recommend(tasks, files);

        assertNotNull(recommendation);
        assertEquals(customWeights, customRecommender.getWeights(),
            "自定义权重应该被正确设置");
    }

    @Test
    @DisplayName("推荐结果应该提供完整的摘要信息")
    void shouldProvideCompleteSummaryInformation() {
        List<String> tasks = List.of("update user profile features");
        List<String> files = List.of("src/user/ProfileService.java");

        ModeRecommendation recommendation = recommender.recommend(tasks, files);

        String summary = recommender.getRecommendationSummary(recommendation);

        assertNotNull(summary);
        assertTrue(summary.contains("推荐模式"), "摘要应该包含推荐模式信息");
        assertTrue(summary.contains("置信度"), "摘要应该包含置信度信息");
        assertTrue(summary.contains("推荐理由"), "摘要应该包含推荐理由");
    }

    @Test
    @DisplayName("集成应该保持推荐的一致性")
    void shouldMaintainRecommendationConsistency() {
        List<String> tasks = List.of("task1", "task2", "task3");
        List<String> files = List.of("file1.java", "file2.java", "file3.java");

        ModeRecommendation rec1 = recommender.recommend(tasks, files);
        ModeRecommendation rec2 = recommender.recommend(tasks, files);
        ModeRecommendation rec3 = recommender.recommend(tasks, files);

        assertEquals(rec1.recommendedMode(), rec2.recommendedMode(),
            "相同输入应该产生一致的推荐模式");
        assertEquals(rec1.recommendedMode(), rec3.recommendedMode(),
            "相同输入应该产生一致的推荐模式");

        assertEquals(rec1.confidence(), rec2.confidence(), 0.001,
            "相同输入应该产生一致的置信度");
        assertEquals(rec1.confidence(), rec3.confidence(), 0.001,
            "相同输入应该产生一致的置信度");
    }

    @Test
    @DisplayName("应该能够处理不同复杂度的任务场景")
    void shouldHandleDifferentComplexityScenarios() {
        // 简单场景
        ModeRecommendation simpleRec = recommender.recommend(
            List.of("fix typo"),
            List.of("README.md")
        );
        assertEquals(ExecutionMode.SOLO, simpleRec.recommendedMode(),
            "简单场景应该推荐 SOLO");

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
        assertTrue(
            complexRec.recommendedMode() == ExecutionMode.BREEZING ||
            complexRec.recommendedMode() == ExecutionMode.PARALLEL,
            "高复杂度应该推荐 BREEZING 或 PARALLEL"
        );
    }

    @Test
    @DisplayName("集成API应该设计清晰易用")
    void integrationAPIShouldBeWellDesigned() {
        // 验证各种重载方法都能正常工作
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

        // 5. 快速推荐API
        ExecutionMode mode = recommender.quickRecommend(tasks, files);
        assertNotNull(mode);
    }

    @Test
    @DisplayName("应该能够处理批推荐场景")
    void shouldHandleBatchRecommendationScenario() {
        List<ModeRecommender.TaskGroup> taskGroups = List.of(
            new ModeRecommender.TaskGroup(
                List.of("task1"),
                List.of("file1.java")
            ),
            new ModeRecommender.TaskGroup(
                List.of("task2", "task3", "task4"),
                List.of("file2.java", "file3.java", "file4.java")
            )
        );

        List<ModeRecommendation> recommendations = recommender.batchRecommend(taskGroups);

        assertNotNull(recommendations);
        assertEquals(2, recommendations.size(),
            "批推荐应该返回对应数量的推荐结果");

        for (ModeRecommendation recommendation : recommendations) {
            assertNotNull(recommendation);
            assertNotNull(recommendation.recommendedMode());
            assertTrue(recommendation.confidence() >= 0.0 && recommendation.confidence() <= 1.0);
        }
    }
}