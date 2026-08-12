package com.chachamaru.harness.cli.command.evidence;

import com.chachamaru.harness.cli.command.HarnessCLI;
import com.chachamaru.harness.protocol.JacksonHookCodec;
import com.chachamaru.harness.protocol.evidence.EvidenceFeed;
import com.chachamaru.harness.protocol.model.EvidenceRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectCommandIntegrationTest {

    private final ObjectMapper objectMapper = JacksonHookCodec.getObjectMapper();

    @Test
    void collectsEvidenceFromStdinIntoStateFile(@TempDir Path projectRoot) throws Exception {
        String originalUserDir = System.getProperty("user.dir");
        java.io.InputStream originalInput = System.in;
        try {
            System.setProperty("user.dir", projectRoot.toString());
            System.setIn(new ByteArrayInputStream(sampleJson().getBytes(StandardCharsets.UTF_8)));

            int exitCode = new CommandLine(new HarnessCLI()).execute("evidence", "collect");

            assertEquals(0, exitCode);
            assertEvidenceFileContains(projectRoot, sampleRecord());
        } finally {
            System.setProperty("user.dir", originalUserDir);
            System.setIn(originalInput);
        }
    }

    @Test
    void handsOffPreviousEvidenceStageWithoutReplayingFullRecord(@TempDir Path projectRoot) throws Exception {
        String originalUserDir = System.getProperty("user.dir");
        java.io.InputStream originalInput = System.in;
        try {
            System.setProperty("user.dir", projectRoot.toString());
            System.setIn(new ByteArrayInputStream(sampleJson().getBytes(StandardCharsets.UTF_8)));

            int exitCode = new CommandLine(new HarnessCLI()).execute("evidence", "collect");
            EvidenceFeed.Handoff handoff = new EvidenceFeed(projectRoot)
                .readHandoff("execution")
                .orElseThrow();

            assertEquals(0, exitCode);
            assertEquals("plan", handoff.fromStage());
            assertEquals("execution", handoff.toStage());
            assertEquals("Steps", handoff.understood());
            assertEquals(List.of("plan.md"), handoff.traceable());
        } finally {
            System.setProperty("user.dir", originalUserDir);
            System.setIn(originalInput);
        }
    }

    @Test
    void collectsEvidenceFromFileIntoStateFile(@TempDir Path projectRoot) throws Exception {
        String originalUserDir = System.getProperty("user.dir");
        java.io.InputStream originalInput = System.in;
        Path inputFile = projectRoot.resolve("evidence-input.json");
        Files.writeString(inputFile, sampleJson(), StandardCharsets.UTF_8);
        try {
            System.setProperty("user.dir", projectRoot.toString());

            int exitCode = new CommandLine(new HarnessCLI()).execute(
                "evidence", "collect", "--label", "integration", "--file", inputFile.toString());

            assertEquals(0, exitCode);
            assertEvidenceFileContains(projectRoot, sampleRecord());
        } finally {
            System.setProperty("user.dir", originalUserDir);
            System.setIn(originalInput);
        }
    }

    private void assertEvidenceFileContains(Path projectRoot, EvidenceRecord expected) throws Exception {
        Path evidenceFile = projectRoot.resolve(".claude/state/evidence.jsonl");
        assertTrue(Files.isRegularFile(evidenceFile));

        List<String> lines = Files.readAllLines(evidenceFile, StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertEquals(expected, objectMapper.readValue(lines.get(0), EvidenceRecord.class));
    }

    private String sampleJson() throws Exception {
        return objectMapper.writeValueAsString(sampleRecord());
    }

    private EvidenceRecord sampleRecord() {
        return EvidenceRecord.create(
            new EvidenceRecord.Story("User goal", "Run workflow", "Goal verified", List.of("story.md")),
            new EvidenceRecord.Scenario("Given input", "When run", "Then output", List.of("scenario.md")),
            new EvidenceRecord.Model("Context", "Apply model", "Model checked", List.of("model.md")),
            new EvidenceRecord.Plan("Steps", "Execute steps", "Plan checked", List.of("plan.md")),
            new EvidenceRecord.Execution("Run completed", "Command ran", "Exit zero", List.of("run.log")),
            new EvidenceRecord.Observation("Observed result", "Compare result", "Observation checked", List.of("observation.md")),
            new EvidenceRecord.Decision("Ship", "Approve", "Decision recorded", List.of("decision.md")),
            new EvidenceRecord.Probe("Open question", "Investigate", "Probe checked", List.of("probe.md"))
        );
    }
}
