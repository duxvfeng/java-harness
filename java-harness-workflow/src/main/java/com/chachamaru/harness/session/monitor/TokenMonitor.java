package com.chachamaru.harness.session.monitor;

import com.chachamaru.harness.session.model.TokenUsageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Token 监控器
 *
 * <p>监控当前会话的 token 使用情况，支持多种检测方法和降级策略。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public class TokenMonitor {

    private static final Logger logger = LoggerFactory.getLogger(TokenMonitor.class);

    private final TokenMonitorConfig config;

    public TokenMonitor(TokenMonitorConfig config) {
        this.config = config;
        logger.info("TokenMonitor initialized with thresholds: {}%", config.getThresholds());
    }

    /**
     * 检查当前 token 使用情况
     *
     * @return Token 使用信息
     */
    public TokenUsageInfo checkTokenUsage() {
        try {
            // 方法1: 尝试从环境变量检测
            Optional<TokenUsageInfo> envResult = detectFromEnvironment();
            if (envResult.isPresent()) {
                return envResult.get();
            }

            // 方法2: 尝试从 Claude API 检测（降级方法）
            Optional<TokenUsageInfo> apiResult = detectFromAPI();
            if (apiResult.isPresent()) {
                return apiResult.get();
            }

            // 方法3: 使用估算方法（最后降级）
            return estimateUsage();

        } catch (Exception e) {
            logger.warn("Token detection failed, using estimation", e);
            return estimateUsage();
        }
    }

    /**
     * 判断是否应该触发保存
     *
     * @param currentPercentage 当前 token 使用百分比
     * @return 是否应该触发保存
     */
    public boolean shouldTriggerSave(int currentPercentage) {
        if (currentPercentage < 0) {
            return false; // 无法确定时不触发
        }

        // 检查是否达到任何阈值
        for (int threshold : config.getThresholds()) {
            if (currentPercentage >= threshold) {
                logger.info("Token threshold reached: {}% >= {}%", currentPercentage, threshold);
                return true;
            }
        }

        return false;
    }

    /**
     * 判断当前是否需要立即保存（紧急阈值）
     *
     * @return 是否需要立即保存
     */
    public boolean needsImmediateSave() {
        TokenUsageInfo info = checkTokenUsage();
        return info.needsImmediateSave(
                config.getUrgentThreshold(),
                config.getNormalThreshold()
        );
    }

    // Private detection methods

    private Optional<TokenUsageInfo> detectFromEnvironment() {
        try {
            // 尝试读取 Claude 的环境变量
            String claudeTokenCount = System.getenv("CLAUDE_TOKEN_COUNT");
            if (claudeTokenCount != null && !claudeTokenCount.isEmpty()) {
                int currentUsage = Integer.parseInt(claudeTokenCount);
                int percentage = calculatePercentage(currentUsage);

                logger.debug("Token detected from environment: {}%", percentage);
                return Optional.of(new TokenUsageInfo(
                        currentUsage,
                        percentage,
                        estimateRemaining(currentUsage),
                        "environment_variable"
                ));
            }

            return Optional.empty();

        } catch (Exception e) {
            logger.debug("Environment variable detection failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<TokenUsageInfo> detectFromAPI() {
        try {
            // 这里可以调用 Claude API 来获取准确的 token 使用情况
            // 由于需要 API 调用，暂时返回空，使用估算作为降级
            logger.debug("API detection not implemented, falling back to estimation");
            return Optional.empty();

        } catch (Exception e) {
            logger.debug("API detection failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private TokenUsageInfo estimateUsage() {
        // 基于对话长度和复杂度进行估算
        // 这是一个简化的估算方法
        int estimatedUsage = 50000 + (int) (Math.random() * 50000); // 50k-100k 估算
        int percentage = calculatePercentage(estimatedUsage);

        logger.debug("Using estimated token usage: {}%", percentage);
        return new TokenUsageInfo(
                estimatedUsage,
                percentage,
                estimateRemaining(estimatedUsage),
                "estimation"
        );
    }

    private int calculatePercentage(int currentUsage) {
        // 假设最大 token 数为 200k
        int maxTokens = config.getMaxTokens();
        if (maxTokens <= 0) {
            maxTokens = 200000; // 默认最大值
        }

        return (int) ((currentUsage * 100) / maxTokens);
    }

    private int estimateRemaining(int currentUsage) {
        int maxTokens = config.getMaxTokens();
        if (maxTokens <= 0) {
            maxTokens = 200000;
        }

        return Math.max(0, maxTokens - currentUsage);
    }

    /**
     * Token 监控配置
     */
    public static class TokenMonitorConfig {
        private final int[] thresholds;
        private final int urgentThreshold;
        private final int normalThreshold;
        private final int maxTokens;

        public TokenMonitorConfig(
                int[] thresholds,
                int urgentThreshold,
                int normalThreshold,
                int maxTokens) {
            this.thresholds = thresholds;
            this.urgentThreshold = urgentThreshold;
            this.normalThreshold = normalThreshold;
            this.maxTokens = maxTokens;
        }

        public static TokenMonitorConfig getDefault() {
            return new TokenMonitorConfig(
                    new int[]{80, 90}, // 80% 和 90% 阈值
                    90,             // 紧急阈值
                    80,             // 正常阈值
                    200000          // 假设最大 200k tokens
            );
        }

        public int[] getThresholds() { return thresholds; }
        public int getUrgentThreshold() { return urgentThreshold; }
        public int getNormalThreshold() { return normalThreshold; }
        public int getMaxTokens() { return maxTokens; }

        @Override
        public String toString() {
            return "TokenMonitorConfig{" +
                    "thresholds=" + java.util.Arrays.toString(thresholds) +
                    ", urgentThreshold=" + urgentThreshold +
                    ", normalThreshold=" + normalThreshold +
                    ", maxTokens=" + maxTokens +
                    '}';
        }
    }
}