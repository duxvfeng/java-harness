package com.chachamaru.harness.foundation.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Minimal resumable execution checkpoint shared by work, review and memory flows. */
public record CheckpointV1(
    String schema,
    @JsonProperty("checkpoint_id") String checkpointId,
    @JsonProperty("task_id") String taskId,
    String backend,
    String status,
    @JsonProperty("worker_output") String workerOutput,
    @JsonProperty("files_changed") List<String> filesChanged,
    Map<String, Object> state,
    @JsonProperty("created_at") Instant createdAt
) {
    public static final String SCHEMA = "checkpoint.v1";

    @JsonCreator
    public CheckpointV1 {
        if (!SCHEMA.equals(schema)) {
            throw new IllegalArgumentException("schema must be " + SCHEMA);
        }
        filesChanged = filesChanged == null ? List.of() : List.copyOf(filesChanged);
        state = state == null ? Map.of() : Map.copyOf(state);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static CheckpointV1 create(String taskId, String backend, String status,
                                      String workerOutput, List<String> filesChanged,
                                      Map<String, Object> state) {
        return new CheckpointV1(SCHEMA, UUID.randomUUID().toString(), taskId, backend,
            status, workerOutput, filesChanged, state, Instant.now());
    }
}
