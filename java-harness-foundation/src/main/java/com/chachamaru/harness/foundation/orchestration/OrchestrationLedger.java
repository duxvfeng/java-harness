package com.chachamaru.harness.foundation.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Append-only orchestration visibility ledger using one JSON object per line. */
public final class OrchestrationLedger {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private OrchestrationLedger() {
    }

    public record Entry(
        Instant ts,
        String backend,
        String subcommand,
        boolean write,
        Integer exitCode,
        Long durationMs,
        String sessionId,
        boolean counts
    ) {
    }

    public static void append(Path ledger, Entry entry) throws IOException {
        if (ledger == null || entry == null) {
            throw new IllegalArgumentException("ledger and entry are required");
        }
        Path parent = ledger.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String line = MAPPER.writeValueAsString(entry) + System.lineSeparator();
        Files.writeString(ledger, line, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
    }

    public static List<Entry> read(Path ledger) throws IOException {
        if (ledger == null || !Files.exists(ledger)) {
            return List.of();
        }
        List<Entry> entries = new ArrayList<>();
        for (String line : Files.readAllLines(ledger, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                entries.add(MAPPER.readValue(line, Entry.class));
            }
        }
        return List.copyOf(entries);
    }
}
