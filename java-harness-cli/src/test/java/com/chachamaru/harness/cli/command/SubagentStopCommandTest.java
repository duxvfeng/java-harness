package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SubagentStopCommand.
 */
class SubagentStopCommandTest {

    private ByteArrayOutputStream captureOutput() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        return baos;
    }

    @Test
    void testStopCommandBasic() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("stop", "-d", ".", "--agent", "worker-1");

        assertEquals(0, exitCode);
    }

    @Test
    void testStopCommandForce() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("stop", "-d", ".", "--agent", "worker-1", "--force");

        assertEquals(0, exitCode);
    }

    @Test
    void testStopCommandWithWait() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("stop", "-d", ".", "--agent", "worker-1", "--wait");

        assertEquals(0, exitCode);
    }

    @Test
    void testStopCommandSaveState() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("stop", "-d", ".", "--agent", "worker-1", "--save-state");

        assertEquals(0, exitCode);
    }

    @Test
    void testStopCommandWithoutAgent() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("stop", "-d", ".");

        assertEquals(2, exitCode);
    }

    @Test
    void testStopAllCommand() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("stop-all", "-d", ".");

        assertEquals(0, exitCode);
    }

    @Test
    void testStopAllCommandWithExclude() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("stop-all", "-d", ".", "--exclude", "worker-1");

        assertEquals(0, exitCode);
    }

    @Test
    void testSignalCommand() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("signal", "-d", ".", "--agent", "worker-1");

        assertEquals(0, exitCode);
    }

    @Test
    void testSignalCommandWithReason() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("signal", "-d", ".", "--agent", "worker-1", "--reason", "Test signal");

        assertEquals(0, exitCode);
    }

    @Test
    void testStatusCommand() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("status", "-d", ".");

        assertEquals(0, exitCode);
    }

    @Test
    void testStatusCommandSpecificAgent() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("status", "-d", ".", "--agent", "worker-1");

        assertEquals(0, exitCode);
    }

    @Test
    void testHelpCommand() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("--help");

        assertEquals(0, exitCode);
        String output = baos.toString();
        assertTrue(output.contains("Subagent") || output.contains("Stop"));
    }

    @Test
    void testStopResultRecord() {
        SubagentStopCommand.StopResult result = new SubagentStopCommand.StopResult(
            "stopped",
            0,
            java.time.LocalDateTime.now(),
            "/state/file.json",
            List.of()
        );

        assertNotNull(result);
        assertEquals("stopped", result.status());
        assertEquals(0, result.exitCode());
    }

    @Test
    void testStopAllResultRecord() {
        SubagentStopCommand.StopAllResult result = new SubagentStopCommand.StopAllResult(
            3,
            2,
            1,
            0,
            List.of("failed-agent")
        );

        assertNotNull(result);
        assertEquals(3, result.totalCount());
        assertEquals(2, result.stoppedCount());
    }

    @Test
    void testSignalResultRecord() {
        SubagentStopCommand.SignalResult result = new SubagentStopCommand.SignalResult(
            "sent",
            12345L,
            List.of()
        );

        assertNotNull(result);
        assertEquals("sent", result.status());
        assertEquals(12345L, result.pid());
    }

    @Test
    void testAgentStopStatusRecord() {
        SubagentStopCommand.AgentStopStatus status = new SubagentStopCommand.AgentStopStatus(
            "worker-1",
            "stopped",
            0,
            java.time.LocalDateTime.now()
        );

        assertNotNull(status);
        assertEquals("worker-1", status.agentName());
        assertEquals("stopped", status.status());
    }

    @Test
    void testSubagentStopper() {
        SubagentStopCommand.SubagentStopper stopper =
            new SubagentStopCommand.SubagentStopper(java.nio.file.Paths.get("."), false);

        SubagentStopCommand.StopResult result = stopper.stopAgent("worker-1", false, 30, false, false);

        assertNotNull(result);
        assertEquals("stopped", result.status());
    }

    @Test
    void testStopAll() {
        SubagentStopCommand.SubagentStopper stopper =
            new SubagentStopCommand.SubagentStopper(java.nio.file.Paths.get("."), false);

        SubagentStopCommand.StopAllResult result = stopper.stopAll(false, 30, false, 500, java.util.Set.of());

        assertNotNull(result);
        assertTrue(result.totalCount() > 0);
    }

    @Test
    void testSendSignal() {
        SubagentStopCommand.SubagentStopper stopper =
            new SubagentStopCommand.SubagentStopper(java.nio.file.Paths.get("."), false);

        SubagentStopCommand.SignalResult result = stopper.sendSignal("worker-1", "SIGTERM", "Test");

        assertNotNull(result);
        assertEquals("sent", result.status());
        assertTrue(result.pid() > 0);
    }

    @Test
    void testGetStopStatus() {
        SubagentStopCommand.SubagentStopper stopper =
            new SubagentStopCommand.SubagentStopper(java.nio.file.Paths.get("."), false);

        List<SubagentStopCommand.AgentStopStatus> statuses = stopper.getStopStatus(null);

        assertNotNull(statuses);
        assertTrue(statuses.size() >= 3);
    }

    @Test
    void testInvalidDirectory() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStopCommand command = new SubagentStopCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("stop", "-d", "/nonexistent/path", "--agent", "worker-1");

        assertEquals(1, exitCode);
    }
}
