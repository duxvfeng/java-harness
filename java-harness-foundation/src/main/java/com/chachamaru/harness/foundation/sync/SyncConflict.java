package com.chachamaru.harness.foundation.sync;

/**
 * 同步冲突
 * 表示检测到的同步冲突
 */
public class SyncConflict {

    private final String taskId;
    private final ConflictType conflictType;
    private final String localValue;
    private final String remoteValue;
    private final String description;
    private final ConflictResolution resolution;

    public enum ConflictType {
        STATUS_CONFLICT,
        DESCRIPTION_CONFLICT,
        STRUCTURAL_CONFLICT,
        DEPENDENCY_CONFLICT
    }

    public enum ConflictResolution {
        UNRESOLVED,
        LOCAL_WINS,
        REMOTE_WINS,
        MERGED,
        MANUAL_REQUIRED
    }

    public SyncConflict(String taskId, ConflictType conflictType,
                       String localValue, String remoteValue, String description) {
        this.taskId = taskId;
        this.conflictType = conflictType;
        this.localValue = localValue;
        this.remoteValue = remoteValue;
        this.description = description;
        this.resolution = ConflictResolution.UNRESOLVED;
    }

    public SyncConflict(String taskId, ConflictType conflictType,
                       String localValue, String remoteValue, String description,
                       ConflictResolution resolution) {
        this.taskId = taskId;
        this.conflictType = conflictType;
        this.localValue = localValue;
        this.remoteValue = remoteValue;
        this.description = description;
        this.resolution = resolution;
    }

    public String getTaskId() {
        return taskId;
    }

    public ConflictType getConflictType() {
        return conflictType;
    }

    public String getLocalValue() {
        return localValue;
    }

    public String getRemoteValue() {
        return remoteValue;
    }

    public String getDescription() {
        return description;
    }

    public ConflictResolution getResolution() {
        return resolution;
    }

    /**
     * 创建已解决的冲突副本
     */
    public SyncConflict withResolution(ConflictResolution newResolution) {
        return new SyncConflict(this.taskId, this.conflictType,
                             this.localValue, this.remoteValue,
                             this.description, newResolution);
    }

    /**
     * 检查冲突是否已解决
     */
    public boolean isResolved() {
        return resolution != ConflictResolution.UNRESOLVED &&
               resolution != ConflictResolution.MANUAL_REQUIRED;
    }

    /**
     * 检查是否需要手动解决
     */
    public boolean requiresManualResolution() {
        return resolution == ConflictResolution.MANUAL_REQUIRED;
    }

    @Override
    public String toString() {
        return "SyncConflict{" +
                "taskId='" + taskId + '\'' +
                ", conflictType=" + conflictType +
                ", localValue='" + localValue + '\'' +
                ", remoteValue='" + remoteValue + '\'' +
                ", description='" + description + '\'' +
                ", resolution=" + resolution +
                '}';
    }
}
