package com.chachamaru.harness.mode;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能执行模式评分器
 * 基于任务特征计算三种执行模式的匹配度评分
 */
public class ModeScorer {

    private final ScoringWeights weights;

    /**
     * 使用默认权重构造
     */
    public ModeScorer() {
        this(ScoringWeights.DEFAULT);
    }

    /**
     * 使用自定义权重构造
     * @param weights 评分权重配置
     */
    public ModeScorer(ScoringWeights weights) {
        this.weights = weights != null ? weights : ScoringWeights.DEFAULT;
    }

    /**
     * 为任务特征计算各执行模式的评分
     * @param characteristics 任务特征
     * @return 模式评分结果
     */
    public ModeScores scoreModes(TaskCharacteristics characteristics) {
        if (characteristics == null) {
            throw new IllegalArgumentException("任务特征不能为null");
        }

        // 计算各特征的原始评分
        double taskCountFactor = calculateTaskCountFactor(characteristics);
        double complexityFactor = calculateComplexityFactor(characteristics);
        double dependencyFactor = calculateDependencyFactor(characteristics);
        double reviewRequirementFactor = calculateReviewRequirementFactor(characteristics);

        // 为每种执行模式计算最终评分
        double soloScore = calculateSoloScore(characteristics, taskCountFactor, complexityFactor,
                                               dependencyFactor, reviewRequirementFactor);
        double parallelScore = calculateParallelScore(characteristics, taskCountFactor, complexityFactor,
                                                      dependencyFactor, reviewRequirementFactor);
        double breezingScore = calculateBreezingScore(characteristics, taskCountFactor, complexityFactor,
                                                      dependencyFactor, reviewRequirementFactor);

        // 创建评分分解信息
        Map<ExecutionMode, Double> breakdown = new HashMap<>();
        breakdown.put(ExecutionMode.SOLO, soloScore);
        breakdown.put(ExecutionMode.PARALLEL, parallelScore);
        breakdown.put(ExecutionMode.BREEZING, breezingScore);

        return new ModeScores(soloScore, parallelScore, breezingScore, breakdown);
    }

    /**
     * 获取推荐的最佳执行模式
     * @param characteristics 任务特征
     * @return 推荐的执行模式
     */
    public ExecutionMode getRecommendedMode(TaskCharacteristics characteristics) {
        ModeScores scores = scoreModes(characteristics);
        return scores.getRecommendedMode();
    }

    /**
     * 获取当前权重配置
     * @return 评分权重配置
     */
    public ScoringWeights getWeights() {
        return weights;
    }

    // ==================== 私有方法：特征评分计算 ====================

    /**
     * 计算任务数量因子 [0, 1]
     * 任务数量越多，越倾向并行和团队模式
     */
    private double calculateTaskCountFactor(TaskCharacteristics characteristics) {
        int taskCount = characteristics.taskCount();

        if (taskCount == 0) {
            return 0.0;  // 无任务
        } else if (taskCount == 1) {
            return 0.2;  // 单任务 - 倾向SOLO
        } else if (taskCount <= 3) {
            return 0.6;  // 小任务组 - 适合PARALLEL
        } else if (taskCount <= 6) {
            return 0.8;  // 中等任务组 - 适合BREEZING
        } else {
            return 1.0;  // 大任务组 - 强烈推荐BREEZING
        }
    }

    /**
     * 计算复杂度因子 [0, 1]
     * 复杂度越高，越倾向团队模式
     */
    private double calculateComplexityFactor(TaskCharacteristics characteristics) {
        return switch (characteristics.complexity()) {
            case SIMPLE -> 0.1;       // 简单 - 倾向SOLO
            case MODERATE -> 0.5;     // 中等 - 适合PARALLEL
            case COMPLEX -> 0.8;      // 复杂 - 适合BREEZING
            case VERY_COMPLEX -> 1.0; // 非常复杂 - 强烈推荐BREEZING
        };
    }

    /**
     * 计算依赖关系因子 [0, 1]
     * 依赖关系越复杂，越倾向团队模式
     */
    private double calculateDependencyFactor(TaskCharacteristics characteristics) {
        return switch (characteristics.dependencies()) {
            case INDEPENDENT -> 0.2;  // 独立 - 适合PARALLEL
            case SEQUENTIAL -> 0.6;   // 顺序依赖 - 需要协调
            case MIXED -> 1.0;        // 混合依赖 - 需要BREEZING协调
        };
    }

    /**
     * 计算审查需求因子 [0, 1]
     * 审查需求越高，越倾向BREEZING（有独立Reviewer）
     */
    private double calculateReviewRequirementFactor(TaskCharacteristics characteristics) {
        return switch (characteristics.reviewNeed()) {
            case NONE -> 0.0;         // 无需审查 - SOLO/PARALLEL都适合
            case OPTIONAL -> 0.3;     // 可选审查 - 轻微倾向BREEZING
            case REQUIRED -> 0.8;      // 必须审查 - 强烈推荐BREEZING（独立Reviewer）
        };
    }

    // ==================== 私有方法：执行模式评分计算 ====================

    /**
     * 计算 SOLO 模式评分
     * SOLO 最适合：单任务、低复杂度、独立任务、无需审查
     */
    private double calculateSoloScore(
        TaskCharacteristics characteristics,
        double taskCountFactor,
        double complexityFactor,
        double dependencyFactor,
        double reviewRequirementFactor
    ) {
        // 特殊处理：无任务时给予最低评分
        if (characteristics.taskCount() == 0) {
            return 0.05;  // 接近0但不完全是0，避免极端值
        }

        double score = 0.0;

        // 任务数量评分（单任务最佳）
        if (characteristics.taskCount() == 1) {
            score += weights.taskCountWeight() * 1.0;  // 单任务完美匹配SOLO
        } else {
            // 多任务时SOLO评分较低
            int penalty = Math.min(characteristics.taskCount() - 1, 8);  // 增加惩罚上限
            score += weights.taskCountWeight() * Math.max(0.0, 1.0 - penalty * 0.12);
        }

        // 复杂度评分（简单任务最佳）
        score += weights.complexityWeight() * (1.0 - complexityFactor);

        // 依赖关系评分（独立任务最佳）
        score += weights.dependencyWeight() * (1.0 - dependencyFactor * 0.7);

        // 审查需求评分（无需审查最佳）
        score += weights.reviewRequirementWeight() * (1.0 - reviewRequirementFactor);

        return clampScore(score);
    }

    /**
     * 计算 PARALLEL 模式评分
     * PARALLEL 最适合：2-4个任务、中等复杂度、独立任务、可选审查
     */
    private double calculateParallelScore(
        TaskCharacteristics characteristics,
        double taskCountFactor,
        double complexityFactor,
        double dependencyFactor,
        double reviewRequirementFactor
    ) {
        // 特殊处理：无任务时给予最低评分
        if (characteristics.taskCount() == 0) {
            return 0.05;  // 接近0但不完全是0，避免极端值
        }

        double score = 0.0;

        // 任务数量评分（2-4个任务最佳）
        int taskCount = characteristics.taskCount();
        if (taskCount >= 2 && taskCount <= 4) {
            score += weights.taskCountWeight() * 1.0;  // 完美匹配PARALLEL
        } else if (taskCount == 1) {
            score += weights.taskCountWeight() * 0.6;  // 单任务也可以PARALLEL
        } else if (taskCount >= 5 && taskCount <= 8) {
            score += weights.taskCountWeight() * 0.7;  // 较大任务组勉强可以
        } else {
            score += weights.taskCountWeight() * 0.3;  // 太大或太小都不太适合
        }

        // 复杂度评分（中等复杂度最佳）
        if (characteristics.complexity() == ComplexityLevel.MODERATE) {
            score += weights.complexityWeight() * 1.0;  // 中等复杂度完美匹配
        } else if (characteristics.complexity() == ComplexityLevel.SIMPLE) {
            score += weights.complexityWeight() * 0.7;  // 简单任务也适合
        } else {
            score += weights.complexityWeight() * 0.4;  // 高复杂度不太适合PARALLEL
        }

        // 依赖关系评分（独立任务最佳）
        if (characteristics.dependencies() == DependencyType.INDEPENDENT) {
            score += weights.dependencyWeight() * 1.0;  // 独立任务完美匹配PARALLEL
        } else if (characteristics.dependencies() == DependencyType.SEQUENTIAL) {
            score += weights.dependencyWeight() * 0.5;  // 顺序依赖降低PARALLEL效果
        } else {
            score += weights.dependencyWeight() * 0.3;  // 混合依赖不适合PARALLEL
        }

        // 审查需求评分（可选审查不影响PARALLEL）
        score += weights.reviewRequirementWeight() * (1.0 - reviewRequirementFactor * 0.5);

        return clampScore(score);
    }

    /**
     * 计算 BREEZING 模式评分
     * BREEZING 最适合：4+个任务、高复杂度、混合依赖、必须审查
     */
    private double calculateBreezingScore(
        TaskCharacteristics characteristics,
        double taskCountFactor,
        double complexityFactor,
        double dependencyFactor,
        double reviewRequirementFactor
    ) {
        // 特殊处理：无任务时给予最低评分
        if (characteristics.taskCount() == 0) {
            return 0.05;  // 接近0但不完全是0，避免极端值
        }

        double score = 0.0;

        // 任务数量评分（4+个任务最佳）
        int taskCount = characteristics.taskCount();
        if (taskCount >= 6) {
            score += weights.taskCountWeight() * 1.0;  // 大任务组完美匹配BREEZING
        } else if (taskCount >= 4) {
            score += weights.taskCountWeight() * 0.8;  // 中等任务组很适合
        } else if (taskCount >= 2) {
            score += weights.taskCountWeight() * 0.5;  // 小任务组可以考虑
        } else {
            score += weights.taskCountWeight() * 0.2;  // 单任务不太适合BREEZING
        }

        // 复杂度评分（高复杂度最佳）
        if (characteristics.complexity() == ComplexityLevel.VERY_COMPLEX) {
            score += weights.complexityWeight() * 1.0;  // 超高复杂度完美匹配
        } else if (characteristics.complexity() == ComplexityLevel.COMPLEX) {
            score += weights.complexityWeight() * 0.9;  // 高复杂度很适合
        } else if (characteristics.complexity() == ComplexityLevel.MODERATE) {
            score += weights.complexityWeight() * 0.5;  // 中等复杂度可以接受
        } else {
            score += weights.complexityWeight() * 0.2;  // 简单任务不太需要BREEZING
        }

        // 依赖关系评分（混合依赖最佳）
        if (characteristics.dependencies() == DependencyType.MIXED) {
            score += weights.dependencyWeight() * 1.0;  // 混合依赖完美匹配BREEZING
        } else if (characteristics.dependencies() == DependencyType.SEQUENTIAL) {
            score += weights.dependencyWeight() * 0.7;  // 顺序依赖需要BREEZING协调
        } else {
            score += weights.dependencyWeight() * 0.4;  // 独立任务也可以BREEZING
        }

        // 审查需求评分（必须审查最佳 - 有独立Reviewer）
        if (characteristics.reviewNeed() == ReviewRequirement.REQUIRED) {
            score += weights.reviewRequirementWeight() * 1.0;  // 必须审查完美匹配
        } else if (characteristics.reviewNeed() == ReviewRequirement.OPTIONAL) {
            score += weights.reviewRequirementWeight() * 0.6;  // 可选审查也可以
        } else {
            score += weights.reviewRequirementWeight() * 0.3;  // 无需审查不太需要BREEZING
        }

        return clampScore(score);
    }

    /**
     * 将评分限制在 [0, 1] 范围内
     * @param score 原始评分
     * @return 限制后的评分
     */
    private double clampScore(double score) {
        return Math.max(0.0, Math.min(1.0, score));
    }
}