package com.chachamaru.harness.session.storage;

import com.chachamaru.harness.session.model.SessionMetadata;
import com.chachamaru.harness.session.model.SessionSummary;
import com.chachamaru.harness.session.model.SessionSaveResult;

import java.util.List;
import java.util.Optional;

/**
 * 会话存储接口
 *
 * <p>定义会话保存和恢复的存储操作，支持多种存储后端实现。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public interface SessionStorage {

    /**
     * 保存会话
     *
     * @param sessionId 会话ID
     * @param sessionData 会话数据（JSON格式）
     * @param metadata 会话元数据
     * @return 保存结果
     */
    SessionSaveResult saveSession(String sessionId, String sessionData, SessionMetadata metadata);

    /**
     * 加载会话数据
     *
     * @param saveId 保存ID
     * @return 会话数据，如果不存在返回空
     */
    Optional<String> loadSessionData(String saveId);

    /**
     * 加载会话元数据
     *
     * @param saveId 保存ID
     * @return 会话元数据，如果不存在返回空
     */
    Optional<SessionMetadata> loadMetadata(String saveId);

    /**
     * 列出所有保存的会话
     *
     * @return 会话元数据列表，按时间戳降序排序
     */
    List<SessionMetadata> listSessions();

    /**
     * 列出最近的N个保存
     *
     * @param limit 最大返回数量
     * @return 会话元数据列表，按时间戳降序排序
     */
    List<SessionMetadata> listRecentSessions(int limit);

    /**
     * 删除特定保存
     *
     * @param saveId 保存ID
     * @return 是否删除成功
     */
    boolean deleteSession(String saveId);

    /**
     * 清理旧保存
     *
     * @param maxToKeep 保留的最大数量
     * @param maxAgeDays 最大保留天数
     * @return 清理的保存数量
     */
    int cleanupOldSessions(int maxToKeep, int maxAgeDays);

    /**
     * 检查存储健康状况
     *
     * @return 存储是否健康
     */
    boolean healthCheck();

    /**
     * 获取存储使用情况
     *
     * @return 存储使用信息
     */
    StorageInfo getStorageInfo();

    /**
     * 存储使用信息
     */
    class StorageInfo {
        private final long totalSizeBytes;
        private final long usedSizeBytes;
        private final int totalSessions;
        private final boolean healthy;

        public StorageInfo(long totalSizeBytes, long usedSizeBytes, int totalSessions, boolean healthy) {
            this.totalSizeBytes = totalSizeBytes;
            this.usedSizeBytes = usedSizeBytes;
            this.totalSessions = totalSessions;
            this.healthy = healthy;
        }

        public long getTotalSizeBytes() { return totalSizeBytes; }
        public long getUsedSizeBytes() { return usedSizeBytes; }
        public int getTotalSessions() { return totalSessions; }
        public boolean isHealthy() { return healthy; }
        public double getUsagePercentage() {
            if (totalSizeBytes <= 0) return 0;
            return (double) usedSizeBytes / totalSizeBytes * 100;
        }
    }
}