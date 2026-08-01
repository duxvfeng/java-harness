package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SubagentStartCommand.
 */
class SubagentStartCommandTest {

    private ByteArrayOutputStream captureOutput() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        return baos;
    }

    @Test
    void testStartCommandBasic() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStartCommand command = new SubagentStartCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("start", "-d", ".", "--type", "worker");

        assertEquals(0, exitCode);
    }

    @Test
    void testStartCommandWithName() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStartCommand command = new SubagentStartCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("start", "-d", ".", "--type", "worker", "--name", "my-worker");

        assertEquals(0, exitCode);
    }

    @Test
    void testStartCommandWithModel() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStartCommand command = new SubagentStartCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("start", "-d", ".", "--type", "reviewer", "--model", "claude-opus-4");

        assertEquals(0, exitCode);
    }

    @Test
    void testStartCommandDetached() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStartCommand command = new SubagentStartCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("start", "-d", ".", "--type", "worker", "--detached");

        assertEquals(0, exitCode);
    }

    @Test
    void testStartCommandWithoutType() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStartCommand command = new SubagentStartCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("start", "-d", ".");

        assertEquals(2, exitCode);
    }

    @Test
    void testListCommand() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStartCommand command = new SubagentStartCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("list", "-d", ".");

        assertEquals(0, exitCode);
    }

    @Test
    void testListCommandWithCustom() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStartCommand command = new SubagentStartCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("list", "-d", ".", "--include-custom");

        assertEquals(0, exitCode);
    }

    @Test
    void testHealthCommand() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStartCommand command = new SubagentStartCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("health", "-d", ".", "--all");

        assertEquals(0, exitCode);
    }

    @Test
    void testHealthCommandSpecificAgent() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStartCommand command = new SubagentStartCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("health", "-d", ".", "--agent", "worker-1");

        assertEquals(0, exitCode);
    }

    @Test
    void testHelpCommand() {
        ByteArrayOutputStream baos = captureOutput();

        SubagentStartCommand command = new SubagentStartCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("--help");

        assertEquals(0, exitCode);
        String output = baos.toString();
        assertTrue(output.contains("Subagent") || output.contains("Start"));
    }

    @Test
    void testAgentConfigRecord() {
        SubagentStartCommand.AgentConfig config = new SubagentStartCommand.AgentConfig(
            "worker",
            "worker-1",
            "claude-opus-4",
            "solo",
            "",
            "",
            "",
            Map.of("KEY", "VALUE"),
            false
        );

        assertNotNull(config);
        assertEquals("worker", config.agentType());
        assertEquals("worker-1", config.agentName());
        assertFalse(config.detached());
    }

    @Test
    void testAgentResultRecord() {
        SubagentStartCommand.AgentResult result = new SubagentStartCommand.AgentResult(
            "agent-123",
            "running",
            12345L,
            "/work/dir",
            List.of()
        );

        assertNotNull(result);
        assertEquals("agent-123", result.agentId());
        assertEquals("running", result.status());
        assertEquals(12345L, result.pid());
    }

    @Test
    void testBatchResultRecord() {
        SubagentStartCommand.BatchResult result = new SubagentStartCommand.BatchResult(
            3,
            2,
            1,
            List.of("failed-agent")
        );

        assertNotNull(result);
        assertEquals(3, result.totalCount());
        assertEquals(2, result.startedCount());
        assertEquals(1, result.failedCount());
    }

    @Test
    void testSubagentManager() {
        SubagentStartCommand.SubagentManager manager =
            new SubagentStartCommand.SubagentManager(java.nio.file.Paths.get("."), false);

        SubagentStartCommand.AgentConfig config = new SubagentStartCommand.AgentConfig(
            "worker",
            "test-worker",
            "default",
            "solo",
            "",
            "",
            "",
            Map.of(),
            false
        );

        SubagentStartCommand.AgentResult result = manager.startAgent(config);

        assertNotNull(result);
        assertEquals("running", result.status());
        assertTrue(result.pid() > 0);
    }

    @Test
    void testListAgentTypes() {
        SubagentStartCommand.SubagentManager manager =
            new SubagentStartCommand.SubagentManager(java.nio.file.Paths.get("."), false);

        List<SubagentStartCommand.AgentTypeInfo> types = manager.listAgentTypes(false);

        assertNotNull(types);
        assertTrue(types.size() >= 3); // worker, reviewer, advisor
    }

    @Test
    void testCheckHealth() {
        SubagentStartCommand.SubagentManager manager =
            new SubagentStartCommand.SubagentManager(java.nio.file.Paths.get("."), false);

        List<SubagentStartCommand.AgentHealth> health = manager.checkHealth(null, true);

        assertNotNull(health);
        assertTrue(health.size() >= 3);
    }
}
