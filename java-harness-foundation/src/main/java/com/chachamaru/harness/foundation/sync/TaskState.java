package com.chachamaru.harness.foundation.sync;

/**
 * 任务状态
 * 表示单个任务的状态信息
 */
public class TaskState {

    private final String taskId;
    private final String status;
    private final String description;
    private final long lastModified;
    private final String modifiedBy;

    public TaskState(String taskId, String status, String description) {
        this(taskId, status, description, System.currentTimeMillis(), null);
    }

    public TaskState(String taskId, String status, String description, long lastModified, String modifiedBy) {
        this.taskId = taskId;
        this.status = status;
        this.description = description;
        this.lastModified = lastModified;
        this.modifiedBy = modifiedBy;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    /**
     * 创建新的状态副本
     */
    public TaskState withStatus(String newStatus) {
        return new TaskState(this.taskId, newStatus, this.description,
                           System.currentTimeMillis(), this.modifiedBy);
    }

    /**
     * 检查状态是否为完成状态
     */
    public boolean isCompleted() {
        return "cc:completed".equalsIgnoreCase(status) ||
               "cc:done".equalsIgnoreCase(status) ||
               status.contains("✅");
    }

    /**
     * 检查状态为进行中
     */
    public boolean isInProgress() {
        return "cc:wip".equalsIgnoreCase(status) ||
               "cc:in-progress".equalsIgnoreCase(status) ||
               status.contains("🔄");
    }

    /**
     * 检查状态为待办
     */
    public boolean isPending() {
        return "cc:todo".equalsIgnoreCase(status) ||
               "cc:pending".equalsIgnoreCase(status) ||
               status.contains("📝");
    }

    @Override
    public String toString() {
        return "TaskState{" +
                "taskId='" + taskId + '\'' +
                ", status='" + status + '\'' +
                ", description='" + description + '\'' +
                ", lastModified=" + lastModified +
                ", modifiedBy='" + modifiedBy + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskState taskState = (TaskState) o;
        return taskId.equals(taskState.taskId) && status.equals(taskState.status);
    }

    @Override
    public int hashCode() {
        int result = taskId.hashCode();
        result = 31 * result + status.hashCode();
        return result;
    }
}
