package com.chachamaru.harness.workflow.orchestration;

import java.util.Objects;

/**
 * Worker 启动配置
 * 包含 effort tier 和选择的模型信息
 */
public class WorkerSpawnConfig {

    private final String effortTier;
    private final String selectedModel;

    /**
     * 创建 Worker 启动配置
     *
     * @param effortTier Effort 等级（low/medium/high/xhigh）
     * @param selectedModel 选择的模型名称
     */
    public WorkerSpawnConfig(String effortTier, String selectedModel) {
        if (effortTier == null || effortTier.trim().isEmpty()) {
            throw new IllegalArgumentException("Effort tier cannot be null or empty");
        }
        if (selectedModel == null || selectedModel.trim().isEmpty()) {
            throw new IllegalArgumentException("Selected model cannot be null or empty");
        }

        this.effortTier = effortTier;
        this.selectedModel = selectedModel;
    }

    /**
     * 获取 Effort 等级
     * @return Effort 等级
     */
    public String getEffortTier() {
        return effortTier;
    }

    /**
     * 获取选择的模型
     * @return 模型名称
     */
    public String getSelectedModel() {
        return selectedModel;
    }

    /**
     * 验证配置的有效性
     *
     * @throws IllegalStateException 如果配置无效
     */
    public void validate() {
        if (effortTier == null || effortTier.isEmpty()) {
            throw new IllegalStateException("Effort tier is not set");
        }
        if (selectedModel == null || selectedModel.isEmpty()) {
            throw new IllegalStateException("Selected model is not set");
        }

        // 验证 effort tier 是有效值
        if (!isValidEffortTier(effortTier)) {
            throw new IllegalStateException("Invalid effort tier: " + effortTier);
        }
    }

    /**
     * 检查 effort tier 是否有效
     *
     * @param tier Effort 等级
     * @return 如果有效返回 true
     */
    private boolean isValidEffortTier(String tier) {
        return "low".equals(tier) ||
               "medium".equals(tier) ||
               "high".equals(tier) ||
               "xhigh".equals(tier);
    }

    /**
     * 获取配置描述（用于日志）
     * @return 配置描述
     */
    public String getDescription() {
        return String.format("WorkerSpawnConfig{effortTier='%s', model='%s'}",
                effortTier, selectedModel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkerSpawnConfig that = (WorkerSpawnConfig) o;
        return Objects.equals(effortTier, that.effortTier) &&
               Objects.equals(selectedModel, that.selectedModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(effortTier, selectedModel);
    }

    @Override
    public String toString() {
        return getDescription();
    }
}