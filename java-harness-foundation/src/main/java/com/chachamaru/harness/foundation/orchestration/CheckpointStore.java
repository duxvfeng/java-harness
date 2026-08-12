package com.chachamaru.harness.foundation.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/** Atomic JSON persistence for resumable checkpoints. */
public final class CheckpointStore {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private CheckpointStore() {
    }

    public static void save(Path target, CheckpointV1 checkpoint) throws IOException {
        if (target == null || checkpoint == null) {
            throw new IllegalArgumentException("target and checkpoint are required");
        }
        Path absolute = target.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
        try {
            String json = MAPPER.writeValueAsString(checkpoint);
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static Optional<CheckpointV1> load(Path target) throws IOException {
        if (target == null || !Files.exists(target)) {
            return Optional.empty();
        }
        return Optional.of(MAPPER.readValue(Files.readString(target, StandardCharsets.UTF_8), CheckpointV1.class));
    }
}
