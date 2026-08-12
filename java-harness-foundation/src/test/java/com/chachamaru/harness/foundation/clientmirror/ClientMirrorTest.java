package com.chachamaru.harness.foundation.clientmirror;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ClientMirrorTest {

    @Test
    void reportsInSyncStateAndStableFingerprint() throws Exception {
        Path root = Files.createTempDirectory("mirror-in-sync");
        String body = "---\nname: demo\n---\n\n# Demo\n";
        writeSkill(root.resolve("skills/demo/SKILL.md"), body);
        writeSkill(root.resolve("codex/.codex/skills/demo/SKILL.md"), body);
        writeSkill(root.resolve("opencode/skills/demo/SKILL.md"), body);

        ClientMirror.State state = ClientMirror.scan(root, Instant.parse("2026-08-12T00:00:00Z"));

        assertTrue(state.healthy());
        assertEquals(ClientMirror.REASON_IN_SYNC, state.reason());
        assertTrue(state.fingerprint().matches("sha256:[a-f0-9]{64}"));
        assertEquals(state.fingerprint(), ClientMirror.scan(root, Instant.parse("2026-08-13T00:00:00Z")).fingerprint());
    }

    @Test
    void reportsDriftAndListsChangedMirror() throws Exception {
        Path root = Files.createTempDirectory("mirror-drift");
        writeSkill(root.resolve("skills/demo/SKILL.md"), "---\nname: demo\n---\n\n# SSOT\n");
        writeSkill(root.resolve("codex/.codex/skills/demo/SKILL.md"), "---\nname: demo\n---\n\n# Drift\n");

        ClientMirror.State state = ClientMirror.scan(root, Instant.now());

        assertFalse(state.healthy());
        assertEquals(ClientMirror.REASON_DRIFT, state.reason());
        assertTrue(ClientMirror.diff(root).stream().anyMatch(value -> value.contains("codex/.codex/skills/demo")));
    }

    @Test
    void treatsMissingMirrorRootAsNotConfigured() throws Exception {
        Path root = Files.createTempDirectory("mirror-missing");
        writeSkill(root.resolve("skills/demo/SKILL.md"), "# Demo\n");

        ClientMirror.State state = ClientMirror.scan(root, Instant.now());

        assertTrue(state.healthy());
        assertEquals(ClientMirror.REASON_NOT_CONFIGURED, state.mirrors().stream()
            .filter(entry -> entry.root().equals(".agents/skills"))
            .findFirst().orElseThrow().status());
    }

    private static void writeSkill(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
