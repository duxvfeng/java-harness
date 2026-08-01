package com.chachamaru.harness.foundation.history;

/**
 * 状态变更事件
 */
public class StateChangeEvent {

    private final String taskId;
    private final String oldStatus;
    private final String newStatus;
    private final long timestamp;
    private final String changedBy;
    private final ChangeType changeType;

    public enum ChangeType {
        STATUS_CHANGED,
        TASK_ADDED,
        TASK_REMOVED,
        TASK_MODIFIED
    }

    public StateChangeEvent(String taskId, String oldStatus, String newStatus,
                          String changedBy, ChangeType changeType) {
        this.taskId = taskId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.timestamp = System.currentTimeMillis();
        this.changedBy = changedBy;
        this.changeType = changeType;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    @Override
    public String toString() {
        return "StateChangeEvent{" +
                "taskId='" + taskId + '\'' +
                ", oldStatus='" + oldStatus + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", timestamp=" + timestamp +
                ", changedBy='" + changedBy + '\'' +
                ", changeType=" + changeType +
                '}';
    }
}
