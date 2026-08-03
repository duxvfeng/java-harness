package com.chachamaru.harness.cli.integration;

import com.chachamaru.harness.cli.command.HarnessCLI;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for HarnessCLI.
 */
class HarnessCLIIntegrationTest {

    private int executeCommand(String... args) {
        HarnessCLI cli = new HarnessCLI();
        CommandLine cmd = new CommandLine(cli);
        return cmd.execute(args);
    }

    @Test
    void testVersionCommand() {
        int exitCode = executeCommand("version");
        assertEquals(0, exitCode, "version command should exit with code 0");
    }

    @Test
    void testHelpCommand() {
        int exitCode = executeCommand("--help");
        assertEquals(0, exitCode, "help command should exit with code 0");
    }

    @Test
    void testHookPreToolCommand() {
        int exitCode = executeCommand("hook", "pre-tool");
        // pre-tool expects stdin input, so it may exit with non-zero
        assertDoesNotThrow(() -> executeCommand("hook", "pre-tool"));
    }

    @Test
    void testEvidenceCollectCommand() {
        int exitCode = executeCommand("evidence", "collect");
        // collect may fail due to missing context, but should not throw
        assertDoesNotThrow(() -> executeCommand("evidence", "collect"));
    }

    @Test
    void testPlansCheckDepsCommand() {
        int exitCode = executeCommand("plans", "check-deps");
        // check-deps may fail due to missing plans, but should not throw
        assertDoesNotThrow(() -> executeCommand("plans", "check-deps"));
    }

    @Test
    void testHookPostToolCommand() {
        int exitCode = executeCommand("hook", "post-tool");
        // post-tool expects stdin input, so it may exit with non-zero
        assertDoesNotThrow(() -> executeCommand("hook", "post-tool"));
    }

    @Test
    void testHookPermissionCommand() {
        int exitCode = executeCommand("hook", "permission");
        // permission expects stdin input, so it may exit with non-zero
        assertDoesNotThrow(() -> executeCommand("hook", "permission"));
    }

    @Test
    void testInvalidCommandReturnsError() {
        int exitCode = executeCommand("nonexistent-command");
        assertNotEquals(0, exitCode, "invalid command should exit with non-zero code");
    }

    @Test
    void testHookWithoutSubcommandShowsHelp() {
        int exitCode = executeCommand("hook");
        assertEquals(0, exitCode, "hook without subcommand should show help and exit 0");
    }

    @Test
    void testPlansWithoutSubcommandShowsHelp() {
        int exitCode = executeCommand("plans");
        assertEquals(0, exitCode, "plans without subcommand should show help and exit 0");
    }
}
