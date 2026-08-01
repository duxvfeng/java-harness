package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for MemCommand.
 */
public class MemCommandTest {

    @Test
    public void testMemHealthCommand(@TempDir Path tempDir) throws Exception {
        MemCommand.HealthCommand command = new MemCommand.HealthCommand();
        command.jsonOutput = false;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        // Health check should return 0 (success) or 1 (unhealthy)
        assertTrue(exitCode == 0 || exitCode == 1);
    }

    @Test
    public void testMemHealthCommandJson(@TempDir Path tempDir) throws Exception {
        MemCommand.HealthCommand command = new MemCommand.HealthCommand();
        command.jsonOutput = true;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertTrue(exitCode == 0 || exitCode == 1);
    }

    @Test
    public void testMemStatusCommand(@TempDir Path tempDir) throws Exception {
        MemCommand.StatusCommand command = new MemCommand.StatusCommand();
        command.jsonOutput = false;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
    }

    @Test
    public void testMemStatusCommandJson(@TempDir Path tempDir) throws Exception {
        MemCommand.StatusCommand command = new MemCommand.StatusCommand();
        command.jsonOutput = true;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
    }

    @Test
    public void testMemSetupCommand(@TempDir Path tempDir) throws Exception {
        MemCommand.SetupCommand command = new MemCommand.SetupCommand();
        command.platform = null;
        command.skipQuality = false;
        command.autoUpdate = false;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
    }

    @Test
    public void testMemDoctorCommand(@TempDir Path tempDir) throws Exception {
        MemCommand.DoctorCommand command = new MemCommand.DoctorCommand();
        command.platform = null;
        command.autoFix = false;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
    }

    @Test
    public void testMemOffCommand(@TempDir Path tempDir) throws Exception {
        MemCommand.OffCommand command = new MemCommand.OffCommand();
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
    }

    @Test
    public void testMemPurgeCommandWithoutConfirmation(@TempDir Path tempDir) throws Exception {
        MemCommand.PurgeCommand command = new MemCommand.PurgeCommand();
        command.confirmPurge = false;
        command.platform = null;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        // Should fail without confirmation
        assertEquals(2, exitCode);
    }

    @Test
    public void testMemPurgeCommandWithConfirmation(@TempDir Path tempDir) throws Exception {
        MemCommand.PurgeCommand command = new MemCommand.PurgeCommand();
        command.confirmPurge = true;
        command.platform = null;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
    }

    @Test
    public void testMemRecordBreezingEventCommand(@TempDir Path tempDir) throws Exception {
        MemCommand.RecordBreezingEventCommand command = new MemCommand.RecordBreezingEventCommand();
        command.type = "brief-confirmed";
        command.project = "test-project";
        command.session = "test-session";
        command.content = "test content";
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
    }

    @Test
    public void testMemRecordBreezingEventCommandInvalidType(@TempDir Path tempDir) throws Exception {
        MemCommand.RecordBreezingEventCommand command = new MemCommand.RecordBreezingEventCommand();
        command.type = "invalid-type";
        command.project = "test-project";
        command.session = "test-session";
        command.content = "test content";
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertEquals(1, exitCode);
    }

    @Test
    public void testMemSearchSimilarCommand(@TempDir Path tempDir) throws Exception {
        MemCommand.SearchSimilarCommand command = new MemCommand.SearchSimilarCommand();
        command.project = "test-project";
        command.query = "test query";
        command.format = "json";
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertEquals(0, exitCode);
    }

    @Test
    public void testMemSearchSimilarCommandMissingParameters(@TempDir Path tempDir) throws Exception {
        MemCommand.SearchSimilarCommand command = new MemCommand.SearchSimilarCommand();
        command.project = null;
        command.query = null;
        command.format = "json";
        command.verbose = false;

        // Should fail-open with empty output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        Integer exitCode = command.call();

        System.setOut(System.out);
        assertNotNull(exitCode);
        assertEquals(0, exitCode);
        assertTrue(outputStream.toString().contains("[]"));
    }

    @Test
    public void testMemHealthChecker(@TempDir Path tempDir) throws Exception {
        MemCommand.MemHealthChecker checker = new MemCommand.MemHealthChecker(false);
        MemCommand.MemHealthOutput result = checker.checkHealth();

        assertNotNull(result);
        assertNotNull(result.reason());
    }

    @Test
    public void testMemStatusCollector(@TempDir Path tempDir) throws Exception {
        MemCommand.MemStatusCollector collector = new MemCommand.MemStatusCollector(false);
        MemCommand.MemStatusReport report = collector.collectStatus();

        assertNotNull(report);
        assertNotNull(report.status());
        assertNotNull(report.backendMode());
    }

    @Test
    public void testMainCommand(@TempDir Path tempDir) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        int exitCode = new CommandLine(new MemCommand()).execute("health", "--json");

        System.setOut(System.out);
        assertTrue(outputStream.toString().contains("healthy"));
    }
}
