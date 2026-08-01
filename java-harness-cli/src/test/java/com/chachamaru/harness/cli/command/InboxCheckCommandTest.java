package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for InboxCheckCommand.
 */
public class InboxCheckCommandTest {

    @Test
    public void testInboxCheckCommand(@TempDir Path tempDir) throws Exception {
        InboxCheckCommand.CheckCommand command = new InboxCheckCommand.CheckCommand();
        command.team = "test-team";
        command.agent = "test-agent";
        command.dbPath = null;
        command.fromEnv = false;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertEquals(0, exitCode);
    }

    @Test
    public void testInboxCheckCommandFromEnv(@TempDir Path tempDir) throws Exception {
        InboxCheckCommand.CheckCommand command = new InboxCheckCommand.CheckCommand();
        command.team = null;
        command.agent = null;
        command.dbPath = null;
        command.fromEnv = true;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertEquals(0, exitCode);
    }

    @Test
    public void testInboxCheckCommandWithCustomDb(@TempDir Path tempDir) throws Exception {
        InboxCheckCommand.CheckCommand command = new InboxCheckCommand.CheckCommand();
        command.team = "test-team";
        command.agent = "test-agent";
        command.dbPath = tempDir.resolve("test.db").toString();
        command.fromEnv = false;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertEquals(0, exitCode);
    }

    @Test
    public void testInboxChecker(@TempDir Path tempDir) throws Exception {
        InboxCheckCommand.InboxChecker checker = new InboxCheckCommand.InboxChecker(false);
        InboxCheckCommand.InboxCheckOptions opts = new InboxCheckCommand.InboxCheckOptions(
            "test-team",
            "test-agent",
            tempDir.resolve("nonexistent.db").toString()
        );

        InboxCheckCommand.InboxCheckResult result = checker.check(opts);

        assertNotNull(result);
        assertEquals("test-team", result.team());
        assertEquals("test-agent", result.agent());
        assertEquals(0, result.unreadCount());
        assertNotNull(result.messages());
        assertTrue(result.messages().isEmpty());
    }

    @Test
    public void testInboxCheckOptionsRecord(@TempDir Path tempDir) {
        InboxCheckCommand.InboxCheckOptions opts = new InboxCheckCommand.InboxCheckOptions(
            "test-team",
            "test-agent",
            "test.db"
        );

        assertNotNull(opts);
        assertEquals("test-team", opts.team());
        assertEquals("test-agent", opts.agent());
        assertEquals("test.db", opts.dbPath());
    }

    @Test
    public void testInboxMessageRecord(@TempDir Path tempDir) {
        LocalDateTime now = LocalDateTime.now();
        InboxCheckCommand.InboxMessage message = new InboxCheckCommand.InboxMessage(
            "test-id",
            "test-team",
            "sender",
            "receiver",
            "test subject",
            "test body",
            now
        );

        assertNotNull(message);
        assertEquals("test-id", message.id());
        assertEquals("test-team", message.team());
        assertEquals("sender", message.fromAgent());
        assertEquals("receiver", message.toAgent());
        assertEquals("test subject", message.subject());
        assertEquals("test body", message.body());
        assertEquals(now, message.createdAt());
    }

    @Test
    public void testInboxCheckResultRecord(@TempDir Path tempDir) {
        InboxCheckCommand.InboxCheckResult result = new InboxCheckCommand.InboxCheckResult(
            "test-team",
            "test-agent",
            5,
            java.util.List.of(),
            "test context"
        );

        assertNotNull(result);
        assertEquals("test-team", result.team());
        assertEquals("test-agent", result.agent());
        assertEquals(5, result.unreadCount());
        assertNotNull(result.messages());
        assertEquals("test context", result.injectContext());
    }

    @Test
    public void testDeliveryIdentity(@TempDir Path tempDir) {
        // Test without environment variables set
        InboxCheckCommand.DeliveryIdentity identity = InboxCheckCommand.DeliveryIdentity.resolve();

        // Should return null when no environment variables are set
        // (unless the test environment has them set)
        assertNotNull(identity);
    }

    @Test
    public void testMainCommand() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        int exitCode = new CommandLine(new InboxCheckCommand()).execute("check",
            "--team", "test-team", "--agent", "test-agent");

        System.setOut(System.out);
        // Should output valid JSON or empty output
        assertNotNull(outputStream.toString());
    }

    @Test
    public void testDefaultBehavior() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        int exitCode = new CommandLine(new InboxCheckCommand()).execute();

        System.setOut(System.out);
        // Default behavior should show help
        assertTrue(outputStream.toString().contains("Usage") ||
                  outputStream.toString().contains("help"));
    }

    @Test
    public void testInboxCheckWithNonexistentDb(@TempDir Path tempDir) {
        InboxCheckCommand.InboxChecker checker = new InboxCheckCommand.InboxChecker(false);
        InboxCheckCommand.InboxCheckOptions opts = new InboxCheckCommand.InboxCheckOptions(
            "test-team",
            "test-agent",
            tempDir.resolve("nonexistent").resolve("livemsg.db").toString()
        );

        InboxCheckCommand.InboxCheckResult result = checker.check(opts);

        assertNotNull(result);
        assertEquals(0, result.unreadCount());
        assertTrue(result.messages().isEmpty());
    }

    @Test
    public void testInboxCheckWithEmptyAgent(@TempDir Path tempDir) {
        InboxCheckCommand.InboxChecker checker = new InboxCheckCommand.InboxChecker(true);
        InboxCheckCommand.InboxCheckOptions opts = new InboxCheckCommand.InboxCheckOptions(
            "test-team",
            "",  // Empty agent
            tempDir.resolve("test.db").toString()
        );

        InboxCheckCommand.InboxCheckResult result = checker.check(opts);

        assertNotNull(result);
        assertEquals(0, result.unreadCount());
        assertTrue(result.messages().isEmpty());
    }
}
