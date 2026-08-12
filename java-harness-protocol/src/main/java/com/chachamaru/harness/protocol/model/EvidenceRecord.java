package com.chachamaru.harness.protocol.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Structured evidence exchanged across the six-stage delivery loop.
 *
 * <p>The contract preserves the canonical sequence
 * Story → Scenario → Model → Plan → Execution → Observation → Decision → Probe.</p>
 */
public record EvidenceRecord(
    @JsonProperty(value = "schema_version", required = true)
    String schemaVersion,

    @JsonProperty(value = "story", required = true)
    Story story,

    @JsonProperty(value = "scenario", required = true)
    Scenario scenario,

    @JsonProperty(value = "model", required = true)
    Model model,

    @JsonProperty(value = "plan", required = true)
    Plan plan,

    @JsonProperty(value = "execution", required = true)
    Execution execution,

    @JsonProperty(value = "observation", required = true)
    Observation observation,

    @JsonProperty(value = "decision", required = true)
    Decision decision,

    @JsonProperty(value = "probe", required = true)
    Probe probe
) {
    public static final String SCHEMA_VERSION = "evidence.v1";

    public EvidenceRecord {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("schema_version must be " + SCHEMA_VERSION);
        }
        requireNonNull(story, "story");
        requireNonNull(scenario, "scenario");
        requireNonNull(model, "model");
        requireNonNull(plan, "plan");
        requireNonNull(execution, "execution");
        requireNonNull(observation, "observation");
        requireNonNull(decision, "decision");
        requireNonNull(probe, "probe");
    }

    public static EvidenceRecord create(
        Story story,
        Scenario scenario,
        Model model,
        Plan plan,
        Execution execution,
        Observation observation,
        Decision decision,
        Probe probe
    ) {
        return new EvidenceRecord(SCHEMA_VERSION, story, scenario, model, plan, execution, observation, decision, probe);
    }

    public record Story(
        @JsonProperty(value = "understood", required = true) String understood,
        @JsonProperty(value = "executable", required = true) String executable,
        @JsonProperty(value = "verifiable", required = true) String verifiable,
        @JsonProperty(value = "traceable", required = true) List<String> traceable
    ) {
        public Story {
            validateStage(understood, executable, verifiable, traceable, "story");
        }
    }

    public record Scenario(
        @JsonProperty(value = "understood", required = true) String understood,
        @JsonProperty(value = "executable", required = true) String executable,
        @JsonProperty(value = "verifiable", required = true) String verifiable,
        @JsonProperty(value = "traceable", required = true) List<String> traceable
    ) {
        public Scenario {
            validateStage(understood, executable, verifiable, traceable, "scenario");
        }
    }

    public record Model(
        @JsonProperty(value = "understood", required = true) String understood,
        @JsonProperty(value = "executable", required = true) String executable,
        @JsonProperty(value = "verifiable", required = true) String verifiable,
        @JsonProperty(value = "traceable", required = true) List<String> traceable
    ) {
        public Model {
            validateStage(understood, executable, verifiable, traceable, "model");
        }
    }

    public record Plan(
        @JsonProperty(value = "understood", required = true) String understood,
        @JsonProperty(value = "executable", required = true) String executable,
        @JsonProperty(value = "verifiable", required = true) String verifiable,
        @JsonProperty(value = "traceable", required = true) List<String> traceable
    ) {
        public Plan {
            validateStage(understood, executable, verifiable, traceable, "plan");
        }
    }

    public record Execution(
        @JsonProperty(value = "understood", required = true) String understood,
        @JsonProperty(value = "executable", required = true) String executable,
        @JsonProperty(value = "verifiable", required = true) String verifiable,
        @JsonProperty(value = "traceable", required = true) List<String> traceable
    ) {
        public Execution {
            validateStage(understood, executable, verifiable, traceable, "execution");
        }
    }

    public record Observation(
        @JsonProperty(value = "understood", required = true) String understood,
        @JsonProperty(value = "executable", required = true) String executable,
        @JsonProperty(value = "verifiable", required = true) String verifiable,
        @JsonProperty(value = "traceable", required = true) List<String> traceable
    ) {
        public Observation {
            validateStage(understood, executable, verifiable, traceable, "observation");
        }
    }

    public record Decision(
        @JsonProperty(value = "understood", required = true) String understood,
        @JsonProperty(value = "executable", required = true) String executable,
        @JsonProperty(value = "verifiable", required = true) String verifiable,
        @JsonProperty(value = "traceable", required = true) List<String> traceable
    ) {
        public Decision {
            validateStage(understood, executable, verifiable, traceable, "decision");
        }
    }

    public record Probe(
        @JsonProperty(value = "understood", required = true) String understood,
        @JsonProperty(value = "executable", required = true) String executable,
        @JsonProperty(value = "verifiable", required = true) String verifiable,
        @JsonProperty(value = "traceable", required = true) List<String> traceable
    ) {
        public Probe {
            validateStage(understood, executable, verifiable, traceable, "probe");
        }
    }

    private static void validateStage(
        String understood,
        String executable,
        String verifiable,
        List<String> traceable,
        String stageName
    ) {
        requireText(understood, stageName + ".understood");
        requireText(executable, stageName + ".executable");
        requireText(verifiable, stageName + ".verifiable");
        if (traceable == null || traceable.isEmpty() || traceable.stream().anyMatch(item -> item == null || item.isBlank())) {
            throw new IllegalArgumentException(stageName + ".traceable cannot be null, empty, or contain blank values");
        }
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
    }
}