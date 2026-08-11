package com.chachamaru.harness.mode;

/**
 * 任务特征数据类
 * 描述用于执行模式推荐的任务特征信息
 *
 * @param taskCount 任务数量 (≥1)
 * @param complexity 任务复杂度等级
 * @param dependencies 任务依赖关系类型
 * @param reviewNeed 代码审查需求
 */
public record TaskCharacteristics(
    int taskCount,
    ComplexityLevel complexity,
    DependencyType dependencies,
    ReviewRequirement reviewNeed
) {
    /**
     * 验证任务特征的有效性
     * @return true 如果任务特征有效，否则 false
     */
    public boolean isValid() {
        return taskCount > 0 &&
               complexity != null &&
               dependencies != null &&
               reviewNeed != null;
    }

    /**
     * 判断是否为单任务
     * @return true 如果任务数量为1
     */
    public boolean isSingleTask() {
        return taskCount == 1;
    }

    /**
     * 判断是否为小任务组 (2-3 个任务)
     * @return true 如果任务数量为2-3
     */
    public boolean isSmallTaskGroup() {
        return taskCount >= 2 && taskCount <= 3;
    }

    /**
     * 判断是否为中等任务组 (4-6 个任务)
     * @return true 如果任务数量为4-6
     */
    public boolean isMediumTaskGroup() {
        return taskCount >= 4 && taskCount <= 6;
    }

    /**
     * 判断是否为大任务组 (7+ 个任务)
     * @return true 如果任务数量≥7
     */
    public boolean isLargeTaskGroup() {
        return taskCount >= 7;
    }

    /**
     * 判断是否为高复杂度任务
     * @return true 如果复杂度为 COMPLEX 或 VERY_COMPLEX
     */
    public boolean isHighComplexity() {
        return complexity == ComplexityLevel.COMPLEX ||
               complexity == ComplexityLevel.VERY_COMPLEX;
    }

    /**
     * 判断是否需要代码审查
     * @return true 如果审查需求为 REQUIRED
     */
    public boolean requiresReview() {
        return reviewNeed == ReviewRequirement.REQUIRED;
    }

    /**
     * 判断任务是否独立（无依赖）
     * @return true 如果依赖类型为 INDEPENDENT
     */
    public boolean isIndependent() {
        return dependencies == DependencyType.INDEPENDENT;
    }
}