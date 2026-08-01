package com.chachamaru.harness.workflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Task entity representing a single workflow task.
 *
 * <p>A Task contains all information needed for workflow execution,
 * including title, description, status, dependencies, and acceptance criteria.</p>
 *
 * @spec_reference spec.md#Data Models
 */
public record Task(
    @JsonProperty("id")
    String id,

    @JsonProperty("title")
    String title,

    @JsonProperty("description")
    String description,

    @JsonProperty("status")
    Status status,

    @JsonProperty("acceptance_criteria")
    String acceptanceCriteria,

    @JsonProperty("dependencies")
    List<String> dependencies,

    @JsonProperty("lane")
    String lane
) {
    /**
     * Creates a Task instance with validation.
     */
    public Task {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be null or blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title cannot be null or blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
    }

    /**
     * Creates a new task with TODO status.
     */
    public static Task createTodo(String id, String title, String description) {
        return new Task(
            id,
            title,
            description,
            Status.CC_TODO,
            null,
            List.of(),
            "implementation"
        );
    }

    /**
     * Creates a task with full specification.
     */
    public static Task create(String id, String title, String description, String acceptanceCriteria, List<String> dependencies, String lane) {
        return new Task(
            id,
            title,
            description,
            Status.CC_TODO,
            acceptanceCriteria,
            dependencies,
            lane
        );
    }

    /**
     * Checks if this task depends on another task.
     */
    public boolean dependsOn(String taskId) {
        List<String> deps = dependencies == null ? List.of() : dependencies;
        return deps.contains(taskId);
    }

    /**
     * Checks if all dependencies are satisfied.
     */
    public boolean areDependenciesSatisfied(List<Task> allTasks) {
        List<String> deps = dependencies == null ? List.of() : dependencies;
        return deps.stream().allMatch(depId ->
            allTasks.stream()
                .anyMatch(t -> t.id().equals(depId) && t.status().isCompleted())
        );
    }

    /**
     * Creates a copy with updated status.
     */
    public Task withStatus(Status newStatus) {
        return new Task(id, title, description, newStatus, acceptanceCriteria, dependencies, lane);
    }

    /**
     * Creates a copy with WIP status.
     */
    public Task markAsWip() {
        return withStatus(Status.CC_WIP);
    }

    /**
     * Creates a copy with DONE status.
     */
    public Task markAsDone() {
        return withStatus(Status.CC_DONE);
    }
}
