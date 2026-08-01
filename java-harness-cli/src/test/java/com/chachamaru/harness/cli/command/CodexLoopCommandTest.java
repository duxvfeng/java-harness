package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CodexLoopCommand.
 */
class CodexLoopCommandTest {

    private ByteArrayOutputStream captureOutput() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        return baos;
    }

    @Test
    void testRunCommandBasic() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("run", "-d", ".", "-p", "Test prompt");

        assertEquals(0, exitCode);
    }

    @Test
    void testRunCommandWithIterations() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("run", "-d", ".", "-p", "Test prompt", "-i", "5");

        assertEquals(0, exitCode);
    }

    @Test
    void testRunCommandWithStrategy() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("run", "-d", ".", "-p", "Test prompt", "--strategy", "aggressive");

        assertEquals(0, exitCode);
    }

    @Test
    void testRunCommandDryRun() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("run", "-d", ".", "-p", "Test prompt", "--dry-run");

        assertEquals(0, exitCode);
        String output = baos.toString();
        assertTrue(output.contains("Dry Run") || output.contains("Would execute"));
    }

    @Test
    void testRunCommandWithoutPrompt() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("run", "-d", ".");

        // Should fail because prompt is required
        assertEquals(2, exitCode);
    }

    @Test
    void testStatusCommand() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("status", "-d", ".");

        assertEquals(0, exitCode);
    }

    @Test
    void testStatusCommandJsonFormat() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("status", "-d", ".", "--format", "json");

        assertEquals(0, exitCode);
    }

    @Test
    void testStatusCommandDetailedFormat() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("status", "-d", ".", "--format", "detailed");

        assertEquals(0, exitCode);
    }

    @Test
    void testStopCommand() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        // First run a loop
        cmd.execute("run", "-d", ".", "-p", "Test prompt", "-i", "3");

        // Then stop it
        baos = captureOutput();
        int exitCode = cmd.execute("stop", "-d", ".");

        assertEquals(0, exitCode);
    }

    @Test
    void testStopCommandForce() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("stop", "-d", ".", "--force");

        assertEquals(0, exitCode);
    }

    @Test
    void testResumeCommand() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        // First run and stop a loop
        cmd.execute("run", "-d", ".", "-p", "Test prompt", "-i", "3");
        cmd.execute("stop", "-d", ".");

        // Then resume it
        baos = captureOutput();
        int exitCode = cmd.execute("resume", "-d", ".");

        assertEquals(0, exitCode);
    }

    @Test
    void testResumeCommandWithAdditionalIterations() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("resume", "-d", ".", "--iterations", "5");

        assertEquals(0, exitCode);
    }

    @Test
    void testHelpCommand() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("--help");

        assertEquals(0, exitCode);
        String output = baos.toString();

        // Help should contain description
        assertTrue(output.contains("Codex") || output.contains("Loop"));
    }

    @Test
    void testLoopStateRecord() {
        CodexLoopCommand.LoopState state = new CodexLoopCommand.LoopState(
            "test-loop",
            "Test prompt",
            java.time.LocalDateTime.now(),
            1,
            10,
            "adaptive",
            "running",
            List.of(),
            Map.of("key", "value")
        );

        assertNotNull(state);
        assertEquals("test-loop", state.loopId());
        assertEquals("Test prompt", state.prompt());
        assertEquals("adaptive", state.strategy());
        assertEquals("running", state.status());
    }

    @Test
    void testIterationEntryRecord() {
        CodexLoopCommand.IterationEntry entry = new CodexLoopCommand.IterationEntry(
            1,
            java.time.LocalDateTime.now(),
            "success",
            0.95,
            "First iteration"
        );

        assertNotNull(entry);
        assertEquals(1, entry.iteration());
        assertEquals("success", entry.status());
        assertEquals(0.95, entry.score());
        assertEquals("First iteration", entry.description());
    }

    @Test
    void testLoopResultRecord() {
        CodexLoopCommand.LoopState state = new CodexLoopCommand.LoopState(
            "test-loop",
            "Test prompt",
            java.time.LocalDateTime.now(),
            5,
            10,
            "adaptive",
            "completed",
            List.of(),
            Map.of()
        );

        CodexLoopCommand.LoopResult result = new CodexLoopCommand.LoopResult(
            state,
            5,
            0.95,
            List.of()
        );

        assertNotNull(result);
        assertEquals(5, result.iterationsCompleted());
        assertEquals(0.95, result.convergenceScore());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void testCodexLoopExecutor() {
        CodexLoopCommand.CodexLoopExecutor executor =
            new CodexLoopCommand.CodexLoopExecutor(Paths.get("."), false);

        CodexLoopCommand.LoopState initialState = new CodexLoopCommand.LoopState(
            "test-loop",
            "Test prompt",
            java.time.LocalDateTime.now(),
            0,
            5,
            "adaptive",
            "running",
            List.of(),
            Map.of()
        );

        CodexLoopCommand.LoopResult result = executor.executeLoop(initialState, 300, 0.95);

        assertNotNull(result);
        assertTrue(result.iterationsCompleted() > 0);
        assertNotNull(result.finalState());
    }

    @Test
    void testCodexLoopExecutorWithHighThreshold() {
        CodexLoopCommand.CodexLoopExecutor executor =
            new CodexLoopCommand.CodexLoopExecutor(Paths.get("."), false);

        CodexLoopCommand.LoopState initialState = new CodexLoopCommand.LoopState(
            "test-loop",
            "Test prompt",
            java.time.LocalDateTime.now(),
            0,
            3,
            "adaptive",
            "running",
            List.of(),
            Map.of()
        );

        // High threshold - may not converge
        CodexLoopCommand.LoopResult result = executor.executeLoop(initialState, 300, 0.99);

        assertNotNull(result);
        assertEquals(3, result.iterationsCompleted()); // Should complete max iterations
    }

    @Test
    void testSaveAndLoadState() throws Exception {
        Path tempDir = Files.createTempDirectory("codex-loop-test");
        Path stateFile = tempDir.resolve("test-state.json");

        CodexLoopCommand.CodexLoopExecutor executor =
            new CodexLoopCommand.CodexLoopExecutor(tempDir, false);

        CodexLoopCommand.LoopState state = new CodexLoopCommand.LoopState(
            "test-loop",
            "Test prompt",
            java.time.LocalDateTime.now(),
            3,
            10,
            "adaptive",
            "running",
            List.of(),
            Map.of()
        );

        executor.saveState(state, stateFile);
        assertTrue(Files.exists(stateFile));

        CodexLoopCommand.LoopState loaded = executor.loadState(stateFile);
        assertNotNull(loaded);

        // Cleanup
        Files.deleteIfExists(stateFile);
        try {
            Files.deleteIfExists(tempDir);
        } catch (IOException e) {
            // Directory not empty, cleanup recursively
            Files.walk(tempDir)
                 .sorted((a, b) -> -a.compareTo(b))
                 .forEach(p -> {
                     try {
                         Files.deleteIfExists(p);
                     } catch (IOException ignored) {
                     }
                 });
        }
    }

    @Test
    void testInvalidDirectory() {
        ByteArrayOutputStream baos = captureOutput();

        CodexLoopCommand command = new CodexLoopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("run", "-d", "/nonexistent/path", "-p", "Test");

        assertEquals(1, exitCode);
    }

    @Test
    void testConvergenceThreshold() {
        CodexLoopCommand.CodexLoopExecutor executor =
            new CodexLoopCommand.CodexLoopExecutor(Paths.get("."), false);

        CodexLoopCommand.LoopState initialState = new CodexLoopCommand.LoopState(
            "test-loop",
            "Test prompt",
            java.time.LocalDateTime.now(),
            0,
            10,
            "adaptive",
            "running",
            List.of(),
            Map.of()
        );

        // Low threshold - should converge quickly
        CodexLoopCommand.LoopResult result = executor.executeLoop(initialState, 300, 0.5);

        assertNotNull(result);
        assertTrue(result.convergenceScore() >= 0.5);
    }

    @Test
    void testMaxIterations() {
        CodexLoopCommand.CodexLoopExecutor executor =
            new CodexLoopCommand.CodexLoopExecutor(Paths.get("."), false);

        CodexLoopCommand.LoopState initialState = new CodexLoopCommand.LoopState(
            "test-loop",
            "Test prompt",
            java.time.LocalDateTime.now(),
            0,
            2,
            "adaptive",
            "running",
            List.of(),
            Map.of()
        );

        CodexLoopCommand.LoopResult result = executor.executeLoop(initialState, 300, 0.99);

        assertNotNull(result);
        assertEquals(2, result.iterationsCompleted());
    }
}
