package com.chachamaru.harness.mode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.List;

/**
 * ModeScorer 评分算法的单元测试
 * 验证智能评分算法对三种执行模式的评分逻辑正确性
 */
@DisplayName("ModeScorer 智能评分算法测试")
class ModeScorerTest {

    private final ModeScorer scorer = new ModeScorer();

    @Test
    @DisplayName("应该能够为单个简单任务正确评分")
    void shouldScoreSingleSimpleTaskCorrectly() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            1, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );

        ModeScores scores = scorer.scoreModes(characteristics);

        assertNotNull(scores);
        assertTrue(scores.soloScore() > scores.parallelScore(), "单个简单任务应该优先 SOLO 模式");
        assertTrue(scores.soloScore() > scores.breezingScore(), "单个简单任务不应该选择 BREEZING 模式");
        assertTrue(scores.soloScore() >= 0.8, "单个简单任务的 SOLO 评分应该很高（≥0.8）");
    }

    @Test
    @DisplayName("应该能够为多个独立中等复杂度任务正确评分")
    void shouldScoreMultipleIndependentTasksCorrectly() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            3, ComplexityLevel.MODERATE, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL
        );

        ModeScores scores = scorer.scoreModes(characteristics);

        assertNotNull(scores);
        assertTrue(scores.parallelScore() > scores.soloScore(), "多个独立任务应该优先 PARALLEL 模式");
        assertTrue(scores.parallelScore() > scores.breezingScore(), "中等复杂度任务组不应该选择 BREEZING");
        assertTrue(scores.parallelScore() >= 0.7, "多个独立任务的 PARALLEL 评分应该较高（≥0.7）");
    }

    @Test
    @DisplayName("应该能够为高复杂度混合依赖任务正确评分")
    void shouldScoreComplexTasksCorrectly() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            5, ComplexityLevel.COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );

        ModeScores scores = scorer.scoreModes(characteristics);

        assertNotNull(scores);
        assertTrue(scores.breezingScore() > scores.parallelScore(), "高复杂度任务应该优先 BREEZING 模式");
        assertTrue(scores.breezingScore() > scores.soloScore(), "高复杂度任务组不应该选择 SOLO");
        assertTrue(scores.breezingScore() >= 0.7, "高复杂度任务的 BREEZING 评分应该较高（≥0.7）");
    }

    @Test
    @DisplayName("应该能够为超高复杂度大任务组正确评分")
    void shouldScoreVeryLargeComplexTaskGroupCorrectly() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            8, ComplexityLevel.VERY_COMPLEX, DependencyType.SEQUENTIAL, ReviewRequirement.REQUIRED
        );

        ModeScores scores = scorer.scoreModes(characteristics);

        assertNotNull(scores);
        assertTrue(scores.breezingScore() >= 0.9, "超高复杂度大任务组的 BREEZING 评分应该很高（≥0.9）");
        assertTrue(scores.breezingScore() > scores.parallelScore() + 0.2, "大任务组应该明显优先 BREEZING");
    }

    @Test
    @DisplayName("应该能够正确处理顺序依赖任务")
    void shouldHandleSequentialDependenciesCorrectly() {
        TaskCharacteristics sequentialTasks = new TaskCharacteristics(
            3, ComplexityLevel.MODERATE, DependencyType.SEQUENTIAL, ReviewRequirement.OPTIONAL
        );

        ModeScores sequentialScores = scorer.scoreModes(sequentialTasks);

        TaskCharacteristics independentTasks = new TaskCharacteristics(
            3, ComplexityLevel.MODERATE, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL
        );

        ModeScores independentScores = scorer.scoreModes(independentTasks);

        // 顺序依赖任务应该降低 PARALLEL 评分
        assertTrue(sequentialScores.parallelScore() < independentScores.parallelScore(),
            "顺序依赖任务应该降低 PARALLEL 模式评分");
    }

    @Test
    @DisplayName("应该能够正确处理审查需求影响")
    void shouldHandleReviewRequirementsCorrectly() {
        TaskCharacteristics noReviewTask = new TaskCharacteristics(
            2, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );

        ModeScores noReviewScores = scorer.scoreModes(noReviewTask);

        TaskCharacteristics requiredReviewTask = new TaskCharacteristics(
            2, ComplexityLevel.COMPLEX, DependencyType.INDEPENDENT, ReviewRequirement.REQUIRED
        );

        ModeScores requiredReviewScores = scorer.scoreModes(requiredReviewTask);

        // 需要审查的任务应该提高 BREEZING 评分（因为有独立 Reviewer）
        assertTrue(requiredReviewScores.breezingScore() > noReviewScores.breezingScore(),
            "需要审查的任务应该提高 BREEZING 模式评分");
    }

    @Test
    @DisplayName("应该能够支持自定义权重配置")
    void shouldSupportCustomWeightConfiguration() {
        ScoringWeights customWeights = new ScoringWeights(
            0.5,  // taskCountWeight
            0.3,  // complexityWeight
            0.1,  // dependencyWeight
            0.1   // reviewRequirementWeight
        );

        ModeScorer customScorer = new ModeScorer(customWeights);

        TaskCharacteristics characteristics = new TaskCharacteristics(
            3, ComplexityLevel.MODERATE, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL
        );

        ModeScores customScores = customScorer.scoreModes(characteristics);
        assertNotNull(customScores);

        // 验证自定义权重产生了不同的评分结果
        ModeScores defaultScores = scorer.scoreModes(characteristics);

        // 由于降低了任务数量的权重，复杂度权重提高，评分应该有所不同
        assertNotEquals(defaultScores.soloScore(), customScores.soloScore(), 0.001,
            "自定义权重应该产生不同的评分结果");
    }

    @Test
    @DisplayName("应该能够返回所有执行模式的评分")
    void shouldReturnAllModeScores() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            2, ComplexityLevel.MODERATE, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL
        );

        ModeScores scores = scorer.scoreModes(characteristics);

        assertNotNull(scores);
        assertTrue(scores.soloScore() >= 0.0 && scores.soloScore() <= 1.0, "SOLO 评分应该在 [0, 1] 范围内");
        assertTrue(scores.parallelScore() >= 0.0 && scores.parallelScore() <= 1.0, "PARALLEL 评分应该在 [0, 1] 范围内");
        assertTrue(scores.breezingScore() >= 0.0 && scores.breezingScore() <= 1.0, "BREEZING 评分应该在 [0, 1] 范围内");
    }

    @Test
    @DisplayName("应该能够提供评分详细分解信息")
    void shouldProvideScoringBreakdown() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            3, ComplexityLevel.MODERATE, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL
        );

        ModeScores scores = scorer.scoreModes(characteristics);

        Map<ExecutionMode, Double> breakdown = scores.scoreBreakdown();
        assertNotNull(breakdown);
        assertEquals(3, breakdown.size(), "应该包含三种执行模式的评分");

        assertTrue(breakdown.containsKey(ExecutionMode.SOLO), "应该包含 SOLO 模式评分");
        assertTrue(breakdown.containsKey(ExecutionMode.PARALLEL), "应该包含 PARALLEL 模式评分");
        assertTrue(breakdown.containsKey(ExecutionMode.BREEZING), "应该包含 BREEZING 模式评分");
    }

    @Test
    @DisplayName("应该能够处理边界条件")
    void shouldHandleBoundaryConditions() {
        // 零任务
        TaskCharacteristics emptyTasks = new TaskCharacteristics(
            0, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );

        ModeScores emptyScores = scorer.scoreModes(emptyTasks);
        assertNotNull(emptyScores);
        // 空任务应该对所有模式给予最低评分
        assertTrue(emptyScores.soloScore() <= 0.1, "空任务应该给予低评分");
        assertTrue(emptyScores.parallelScore() <= 0.1, "空任务应该给予低评分");
        assertTrue(emptyScores.breezingScore() <= 0.1, "空任务应该给予低评分");

        // 大任务组
        TaskCharacteristics largeTasks = new TaskCharacteristics(
            20, ComplexityLevel.VERY_COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );

        ModeScores largeScores = scorer.scoreModes(largeTasks);
        assertNotNull(largeScores);
        assertTrue(largeScores.breezingScore() >= 0.8, "超大任务组应该强烈推荐 BREEZING");
    }

    @Test
    @DisplayName("应该能够返回推荐的最佳模式")
    void shouldReturnRecommendedMode() {
        // 单个简单任务 - 推荐 SOLO
        TaskCharacteristics simpleTask = new TaskCharacteristics(
            1, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );

        assertEquals(ExecutionMode.SOLO, scorer.getRecommendedMode(simpleTask),
            "单个简单任务应该推荐 SOLO 模式");

        // 多个独立任务 - 推荐 PARALLEL
        TaskCharacteristics parallelTasks = new TaskCharacteristics(
            3, ComplexityLevel.MODERATE, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL
        );

        assertEquals(ExecutionMode.PARALLEL, scorer.getRecommendedMode(parallelTasks),
            "多个独立任务应该推荐 PARALLEL 模式");

        // 高复杂度大任务组 - 推荐 BREEZING
        TaskCharacteristics breezingTasks = new TaskCharacteristics(
            6, ComplexityLevel.COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );

        assertEquals(ExecutionMode.BREEZING, scorer.getRecommendedMode(breezingTasks),
            "高复杂度大任务组应该推荐 BREEZING 模式");
    }

    @Test
    @DisplayName("评分权重总和应该为1")
    void scoringWeightsShouldSumToOne() {
        ScoringWeights weights = scorer.getWeights();
        double sum = weights.taskCountWeight() + weights.complexityWeight() +
                     weights.dependencyWeight() + weights.reviewRequirementWeight();

        assertEquals(1.0, sum, 0.001, "评分权重总和应该为1");
    }

    @Test
    @DisplayName("应该能够使用默认权重构造")
    void shouldConstructWithDefaultWeights() {
        ModeScorer defaultScorer = new ModeScorer();
        ScoringWeights weights = defaultScorer.getWeights();

        assertNotNull(weights);
        // 验证默认权重合理
        assertTrue(weights.taskCountWeight() > 0, "任务数量权重应该大于0");
        assertTrue(weights.complexityWeight() > 0, "复杂度权重应该大于0");
        assertTrue(weights.dependencyWeight() > 0, "依赖关系权重应该大于0");
        assertTrue(weights.reviewRequirementWeight() > 0, "审查需求权重应该大于0");
    }
}