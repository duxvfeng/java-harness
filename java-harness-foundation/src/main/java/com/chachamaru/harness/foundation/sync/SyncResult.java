package com.chachamaru.harness.foundation.sync;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步结果
 * 表示同步操作的执行结果
 */
public class SyncResult {

    private final boolean success;
    private final String errorMessage;
    private final List<TaskSyncChange> changes;
    private final List<SyncConflict> conflicts;
    private final String plansHash;
    private final long timestamp;

    private SyncResult(boolean success, String errorMessage, List<TaskSyncChange> changes,
                      List<SyncConflict> conflicts, String plansHash, long timestamp) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.changes = List.copyOf(changes);
        this.conflicts = List.copyOf(conflicts);
        this.plansHash = plansHash;
        this.timestamp = timestamp;
    }

    /**
     * 创建成功的结果
     */
    public static SyncResult success(List<TaskSyncChange> changes, String plansHash) {
        return new SyncResult(true, null, changes, List.of(), plansHash, System.currentTimeMillis());
    }

    /**
     * 创建带冲突的结果
     */
    public static SyncResult withConflicts(List<TaskSyncChange> changes, List<SyncConflict> conflicts, String plansHash) {
        return new SyncResult(true, null, changes, conflicts, plansHash, System.currentTimeMillis());
    }

    /**
     * 创建失败的结果
     */
    public static SyncResult failure(String errorMessage) {
        return new SyncResult(false, errorMessage, List.of(), List.of(), null, System.currentTimeMillis());
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<TaskSyncChange> getChanges() {
        return changes;
    }

    public List<SyncConflict> getConflicts() {
        return conflicts;
    }

    public String getPlansHash() {
        return plansHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 检查是否有冲突
     */
    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }

    /**
     * 检查是否有变更
     */
    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    /**
     * 获取变更数量
     */
    public int getChangeCount() {
        return changes.size();
    }

    /**
     * 获取冲突数量
     */
    public int getConflictCount() {
        return conflicts.size();
    }

    @Override
    public String toString() {
        return "SyncResult{" +
                "success=" + success +
                ", changes=" + changes.size() +
                ", conflicts=" + conflicts.size() +
                ", plansHash='" + plansHash + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
