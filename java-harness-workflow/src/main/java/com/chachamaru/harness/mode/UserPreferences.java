package com.chachamaru.harness.mode;

import java.util.Map;
import java.util.Optional;

/**
 * 用户偏好统计
 * 记录和分析用户对执行模式的选择偏好
 */
public record UserPreferences(
    Map<ExecutionMode, Integer> modeSelectionCounts,  // 各模式选择次数
    int totalSelections                                // 总选择次数
) {
    /**
     * 创建用户偏好
     */
    public UserPreferences {
        // 确保不可变
        modeSelectionCounts = Map.copyOf(modeSelectionCounts != null ? modeSelectionCounts : Map.of());
    }

    /**
     * 获取指定模式的选择次数
     * @param mode 执行模式
     * @return 选择次数
     */
    public int getModeSelectionCount(ExecutionMode mode) {
        return modeSelectionCounts.getOrDefault(mode, 0);
    }

    /**
     * 获取最偏好的模式
     * @return 最偏好的模式（如果有）
     */
    public Optional<ExecutionMode> getMostPreferredMode() {
        return modeSelectionCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);
    }

    /**
     * 获取最不偏好的模式
     * @return 最不偏好的模式（如果有）
     */
    public Optional<ExecutionMode> getLeastPreferredMode() {
        return modeSelectionCounts.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);
    }

    /**
     * 获取模式使用百分比
     * @param mode 执行模式
     * @return 使用百分比（0-100）
     */
    public double getModePercentage(ExecutionMode mode) {
        if (totalSelections == 0) {
            return 0.0;
        }
        return (double) getModeSelectionCount(mode) / totalSelections * 100;
    }

    /**
     * 判断是否有足够的数据
     * @param minimumCount 最小数据要求
     * @return 是否有足够数据
     */
    public boolean hasSufficientData(int minimumCount) {
        return totalSelections >= minimumCount;
    }

    /**
     * 获取偏好多样性指数（0-1，1表示多样性最高）
     * @return 多样性指数
     */
    public double getDiversityIndex() {
        if (totalSelections == 0) {
            return 0.0;
        }

        // 使用熵来计算多样性
        double entropy = 0.0;
        for (int count : modeSelectionCounts.values()) {
            if (count > 0) {
                double probability = (double) count / totalSelections;
                entropy -= probability * Math.log(probability);
            }
        }

        // 归一化到 [0, 1]
        double maxEntropy = Math.log(Math.min(modeSelectionCounts.size(), 3));
        return maxEntropy > 0 ? entropy / maxEntropy : 0.0;
    }

    /**
     * 判断用户是否有明显的偏好倾向
     * @return 是否有明显偏好
     */
    public boolean hasStrongPreference() {
        if (totalSelections < 5) {
            return false;
        }

        Optional<ExecutionMode> mostPreferred = getMostPreferredMode();
        if (mostPreferred.isEmpty()) {
            return false;
        }

        double percentage = getModePercentage(mostPreferred.get());
        return percentage > 60.0; // 超过60%认为是强偏好
    }
}