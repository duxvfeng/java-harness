package com.chachamaru.harness.protocol.evidence;

import com.chachamaru.harness.protocol.JacksonHookCodec;
import com.chachamaru.harness.protocol.model.EvidenceRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Shared JSONL evidence feed used to hand off the previous stage's evidence.
 */
public final class EvidenceFeed {

    public static final String EVIDENCE_FILE_NAME = "evidence.jsonl";
    private static final List<String> STAGES = List.of(
        "story", "scenario", "model", "plan", "execution", "observation", "decision", "probe"
    );
    private static final ObjectMapper OBJECT_MAPPER = JacksonHookCodec.getObjectMapper();

    private final Path evidenceFile;

    public EvidenceFeed(Path projectRoot) {
        this.evidenceFile = projectRoot.resolve(".claude").resolve("state").resolve(EVIDENCE_FILE_NAME);
    }

    public Path evidenceFile() {
        return evidenceFile;
    }

    public void append(EvidenceRecord record) throws IOException {
        try {
            Files.createDirectories(evidenceFile.getParent());
            Files.writeString(
                evidenceFile,
                compactJson(record) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (JsonProcessingException exception) {
            throw new IOException("Unable to serialize evidence.v1 record", exception);
        }
    }

    public Optional<EvidenceRecord> latest() throws IOException {
        if (!Files.exists(evidenceFile)) {
            return Optional.empty();
        }
        EvidenceRecord latest = null;
        for (String line : Files.readAllLines(evidenceFile, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            try {
                latest = OBJECT_MAPPER.readValue(line, EvidenceRecord.class);
            } catch (JsonProcessingException exception) {
                throw new IOException("Evidence feed contains invalid evidence.v1 JSON", exception);
            }
        }
        return Optional.ofNullable(latest);
    }

    public Optional<Handoff> readHandoff(String toStage) throws IOException {
        int targetIndex = STAGES.indexOf(toStage);
        if (targetIndex < 0) {
            throw new IllegalArgumentException("Unknown evidence stage: " + toStage);
        }
        if (targetIndex == 0) {
            throw new IllegalArgumentException("Stage story has no previous evidence stage");
        }

        Optional<EvidenceRecord> latestRecord = latest();
        if (latestRecord.isEmpty()) {
            return Optional.empty();
        }
        String fromStage = STAGES.get(targetIndex - 1);
        StageValues values = valuesFor(latestRecord.orElseThrow(), fromStage);
        return Optional.of(new Handoff(
            fromStage,
            toStage,
            values.understood(),
            values.executable(),
            values.verifiable(),
            values.traceable()
        ));
    }

    private String compactJson(EvidenceRecord record) throws JsonProcessingException {
        return OBJECT_MAPPER.copy().disable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
            .writeValueAsString(record);
    }

    private StageValues valuesFor(EvidenceRecord record, String stage) {
        return switch (stage) {
            case "story" -> values(record.story());
            case "scenario" -> values(record.scenario());
            case "model" -> values(record.model());
            case "plan" -> values(record.plan());
            case "execution" -> values(record.execution());
            case "observation" -> values(record.observation());
            case "decision" -> values(record.decision());
            case "probe" -> values(record.probe());
            default -> throw new IllegalArgumentException("Unknown evidence stage: " + stage);
        };
    }

    private StageValues values(EvidenceRecord.Story stage) {
        return new StageValues(stage.understood(), stage.executable(), stage.verifiable(), stage.traceable());
    }

    private StageValues values(EvidenceRecord.Scenario stage) {
        return new StageValues(stage.understood(), stage.executable(), stage.verifiable(), stage.traceable());
    }

    private StageValues values(EvidenceRecord.Model stage) {
        return new StageValues(stage.understood(), stage.executable(), stage.verifiable(), stage.traceable());
    }

    private StageValues values(EvidenceRecord.Plan stage) {
        return new StageValues(stage.understood(), stage.executable(), stage.verifiable(), stage.traceable());
    }

    private StageValues values(EvidenceRecord.Execution stage) {
        return new StageValues(stage.understood(), stage.executable(), stage.verifiable(), stage.traceable());
    }

    private StageValues values(EvidenceRecord.Observation stage) {
        return new StageValues(stage.understood(), stage.executable(), stage.verifiable(), stage.traceable());
    }

    private StageValues values(EvidenceRecord.Decision stage) {
        return new StageValues(stage.understood(), stage.executable(), stage.verifiable(), stage.traceable());
    }

    private StageValues values(EvidenceRecord.Probe stage) {
        return new StageValues(stage.understood(), stage.executable(), stage.verifiable(), stage.traceable());
    }

    private record StageValues(String understood, String executable, String verifiable, List<String> traceable) {
    }

    public record Handoff(
        String fromStage,
        String toStage,
        String understood,
        String executable,
        String verifiable,
        List<String> traceable
    ) {
    }
}
