package com.chachamaru.harness.protocol;

import com.chachamaru.harness.protocol.evidence.EvidenceFeed;
import com.chachamaru.harness.protocol.model.EvidenceRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvidenceFeedTest {

    @Test
    void readsOnlyThePreviousStageForHandoff(@TempDir Path projectRoot) throws Exception {
        EvidenceFeed feed = new EvidenceFeed(projectRoot);
        feed.append(sampleRecord());

        EvidenceFeed.Handoff handoff = feed.readHandoff("execution").orElseThrow();

        assertEquals("plan", handoff.fromStage());
        assertEquals("execution", handoff.toStage());
        assertEquals("Steps", handoff.understood());
        assertEquals("Execute steps", handoff.executable());
        assertEquals("Plan checked", handoff.verifiable());
        assertEquals(List.of("plan.md"), handoff.traceable());
    }

    @Test
    void returnsEmptyHandoffWhenFeedHasNoRecords(@TempDir Path projectRoot) throws Exception {
        EvidenceFeed feed = new EvidenceFeed(projectRoot);

        assertFalse(feed.readHandoff("scenario").isPresent());
    }

    @Test
    void rejectsUnknownOrFirstStageHandoff(@TempDir Path projectRoot) throws Exception {
        EvidenceFeed feed = new EvidenceFeed(projectRoot);
        feed.append(sampleRecord());

        assertThrows(IllegalArgumentException.class, () -> feed.readHandoff("unknown"));
        assertThrows(IllegalArgumentException.class, () -> feed.readHandoff("story"));
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
