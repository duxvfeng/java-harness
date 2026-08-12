package com.chachamaru.harness.foundation.orchestration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckpointStoreTest {

    @Test
    void savesAndRestoresCheckpointAtomically(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("checkpoint.json");
        CheckpointV1 checkpoint = CheckpointV1.create(
            "13.3", "codex", "in_progress", "worker output",
            List.of("src/App.java"), Map.of("iteration", 2));

        CheckpointStore.save(file, checkpoint);
        CheckpointV1 restored = CheckpointStore.load(file).orElseThrow();

        assertEquals("checkpoint.v1", restored.schema());
        assertEquals("13.3", restored.taskId());
        assertEquals(2, restored.state().get("iteration"));
        assertEquals(checkpoint.checkpointId(), restored.checkpointId());
    }
}
