package com.chachamaru.harness.foundation.recovery;

import java.util.*;

/**
 * 状态恢复机制
 * 提供崩溃后状态恢复功能
 */
public class StateRecoveryService {

    private final Map<String, byte[]> stateBackups;
    private final Map<String, Long> backupTimestamps;

    public StateRecoveryService() {
        this.stateBackups = new HashMap<>();
        this.backupTimestamps = new HashMap<>();
    }

    /**
     * 创建状态检查点
     */
    public String createCheckpoint(byte[] stateData) throws RecoveryException {
        String backupId = UUID.randomUUID().toString();
        stateBackups.put(backupId, stateData);
        backupTimestamps.put(backupId, System.currentTimeMillis());
        return backupId;
    }

    /**
     * 从备份恢复状态
     */
    public Optional<byte[]> recoverState(String backupId) throws RecoveryException {
        byte[] backup = stateBackups.get(backupId);
        if (backup == null) {
            return Optional.empty();
        }
        return Optional.of(backup);
    }

    /**
     * 检查状态文件完整性
     */
    public boolean validateStateData(byte[] stateData) {
        return stateData != null && stateData.length > 0;
    }

    /**
     * 获取所有备份ID
     */
    public List<String> getBackupIds() {
        return new ArrayList<>(stateBackups.keySet());
    }

    /**
     * 删除备份
     */
    public void removeBackup(String backupId) {
        stateBackups.remove(backupId);
        backupTimestamps.remove(backupId);
    }

    /**
     * 清理过期备份
     */
    public void cleanupOldBackups(long maxAgeMillis) {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = backupTimestamps.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (currentTime - entry.getValue() > maxAgeMillis) {
                String backupId = entry.getKey();
                stateBackups.remove(backupId);
                iterator.remove();
            }
        }
    }
}
