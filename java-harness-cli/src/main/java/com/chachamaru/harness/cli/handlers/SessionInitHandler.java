package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import com.chachamaru.harness.session.restore.SessionRestoreManager;
import com.chachamaru.harness.session.model.RestoreSuggestion;
import com.chachamaru.harness.session.model.SessionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * SessionInit hook handler with restore prompt functionality.
 *
 * <p>Handles session initialization with intelligent restore detection and prompt generation.
 * This hook integrates with SessionRestoreManager to detect available session saves
 * and provide intelligent restore suggestions when a new session starts.</p>
 *
 * @since 2026-08-09
 */
public class SessionInitHandler implements HookHandler {
    private static final Logger logger = LoggerFactory.getLogger(SessionInitHandler.class);
    private static final String SESSION_INIT = "SessionInit";

    private final SessionRestoreManager restoreManager;
    private final RestorePromptConfig config;

    /**
     * Constructor with default configuration
     */
    public SessionInitHandler(SessionRestoreManager restoreManager) {
        this(restoreManager, RestorePromptConfig.getDefault());
    }

    /**
     * Constructor with custom configuration
     */
    public SessionInitHandler(SessionRestoreManager restoreManager, RestorePromptConfig config) {
        this.restoreManager = restoreManager;
        this.config = config;
        logger.info("SessionInitHandler initialized with config: enabled={}, autoShow={}",
                config.isEnabled(), config.isAutoShow());
    }

    @Override
    public String getEventName() {
        return SESSION_INIT;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        logger.info("Session init triggered: {}", input.sessionId());

        // Check if restore functionality is enabled
        if (!config.isEnabled()) {
            logger.debug("Restore prompt disabled, skipping");
            return HookOutput.allow();
        }

        try {
            // Check for restore opportunities
            Optional<RestoreSuggestion> suggestion = restoreManager.checkRestoreOpportunity();

            if (suggestion.isPresent()) {
                RestoreSuggestion restoreSuggestion = suggestion.get();
                logger.info("Restore opportunity detected: {}", restoreSuggestion.getSaveId());

                // Generate restore prompt
                String restorePrompt = generateRestorePrompt(restoreSuggestion);

                // Display prompt if auto-show is enabled
                if (config.isAutoShow()) {
                    displayRestorePrompt(restorePrompt, restoreSuggestion);
                }

                // Store restore information in additional context
                String contextInfo = String.format(
                        "RestoreAvailable=true|SaveId=%s|NeedsDetailedContext=%s|Confidence=%.2f",
                        restoreSuggestion.getSaveId(),
                        restoreSuggestion.needsDetailedContext(),
                        restoreSuggestion.getConfidence()
                );

                return new HookOutput(
                        input.hookEventName(),
                        "allow",
                        null,
                        config.isIncludePromptInContext() ? restorePrompt : contextInfo
                );
            } else {
                logger.debug("No restore opportunity found");
                return HookOutput.allow();
            }

        } catch (Exception e) {
            logger.error("Failed to process session init restore check", e);
            // Fail-open: don't block session start on restore check failure
            return HookOutput.allow();
        }
    }

    /**
     * Generate restore prompt with detailed information
     */
    private String generateRestorePrompt(RestoreSuggestion suggestion) {
        StringBuilder prompt = new StringBuilder();

        // Header
        prompt.append("╔════════════════════════════════════════════════════════════════╗\n");
        prompt.append("║            💾 会话恢复建议 (Session Restore Available)          ║\n");
        prompt.append("╚════════════════════════════════════════════════════════════════╝\n\n");

        // Session summary
        SessionSummary summary = suggestion.getSummary();
        prompt.append("📋 会话概述 (Session Overview)\n");
        prompt.append("─────────────────────────────────────────────────────────────\n");
        prompt.append(summary.getQuickOverview()).append("\n\n");

        // Current work status
        prompt.append("🔄 当前工作 (Current Work)\n");
        prompt.append("─────────────────────────────────────────────────────────────\n");
        prompt.append(summary.getCurrentWork()).append("\n\n");

        // Recent progress
        if (!summary.getRecentProgress().isEmpty()) {
            prompt.append("📈 最近进度 (Recent Progress)\n");
            prompt.append("─────────────────────────────────────────────────────────────\n");
            for (String progress : summary.getRecentProgress()) {
                prompt.append("  ").append(progress).append("\n");
            }
            prompt.append("\n");
        }

        // AI decision
        SessionSummary.AIDecision aiDecision = summary.getAiDecision();
        prompt.append("🤖 AI 决策 (AI Decision)\n");
        prompt.append("─────────────────────────────────────────────────────────────\n");
        prompt.append("建议: ").append(aiDecision.getReason()).append("\n");
        prompt.append("置信度: ").append(String.format("%.1f%%", aiDecision.getConfidence() * 100)).append("\n\n");

        // Restore suggestion details
        prompt.append("📊 恢复信息 (Restore Details)\n");
        prompt.append("─────────────────────────────────────────────────────────────\n");
        prompt.append("保存 ID: ").append(suggestion.getSaveId()).append("\n");
        prompt.append("保存时间: ").append(formatTimestamp(suggestion.getSaveTimestamp())).append("\n");
        prompt.append("时间差: ").append(suggestion.getTimeSinceSaveHours()).append(" 小时前\n");
        prompt.append("恢复置信度: ").append(String.format("%.1f%%", suggestion.getConfidence() * 100)).append("\n\n");

        // Recommendation
        prompt.append("💡 建议 (Recommendation)\n");
        prompt.append("─────────────────────────────────────────────────────────────\n");
        prompt.append(summary.getRecommendation()).append("\n\n");

        // Restore command hint
        prompt.append("🚀 恢复命令 (Restore Command)\n");
        prompt.append("─────────────────────────────────────────────────────────────\n");
        prompt.append("使用以下命令恢复会话:\n");
        prompt.append("  /harness-session restore ").append(suggestion.getSaveId()).append("\n\n");

        return prompt.toString();
    }

    /**
     * Display restore prompt to user
     */
    private void displayRestorePrompt(String restorePrompt, RestoreSuggestion suggestion) {
        // Log to console (visible to user)
        System.out.println("\n" + restorePrompt);

        // Log to system logger
        logger.info("Restore prompt displayed for save: {}", suggestion.getSaveId());

        // If detailed context is needed, log additional info
        if (suggestion.needsDetailedContext()) {
            logger.info("Detailed context restoration recommended for: {}", suggestion.getSaveId());
        }
    }

    /**
     * Format timestamp for display
     */
    private String formatTimestamp(java.time.Instant timestamp) {
        return LocalDateTime.ofInstant(timestamp, java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Restore prompt configuration
     */
    public static class RestorePromptConfig {
        private final boolean enabled;
        private final boolean autoShow;
        private final boolean includePromptInContext;
        private final int maxPromptLength;

        public RestorePromptConfig(boolean enabled, boolean autoShow, boolean includePromptInContext, int maxPromptLength) {
            this.enabled = enabled;
            this.autoShow = autoShow;
            this.includePromptInContext = includePromptInContext;
            this.maxPromptLength = maxPromptLength;
        }

        public static RestorePromptConfig getDefault() {
            return new RestorePromptConfig(true, true, false, 2000);
        }

        public boolean isEnabled() { return enabled; }
        public boolean isAutoShow() { return autoShow; }
        public boolean isIncludePromptInContext() { return includePromptInContext; }
        public int getMaxPromptLength() { return maxPromptLength; }
    }
}