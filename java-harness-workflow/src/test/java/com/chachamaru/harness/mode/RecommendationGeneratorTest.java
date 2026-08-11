package com.chachamaru.harness.mode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

/**
 * RecommendationGenerator 推荐生成器的单元测试
 * 验证推荐结果生成、理由解释、置信度计算、备选方案提供
 */
@DisplayName("RecommendationGenerator 推荐生成器测试")
class RecommendationGeneratorTest {

    private final RecommendationGenerator generator = new RecommendationGenerator();

    @Test
    @DisplayName("应该能够为简单任务生成 SOLO 推荐结果")
    void shouldGenerateSoloRecommendationForSimpleTask() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            1, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );

        ModeScores scores = new ModeScores(
            0.9,  // SOLO - 高评分
            0.3,  // PARALLEL - 低评分
            0.2,  // BREEZING - 最低评分
            Map.of(ExecutionMode.SOLO, 0.9,
                   ExecutionMode.PARALLEL, 0.3,
                   ExecutionMode.BREEZING, 0.2)
        );

        ModeRecommendation recommendation = generator.generate(characteristics, scores);

        assertNotNull(recommendation);
        assertEquals(ExecutionMode.SOLO, recommendation.recommendedMode());
        assertTrue(recommendation.confidence() >= 0.8, "高置信度推荐应该 >= 0.8");
        assertFalse(recommendation.reason().isEmpty(), "推荐理由不能为空");
        assertTrue(recommendation.alternativeModes().contains(ExecutionMode.PARALLEL),
            "备选方案应该包含 PARALLEL");
    }

    @Test
    @DisplayName("应该能够为中等复杂度任务生成 PARALLEL 推荐结果")
    void shouldGenerateParallelRecommendationForModerateTask() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            3, ComplexityLevel.MODERATE, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL
        );

        ModeScores scores = new ModeScores(
            0.5,  // SOLO - 中等评分
            0.8,  // PARALLEL - 高评分
            0.4,  // BREEZING - 低评分
            Map.of(ExecutionMode.SOLO, 0.5,
                   ExecutionMode.PARALLEL, 0.8,
                   ExecutionMode.BREEZING, 0.4)
        );

        ModeRecommendation recommendation = generator.generate(characteristics, scores);

        assertNotNull(recommendation);
        assertEquals(ExecutionMode.PARALLEL, recommendation.recommendedMode());
        assertTrue(recommendation.confidence() >= 0.7, "推荐置信度应该 >= 0.7");
        assertTrue(recommendation.reason().toLowerCase().contains("并行") ||
                   recommendation.reason().toLowerCase().contains("parallel"),
            "推荐理由应该提到并行优势");
    }

    @Test
    @DisplayName("应该能够为复杂任务生成 BREEZING 推荐结果")
    void shouldGenerateBreezingRecommendationForComplexTask() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            6, ComplexityLevel.COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );

        ModeScores scores = new ModeScores(
            0.2,  // SOLO - 低评分
            0.5,  // PARALLEL - 中等评分
            0.9,  // BREEZING - 高评分
            Map.of(ExecutionMode.SOLO, 0.2,
                   ExecutionMode.PARALLEL, 0.5,
                   ExecutionMode.BREEZING, 0.9)
        );

        ModeRecommendation recommendation = generator.generate(characteristics, scores);

        assertNotNull(recommendation);
        assertEquals(ExecutionMode.BREEZING, recommendation.recommendedMode());
        assertTrue(recommendation.confidence() >= 0.8, "高置信度推荐应该 >= 0.8");
        assertTrue(recommendation.reason().toLowerCase().contains("团队") ||
                   recommendation.reason().toLowerCase().contains("协作") ||
                   recommendation.reason().toLowerCase().contains("breezing"),
            "推荐理由应该强调团队协作");
    }

    @Test
    @DisplayName("应该能够计算正确的置信度")
    void shouldCalculateCorrectConfidence() {
        // 高置信度场景：明显最优
        ModeScores clearWinner = new ModeScores(
            0.9,  // SOLO - 明显最高
            0.3,  // PARALLEL - 明显较低
            0.2,  // BREEZING - 最低
            Map.of(ExecutionMode.SOLO, 0.9,
                   ExecutionMode.PARALLEL, 0.3,
                   ExecutionMode.BREEZING, 0.2)
        );

        ModeRecommendation highConfidence = generator.generate(
            new TaskCharacteristics(1, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE),
            clearWinner
        );

        assertTrue(highConfidence.confidence() >= 0.85, "明显最优应该有高置信度 >= 0.85");

        // 中等置信度场景：有一定优势但不是压倒性
        ModeScores moderateWinner = new ModeScores(
            0.6,  // SOLO - 最高但优势不明显
            0.5,  // PARALLEL - 接近
            0.3,  // BREEZING - 较低
            Map.of(ExecutionMode.SOLO, 0.6,
                   ExecutionMode.PARALLEL, 0.5,
                   ExecutionMode.BREEZING, 0.3)
        );

        ModeRecommendation moderateConfidence = generator.generate(
            new TaskCharacteristics(2, ComplexityLevel.MODERATE, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL),
            moderateWinner
        );

        assertTrue(moderateConfidence.confidence() >= 0.6 && moderateConfidence.confidence() < 0.85,
            "中等优势应该有中等置信度 [0.6, 0.85]");

        // 低置信度场景：多模式评分接近
        ModeScores closeScores = new ModeScores(
            0.5,  // SOLO - 与其他模式接近
            0.4,  // PARALLEL - 接近
            0.45, // BREEZING - 很接近
            Map.of(ExecutionMode.SOLO, 0.5,
                   ExecutionMode.PARALLEL, 0.4,
                   ExecutionMode.BREEZING, 0.45)
        );

        ModeRecommendation lowConfidence = generator.generate(
            new TaskCharacteristics(3, ComplexityLevel.MODERATE, DependencyType.MIXED, ReviewRequirement.OPTIONAL),
            closeScores
        );

        assertTrue(lowConfidence.confidence() < 0.7, "评分接近应该有较低置信度 < 0.7");
    }

    @Test
    @DisplayName("应该能够生成清晰合理的推荐理由")
    void shouldGenerateClearAndReasonableRecommendationReason() {
        // 测试不同场景的理由生成
        TaskCharacteristics simpleTask = new TaskCharacteristics(
            1, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );

        ModeScores simpleScores = new ModeScores(
            0.9, 0.2, 0.1,
            Map.of(ExecutionMode.SOLO, 0.9, ExecutionMode.PARALLEL, 0.2, ExecutionMode.BREEZING, 0.1)
        );
        ModeRecommendation simpleRecommendation = generator.generate(simpleTask, simpleScores);

        String simpleReason = simpleRecommendation.reason().toLowerCase();
        assertTrue(simpleReason.contains("单") || simpleReason.contains("1") ||
                   simpleReason.contains("单个") || simpleReason.contains("简单"),
            "简单任务推荐理由应该提到任务数量少或简单");

        TaskCharacteristics complexTask = new TaskCharacteristics(
            8, ComplexityLevel.VERY_COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );

        ModeScores complexScores = new ModeScores(
            0.1, 0.4, 0.9,
            Map.of(ExecutionMode.SOLO, 0.1, ExecutionMode.PARALLEL, 0.4, ExecutionMode.BREEZING, 0.9)
        );
        ModeRecommendation complexRecommendation = generator.generate(complexTask, complexScores);

        String complexReason = complexRecommendation.reason().toLowerCase();
        assertTrue(complexReason.contains("复杂") || complexReason.contains("审查") ||
                   complexReason.contains("团队") || complexReason.contains("协调"),
            "复杂任务推荐理由应该提到复杂度或审查或团队协调");

        // 验证理由长度合理
        assertTrue(simpleRecommendation.reason().length() >= 20,
            "推荐理由应该有足够的详细信息，长度 >= 20");
        assertTrue(simpleRecommendation.reason().length() <= 300,
            "推荐理由不应该过长，长度 <= 300");
    }

    @Test
    @DisplayName("应该能够提供合理的备选方案")
    void shouldProvideReasonableAlternatives() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            3, ComplexityLevel.MODERATE, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL
        );

        ModeScores scores = new ModeScores(
            0.4, 0.8, 0.5,
            Map.of(ExecutionMode.SOLO, 0.4, ExecutionMode.PARALLEL, 0.8, ExecutionMode.BREEZING, 0.5)
        );
        ModeRecommendation recommendation = generator.generate(characteristics, scores);

        List<ExecutionMode> alternatives = recommendation.alternativeModes();

        assertNotNull(alternatives);
        assertTrue(alternatives.size() >= 1, "至少应该提供一个备选方案");
        assertTrue(alternatives.size() <= 2, "备选方案不应该超过2个");

        // 验证推荐的方案不在备选列表中
        assertFalse(alternatives.contains(recommendation.recommendedMode()),
            "推荐的方案不应该出现在备选列表中");

        // 验证备选方案是评分较高的其他模式
        for (ExecutionMode alternative : alternatives) {
            double alternativeScore = scores.getScore(alternative);
            assertTrue(alternativeScore >= 0.3, "备选方案应该有合理的评分 >= 0.3");
        }
    }

    @Test
    @DisplayName("应该能够处理边界条件")
    void shouldHandleBoundaryConditions() {
        // 所有评分都相同的极端情况
        ModeScores equalScores = new ModeScores(
            0.5, 0.5, 0.5,
            Map.of(ExecutionMode.SOLO, 0.5, ExecutionMode.PARALLEL, 0.5, ExecutionMode.BREEZING, 0.5)
        );

        ModeRecommendation equalRecommendation = generator.generate(
            new TaskCharacteristics(2, ComplexityLevel.MODERATE, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL),
            equalScores
        );

        assertNotNull(equalRecommendation);
        assertTrue(equalRecommendation.confidence() < 0.6, "评分相同时置信度应该较低 < 0.6");

        // 最高评分与第二高评分很接近的情况
        ModeScores veryCloseScores = new ModeScores(
            0.51, 0.50, 0.49,
            Map.of(ExecutionMode.SOLO, 0.51, ExecutionMode.PARALLEL, 0.50, ExecutionMode.BREEZING, 0.49)
        );

        ModeRecommendation veryCloseRecommendation = generator.generate(
            new TaskCharacteristics(2, ComplexityLevel.MODERATE, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL),
            veryCloseScores
        );

        assertNotNull(veryCloseRecommendation);
        assertTrue(veryCloseRecommendation.confidence() < 0.7, "评分很接近时置信度应该较低 < 0.7");
    }

    @Test
    @DisplayName("应该能够生成多语言推荐理由")
    void shouldGenerateMultilingualRecommendationReason() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            1, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );

        ModeScores scores = new ModeScores(
            0.9, 0.2, 0.1,
            Map.of(ExecutionMode.SOLO, 0.9, ExecutionMode.PARALLEL, 0.2, ExecutionMode.BREEZING, 0.1)
        );

        // 中文环境
        RecommendationGenerator chineseGenerator = new RecommendationGenerator();
        ModeRecommendation chineseRecommendation = chineseGenerator.generate(characteristics, scores);

        assertNotNull(chineseRecommendation.reason());
        assertFalse(chineseRecommendation.reason().isEmpty(), "中文推荐理由不能为空");
    }

    @Test
    @DisplayName("应该能够针对不同任务特征定制推荐理由")
    void shouldCustomizeReasonForDifferentTaskCharacteristics() {
        // 高审查需求任务
        TaskCharacteristics reviewRequiredTask = new TaskCharacteristics(
            4, ComplexityLevel.COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );

        ModeScores reviewScores = new ModeScores(
            0.2, 0.5, 0.9,
            Map.of(ExecutionMode.SOLO, 0.2, ExecutionMode.PARALLEL, 0.5, ExecutionMode.BREEZING, 0.9)
        );
        ModeRecommendation reviewRecommendation = generator.generate(reviewRequiredTask, reviewScores);

        assertTrue(reviewRecommendation.reason().toLowerCase().contains("审查") ||
                   reviewRecommendation.reason().toLowerCase().contains("review"),
            "需要审查的任务推荐理由应该提到审查需求");

        // 顺序依赖任务
        TaskCharacteristics sequentialTask = new TaskCharacteristics(
            3, ComplexityLevel.MODERATE, DependencyType.SEQUENTIAL, ReviewRequirement.OPTIONAL
        );

        ModeScores sequentialScores = new ModeScores(
            0.3, 0.4, 0.7,
            Map.of(ExecutionMode.SOLO, 0.3, ExecutionMode.PARALLEL, 0.4, ExecutionMode.BREEZING, 0.7)
        );
        ModeRecommendation sequentialRecommendation = generator.generate(sequentialTask, sequentialScores);

        assertTrue(sequentialRecommendation.reason().toLowerCase().contains("依赖") ||
                   sequentialRecommendation.reason().toLowerCase().contains("顺序") ||
                   sequentialRecommendation.reason().toLowerCase().contains("协调"),
            "有依赖关系的任务推荐理由应该提到依赖协调");
    }

    @Test
    @DisplayName("应该能够处理特殊情况：零任务")
    void shouldHandleSpecialCaseZeroTasks() {
        TaskCharacteristics emptyTask = new TaskCharacteristics(
            0, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );

        ModeScores emptyScores = new ModeScores(
            0.05, 0.05, 0.05,
            Map.of(ExecutionMode.SOLO, 0.05, ExecutionMode.PARALLEL, 0.05, ExecutionMode.BREEZING, 0.05)
        );

        ModeRecommendation emptyRecommendation = generator.generate(emptyTask, emptyScores);

        assertNotNull(emptyRecommendation);
        assertTrue(emptyRecommendation.reason().toLowerCase().contains("无") ||
                   emptyRecommendation.reason().toLowerCase().contains("空") ||
                   emptyRecommendation.reason().toLowerCase().contains("没有"),
            "空任务推荐理由应该提到没有任务");
    }

    @Test
    @DisplayName("应该能够处理特殊情况：超大任务组")
    void shouldHandleSpecialCaseLargeTaskGroup() {
        TaskCharacteristics largeTaskGroup = new TaskCharacteristics(
            20, ComplexityLevel.VERY_COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );

        ModeScores largeScores = new ModeScores(
            0.0, 0.3, 1.0,
            Map.of(ExecutionMode.SOLO, 0.0, ExecutionMode.PARALLEL, 0.3, ExecutionMode.BREEZING, 1.0)
        );

        ModeRecommendation largeRecommendation = generator.generate(largeTaskGroup, largeScores);

        assertNotNull(largeRecommendation);
        assertEquals(ExecutionMode.BREEZING, largeRecommendation.recommendedMode());
        assertTrue(largeRecommendation.confidence() >= 0.9, "超大任务组应该有极高的置信度 >= 0.9");
        assertTrue(largeRecommendation.reason().toLowerCase().contains("大") ||
                   largeRecommendation.reason().toLowerCase().contains("规模") ||
                   largeRecommendation.reason().toLowerCase().contains("20"),
            "超大任务组推荐理由应该提到规模大");
    }

    @Test
    @DisplayName("推荐结果应该包含所有必要字段")
    void recommendationShouldContainAllRequiredFields() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            2, ComplexityLevel.MODERATE, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL
        );

        ModeScores scores = new ModeScores(
            0.5, 0.8, 0.4,
            Map.of(ExecutionMode.SOLO, 0.5, ExecutionMode.PARALLEL, 0.8, ExecutionMode.BREEZING, 0.4)
        );
        ModeRecommendation recommendation = generator.generate(characteristics, scores);

        // 验证所有必要字段都存在且合理
        assertNotNull(recommendation.recommendedMode(), "推荐模式不能为null");
        assertTrue(recommendation.confidence() >= 0.0 && recommendation.confidence() <= 1.0,
            "置信度必须在 [0, 1] 范围内");
        assertFalse(recommendation.reason().isEmpty(), "推荐理由不能为空");
        assertNotNull(recommendation.alternativeModes(), "备选方案列表不能为null");
    }

    @Test
    @DisplayName("应该能够正确处理各种复杂度等级的理由生成")
    void shouldHandleDifferentComplexityLevelsInReasonGeneration() {
        // 测试所有复杂度等级
        ComplexityLevel[] levels = {
            ComplexityLevel.SIMPLE,
            ComplexityLevel.MODERATE,
            ComplexityLevel.COMPLEX,
            ComplexityLevel.VERY_COMPLEX
        };

        for (ComplexityLevel level : levels) {
            TaskCharacteristics characteristics = new TaskCharacteristics(
                2, level, DependencyType.INDEPENDENT, ReviewRequirement.OPTIONAL
            );

            ModeScores scores = switch (level) {
                case SIMPLE -> new ModeScores(0.9, 0.3, 0.2,
                    Map.of(ExecutionMode.SOLO, 0.9, ExecutionMode.PARALLEL, 0.3, ExecutionMode.BREEZING, 0.2));
                case MODERATE -> new ModeScores(0.4, 0.8, 0.3,
                    Map.of(ExecutionMode.SOLO, 0.4, ExecutionMode.PARALLEL, 0.8, ExecutionMode.BREEZING, 0.3));
                case COMPLEX -> new ModeScores(0.2, 0.5, 0.8,
                    Map.of(ExecutionMode.SOLO, 0.2, ExecutionMode.PARALLEL, 0.5, ExecutionMode.BREEZING, 0.8));
                case VERY_COMPLEX -> new ModeScores(0.1, 0.3, 0.9,
                    Map.of(ExecutionMode.SOLO, 0.1, ExecutionMode.PARALLEL, 0.3, ExecutionMode.BREEZING, 0.9));
            };

            ModeRecommendation recommendation = generator.generate(characteristics, scores);

            assertNotNull(recommendation.reason(), "复杂度 " + level + " 的推荐理由不能为空");
            assertTrue(recommendation.reason().length() >= 20,
                "复杂度 " + level + " 的推荐理由应该有足够细节");
        }
    }
}