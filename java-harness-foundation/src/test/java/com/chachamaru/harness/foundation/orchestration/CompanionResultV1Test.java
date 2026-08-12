package com.chachamaru.harness.foundation.orchestration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompanionResultV1Test {

    @Test
    void normalizesOutputToStableSchema() throws Exception {
        CompanionResultV1 result = CompanionResultV1.normalize(
            "codex", "13.1", 0, "Updated\nsrc/main/App.java\nsrc/main/App.java", "", 120L);

        assertEquals("companion-result.v1", result.schema());
        assertTrue(result.success());
        assertEquals("Updated", result.summary());
        assertEquals(List.of("src/main/App.java"), result.filesChanged());
        assertEquals(result, CompanionResultV1.parse(result.toJson()));
    }

    @Test
    void fallsBackToStderrAndCapsSummary() {
        CompanionResultV1 result = CompanionResultV1.normalize(
            "cursor", "13.2", 1, "", "failure", 40L);

        assertFalse(result.success());
        assertEquals("failure", result.summary());
        assertEquals(1, result.exitCode());
    }
}
