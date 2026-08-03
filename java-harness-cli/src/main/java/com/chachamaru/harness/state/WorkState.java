package com.chachamaru.harness.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Work state management.
 * Tracks work session information including tasks, status, and progress.
 */
public class WorkState {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    private final String workId;
    private final Instant startTime;
    private Instant endTime;
    private Status status = Status.PENDING;
    private final Map<String, WorkItem> workItems = new HashMap<>();
    private String sessionId;
    private String backend;
    private String effort;

    /**
     * Work status enumeration.
     */
    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        BLOCKED
    }

    /**
     * Work item representation.
     */
    public static class WorkItem {
        private String id;
        private String description;
        private String status;
        private Instant created;
        private Instant updated;

        public WorkItem() {
            this.created = Instant.now();
            this.updated = Instant.now();
        }

        public WorkItem(String id, String description, String status) {
            this();
            this.id = id;
            this.description = description;
            this.status = status;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
            this.updated = Instant.now();
        }

        public Instant getCreated() {
            return created;
        }

        public Instant getUpdated() {
            return updated;
        }
    }

    public WorkState() {
        this.workId = UUID.randomUUID().toString();
        this.startTime = Instant.now();
    }

    /**
     * Get work ID.
     *
     * @return Unique work identifier
     */
    public String getWorkId() {
        return workId;
    }

    /**
     * Get work start time.
     *
     * @return Start timestamp
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Get work end time.
     *
     * @return End timestamp, or null if not completed
     */
    public Instant getEndTime() {
        return endTime;
    }

    /**
     * Get work status.
     *
     * @return Current status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Set work status.
     *
     * @param status New status
     */
    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.COMPLETED || status == Status.FAILED) {
            this.endTime = Instant.now();
        }
    }

    /**
     * Get session ID associated with this work.
     *
     * @return Session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Set session ID.
     *
     * @param sessionId Session ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Get backend type.
     *
     * @return Backend type (codex, cursor, etc.)
     */
    public String getBackend() {
        return backend;
    }

    /**
     * Set backend type.
     *
     * @param backend Backend type
     */
    public void setBackend(String backend) {
        this.backend = backend;
    }

    /**
     * Get effort level.
     *
     * @return Effort level (low, medium, high, max)
     */
    public String getEffort() {
        return effort;
    }

    /**
     * Set effort level.
     *
     * @param effort Effort level
     */
    public void setEffort(String effort) {
        this.effort = effort;
    }

    /**
     * Add a work item.
     *
     * @param id Item ID
     * @param description Item description
     * @param status Item status
     */
    public void addWorkItem(String id, String description, String status) {
        workItems.put(id, new WorkItem(id, description, status));
    }

    /**
     * Update work item status.
     *
     * @param id Item ID
     * @param status New status
     */
    public void updateWorkItemStatus(String id, String status) {
        WorkItem item = workItems.get(id);
        if (item != null) {
            item.setStatus(status);
        }
    }

    /**
     * Get all work items.
     *
     * @return Map of work items
     */
    public Map<String, WorkItem> getWorkItems() {
        return new HashMap<>(workItems);
    }

    /**
     * Get work duration in seconds.
     *
     * @return Duration in seconds
     */
    public long getDuration() {
        Instant end = endTime != null ? endTime : Instant.now();
        return ChronoUnit.SECONDS.between(startTime, end);
    }

    /**
     * Calculate work progress (0.0 to 1.0).
     *
     * @return Progress fraction
     */
    public double getProgress() {
        if (workItems.isEmpty()) {
            return 0.0;
        }

        long completed = workItems.values().stream()
                .filter(item -> "DONE".equalsIgnoreCase(item.getStatus()) ||
                               "COMPLETED".equalsIgnoreCase(item.getStatus()))
                .count();

        return (double) completed / workItems.size();
    }

    /**
     * Convert work state to JSON.
     *
     * @return JSON string representation
     */
    public String toJson() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("work_id", workId);
            data.put("start_time", startTime.toString());
            data.put("end_time", endTime != null ? endTime.toString() : null);
            data.put("status", status.toString());
            data.put("session_id", sessionId);
            data.put("backend", backend);
            data.put("effort", effort);
            data.put("duration_seconds", getDuration());
            data.put("progress", getProgress());

            // Convert work items to simple maps
            Map<String, Map<String, Object>> itemsData = new HashMap<>();
            for (WorkItem item : workItems.values()) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("id", item.getId());
                itemData.put("description", item.getDescription());
                itemData.put("status", item.getStatus());
                itemData.put("created", item.getCreated().toString());
                itemData.put("updated", item.getUpdated().toString());
                itemsData.put(item.getId(), itemData);
            }
            data.put("work_items", itemsData);

            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize work state", e);
        }
    }

    /**
     * Create WorkState from JSON.
     *
     * @param json JSON string
     * @return WorkState instance
     */
    @SuppressWarnings("unchecked")
    public static WorkState fromJson(String json) {
        try {
            Map<String, Object> data = mapper.readValue(json, Map.class);

            WorkState state = new WorkState();

            // Restore work ID and start time
            String originalWorkId = (String) data.get("work_id");
            if (originalWorkId != null) {
                var field = WorkState.class.getDeclaredField("workId");
                field.setAccessible(true);
                field.set(state, originalWorkId);
            }

            String startTimeStr = (String) data.get("start_time");
            if (startTimeStr != null) {
                Instant originalStartTime = Instant.parse(startTimeStr);
                var field = WorkState.class.getDeclaredField("startTime");
                field.setAccessible(true);
                field.set(state, originalStartTime);
            }

            // Restore status
            String statusStr = (String) data.get("status");
            if (statusStr != null) {
                state.status = Status.valueOf(statusStr);
            }

            // Restore other fields
            state.sessionId = (String) data.get("session_id");
            state.backend = (String) data.get("backend");
            state.effort = (String) data.get("effort");

            String endTimeStr = (String) data.get("end_time");
            if (endTimeStr != null) {
                state.endTime = Instant.parse(endTimeStr);
            }

            // Restore work items
            Map<String, Map<String, Object>> items = (Map<String, Map<String, Object>>) data.get("work_items");
            if (items != null) {
                for (Map.Entry<String, Map<String, Object>> entry : items.entrySet()) {
                    Map<String, Object> itemData = entry.getValue();
                    WorkItem item = new WorkItem();
                    item.id = entry.getKey();
                    item.description = (String) itemData.get("description");
                    item.status = (String) itemData.get("status");
                    state.workItems.put(entry.getKey(), item);
                }
            }

            return state;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize work state", e);
        }
    }

    @Override
    public String toString() {
        return "WorkState{" +
                "workId='" + workId + '\'' +
                ", status=" + status +
                ", progress=" + getProgress() +
                ", duration=" + getDuration() + "s" +
                '}';
    }
}
