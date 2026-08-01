package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for BreezingSignalCommand.
 */
class BreezingSignalCommandTest {

    private ByteArrayOutputStream captureOutput() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        return baos;
    }

    @Test
    void testSendCommandBasic() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("send", "-d", ".", "--type", "status");

        assertEquals(0, exitCode);
    }

    @Test
    void testSendCommandWithTarget() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("send", "-d", ".", "--type", "start", "--target", "worker");

        assertEquals(0, exitCode);
    }

    @Test
    void testSendCommandWithWait() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("send", "-d", ".", "--type", "status", "--wait");

        assertEquals(0, exitCode);
        String output = baos.toString();
        assertTrue(output.contains("Response") || output.contains("delivered"));
    }

    @Test
    void testSendCommandWithPriority() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("send", "-d", ".", "--type", "interrupt", "--priority", "urgent");

        assertEquals(0, exitCode);
    }

    @Test
    void testSendCommandWithoutType() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("send", "-d", ".");

        // Should fail because type is required
        assertEquals(2, exitCode);
    }

    @Test
    void testReceiveCommand() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("receive", "-d", ".", "--count", "3");

        assertEquals(0, exitCode);
    }

    @Test
    void testReceiveCommandWithComponent() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("receive", "-d", ".", "--component", "lead");

        assertEquals(0, exitCode);
    }

    @Test
    void testReceiveCommandJsonFormat() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("receive", "-d", ".", "--format", "json");

        assertEquals(0, exitCode);
    }

    @Test
    void testStatusCommand() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("status", "-d", ".");

        assertEquals(0, exitCode);
    }

    @Test
    void testStatusCommandWithFilter() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("status", "-d", ".", "--component", "worker");

        assertEquals(0, exitCode);
    }

    @Test
    void testSyncCommand() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("sync", "-d", ".");

        assertEquals(0, exitCode);
    }

    @Test
    void testSyncCommandWithForce() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("sync", "-d", ".", "--force");

        assertEquals(0, exitCode);
    }

    @Test
    void testSyncCommandDryRun() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("sync", "-d", ".", "--dry-run");

        assertEquals(0, exitCode);
        String output = baos.toString();
        assertTrue(output.contains("Dry run"));
    }

    @Test
    void testHelpCommand() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("--help");

        assertEquals(0, exitCode);
        String output = baos.toString();

        // Help should contain description
        assertTrue(output.contains("Breezing") || output.contains("Signal"));
    }

    @Test
    void testSignalRecord() {
        BreezingSignalCommand.Signal signal = new BreezingSignalCommand.Signal(
            "test-signal",
            "start",
            "worker",
            "high",
            "Test message",
            java.time.LocalDateTime.now(),
            "pending"
        );

        assertNotNull(signal);
        assertEquals("test-signal", signal.signalId());
        assertEquals("start", signal.type());
        assertEquals("worker", signal.target());
    }

    @Test
    void testSignalResponseRecord() {
        BreezingSignalCommand.SignalResponse response = new BreezingSignalCommand.SignalResponse(
            "test-signal",
            "worker",
            "Response message",
            java.time.LocalDateTime.now()
        );

        assertNotNull(response);
        assertEquals("test-signal", response.signalId());
        assertEquals("worker", response.from());
    }

    @Test
    void testSignalResultRecord() {
        BreezingSignalCommand.SignalResult result = new BreezingSignalCommand.SignalResult(
            "delivered",
            null,
            List.of()
        );

        assertNotNull(result);
        assertEquals("delivered", result.status());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void testSignalStatusRecord() {
        BreezingSignalCommand.SignalStatus status = new BreezingSignalCommand.SignalStatus(
            "worker",
            "active",
            5,
            5,
            java.time.LocalDateTime.now()
        );

        assertNotNull(status);
        assertEquals("worker", status.component());
        assertEquals("active", status.state());
        assertEquals(5, status.signalsReceived());
    }

    @Test
    void testSyncResultRecord() {
        BreezingSignalCommand.SyncResult result = new BreezingSignalCommand.SyncResult(
            3,
            false,
            0,
            List.of()
        );

        assertNotNull(result);
        assertEquals(3, result.componentsToSync());
        assertFalse(result.hasConflicts());
    }

    @Test
    void testBreezingSignalHandler() {
        BreezingSignalCommand.BreezingSignalHandler handler =
            new BreezingSignalCommand.BreezingSignalHandler(java.nio.file.Paths.get("."), false);

        BreezingSignalCommand.Signal signal = new BreezingSignalCommand.Signal(
            "test-signal",
            "status",
            "all",
            "normal",
            "Test",
            java.time.LocalDateTime.now(),
            "pending"
        );

        BreezingSignalCommand.SignalResult result = handler.sendSignal(signal, false, 30);

        assertNotNull(result);
        assertEquals("delivered", result.status());
    }

    @Test
    void testReceiveSignals() {
        BreezingSignalCommand.BreezingSignalHandler handler =
            new BreezingSignalCommand.BreezingSignalHandler(java.nio.file.Paths.get("."), false);

        List<BreezingSignalCommand.Signal> signals = handler.receiveSignals("worker", 3, 0);

        assertNotNull(signals);
        assertEquals(3, signals.size());
    }

    @Test
    void testGetSignalStatus() {
        BreezingSignalCommand.BreezingSignalHandler handler =
            new BreezingSignalCommand.BreezingSignalHandler(java.nio.file.Paths.get("."), false);

        List<BreezingSignalCommand.SignalStatus> statuses = handler.getSignalStatus(null, null);

        assertNotNull(statuses);
        assertEquals(3, statuses.size()); // lead, worker, reviewer
    }

    @Test
    void testSynchronizeState() {
        BreezingSignalCommand.BreezingSignalHandler handler =
            new BreezingSignalCommand.BreezingSignalHandler(java.nio.file.Paths.get("."), false);

        BreezingSignalCommand.SyncResult result = handler.synchronizeState(false, false);

        assertNotNull(result);
        assertTrue(result.componentsToSync() > 0);
    }

    @Test
    void testSynchronizeStateWithForce() {
        BreezingSignalCommand.BreezingSignalHandler handler =
            new BreezingSignalCommand.BreezingSignalHandler(java.nio.file.Paths.get("."), false);

        BreezingSignalCommand.SyncResult result = handler.synchronizeState(true, false);

        assertNotNull(result);
        // With force, no conflicts should remain
        assertFalse(result.hasConflicts() || result.conflictCount() == 0);
    }

    @Test
    void testInvalidDirectory() {
        ByteArrayOutputStream baos = captureOutput();

        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("send", "-d", "/nonexistent/path", "--type", "status");

        assertEquals(1, exitCode);
    }

    @Test
    void testSendWithWaitAndTimeout() {
        BreezingSignalCommand command = new BreezingSignalCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("send", "-d", ".", "--type", "status", "--wait", "--timeout", "10");

        assertEquals(0, exitCode);
    }

    @Test
    void testSignalTypes() {
        String[] signalTypes = {"start", "stop", "pause", "resume", "status", "interrupt"};

        for (String type : signalTypes) {
            BreezingSignalCommand command = new BreezingSignalCommand();
            CommandLine cmd = new CommandLine(command);

            int exitCode = cmd.execute("send", "-d", ".", "--type", type);

            assertEquals(0, exitCode, "Failed for signal type: " + type);
        }
    }

    @Test
    void testSignalPriorities() {
        String[] priorities = {"low", "normal", "high", "urgent"};

        for (String priority : priorities) {
            BreezingSignalCommand command = new BreezingSignalCommand();
            CommandLine cmd = new CommandLine(command);

            int exitCode = cmd.execute("send", "-d", ".", "--type", "status", "--priority", priority);

            assertEquals(0, exitCode, "Failed for priority: " + priority);
        }
    }
}
