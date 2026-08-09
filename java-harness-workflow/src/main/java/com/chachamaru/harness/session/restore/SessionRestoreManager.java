package com.chachamaru.harness.session.restore;

import com.chachamaru.harness.session.model.SessionMetadata;
import com.chachamaru.harness.session.model.SessionSummary;
import com.chachamaru.harness.session.model.RestoreSuggestion;
import com.chachamaru.harness.session.storage.SessionStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * 会话恢复管理器
 *
 * <p>负责检测会话保存、生成恢复建议和决策。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public class SessionRestoreManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionRestoreManager.class);

    private final SessionStorage storage;
    private final RestoreConfig config;

    public SessionRestoreManager(SessionStorage storage, RestoreConfig config) {
        this.storage = storage;
        this.config = config;
        logger.info("SessionRestoreManager initialized");
    }

    /**
     * 检查恢复机会
     *
     * @return 恢复建议（如果有）
     */
    public Optional<RestoreSuggestion> checkRestoreOpportunity() {
        try {
            // 获取最近的会话保存
            List<SessionMetadata> recentSessions = storage.listRecentSessions(5);
            if (recentSessions.isEmpty()) {
                logger.debug("No session saves found");
                return Optional.empty();
            }

            // 找到最新的保存
            SessionMetadata latest = recentSessions.get(0);
            Instant saveTime = latest.getTimestamp();
            Instant now = Instant.now();

            // 检查时间范围
            long hoursSinceSave = ChronoUnit.HOURS.between(saveTime, now);
            if (hoursSinceSave > config.getMaxHistoryAgeHours()) {
                logger.debug("Latest save is too old: {} hours", hoursSinceSave);
                return Optional.empty();
            }

            // 生成恢复建议
            RestoreSuggestion suggestion = generateSuggestion(latest, hoursSinceSave);
            logger.info("Restore suggestion generated: {}", suggestion.getSaveId());

            return Optional.of(suggestion);

        } catch (Exception e) {
            logger.error("Failed to check restore opportunity", e);
            return Optional.empty();
        }
    }

    /**
     * 生成会话摘要
     *
     * @param saveId 保存ID
     * @return 会话摘要
     */
    public Optional<SessionSummary> generateSummary(String saveId) {
        try {
            Optional<SessionMetadata> metadataOpt = storage.loadMetadata(saveId);
            if (metadataOpt.isEmpty()) {
                return Optional.empty();
            }

            SessionMetadata metadata = metadataOpt.get();

            // 生成摘要
            String quickOverview = generateQuickOverview(metadata);
            String currentWork = generateCurrentWork(metadata);
            List<String> recentProgress = generateRecentProgress(metadata);
            String recommendation = generateRecommendation(metadata);

            // AI 决策
            SessionSummary.AIDecision aiDecision = makeAIDecision(metadata);

            SessionSummary summary = new SessionSummary(
                    saveId,
                    quickOverview,
                    currentWork,
                    recentProgress,
                    recommendation,
                    aiDecision
            );

            return Optional.of(summary);

        } catch (Exception e) {
            logger.error("Failed to generate summary for: {}", saveId, e);
            return Optional.empty();
        }
    }

    /**
     * 验证保存完整性
     *
     * @param saveId 保存ID
     * @return 是否完整
     */
    public boolean validateSaveIntegrity(String saveId) {
        try {
            // 检查元数据是否存在
            Optional<SessionMetadata> metadata = storage.loadMetadata(saveId);
            if (metadata.isEmpty()) {
                return false;
            }

            // 检查会话数据是否存在
            Optional<String> sessionData = storage.loadSessionData(saveId);
            if (sessionData.isEmpty()) {
                return false;
            }

            // 基本完整性检查
            String data = sessionData.get();
            return !data.isEmpty() && data.length() > 0;

        } catch (Exception e) {
            logger.warn("Failed to validate save integrity: {}", saveId, e);
            return false;
        }
    }

    // Private helper methods

    private RestoreSuggestion generateSuggestion(SessionMetadata metadata, long timeSinceSaveHours) {
        String saveId = metadata.getSaveId();

        // 生成会话摘要
        SessionSummary summary = generateSummary(saveId).orElse(createFallbackSummary(saveId));

        // AI 决策
        boolean needsDetailedContext = summary.getAiDecision().needsDetailedContext();
        double confidence = calculateConfidence(metadata, timeSinceSaveHours);

        return new RestoreSuggestion(
                saveId,
                summary,
                needsDetailedContext,
                summary.getAiDecision().getReason(),
                confidence,
                timeSinceSaveHours,
                metadata.getTimestamp()
        );
    }

    private SessionSummary createFallbackSummary(String saveId) {
        SessionSummary.AIDecision aiDecision = new SessionSummary.AIDecision(
                true,
                "基本恢复建议",
                0.7
        );

        return new SessionSummary(
                saveId,
                "会话保存可用",
                "恢复上次的工作进度",
                List.of("检测到会话保存"),
                "建议恢复以继续工作",
                aiDecision
        );
    }

    private String generateQuickOverview(SessionMetadata metadata) {
        return String.format("Token使用率: %d%% - %s",
                metadata.getTokenUsage(),
                metadata.getSummary());
    }

    private String generateCurrentWork(SessionMetadata metadata) {
        SessionMetadata.TaskContext taskContext = metadata.getTaskContext();
        if (taskContext != null) {
            return String.format("正在执行 %s (任务 %s/%d)",
                    taskContext.getCurrentPhase(),
                    taskContext.getCurrentTask(),
                    taskContext.getTotalTasks());
        }
        return "会话恢复";
    }

    private List<String> generateRecentProgress(SessionMetadata metadata) {
        SessionMetadata.TaskContext taskContext = metadata.getTaskContext();
        if (taskContext != null && taskContext.getCompletedTasks() != null) {
            return List.of(
                    String.format("✅ 已完成 %d 个任务", taskContext.getCompletedTasks().size()),
                    String.format("🔄 当前任务: %s", taskContext.getCurrentTask())
            );
        }
        return List.of("会话可恢复");
    }

    private String generateRecommendation(SessionMetadata metadata) {
        if (metadata.getTokenUsage() > 80) {
            return "建议恢复会话并继续工作，注意 token 使用情况";
        }
        return "建议恢复会话以继续上次的工作";
    }

    private SessionSummary.AIDecision makeAIDecision(SessionMetadata metadata) {
        int score = 0;

        // 评估因素
        if (metadata.getTokenUsage() > 85) score += 3;
        if (metadata.getGitState() != null && metadata.getGitState().hasUncommittedChanges()) score += 1;
        if (metadata.getTaskContext() != null) {
            int pendingTasks = metadata.getTaskContext().getTotalTasks() -
                           metadata.getTaskContext().getCompletedTasks().size();
            if (pendingTasks > 10) score += 2;
            if (pendingTasks > 5) score += 1;
        }

        boolean needsDetailedContext = score >= 2;
        String reason = score >= 3 ? "复杂任务，建议恢复完整上下文" : "简单任务，可选择性恢复";
        double confidence = 0.8;

        return new SessionSummary.AIDecision(needsDetailedContext, reason, confidence);
    }

    private double calculateConfidence(SessionMetadata metadata, long timeSinceSaveHours) {
        double confidence = 0.8; // 基础置信度

        // 时间越近，置信度越高
        if (timeSinceSaveHours < 1) {
            confidence += 0.1;
        } else if (timeSinceSaveHours > 24) {
            confidence -= 0.2;
        }

        // Token 使用率较高时，置信度增加
        if (metadata.getTokenUsage() > 80) {
            confidence += 0.1;
        }

        return Math.min(1.0, Math.max(0.0, confidence));
    }

    /**
     * 恢复配置
     */
    public static class RestoreConfig {
        private final long maxHistoryAgeHours;

        public RestoreConfig(long maxHistoryAgeHours) {
            this.maxHistoryAgeHours = maxHistoryAgeHours;
        }

        public static RestoreConfig getDefault() {
            return new RestoreConfig(7 * 24); // 7天
        }

        public long getMaxHistoryAgeHours() { return maxHistoryAgeHours; }
    }
}