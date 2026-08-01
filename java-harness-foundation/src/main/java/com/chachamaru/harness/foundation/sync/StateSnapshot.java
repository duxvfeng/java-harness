package com.chachamaru.harness.foundation.sync;

import java.util.Map;

/**
 * 状态快照
 * 表示某一时刻的状态数据
 */
public class StateSnapshot {

    private final Map<String, TaskState> taskStates;
    private final long timestamp;
    private final String sessionId;

    public StateSnapshot(Map<String, TaskState> taskStates, String sessionId) {
        this.taskStates = Map.copyOf(taskStates); // 不可变副本
        this.timestamp = System.currentTimeMillis();
        this.sessionId = sessionId;
    }

    public Map<String, TaskState> getTaskStates() {
        return taskStates;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    /**
     * 获取特定任务的状态
     */
    public TaskState getTaskState(String taskId) {
        return taskStates.get(taskId);
    }

    /**
     * 检查是否包含指定任务
     */
    public boolean containsTask(String taskId) {
        return taskStates.containsKey(taskId);
    }

    /**
     * 获取任务数量
     */
    public int getTaskCount() {
        return taskStates.size();
    }
}
