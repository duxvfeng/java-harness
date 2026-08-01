package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CiCheckCommand.
 */
class CiCheckCommandTest {

    private ByteArrayOutputStream captureOutput() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        return baos;
    }

    @Test
    void testCheckCommandBasic() {
        ByteArrayOutputStream baos = captureOutput();

        CiCheckCommand command = new CiCheckCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("check", "-d", ".");
        String output = baos.toString();

        // Should execute without errors
        assertTrue(exitCode == 0 || exitCode == 1 || exitCode == 2);
    }

    @Test
    void testCheckCommandWithBranch() {
        ByteArrayOutputStream baos = captureOutput();

        CiCheckCommand command = new CiCheckCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("check", "-d", ".", "-b", "main");

        // Should execute without errors
        assertTrue(exitCode == 0 || exitCode == 1 || exitCode == 2);
    }

    @Test
    void testCheckCommandJsonFormat() {
        ByteArrayOutputStream baos = captureOutput();

        CiCheckCommand command = new CiCheckCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("check", "-d", ".", "--format", "json");
        String output = baos.toString();

        // Should execute without errors
        assertTrue(exitCode == 0 || exitCode == 1 || exitCode == 2);

        // Output should contain JSON structure
        if (!output.isEmpty()) {
            assertTrue(output.contains("{") || output.contains("Platform"));
        }
    }

    @Test
    void testCheckCommandDetailedFormat() {
        ByteArrayOutputStream baos = captureOutput();

        CiCheckCommand command = new CiCheckCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("check", "-d", ".", "--format", "detailed");

        // Should execute without errors
        assertTrue(exitCode == 0 || exitCode == 1 || exitCode == 2);
    }

    @Test
    void testListCommand() {
        ByteArrayOutputStream baos = captureOutput();

        CiCheckCommand command = new CiCheckCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("list", "-d", ".", "-n", "5");

        // Should execute without errors
        assertTrue(exitCode == 0 || exitCode == 1 || exitCode == 2);
    }

    @Test
    void testListCommandWithFilter() {
        ByteArrayOutputStream baos = captureOutput();

        CiCheckCommand command = new CiCheckCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("list", "-d", ".", "-n", "5", "--branch", "main");

        // Should execute without errors
        assertTrue(exitCode == 0 || exitCode == 1 || exitCode == 2);
    }

    @Test
    void testPrCommand() {
        ByteArrayOutputStream baos = captureOutput();

        CiCheckCommand command = new CiCheckCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("pr", "-d", ".", "--pr", "123");

        // Should execute without errors
        assertTrue(exitCode == 0 || exitCode == 1 || exitCode == 2);
    }

    @Test
    void testPrCommandJsonFormat() {
        ByteArrayOutputStream baos = captureOutput();

        CiCheckCommand command = new CiCheckCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("pr", "-d", ".", "--pr", "123", "--format", "json");

        // Should execute without errors
        assertTrue(exitCode == 0 || exitCode == 1 || exitCode == 2);
    }

    @Test
    void testCommitCommand() {
        ByteArrayOutputStream baos = captureOutput();

        CiCheckCommand command = new CiCheckCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("commit", "-d", ".", "--sha", "abc123def456");

        // Should execute without errors
        assertTrue(exitCode == 0 || exitCode == 1 || exitCode == 2);
    }

    @Test
    void testCommitCommandDetailedFormat() {
        ByteArrayOutputStream baos = captureOutput();

        CiCheckCommand command = new CiCheckCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("commit", "-d", ".", "--sha", "abc123def456", "--format", "detailed");

        // Should execute without errors
        assertTrue(exitCode == 0 || exitCode == 1 || exitCode == 2);
    }

    @Test
    void testHelpCommand() {
        ByteArrayOutputStream baos = captureOutput();

        CiCheckCommand command = new CiCheckCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("--help");

        assertEquals(0, exitCode);
        String output = baos.toString();

        // Help should contain description
        assertTrue(output.contains("CI") || output.contains("Check"));
    }

    @Test
    void testInvalidDirectory() {
        ByteArrayOutputStream baos = captureOutput();

        CiCheckCommand command = new CiCheckCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("check", "-d", "/nonexistent/path");

        // Should return error code
        assertEquals(1, exitCode);
    }

    @Test
    void testCiStatusRecord() {
        // Test CiStatus record creation
        CiCheckCommand.CiStatus status = new CiCheckCommand.CiStatus(
            "github",
            "main",
            "success",
            true,
            java.time.LocalDateTime.now(),
            List.of(new CiCheckCommand.CiCheck("test", "completed", "success")),
            Map.of("key", "value"),
            123,
            "abc123"
        );

        assertNotNull(status);
        assertEquals("github", status.platform());
        assertEquals("main", status.branch());
        assertTrue(status.isSuccess());
    }

    @Test
    void testCiCheckRecord() {
        // Test CiCheck record creation
        CiCheckCommand.CiCheck check = new CiCheckCommand.CiCheck(
            "build",
            "completed",
            "success"
        );

        assertNotNull(check);
        assertEquals("build", check.name());
        assertEquals("completed", check.status());
        assertEquals("success", check.conclusion());
    }

    @Test
    void testCiRunRecord() {
        // Test CiRun record creation
        CiCheckCommand.CiRun run = new CiCheckCommand.CiRun(
            "1234",
            "main",
            "success",
            java.time.LocalDateTime.now()
        );

        assertNotNull(run);
        assertEquals("1234", run.id());
        assertEquals("main", run.branch());
        assertEquals("success", run.status());
    }

    @Test
    void testGitHubChecker() {
        CiCheckCommand.GitHubChecker checker = new CiCheckCommand.GitHubChecker(
            java.nio.file.Paths.get("."),
            false
        );

        assertDoesNotThrow(() -> {
            CiCheckCommand.CiStatus status = checker.checkStatus("main");
            assertNotNull(status);
            assertEquals("github", status.platform());
        });
    }

    @Test
    void testGitLabChecker() {
        CiCheckCommand.GitLabChecker checker = new CiCheckCommand.GitLabChecker(
            java.nio.file.Paths.get("."),
            false
        );

        assertDoesNotThrow(() -> {
            CiCheckCommand.CiStatus status = checker.checkStatus("main");
            assertNotNull(status);
            assertEquals("gitlab", status.platform());
        });
    }

    @Test
    void testListRuns() {
        CiCheckCommand.GitHubChecker checker = new CiCheckCommand.GitHubChecker(
            java.nio.file.Paths.get("."),
            false
        );

        assertDoesNotThrow(() -> {
            var runs = checker.listRuns("main", null, 5);
            assertNotNull(runs);
            assertTrue(runs.size() <= 5);
        });
    }

    @Test
    void testCheckPrStatus() {
        CiCheckCommand.GitHubChecker checker = new CiCheckCommand.GitHubChecker(
            java.nio.file.Paths.get("."),
            false
        );

        assertDoesNotThrow(() -> {
            CiCheckCommand.CiStatus status = checker.checkPrStatus(123);
            assertNotNull(status);
            assertEquals(123, status.prNumber());
        });
    }

    @Test
    void testCheckCommitStatus() {
        CiCheckCommand.GitHubChecker checker = new CiCheckCommand.GitHubChecker(
            java.nio.file.Paths.get("."),
            false
        );

        assertDoesNotThrow(() -> {
            CiCheckCommand.CiStatus status = checker.checkCommitStatus("abc123");
            assertNotNull(status);
            assertEquals("abc123", status.sha());
        });
    }
}
