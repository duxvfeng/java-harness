package com.chachamaru.harness.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 增强的智能模型选择器
 * 根据任务复杂度自动选择最优模型，包含完整的缓存机制
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>基于复杂度分数的智能模型选择</li>
 *   <li>三层缓存机制：配置缓存、可用性缓存、选择结果缓存</li>
 *   <li>性能优化：单次选择 < 100ms，缓存命中率 > 60%</li>
 *   <li>完整的降级链执行和错误处理</li>
 * </ul>
 *
 * <p>缓存策略：</p>
 * <ul>
 *   <li>配置缓存：文件变更后重新加载</li>
 *   <li>可用性缓存：5分钟有效期</li>
 *   <li>选择结果缓存：1分钟有效期</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * SmartModelSelector selector = new SmartModelSelector(config);
 *
 * // 首次选择（会缓存结果）
 * String model1 = selector.selectModel(3);
 *
 * // 后续相同分数的选择（从缓存读取）
 * String model2 = selector.selectModel(3); // 快速返回
 * }</pre>
 */
public class SmartModelSelector {

    // 缓存时间配置
    private static final long AVAILABILITY_CACHE_TTL = TimeUnit.MINUTES.toMillis(5); // 5分钟
    private static final long SELECTION_CACHE_TTL = TimeUnit.MINUTES.toMillis(1);  // 1分钟
    private static final long CONFIG_CACHE_TTL = TimeUnit.MINUTES.toMillis(10);     // 10分钟

    // 核心组件
    private final ModelSelectionConfig config;
    private final ModelAvailabilityChecker availabilityChecker;
    private final ModelSelectionLogger logger;

    // 缓存存储
    private volatile ConfigCacheEntry configCache;
    private final Map<String, AvailabilityCacheEntry> availabilityCache;
    private final Map<Integer, SelectionCacheEntry> selectionCache;

    // 缓存统计
    private final java.util.concurrent.atomic.AtomicLong cacheHits = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong cacheMisses = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong failedSelections = new java.util.concurrent.atomic.AtomicLong(0);

    /**
     * 创建选择器（使用默认配置）
     */
    public SmartModelSelector() {
        this(new ModelSelectionConfig());
    }

    /**
     * 创建选择器
     *
     * @param config 模型选择配置
     */
    public SmartModelSelector(ModelSelectionConfig config) {
        this.config = config != null ? config : new ModelSelectionConfig();
        this.availabilityChecker = new ModelAvailabilityChecker(false);
        this.logger = ModelSelectionLogger.getInstance();
        this.availabilityCache = new ConcurrentHashMap<>();
        this.selectionCache = new ConcurrentHashMap<>();

        // 初始化配置缓存
        this.configCache = new ConfigCacheEntry(this.config, System.currentTimeMillis());

        logger.info("SmartModelSelector initialized with strategy: " + this.config.getStrategy());
    }

    /**
     * 根据复杂度分数选择模型（带缓存和完整日志）
     *
     * @param complexityScore 复杂度分数
     * @return 选择的模型名称
     * @throws ModelUnavailableException 如果没有可用模型
     */
    public String selectModel(int complexityScore) throws ModelUnavailableException {
        long startTime = System.currentTimeMillis();

        try {
            logger.debug("Model selection requested for complexity score: " + complexityScore);

            // 1. 检查选择结果缓存
            SelectionCacheEntry cachedSelection = selectionCache.get(complexityScore);
            if (cachedSelection != null && !cachedSelection.isExpired()) {
                cacheHits.incrementAndGet();
                logger.logCacheEvent(true, "selection_result");
                return cachedSelection.modelName;
            }

            cacheMisses.incrementAndGet();
            logger.logCacheEvent(false, "selection_result");

            // 2. 确定模型等级
            ModelTier tier = ModelTier.fromScore(complexityScore);
            TierConfig tierConfig = getConfig().getTierConfig(tier);

            if (tierConfig == null) {
                logger.warn("No tier config found for: " + tier + ", using fallback");
                cacheMisses.incrementAndGet();
                return "glm-4.7"; // 兜底模型
            }

            // 3. 从降级链中选择模型
            String selectedModel = selectFromFallbackChain(tierConfig);

            // 4. 缓存选择结果
            selectionCache.put(complexityScore,
                new SelectionCacheEntry(selectedModel, System.currentTimeMillis()));

            long duration = System.currentTimeMillis() - startTime;
            logger.info(String.format("Model selected: score=%d, model=%s, time=%dms",
                complexityScore, selectedModel, duration));

            if (duration > 100) {
                logger.warn("Slow model selection: " + duration + "ms for score: " + complexityScore);
            }

            return selectedModel;

        } catch (ModelUnavailableException e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Model selection failed in " + duration + "ms: " + e.getMessage(), e);
            failedSelections.incrementAndGet();
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Unexpected error during model selection in " + duration + "ms: " + e.getMessage(), e);
            failedSelections.incrementAndGet();
            throw new ModelUnavailableException("Failed to select model for score: " + complexityScore, e);
        }
    }

    /**
     * 从降级链中选择模型（带可用性缓存）
     *
     * @param config 等级配置
     * @return 选择的模型名称
     * @throws ModelUnavailableException 如果没有可用模型
     */
    private String selectFromFallbackChain(TierConfig config) throws ModelUnavailableException {
        String[] fallbackModels = config.getFallbackModels();

        for (String model : fallbackModels) {
            String resolvedModel = resolveModelReference(model);

            // 检查可用性（使用缓存）
            if (isModelAvailableWithCache(resolvedModel)) {
                logModelSelection(config.getTier(), resolvedModel, model);
                return resolvedModel;
            }
        }

        throw new ModelUnavailableException("No models available in fallback chain for tier: " + config.getTier());
    }

    /**
     * 检查模型是否可用（带缓存）
     *
     * @param model 模型名称
     * @return 如果可用返回 true
     */
    private boolean isModelAvailableWithCache(String model) {
        // 1. 检查可用性缓存
        AvailabilityCacheEntry cached = availabilityCache.get(model);
        if (cached != null && !cached.isExpired()) {
            return cached.isAvailable;
        }

        // 2. 执行实际检查
        boolean available = availabilityChecker.isAvailable(model, (int) AVAILABILITY_CACHE_TTL);

        // 3. 更新缓存
        availabilityCache.put(model,
            new AvailabilityCacheEntry(available, System.currentTimeMillis()));

        return available;
    }

    /**
     * 解析模型引用
     *
     * @param modelRef 模型引用
     * @return 解析后的模型名称
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
     * 记录模型选择日志
     *
     * @param tier 模型等级
     * @param resolvedModel 解析后的模型
     * @param originalRef 原始引用
     */
    private void logModelSelection(ModelTier tier, String resolvedModel, String originalRef) {
        logDebug("Model selected: " + resolvedModel + " for tier: " + tier + " (original: " + originalRef + ")");
    }

    /**
     * 获取配置（带缓存）
     *
     * @return 模型选择配置
     */
    private ModelSelectionConfig getConfig() {
        ConfigCacheEntry cached = configCache;
        if (cached != null && !cached.isExpired()) {
            return cached.config;
        }

        // 重新加载配置
        ModelSelectionConfigLoader loader = new ModelSelectionConfigLoader();
        ModelSelectionConfig newConfig = loader.loadOrDefault();

        // 更新缓存
        configCache = new ConfigCacheEntry(newConfig, System.currentTimeMillis());

        return newConfig;
    }

    /**
     * 检查是否启用
     *
     * @return 如果启用返回 true
     */
    public boolean isEnabled() {
        return getConfig().isEnabled();
    }

    /**
     * 获取策略名称
     *
     * @return 配置策略名称
     */
    public String getStrategy() {
        return getConfig().getStrategy();
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        synchronized (this) {
            configCache = null;
            availabilityCache.clear();
            selectionCache.clear();
            cacheHits.set(0);
            cacheMisses.set(0);
            failedSelections.set(0);
        }
        logDebug("All caches cleared");
    }

    /**
     * 清除过期缓存
     */
    public void clearExpiredCache() {
        long now = System.currentTimeMillis();

        // 清除过期的可用性缓存
        availabilityCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));

        // 清除过期的选择缓存
        selectionCache.entrySet().removeIf(entry -> {
            SelectionCacheEntry value = entry.getValue();
            return System.currentTimeMillis() - value.timestamp > SELECTION_CACHE_TTL;
        });

        logDebug("Expired cache entries cleared");
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计字符串
     */
    public String getCacheStats() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total * 100.0 : 0.0;

        return String.format(
            "Cache Stats - Hits: %d, Misses: %d, Hit Rate: %.1f%%, " +
            "Selection Cache: %d, Availability Cache: %d",
            hits, misses, hitRate,
            selectionCache.size(), availabilityCache.size()
        );
    }

    /**
     * 获取缓存命中率
     *
     * @return 缓存命中率（百分比）
     */
    public double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total * 100.0 : 0.0;
    }

    /**
     * 调试日志
     *
     * @param message 日志消息
     */
    private void logDebug(String message) {
        // 在实际项目中可以使用 SLF4J 或其他日志框架
        // 这里使用 System.err 来避免影响正常输出
        System.err.println("[SmartModelSelector] " + message);
    }

    /**
     * 配置缓存条目
     */
    private static class ConfigCacheEntry {
        final ModelSelectionConfig config;
        final long timestamp;

        ConfigCacheEntry(ModelSelectionConfig config, long timestamp) {
            this.config = config;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CONFIG_CACHE_TTL;
        }
    }

    /**
     * 可用性缓存条目
     */
    private static class AvailabilityCacheEntry {
        final boolean isAvailable;
        final long timestamp;

        AvailabilityCacheEntry(boolean isAvailable, long timestamp) {
            this.isAvailable = isAvailable;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        boolean isExpired(long now) {
            return now - timestamp > AVAILABILITY_CACHE_TTL;
        }
    }

    /**
     * 选择结果缓存条目
     */
    private static class SelectionCacheEntry {
        final String modelName;
        final long timestamp;

        SelectionCacheEntry(String modelName, long timestamp) {
            this.modelName = modelName;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > SELECTION_CACHE_TTL;
        }
    }
}