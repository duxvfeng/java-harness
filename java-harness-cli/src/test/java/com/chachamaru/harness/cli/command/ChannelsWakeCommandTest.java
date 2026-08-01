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
 * Test suite for ChannelsWakeCommand.
 */
public class ChannelsWakeCommandTest {

    @Test
    public void testChannelsWakeCheckCommand(@TempDir Path tempDir) throws Exception {
        ChannelsWakeCommand.CheckCommand command = new ChannelsWakeCommand.CheckCommand();
        command.jsonOutput = false;
        command.eventDir = null;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertTrue(exitCode == 0 || exitCode == 1);
    }

    @Test
    public void testChannelsWakeCheckCommandJson(@TempDir Path tempDir) throws Exception {
        ChannelsWakeCommand.CheckCommand command = new ChannelsWakeCommand.CheckCommand();
        command.jsonOutput = true;
        command.eventDir = null;
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertTrue(exitCode == 0 || exitCode == 1);
    }

    @Test
    public void testChannelsWakeCheckCommandWithCustomDir(@TempDir Path tempDir) throws Exception {
        ChannelsWakeCommand.CheckCommand command = new ChannelsWakeCommand.CheckCommand();
        command.jsonOutput = false;
        command.eventDir = tempDir.toString();
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertTrue(exitCode == 0 || exitCode == 1);
    }

    @Test
    public void testChannelsWakeChecker(@TempDir Path tempDir) throws Exception {
        ChannelsWakeCommand.ChannelsWakeChecker checker =
            new ChannelsWakeCommand.ChannelsWakeChecker(false);
        ChannelsWakeCommand.ChannelsWakeResult result = checker.check(null);

        assertNotNull(result);
        assertNotNull(result.lastEvent());
        assertNotNull(result.events());
    }

    @Test
    public void testChannelsWakeCheckerWithCustomDir(@TempDir Path tempDir) throws Exception {
        ChannelsWakeCommand.ChannelsWakeChecker checker =
            new ChannelsWakeCommand.ChannelsWakeChecker(false);
        ChannelsWakeCommand.ChannelsWakeResult result = checker.check(tempDir.toString());

        assertNotNull(result);
        assertNotNull(result.lastEvent());
        assertNotNull(result.events());
        // Empty directory should have no events
        assertFalse(result.hasEvents());
        assertEquals(0, result.eventCount());
    }

    @Test
    public void testChannelWakeEventRecord(@TempDir Path tempDir) {
        LocalDateTime now = LocalDateTime.now();
        ChannelsWakeCommand.ChannelWakeEvent event = new ChannelsWakeCommand.ChannelWakeEvent(
            "test-id",
            "test-channel",
            "test-source",
            now,
            false
        );

        assertNotNull(event);
        assertEquals("test-id", event.id());
        assertEquals("test-channel", event.channel());
        assertEquals("test-source", event.source());
        assertEquals(now, event.timestamp());
        assertFalse(event.processed());
    }

    @Test
    public void testChannelsWakeResultRecord(@TempDir Path tempDir) {
        LocalDateTime now = LocalDateTime.now();
        ChannelsWakeCommand.ChannelsWakeResult result = new ChannelsWakeCommand.ChannelsWakeResult(
            true,
            5,
            now,
            java.util.List.of()
        );

        assertNotNull(result);
        assertTrue(result.hasEvents());
        assertEquals(5, result.eventCount());
        assertEquals(now, result.lastEvent());
        assertNotNull(result.events());
    }

    @Test
    public void testMainCommand() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        int exitCode = new CommandLine(new ChannelsWakeCommand()).execute("check", "--json");

        System.setOut(System.out);
        assertTrue(outputStream.toString().contains("has_events") ||
                  outputStream.toString().contains("event_count"));
    }

    @Test
    public void testDefaultBehavior() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        int exitCode = new CommandLine(new ChannelsWakeCommand()).execute();

        System.setOut(System.out);
        // Default behavior should show help
        assertTrue(outputStream.toString().contains("Usage") ||
                  outputStream.toString().contains("help"));
    }
}
