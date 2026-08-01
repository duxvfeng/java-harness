package com.chachamaru.harness.foundation.sync;

/**
 * 任务同步变更
 * 表示单个任务的变更信息
 */
public class TaskSyncChange {

    private final String taskId;
    private final ChangeType changeType;
    private final String oldValue;
    private final String newValue;
    private final long timestamp;

    public enum ChangeType {
        STATUS_CHANGED,
        TASK_ADDED,
        TASK_REMOVED,
        DESCRIPTION_CHANGED,
        DEPENDENCY_CHANGED
    }

    public TaskSyncChange(String taskId, ChangeType changeType, String oldValue, String newValue) {
        this.taskId = taskId;
        this.changeType = changeType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.timestamp = System.currentTimeMillis();
    }

    public String getTaskId() {
        return taskId;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "TaskSyncChange{" +
                "taskId='" + taskId + '\'' +
                ", changeType=" + changeType +
                ", oldValue='" + oldValue + '\'' +
                ", newValue='" + newValue + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
