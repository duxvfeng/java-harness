package com.chachamaru.harness.session.manager;

import com.chachamaru.harness.session.model.SessionMetadata;
import com.chachamaru.harness.session.model.SessionSaveResult;
import com.chachamaru.harness.session.storage.SessionStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 会话保存管理器
 *
 * <p>负责管理会话保存操作，包括自动保存、手动保存、并发控制、间隔限制和空间检查。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public class SessionSaveManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionSaveManager.class);

    private final SessionStorage storage;
    private final SessionSaveConfig config;
    private final Object saveLock = new Object();
    private final AtomicBoolean isSaving = new AtomicBoolean(false);

    private Instant lastSaveTime = Instant.EPOCH;
    private int consecutiveFailures = 0;

    public SessionSaveManager(SessionStorage storage, SessionSaveConfig config) {
        this.storage = storage;
        this.config = config;
        logger.info("SessionSaveManager initialized with config: {}", config);
    }

    /**
     * 执行会话保存
     *
     * @param context 会话上下文
     * @param reason 保存原因
     * @return 保存结果
     */
    public SessionSaveResult saveSession(SessionContext context, String reason) {
        synchronized (saveLock) {
            // 检查是否已有保存进行中
            if (isSaving.get()) {
                logger.debug("Save already in progress, skipping duplicate request");
                return SessionSaveResult.skipped("保存已在进行中");
            }

            // 检查保存间隔限制
            if (!shouldSaveNow(reason)) {
                logger.debug("Save skipped due to interval limit");
                return SessionSaveResult.skipped("保存间隔过短，需要等待 " + getConfiguredMinIntervalMinutes() + " 分钟");
            }

            setSavingInProgress(true);
            try {
                return performSave(context, reason);
            } finally {
                setSavingInProgress(false);
            }
        }
    }

    /**
     * 强制保存（忽略间隔限制）
     *
     * @param context 会话上下文
     * @param reason 保存原因
     * @return 保存结果
     */
    public SessionSaveResult forceSave(SessionContext context, String reason) {
        synchronized (saveLock) {
            if (isSaving.get()) {
                return SessionSaveResult.skipped("保存已在进行中");
            }

            setSavingInProgress(true);
            try {
                return performSave(context, reason);
            } finally {
                setSavingInProgress(false);
            }
        }
    }

    /**
     * 列出所有保存的会话
     *
     * @return 会话元数据列表
     */
    public java.util.List<SessionMetadata> listSessions() {
        return storage.listSessions();
    }

    /**
     * 列出最近的保存
     *
     * @param limit 最大返回数量
     * @return 会话元数据列表
     */
    public java.util.List<SessionMetadata> listRecentSessions(int limit) {
        return storage.listRecentSessions(limit);
    }

    /**
     * 清理旧保存
     *
     * @return 清理的保存数量
     */
    public int cleanupOldSessions() {
        synchronized (saveLock) {
            return storage.cleanupOldSessions(
                    config.getMaxSaves(),
                    config.getMaxAgeDays()
            );
        }
    }

    /**
     * 检查存储健康状况
     *
     * @return 存储是否健康
     */
    public boolean isStorageHealthy() {
        return storage.healthCheck();
    }

    /**
     * 获取存储信息
     *
     * @return 存储信息
     */
    public SessionStorage.StorageInfo getStorageInfo() {
        return storage.getStorageInfo();
    }

    // Private helper methods

    private boolean shouldSaveNow(String reason) {
        // 检查是否是强制保存
        if (reason != null && reason.contains("force")) {
            return true;
        }

        // 检查距离上次保存的时间间隔
        if (lastSaveTime.equals(Instant.EPOCH)) {
            return true; // 从未保存过
        }

        Duration timeSinceLastSave = Duration.between(lastSaveTime, Instant.now());
        Duration minInterval = Duration.ofMinutes(getConfiguredMinIntervalMinutes());

        return timeSinceLastSave.compareTo(minInterval) >= 0;
    }

    private SessionSaveResult performSave(SessionContext context, String reason) {
        try {
            logger.info("Starting session save: reason='{}'", reason);

            // 检查存储空间
            if (!hasEnoughSpace()) {
                logger.warn("Insufficient storage space, attempting cleanup");
                int cleaned = cleanupOldSessions();

                if (!hasEnoughSpace()) {
                    String errorMsg = "磁盘空间不足，清理后仍无法保存";
                    logger.error(errorMsg);
                    consecutiveFailures++;
                    return SessionSaveResult.failed(errorMsg);
                }

                logger.info("Cleanup freed space, cleaned {} sessions", cleaned);
            }

            // 准备会话数据
            String sessionId = generateSessionId(reason);
            String sessionData = context.getSessionData();
            SessionMetadata metadata = context.getMetadata();

            // 尝试完整保存
            SessionSaveResult result = storage.saveSession(sessionId, sessionData, metadata);

            if (result.isSuccess()) {
                lastSaveTime = Instant.now();
                consecutiveFailures = 0;
                logger.info("Session saved successfully: {}", sessionId);

                // 保存成功后触发后台清理
                triggerBackgroundCleanup();
            } else {
                consecutiveFailures++;
                logger.error("Session save failed: {}", result.getErrorMessage());

                // 尝试降级保存
                if (consecutiveFailures <= 3) {
                    logger.info("Attempting minimal save as fallback");
                    return performMinimalSave(context, reason);
                }
            }

            return result;

        } catch (Exception e) {
            consecutiveFailures++;
            logger.error("Unexpected error during session save", e);

            // 尝试降级保存
            if (consecutiveFailures <= 3) {
                return performMinimalSave(context, reason);
            }

            return SessionSaveResult.failed("保存异常: " + e.getMessage());
        }
    }

    private SessionSaveResult performMinimalSave(SessionContext context, String reason) {
        try {
            logger.info("Performing minimal save");

            String sessionId = generateSessionId(reason + "-minimal");
            SessionMetadata minimalMetadata = createMinimalMetadata(context, reason);

            // 只保存元数据，不保存完整会话数据
            SessionSaveResult result = storage.saveSession(sessionId, "", minimalMetadata);

            if (result.isSuccess()) {
                logger.info("Minimal save succeeded");
            }

            return result;

        } catch (Exception e) {
            logger.error("Minimal save also failed", e);
            return SessionSaveResult.failed("降级保存失败: " + e.getMessage());
        }
    }

    private boolean hasEnoughSpace() {
        SessionStorage.StorageInfo info = storage.getStorageInfo();
        long requiredSpace = config.getMaxSingleSaveBytes();

        // 检查单个保存大小限制
        if (requiredSpace > (info.getTotalSizeBytes() - info.getUsedSizeBytes())) {
            return false;
        }

        // 检查总体存储使用率
        return info.getUsagePercentage() < 90;
    }

    private void triggerBackgroundCleanup() {
        // 在后台线程中执行清理
        new Thread(() -> {
            try {
                int cleaned = cleanupOldSessions();
                if (cleaned > 0) {
                    logger.info("Background cleanup completed: {} sessions removed", cleaned);
                }
            } catch (Exception e) {
                logger.warn("Background cleanup failed", e);
            }
        }, "session-cleanup").start();
    }

    private String generateSessionId(String reason) {
        String timestamp = java.time.format.DateTimeFormatter
                .ofPattern("yyyyMMdd-HHmmss")
                .format(java.time.LocalDateTime.now());

        String reasonSuffix = reason
                .replaceAll("[^a-zA-Z0-9]", "-")
                .toLowerCase()
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        return timestamp + (reasonSuffix.isEmpty() ? "" : "-" + reasonSuffix);
    }

    private SessionMetadata createMinimalMetadata(SessionContext context, String reason) {
        // 创建最小元数据，只包含关键信息
        SessionMetadata.TaskContext taskContext = context.getMetadata().getTaskContext();
        SessionMetadata.GitState gitState = new SessionMetadata.GitState(
                getGitBranch(),
                getGitCommit(),
                getModifiedFilesCount(),
                hasUncommittedChanges()
        );

        SessionMetadata.SaveSize saveSize = new SessionMetadata.SaveSize(
                1, // 只有一个元数据文件
                "1KB",
                "1KB"
        );

        return new SessionMetadata(
                generateSessionId(reason + "-minimal"),
                java.time.Instant.now(),
                "MINIMAL SAVE: " + reason,
                -1, // 未知 token 使用率
                taskContext,
                gitState,
                "最小保存 - 仅保存关键元数据",
                saveSize
        );
    }

    private String getGitBranch() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "branch", "--show-current");
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String branch = reader.readLine();
            process.waitFor();
            return branch != null ? branch : "unknown";
        } catch (Exception e) {
            logger.warn("Failed to get git branch", e);
            return "unknown";
        }
    }

    private String getGitCommit() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String commit = reader.readLine();
            process.waitFor();
            return commit != null ? commit.substring(0, 7) : "unknown";
        } catch (Exception e) {
            logger.warn("Failed to get git commit", e);
            return "unknown";
        }
    }

    private int getModifiedFilesCount() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));

            int count = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    count++;
                }
            }

            process.waitFor();
            return count;
        } catch (Exception e) {
            logger.warn("Failed to get modified files count", e);
            return 0;
        }
    }

    private boolean hasUncommittedChanges() {
        return getModifiedFilesCount() > 0;
    }

    private int getConfiguredMinIntervalMinutes() {
        return config.getMinSaveIntervalMinutes();
    }

    private void setSavingInProgress(boolean saving) {
        isSaving.set(saving);
        if (saving) {
            logger.debug("Save operation started");
        } else {
            logger.debug("Save operation completed");
        }
    }

    /**
     * 会话保存配置
     */
    public static class SessionSaveConfig {
        private final int maxSaves;
        private final int maxAgeDays;
        private final long maxSingleSaveBytes;
        private final int minSaveIntervalMinutes;

        public SessionSaveConfig(
                int maxSaves,
                int maxAgeDays,
                long maxSingleSaveBytes,
                int minSaveIntervalMinutes) {
            this.maxSaves = maxSaves;
            this.maxAgeDays = maxAgeDays;
            this.maxSingleSaveBytes = maxSingleSaveBytes;
            this.minSaveIntervalMinutes = minSaveIntervalMinutes;
        }

        public static SessionSaveConfig getDefault() {
            return new SessionSaveConfig(
                    10,     // 最多保留10个保存
                    7,      // 最多保留7天
                    50 * 1024 * 1024, // 单个保存最大50MB
                    5       // 最小保存间隔5分钟
            );
        }

        public int getMaxSaves() { return maxSaves; }
        public int getMaxAgeDays() { return maxAgeDays; }
        public long getMaxSingleSaveBytes() { return maxSingleSaveBytes; }
        public int getMinSaveIntervalMinutes() { return minSaveIntervalMinutes; }

        @Override
        public String toString() {
            return "SessionSaveConfig{" +
                    "maxSaves=" + maxSaves +
                    ", maxAgeDays=" + maxAgeDays +
                    ", maxSingleSaveBytes=" + maxSingleSaveBytes +
                    ", minSaveIntervalMinutes=" + minSaveIntervalMinutes +
                    '}';
        }
    }

    /**
     * 会话上下文
     */
    public static class SessionContext {
        private final java.util.Map<String, Object> sessionData;
        private final SessionMetadata metadata;

        public SessionContext(java.util.Map<String, Object> sessionData, SessionMetadata metadata) {
            this.sessionData = sessionData;
            this.metadata = metadata;
        }

        public String getSessionData() {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.writeValueAsString(sessionData);
            } catch (Exception e) {
                logger.error("Failed to serialize session data", e);
                return "{}";
            }
        }

        public SessionMetadata getMetadata() {
            return metadata;
        }
    }
}