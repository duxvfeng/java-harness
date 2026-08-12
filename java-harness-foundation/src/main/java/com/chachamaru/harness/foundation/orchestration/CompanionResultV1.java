package com.chachamaru.harness.foundation.orchestration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Stable normalized result for one Codex or Cursor companion run. */
public record CompanionResultV1(
    String schema,
    String backend,
    @JsonProperty("task_id") String taskId,
    boolean success,
    @JsonProperty("exit_code") int exitCode,
    String summary,
    @JsonProperty("files_changed") List<String> filesChanged,
    @JsonProperty("duration_ms") long durationMs
) {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    public static final String SCHEMA = "companion-result.v1";
    private static final int SUMMARY_CAP = 200;

    @JsonCreator
    public CompanionResultV1 {
        if (!SCHEMA.equals(schema)) {
            throw new IllegalArgumentException("schema must be " + SCHEMA);
        }
        if (backend == null || backend.isBlank()) {
            throw new IllegalArgumentException("backend cannot be blank");
        }
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId cannot be blank");
        }
        summary = summary == null ? "" : summary;
        filesChanged = filesChanged == null ? List.of() : List.copyOf(filesChanged);
    }

    public static CompanionResultV1 normalize(String backend, String taskId, int exitCode,
                                               String stdout, String stderr, long durationMs) {
        String summary = firstNonEmptyLine(stdout);
        if (summary.isEmpty()) {
            summary = firstNonEmptyLine(stderr);
        }
        return new CompanionResultV1(
            SCHEMA,
            backend,
            taskId,
            exitCode == 0,
            exitCode,
            cap(summary),
            extractFilePaths(stdout),
            durationMs
        );
    }

    public static CompanionResultV1 parse(String json) throws IOException {
        CompanionResultV1 result = MAPPER.readValue(json, CompanionResultV1.class);
        if (!SCHEMA.equals(result.schema())) {
            throw new IOException("unexpected schema: " + result.schema());
        }
        return result;
    }

    public String toJson() throws IOException {
        return MAPPER.writeValueAsString(this);
    }

    private static String firstNonEmptyLine(String value) {
        if (value == null) {
            return "";
        }
        for (String line : value.split("\\R")) {
            if (!line.trim().isEmpty()) {
                return line.trim();
            }
        }
        return "";
    }

    private static String cap(String value) {
        return value.length() <= SUMMARY_CAP ? value : value.substring(0, SUMMARY_CAP) + "…";
    }

    private static List<String> extractFilePaths(String stdout) {
        Set<String> paths = new LinkedHashSet<>();
        if (stdout == null) {
            return List.of();
        }
        for (String line : stdout.split("\\R")) {
            String candidate = line.trim();
            if (candidate.isEmpty() || candidate.matches(".*\\s+.*") || !candidate.contains("/")) {
                continue;
            }
            int slash = candidate.lastIndexOf('/');
            int dot = candidate.lastIndexOf('.');
            if (slash >= 0 && dot > slash + 1 && dot < candidate.length() - 1) {
                paths.add(candidate);
            }
        }
        return new ArrayList<>(paths);
    }
}
