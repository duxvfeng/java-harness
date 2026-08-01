package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CiStatusCommand.
 */
class CiStatusCommandTest {

    private ByteArrayOutputStream captureOutput() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        return baos;
    }

    @Test
    void testShowCommandBasic() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("show", "-d", ".");

        // Should execute without errors
        assertEquals(0, exitCode);
    }

    @Test
    void testShowCommandWithBranch() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("show", "-d", ".", "-b", "main");

        assertEquals(0, exitCode);
    }

    @Test
    void testShowCommandJsonFormat() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("show", "-d", ".", "--format", "json");
        String output = baos.toString();

        assertEquals(0, exitCode);

        // Output should contain JSON structure
        assertTrue(output.contains("{") || output.contains("platform"));
    }

    @Test
    void testShowCommandCompactFormat() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("show", "-d", ".", "--compact");

        assertEquals(0, exitCode);
    }

    @Test
    void testShowCommandDetailedFormat() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("show", "-d", ".", "--format", "detailed");

        assertEquals(0, exitCode);
    }

    @Test
    void testQueryCommand() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("query", "-d", ".", "--limit", "5");

        assertEquals(0, exitCode);
    }

    @Test
    void testQueryCommandWithFilters() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("query", "-d", ".", "--branch", "main", "--status", "success");

        assertEquals(0, exitCode);
    }

    @Test
    void testQueryCommandCsvFormat() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("query", "-d", ".", "--format", "csv");
        String output = baos.toString();

        assertEquals(0, exitCode);

        // CSV output should contain header
        assertTrue(output.contains("platform") || output.contains("branch"));
    }

    @Test
    void testHistoryCommand() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("history", "-d", ".", "-n", "10");

        assertEquals(0, exitCode);
    }

    @Test
    void testHistoryCommandWithType() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("history", "-d", ".", "-n", "10", "--type", "failures");

        assertEquals(0, exitCode);
    }

    @Test
    void testHistoryCommandJsonFormat() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("history", "-d", ".", "--format", "json");

        assertEquals(0, exitCode);
    }

    @Test
    void testHelpCommand() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("--help");

        assertEquals(0, exitCode);
        String output = baos.toString();

        // Help should contain description
        assertTrue(output.contains("CI") || output.contains("Status"));
    }

    @Test
    void testCiStatusDetailRecord() {
        // Test CiStatusDetail record creation
        CiStatusCommand.CiStatusDetail detail = new CiStatusCommand.CiStatusDetail(
            "github",
            "main",
            "success",
            "active",
            java.time.LocalDateTime.now(),
            List.of(new CiStatusCommand.PipelineInfo("123", "Build", "success", 300, "http://example.com")),
            0,
            Map.of("key", "value")
        );

        assertNotNull(detail);
        assertEquals("github", detail.platform());
        assertEquals("main", detail.branch());
        assertEquals("success", detail.status());
    }

    @Test
    void testPipelineInfoRecord() {
        // Test PipelineInfo record creation
        CiStatusCommand.PipelineInfo pipeline = new CiStatusCommand.PipelineInfo(
            "123",
            "Build",
            "success",
            300,
            "http://example.com"
        );

        assertNotNull(pipeline);
        assertEquals("123", pipeline.id());
        assertEquals("Build", pipeline.name());
        assertEquals("success", pipeline.status());
        assertEquals(300, pipeline.duration());
    }

    @Test
    void testStatusHistoryEntryRecord() {
        // Test StatusHistoryEntry record creation
        CiStatusCommand.StatusHistoryEntry entry = new CiStatusCommand.StatusHistoryEntry(
            java.time.LocalDateTime.now(),
            "main",
            "success",
            "completed"
        );

        assertNotNull(entry);
        assertEquals("main", entry.branch());
        assertEquals("success", entry.status());
        assertEquals("completed", entry.changeType());
    }

    @Test
    void testGitHubStatusQuerier() {
        CiStatusCommand.GitHubStatusQuerier querier = new CiStatusCommand.GitHubStatusQuerier(
            java.nio.file.Paths.get("."),
            false
        );

        assertDoesNotThrow(() -> {
            CiStatusCommand.CiStatusDetail status = querier.getCurrentStatus("main");
            assertNotNull(status);
            assertEquals("github", status.platform());
        });
    }

    @Test
    void testGitLabStatusQuerier() {
        CiStatusCommand.GitLabStatusQuerier querier = new CiStatusCommand.GitLabStatusQuerier(
            java.nio.file.Paths.get("."),
            false
        );

        assertDoesNotThrow(() -> {
            CiStatusCommand.CiStatusDetail status = querier.getCurrentStatus("main");
            assertNotNull(status);
            assertEquals("gitlab", status.platform());
        });
    }

    @Test
    void testQueryStatus() {
        CiStatusCommand.GitHubStatusQuerier querier = new CiStatusCommand.GitHubStatusQuerier(
            java.nio.file.Paths.get("."),
            false
        );

        assertDoesNotThrow(() -> {
            var results = querier.queryStatus("main", "success", null, null, 5, "time");
            assertNotNull(results);
            assertTrue(results.size() <= 5);
        });
    }

    @Test
    void testGetHistory() {
        CiStatusCommand.GitHubStatusQuerier querier = new CiStatusCommand.GitHubStatusQuerier(
            java.nio.file.Paths.get("."),
            false
        );

        assertDoesNotThrow(() -> {
            var history = querier.getHistory("main", "all", 10);
            assertNotNull(history);
            assertTrue(history.size() <= 10);
        });
    }

    @Test
    void testGetHistoryWithFailuresFilter() {
        CiStatusCommand.GitHubStatusQuerier querier = new CiStatusCommand.GitHubStatusQuerier(
            java.nio.file.Paths.get("."),
            false
        );

        assertDoesNotThrow(() -> {
            var history = querier.getHistory("main", "failures", 10);
            assertNotNull(history);

            // All entries should be failures
            for (var entry : history) {
                assertTrue(entry.status().toLowerCase().contains("fail"));
            }
        });
    }

    @Test
    void testGetHistoryWithSuccessesFilter() {
        CiStatusCommand.GitHubStatusQuerier querier = new CiStatusCommand.GitHubStatusQuerier(
            java.nio.file.Paths.get("."),
            false
        );

        assertDoesNotThrow(() -> {
            var history = querier.getHistory("main", "successes", 10);
            assertNotNull(history);

            // All entries should be successes
            for (var entry : history) {
                assertEquals("success", entry.status());
            }
        });
    }

    @Test
    void testInvalidDirectory() {
        ByteArrayOutputStream baos = captureOutput();

        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("show", "-d", "/nonexistent/path");

        // Should return error code
        assertEquals(1, exitCode);
    }

    @Test
    void testPlatformDetection() {
        // Test that platform detection doesn't throw errors
        CiStatusCommand command = new CiStatusCommand();
        CommandLine cmd = new CommandLine(command);

        assertDoesNotThrow(() -> {
            cmd.execute("show", "-d", ".", "--platform", "github");
            cmd.execute("show", "-d", ".", "--platform", "gitlab");
            cmd.execute("show", "-d", ".", "--platform", "auto");
        });
    }

    @Test
    void testQueryWithSortBy() {
        CiStatusCommand.GitHubStatusQuerier querier = new CiStatusCommand.GitHubStatusQuerier(
            java.nio.file.Paths.get("."),
            false
        );

        assertDoesNotThrow(() -> {
            var results = querier.queryStatus("main", null, null, null, 5, "status");
            assertNotNull(results);
        });
    }
}
