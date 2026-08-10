package com.chachamaru.harness.model;

/**
 * 智能模型选择器
 * 根据任务复杂度自动选择最优的 AI 大模型
 *
 * <p>核心功能：</p>
 * <ul>
 *   <li>根据复杂度分数确定模型等级</li>
 *   <li>执行完整的降级链逻辑</li>
 *   <li>解析环境变量引用</li>
 *   <li>检查模型可用性</li>
 *   <li>处理各种异常情况</li>
 * </ul>
 *
 * <p>选择流程：</p>
 * <ol>
 *   <li>根据复杂度分数确定模型等级（FAST/BALANCED/QUALITY/POWERFUL）</li>
 *   <li>获取该等级的降级链配置</li>
 *   <li>按优先级尝试每个候选模型：</li>
 *   <li>解析环境变量引用（如果需要）</li>
 *   <li>检查模型可用性</li>
 *   <li>返回第一个可用的模型</li>
 * </ol>
 *
 * <p>异常处理：</p>
 * <ul>
 *   <li>配置无效：抛出 IllegalArgumentException</li>
 *   <li>所有模型不可用：抛出 ModelUnavailableException</li>
 *   <li>环境变量缺失：跳过该候选，继续降级链</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * ModelSelectionConfigLoader loader = new ModelSelectionConfigLoader();
 * ModelSelectionConfig config = loader.loadOrDefault();
 * SmartModelSelector selector = new SmartModelSelector(config);
 *
 * // 根据复杂度分数选择模型
 * String model = selector.selectModel(5); // 返回 QUALITY 等级的模型
 * }</pre>
 */
public class SmartModelSelector {

    private final ModelSelectionConfig config;
    private final ModelReferenceResolver resolver;
    private final ModelAvailabilityChecker availabilityChecker;

    /**
     * 创建智能模型选择器
     *
     * @param config 模型选择配置
     * @throws IllegalArgumentException 如果配置无效
     */
    public SmartModelSelector(ModelSelectionConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ModelSelectionConfig cannot be null");
        }

        // 验证配置
        config.validate();

        this.config = config;
        this.resolver = new ModelReferenceResolver();
        this.availabilityChecker = new ModelAvailabilityChecker();
    }

    /**
     * 根据复杂度分数选择最优模型
     *
     * @param complexityScore 复杂度分数（0-999）
     * @return 选择的模型名称
     * @throws ModelUnavailableException 如果所有候选模型都不可用
     * @throws IllegalArgumentException 如果分数无效
     */
    public String selectModel(int complexityScore) {
        // 1. 确定模型等级
        ModelTier tier = determineTier(complexityScore);

        // 2. 获取该等级的配置
        TierConfig tierConfig = getTierConfig(tier);

        // 3. 执行降级链
        return executeFallbackChain(tierConfig, complexityScore);
    }

    /**
     * 根据复杂度分数确定模型等级
     *
     * @param score 复杂度分数
     * @return 模型等级
     */
    private ModelTier determineTier(int score) {
        return ModelTier.fromScore(score);
    }

    /**
     * 获取指定等级的配置
     *
     * @param tier 模型等级
     * @return 等级配置
     * @throws ModelUnavailableException 如果等级配置不存在
     */
    private TierConfig getTierConfig(ModelTier tier) {
        return config.getTierConfig(tier)
                .orElseThrow(() -> new ModelUnavailableException(tier,
                        "No configuration found for tier: " + tier));
    }

    /**
     * 执行降级链，找到第一个可用的模型
     *
     * @param tierConfig 等级配置
     * @param complexityScore 复杂度分数（用于日志）
     * @return 可用的模型名称
     * @throws ModelUnavailableException 如果所有候选模型都不可用
     */
    private String executeFallbackChain(TierConfig tierConfig, int complexityScore) {
        String[] fallbackChain = tierConfig.getFallbackChain();

        if (fallbackChain == null || fallbackChain.length == 0) {
            throw new ModelUnavailableException(tierConfig.getTier(),
                    "Empty fallback chain for tier: " + tierConfig.getTierName());
        }

        // 遍历降级链
        for (int i = 0; i < fallbackChain.length; i++) {
            String candidate = fallbackChain[i];

            try {
                String resolvedModel = resolveModelReference(candidate);

                // 检查模型可用性
                if (isModelAvailable(resolvedModel)) {
                    logModelSelection(tierConfig, resolvedModel, candidate, i, complexityScore);
                    return resolvedModel;
                }

                // 模型不可用，记录日志并继续下一个候选
                logModelUnavailable(candidate, i);

            } catch (ConfigException e) {
                // 环境变量解析失败，记录日志并继续下一个候选
                logEnvReferenceFailure(candidate, e.getMessage());
            } catch (Exception e) {
                // 其他异常，记录日志并继续下一个候选
                logUnexpectedError(candidate, e.getMessage());
            }
        }

        // 所有候选模型都不可用
        throw new ModelUnavailableException(tierConfig.getTier(),
                String.format("No models available for tier %s after trying %d candidates",
                        tierConfig.getTierName(), fallbackChain.length));
    }

    /**
     * 解析模型引用
     *
     * @param reference 模型引用（环境变量引用或直接模型名称）
     * @return 解析后的模型名称
     * @throws ConfigException 如果环境变量引用无效
     */
    private String resolveModelReference(String reference) {
        return resolver.resolve(reference);
    }

    /**
     * 检查模型是否可用
     *
     * @param model 模型名称
     * @return 如果模型可用返回 true
     */
    private boolean isModelAvailable(String model) {
        return availabilityChecker.isAvailable(model, config.getTimeout());
    }

    /**
     * 记录模型选择日志
     *
     * @param tierConfig 等级配置
     * @param selectedModel 选择的模型
     * @param originalCandidate 原始候选
     * @param index 候选索引
     * @param complexityScore 复杂度分数
     */
    private void logModelSelection(TierConfig tierConfig, String selectedModel,
                                  String originalCandidate, int index, int complexityScore) {
        // 在实际实现中，这里应该记录到日志系统
        // 当前仅作为占位符
        String message = String.format(
                "Selected model '%s' for tier %s (score: %d, candidate index: %d)",
                selectedModel, tierConfig.getTierName(), complexityScore, index
        );
        // logger.info(message);
    }

    /**
     * 记录模型不可用日志
     *
     * @param candidate 候选模型
     * @param index 索引
     */
    private void logModelUnavailable(String candidate, int index) {
        String message = String.format(
                "Model candidate '%s' (index %d) is not available",
                candidate, index
        );
        // logger.debug(message);
    }

    /**
     * 记录环境变量引用失败日志
     *
     * @param candidate 候选模型
     * @param errorMessage 错误消息
     */
    private void logEnvReferenceFailure(String candidate, String errorMessage) {
        String message = String.format(
                "Environment variable reference '%s' failed: %s",
                candidate, errorMessage
        );
        // logger.debug(message);
    }

    /**
     * 记录意外错误日志
     *
     * @param candidate 候选模型
     * @param errorMessage 错误消息
     */
    private void logUnexpectedError(String candidate, String errorMessage) {
        String message = String.format(
                "Unexpected error for candidate '%s': %s",
                candidate, errorMessage
        );
        // logger.warn(message);
    }

    /**
     * 获取配置
     * @return 模型选择配置
     */
    public ModelSelectionConfig getConfiguration() {
        return config;
    }

    /**
     * 获取模型引用解析器
     * @return 模型引用解析器
     */
    public ModelReferenceResolver getResolver() {
        return resolver;
    }

    /**
     * 获取模型可用性检查器
     * @return 模型可用性检查器
     */
    public ModelAvailabilityChecker getAvailabilityChecker() {
        return availabilityChecker;
    }

    /**
     * 检查选择器是否已启用
     * @return 如果启用返回 true
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /**
     * 获取选择策略
     * @return 选择策略名称
     */
    public String getStrategy() {
        return config.getStrategy();
    }

    /**
     * 快速检查是否可以为指定分数选择模型
     *
     * @param complexityScore 复杂度分数
     * @return 如果可以选择返回 true
     */
    public boolean canSelectModel(int complexityScore) {
        try {
            ModelTier tier = determineTier(complexityScore);
            return config.getTierConfig(tier).isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}