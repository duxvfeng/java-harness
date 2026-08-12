package com.chachamaru.harness.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.chachamaru.harness.protocol.model.EvidenceRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

class EvidenceRecordTest {

    private final ObjectMapper objectMapper = JacksonHookCodec.getObjectMapper();

    @Test
    void roundTripPreservesAllEvidenceStages() throws JsonProcessingException {
        EvidenceRecord original = sampleRecord();

        String json = objectMapper.writeValueAsString(original);
        EvidenceRecord decoded = objectMapper.readValue(json, EvidenceRecord.class);

        assertEquals(original, decoded);
        assertEquals("evidence.v1", decoded.schemaVersion());
        assertEquals("User goal", decoded.story().understood());
        assertEquals("Observation checked", decoded.observation().verifiable());
        assertEquals(List.of("probe-result.json"), decoded.probe().traceable());
    }

    @Test
    void serializesCanonicalStageFieldNames() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(sampleRecord());

        assertEquals(true, json.contains("\"schema_version\""));
        assertEquals(true, json.contains("\"story\""));
        assertEquals(true, json.contains("\"scenario\""));
        assertEquals(true, json.contains("\"model\""));
        assertEquals(true, json.contains("\"plan\""));
        assertEquals(true, json.contains("\"execution\""));
        assertEquals(true, json.contains("\"observation\""));
        assertEquals(true, json.contains("\"decision\""));
        assertEquals(true, json.contains("\"probe\""));
    }

    @Test
    void rejectsMissingTopLevelEvidenceStage() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(sampleRecord())
            .replaceFirst("(?s),\\s*\\\"probe\\\"\\s*:\\s*\\{.*", "}");

        assertThrows(JsonProcessingException.class, () ->
            objectMapper.readValue(json, EvidenceRecord.class));
    }

    @Test
    void rejectsMissingRequiredStageField() {
        String json = """
            {
              "schema_version": "evidence.v1",
              "story": {
                "executable": "Run workflow",
                "verifiable": "Goal verified",
                "traceable": ["story.md"]
              },
              "scenario": {"understood":"Given input", "executable":"When run", "verifiable":"Then output", "traceable":["scenario.md"]},
              "model": {"understood":"Context", "executable":"Apply model", "verifiable":"Model checked", "traceable":["model.md"]},
              "plan": {"understood":"Steps", "executable":"Execute steps", "verifiable":"Plan checked", "traceable":["plan.md"]},
              "execution": {"understood":"Run completed", "executable":"Command ran", "verifiable":"Exit zero", "traceable":["run.log"]},
              "observation": {"understood":"Observed result", "executable":"Compare result", "verifiable":"Observation checked", "traceable":["observation.md"]},
              "decision": {"understood":"Ship", "executable":"Approve", "verifiable":"Decision recorded", "traceable":["decision.md"]},
              "probe": {"understood":"Open question", "executable":"Investigate", "verifiable":"Probe checked", "traceable":["probe.md"]}
            }
            """;

        assertThrows(JsonProcessingException.class, () ->
            objectMapper.readValue(json, EvidenceRecord.class));
    }

    private static EvidenceRecord sampleRecord() {
        return EvidenceRecord.create(
            new EvidenceRecord.Story("User goal", "Run workflow", "Goal verified", List.of("story.md")),
            new EvidenceRecord.Scenario("Given input", "When run", "Then output", List.of("scenario.md")),
            new EvidenceRecord.Model("Context", "Apply model", "Model checked", List.of("model.md")),
            new EvidenceRecord.Plan("Steps", "Execute steps", "Plan checked", List.of("plan.md")),
            new EvidenceRecord.Execution("Run completed", "Command ran", "Exit zero", List.of("run.log")),
            new EvidenceRecord.Observation("Observed result", "Compare result", "Observation checked", List.of("observation.md")),
            new EvidenceRecord.Decision("Ship", "Approve", "Decision recorded", List.of("decision.md")),
            new EvidenceRecord.Probe("Open question", "Investigate", "Probe checked", List.of("probe-result.json"))
        );
    }
}