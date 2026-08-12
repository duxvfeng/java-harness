package com.chachamaru.harness.foundation.worktree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorktreeFingerprintTest {

    @Test
    void capturesAndDetectsChangedFiles(@TempDir Path tempDir) throws Exception {
        Path watched = tempDir.resolve("settings.json");
        Files.writeString(watched, "{\"deny\":[]}");

        WorktreeFingerprint.Snapshot before = WorktreeFingerprint.capture(List.of(watched));
        Files.writeString(watched, "{\"deny\":[\"Bash(rm -rf)\"]}");
        WorktreeFingerprint.Snapshot after = WorktreeFingerprint.capture(List.of(watched));

        List<String> changed = WorktreeFingerprint.diff(before, after);
        assertEquals(1, changed.size());
        assertTrue(changed.get(0).endsWith("/settings.json") || changed.get(0).equals("settings.json"));
    }

    @Test
    void omitsMissingFiles() throws Exception {
        WorktreeFingerprint.Snapshot snapshot = WorktreeFingerprint.capture(
            List.of(Path.of("C:/does-not-exist/java-harness-watch.json")));

        assertTrue(snapshot.files().isEmpty());
    }
}
