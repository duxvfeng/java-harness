package com.chachamaru.harness.mode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 推荐生成器
 * 基于任务特征和评分结果生成智能推荐
 */
public class RecommendationGenerator {

    /**
     * 生成推荐结果
     * @param characteristics 任务特征
     * @param scores 模式评分结果
     * @return 推荐结果
     */
    public ModeRecommendation generate(TaskCharacteristics characteristics, ModeScores scores) {
        if (characteristics == null) {
            throw new IllegalArgumentException("任务特征不能为null");
        }
        if (scores == null) {
            throw new IllegalArgumentException("评分结果不能为null");
        }

        // 确定推荐模式
        ExecutionMode recommendedMode = determineRecommendedMode(scores);

        // 计算置信度
        double confidence = calculateConfidence(scores, recommendedMode);

        // 生成推荐理由
        String reason = generateRecommendationReason(characteristics, scores, recommendedMode, confidence);

        // 选择备选方案
        List<ExecutionMode> alternatives = selectAlternativeModes(scores, recommendedMode);

        return new ModeRecommendation(recommendedMode, confidence, reason, alternatives);
    }

    /**
     * 确定推荐模式
     */
    private ExecutionMode determineRecommendedMode(ModeScores scores) {
        return scores.getRecommendedMode();
    }

    /**
     * 计算置信度
     * 基于评分差异和推荐模式的明显程度
     */
    private double calculateConfidence(ModeScores scores, ExecutionMode recommendedMode) {
        double recommendedScore = scores.getScore(recommendedMode);

        // 获取第二高的评分
        double secondHighestScore = getSecondHighestScore(scores, recommendedMode);

        // 计算评分差异
        double scoreDifference = recommendedScore - secondHighestScore;

        // 基于评分差异计算置信度
        double confidence;
        if (scoreDifference >= 0.4) {
            // 差异很大，高置信度
            confidence = 0.85 + (scoreDifference - 0.4) * 0.15; // [0.85, 1.0]
        } else if (scoreDifference >= 0.2) {
            // 差异明显，中高置信度
            confidence = 0.70 + (scoreDifference - 0.2) * 0.75; // [0.70, 0.85]
        } else if (scoreDifference >= 0.05) {
            // 差异较小，中等置信度
            confidence = 0.55 + (scoreDifference - 0.05) * 3.0; // [0.55, 0.70] (当 scoreDifference=0.1 时为 0.70)
        } else {
            // 差异很小，低置信度
            confidence = 0.40 + scoreDifference * 3.0;           // [0.40, 0.55]
        }

        // 考虑推荐模式的绝对评分
        if (recommendedScore >= 0.8) {
            // 推荐模式本身评分很高，提高置信度
            confidence = Math.min(1.0, confidence + 0.05);
        } else if (recommendedScore <= 0.4) {
            // 推荐模式本身评分不高，降低置信度
            confidence = Math.max(0.3, confidence - 0.05);
        }

        return Math.max(0.0, Math.min(1.0, confidence));
    }

    /**
     * 获取第二高的评分
     */
    private double getSecondHighestScore(ModeScores scores, ExecutionMode recommendedMode) {
        if (recommendedMode == ExecutionMode.SOLO) {
            return Math.max(scores.parallelScore(), scores.breezingScore());
        } else if (recommendedMode == ExecutionMode.PARALLEL) {
            return Math.max(scores.soloScore(), scores.breezingScore());
        } else {
            return Math.max(scores.soloScore(), scores.parallelScore());
        }
    }

    /**
     * 生成推荐理由
     * 基于任务特征和评分结果生成清晰的理由说明
     */
    private String generateRecommendationReason(
        TaskCharacteristics characteristics,
        ModeScores scores,
        ExecutionMode recommendedMode,
        double confidence
    ) {
        StringBuilder reason = new StringBuilder();

        // 特殊情况：无任务
        if (characteristics.taskCount() == 0) {
            return "当前没有需要处理的任务，建议等待任务分配后再选择执行模式。";
        }

        // 根据推荐模式生成理由
        switch (recommendedMode) {
            case SOLO:
                reason.append(generateSoloReason(characteristics, scores, confidence));
                break;
            case PARALLEL:
                reason.append(generateParallelReason(characteristics, scores, confidence));
                break;
            case BREEZING:
                reason.append(generateBreezingReason(characteristics, scores, confidence));
                break;
        }

        return reason.toString();
    }

    /**
     * 生成 SOLO 模式推荐理由
     */
    private String generateSoloReason(
        TaskCharacteristics characteristics,
        ModeScores scores,
        double confidence
    ) {
        StringBuilder reason = new StringBuilder();

        reason.append("推荐使用 SOLO 模式执行");

        if (characteristics.taskCount() == 1) {
            reason.append("，因为当前只有 1 个任务");
        } else if (characteristics.taskCount() <= 2) {
            reason.append("，因为任务数量较少（").append(characteristics.taskCount()).append("个）");
        }

        // 复杂度说明
        if (characteristics.complexity() == ComplexityLevel.SIMPLE) {
            reason.append("，且任务复杂度较低");
        }

        // 依赖关系说明
        if (characteristics.dependencies() == DependencyType.INDEPENDENT) {
            reason.append("，任务之间无依赖关系");
        }

        // 审查需求说明
        if (characteristics.reviewNeed() == ReviewRequirement.NONE) {
            reason.append("，无需代码审查");
        }

        // 性能说明
        reason.append("。SOLO 模式可以快速完成单个任务，减少协调开销");

        return reason.toString();
    }

    /**
     * 生成 PARALLEL 模式推荐理由
     */
    private String generateParallelReason(
        TaskCharacteristics characteristics,
        ModeScores scores,
        double confidence
    ) {
        StringBuilder reason = new StringBuilder();

        reason.append("推荐使用 PARALLEL 模式执行");

        // 任务数量说明
        if (characteristics.taskCount() >= 2 && characteristics.taskCount() <= 4) {
            reason.append("，因为有 ").append(characteristics.taskCount()).append(" 个任务可以并行处理");
        } else if (characteristics.taskCount() >= 5) {
            reason.append("，任务数量适中，可以通过并行提高效率");
        }

        // 复杂度说明
        if (characteristics.complexity() == ComplexityLevel.MODERATE) {
            reason.append("，任务复杂度适中");
        }

        // 依赖关系说明
        if (characteristics.dependencies() == DependencyType.INDEPENDENT) {
            reason.append("，且任务之间相互独立");
        } else if (characteristics.dependencies() == DependencyType.SEQUENTIAL) {
            reason.append("，虽有顺序依赖但仍可部分并行");
        }

        // 性能说明
        reason.append("。PARALLEL 模式可以同时处理多个独立任务，显著提升执行效率");

        // 审查说明
        if (characteristics.reviewNeed() == ReviewRequirement.OPTIONAL) {
            reason.append("，审查需求可选");
        }

        return reason.toString();
    }

    /**
     * 生成 BREEZING 模式推荐理由
     */
    private String generateBreezingReason(
        TaskCharacteristics characteristics,
        ModeScores scores,
        double confidence
    ) {
        StringBuilder reason = new StringBuilder();

        reason.append("推荐使用 BREEZING 模式执行");

        // 任务数量说明
        if (characteristics.taskCount() >= 6) {
            reason.append("，因为有 ").append(characteristics.taskCount()).append(" 个任务需要团队协作");
        } else if (characteristics.taskCount() >= 4) {
            reason.append("，任务数量较多，需要协调管理");
        }

        // 复杂度说明
        if (characteristics.complexity() == ComplexityLevel.VERY_COMPLEX) {
            reason.append("，且任务复杂度极高");
        } else if (characteristics.complexity() == ComplexityLevel.COMPLEX) {
            reason.append("，任务较为复杂");
        }

        // 依赖关系说明
        if (characteristics.dependencies() == DependencyType.MIXED) {
            reason.append("，任务间存在复杂的依赖关系");
        } else if (characteristics.dependencies() == DependencyType.SEQUENTIAL) {
            reason.append("，任务间有顺序依赖需要协调");
        }

        // 审查需求说明
        if (characteristics.reviewNeed() == ReviewRequirement.REQUIRED) {
            reason.append("，需要严格的代码审查");
        }

        // 团队协作说明
        reason.append("。BREEZING 模式通过 Lead/Worker/Reviewer 角色分离，可以有效协调复杂任务，保证代码质量");

        return reason.toString();
    }

    /**
     * 选择备选方案
     * 选择评分较高的其他模式作为备选
     */
    private List<ExecutionMode> selectAlternativeModes(ModeScores scores, ExecutionMode recommendedMode) {
        List<ExecutionMode> alternatives = new ArrayList<>();

        // 收集所有非推荐模式及其评分
        List<ModeScore> otherModes = new ArrayList<>();
        if (recommendedMode != ExecutionMode.SOLO) {
            otherModes.add(new ModeScore(ExecutionMode.SOLO, scores.soloScore()));
        }
        if (recommendedMode != ExecutionMode.PARALLEL) {
            otherModes.add(new ModeScore(ExecutionMode.PARALLEL, scores.parallelScore()));
        }
        if (recommendedMode != ExecutionMode.BREEZING) {
            otherModes.add(new ModeScore(ExecutionMode.BREEZING, scores.breezingScore()));
        }

        // 如果没有其他模式（不太可能），返回空列表
        if (otherModes.isEmpty()) {
            return alternatives;
        }

        // 按评分排序
        otherModes.sort(Comparator.comparingDouble(ModeScore::score).reversed());

        // 选择评分最高的 1-2 个作为备选方案
        // 降低备选方案的门槛（至少要 > 0.2）以提供更多选择
        int maxAlternatives = Math.min(2, otherModes.size());
        for (int i = 0; i < maxAlternatives; i++) {
            ModeScore candidate = otherModes.get(i);
            if (candidate.score() > 0.2) {  // 降低门槛从 0.3 到 0.2
                alternatives.add(candidate.mode());
            }
        }

        // 确保至少返回一个备选方案（如果评分 > 0.1）
        if (alternatives.isEmpty() && !otherModes.isEmpty() && otherModes.get(0).score() > 0.1) {
            alternatives.add(otherModes.get(0).mode());
        }

        return alternatives;
    }

    /**
     * 模式评分记录（内部使用）
     */
    private record ModeScore(ExecutionMode mode, double score) {}
}