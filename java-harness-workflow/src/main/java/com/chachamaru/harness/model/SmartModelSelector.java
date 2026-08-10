package com.chachamaru.harness.model;

/**
 * 智能模型选择器
 * 根据任务复杂度自动选择最优模型
 */
public class SmartModelSelector {

    private final ModelSelectionConfig config;

    /**
     * 创建选择器
     */
    public SmartModelSelector(ModelSelectionConfig config) {
        this.config = config != null ? config : new ModelSelectionConfig();
    }

    /**
     * 根据复杂度分数选择模型
     */
    public String selectModel(int complexityScore) throws ModelUnavailableException {
        if (!config.isEnabled()) {
            return "glm-4.7"; // 默认模型
        }

        ModelTier tier = ModelTier.fromScore(complexityScore);
        TierConfig tierConfig = config.getTierConfig(tier);

        if (tierConfig == null) {
            return "glm-4.7"; // 兜底模型
        }

        return selectFromFallbackChain(tierConfig);
    }

    /**
     * 从降级链中选择模型
     */
    private String selectFromFallbackChain(TierConfig config) throws ModelUnavailableException {
        String[] fallbackModels = config.getFallbackModels();

        for (String model : fallbackModels) {
            if (isModelAvailable(model)) {
                return resolveModelReference(model);
            }
        }

        throw new ModelUnavailableException("No models available in fallback chain");
    }

    /**
     * 检查模型是否可用
     */
    private boolean isModelAvailable(String modelRef) {
        // 简化实现：假设所有模型都可用
        // 实际应该检查环境变量和模型格式
        return modelRef != null && !modelRef.trim().isEmpty();
    }

    /**
     * 解析模型引用
     */
    private String resolveModelReference(String modelRef) {
        if (modelRef.startsWith("env:")) {
            String envVar = modelRef.substring(4);
            String envValue = System.getenv(envVar);
            return envValue != null && !envValue.isEmpty() ? envValue : "glm-4.7";
        }
        return modelRef;
    }

    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /**
     * 获取策略名称
     */
    public String getStrategy() {
        return config.getStrategy();
    }
}