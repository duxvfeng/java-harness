package com.chachamaru.harness.workflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Plans.md document representation.
 *
 * <p>Contains all tasks from a Plans.md file with metadata.
 * This is the primary input for workflow orchestration.</p>
 *
 * @spec_reference spec.md#Data Models
 */
public record PlansDocument(
    @JsonProperty("title")
    String title,

    @JsonProperty("metadata")
    String metadata,

    @JsonProperty("last_modified")
    LocalDateTime lastModified,

    @JsonProperty("tasks")
    List<Task> tasks
) {
    /**
     * Creates a PlansDocument with current timestamp.
     */
    public PlansDocument {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title cannot be null or blank");
        }
        if (tasks == null) {
            tasks = List.of();
        }
        if (lastModified == null) {
            lastModified = LocalDateTime.now();
        }
    }

    /**
     * Creates an empty PlansDocument.
     */
    public static PlansDocument empty(String title) {
        return new PlansDocument(title, null, LocalDateTime.now(), List.of());
    }

    /**
     * Gets tasks by status.
     */
    public List<Task> getTasksByStatus(Status status) {
        return tasks.stream()
            .filter(t -> t.status() == status)
            .toList();
    }

    /**
     * Gets tasks by lane.
     */
    public List<Task> getTasksByLane(String lane) {
        return tasks.stream()
            .filter(t -> t.lane().equals(lane))
            .toList();
    }

    /**
     * Gets tasks ready to execute (TODO with satisfied dependencies).
     */
    public List<Task> getReadyTasks() {
        return tasks.stream()
            .filter(t -> t.status() == Status.CC_TODO)
            .filter(t -> t.areDependenciesSatisfied(tasks))
            .toList();
    }

    /**
     * Gets a task by ID.
     */
    public Task getTaskById(String id) {
        return tasks.stream()
            .filter(t -> t.id().equals(id))
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks if all tasks are completed.
     */
    public boolean isComplete() {
        return tasks.stream().allMatch(t -> t.status().isCompleted());
    }

    /**
     * Creates a copy with an updated task.
     */
    public PlansDocument withUpdatedTask(Task updatedTask) {
        List<Task> newTasks = tasks.stream()
            .map(t -> t.id().equals(updatedTask.id()) ? updatedTask : t)
            .toList();
        return new PlansDocument(title, metadata, lastModified, newTasks);
    }
}
