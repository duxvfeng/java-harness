package com.chachamaru.harness.foundation.orchestration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrchestrationLedgerTest {

    @Test
    void appendsAndReadsJsonlEntries(@TempDir Path tempDir) throws Exception {
        Path ledger = tempDir.resolve("orchestration-ledger.jsonl");
        OrchestrationLedger.Entry entry = new OrchestrationLedger.Entry(
            Instant.parse("2026-08-12T03:00:00Z"), "codex", "companion-result",
            true, 0, 250L, "session-1", true);

        OrchestrationLedger.append(ledger, entry);
        OrchestrationLedger.append(ledger, entry);
        List<OrchestrationLedger.Entry> entries = OrchestrationLedger.read(ledger);

        assertEquals(2, entries.size());
        assertEquals("companion-result", entries.get(0).subcommand());
        assertTrue(Files.readString(ledger).endsWith(System.lineSeparator()));
    }
}
