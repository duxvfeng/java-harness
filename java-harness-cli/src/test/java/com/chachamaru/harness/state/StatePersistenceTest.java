package com.chachamaru.harness.state;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

public class StatePersistenceTest {
    @Test
    void testSaveSessionState() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        StatePersistence persistence = new StatePersistence(tempDir.toString());

        SessionState state = new SessionState();
        state.setAttribute("cwd", "/test/path");

        persistence.saveSessionState(state);

        Path sessionFile = tempDir.resolve(".claude/state/session.jsonl");
        assertTrue(Files.exists(sessionFile));

        deleteDirectory(tempDir);
    }

    @Test
    void testLoadLatestSessionState() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        StatePersistence persistence = new StatePersistence(tempDir.toString());

        SessionState original = new SessionState();
        original.setAttribute("backend", "codex");

        persistence.saveSessionState(original);

        SessionState loaded = persistence.loadLatestSessionState();
        assertNotNull(loaded);
        assertEquals("codex", loaded.getAttribute("backend"));

        deleteDirectory(tempDir);
    }

    @Test
    void testSaveWorkState() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        StatePersistence persistence = new StatePersistence(tempDir.toString());

        WorkState state = new WorkState();
        state.setStatus(WorkState.Status.IN_PROGRESS);
        state.addWorkItem("task-1", "Test task", "TODO");

        persistence.saveWorkState(state);

        Path workFile = tempDir.resolve(".claude/state/work.jsonl");
        assertTrue(Files.exists(workFile));

        deleteDirectory(tempDir);
    }

    @Test
    void testLoadWorkStates() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        StatePersistence persistence = new StatePersistence(tempDir.toString());

        WorkState state1 = new WorkState();
        state1.setStatus(WorkState.Status.IN_PROGRESS);

        WorkState state2 = new WorkState();
        state2.setStatus(WorkState.Status.COMPLETED);

        persistence.saveWorkState(state1);
        persistence.saveWorkState(state2);

        List<WorkState> states = persistence.loadWorkStates(10);
        assertEquals(2, states.size());

        deleteDirectory(tempDir);
    }

    @Test
    void testGetStateFiles() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        StatePersistence persistence = new StatePersistence(tempDir.toString());

        Map<String, String> files = persistence.getStateFiles();

        assertNotNull(files);
        assertTrue(files.containsKey("session"));
        assertTrue(files.containsKey("work"));

        deleteDirectory(tempDir);
    }

    @Test
    void testCleanupOldStates() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        StatePersistence persistence = new StatePersistence(tempDir.toString());

        // Save multiple states
        for (int i = 0; i < 5; i++) {
            SessionState state = new SessionState();
            persistence.saveSessionState(state);
        }

        // Cleanup, keeping only last 2
        persistence.cleanupOldStates("session", 2);

        List<String> lines = JsonlWriter.readAll(
                tempDir.resolve(".claude/state/session.jsonl").toString()
        );

        assertEquals(2, lines.size());

        deleteDirectory(tempDir);
    }

    @Test
    void testInitializeStateDirectory() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        Path stateDir = tempDir.resolve(".claude/state");

        // Delete state directory if exists
        if (Files.exists(stateDir)) {
            deleteDirectory(stateDir);
        }

        StatePersistence persistence = new StatePersistence(tempDir.toString());
        persistence.initializeStateDirectory();

        assertTrue(Files.exists(stateDir));

        deleteDirectory(tempDir);
    }

    // Helper method to recursively delete directory
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> walk = Files.walk(directory)) {
                walk.map(Path::toFile)
                        .sorted((a, b) -> -b.compareTo(a)) // Reverse order to delete files before directories
                        .forEach(java.io.File::delete);
            }
        }
    }
}
