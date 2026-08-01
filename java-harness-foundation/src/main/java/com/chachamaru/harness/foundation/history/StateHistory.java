package com.chachamaru.harness.foundation.history;

import java.util.ArrayList;
import java.util.List;

/**
 * 状态历史记录
 * 追踪状态变更的历史
 */
public class StateHistory {

    private final List<StateChangeEvent> events;
    private final long createdAt;
    private long lastModified;

    public StateHistory() {
        this.events = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.lastModified = this.createdAt;
    }

    public StateHistory(List<StateChangeEvent> events) {
        this.events = new ArrayList<>(events);
        this.createdAt = System.currentTimeMillis();
        this.lastModified = this.createdAt;
    }

    /**
     * 添加状态变更事件
     */
    public void addEvent(StateChangeEvent event) {
        events.add(event);
        this.lastModified = System.currentTimeMillis();
    }

    /**
     * 获取所有事件
     */
    public List<StateChangeEvent> getEvents() {
        return List.copyOf(events);
    }

    /**
     * 按任务ID过滤事件
     */
    public List<StateChangeEvent> getEventsByTask(String taskId) {
        return events.stream()
                .filter(e -> e.getTaskId().equals(taskId))
                .toList();
    }

    /**
     * 获取最近N个事件
     */
    public List<StateChangeEvent> getRecentEvents(int count) {
        int fromIndex = Math.max(0, events.size() - count);
        return List.copyOf(events.subList(fromIndex, events.size()));
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastModified() {
        return lastModified;
    }

    public int getEventCount() {
        return events.size();
    }
}
