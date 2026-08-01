package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StatusCommand
 */
class StatusCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void testShowCommandInstantiation() {
        StatusCommand.ShowCommand showCommand = new StatusCommand.ShowCommand();
        assertNotNull(showCommand, "ShowCommand should be instantiated");
    }

    @Test
    void testListCommandInstantiation() {
        StatusCommand.ListCommand listCommand = new StatusCommand.ListCommand();
        assertNotNull(listCommand, "ListCommand should be instantiated");
    }

    @Test
    void testHistoryCommandInstantiation() {
        StatusCommand.HistoryCommand historyCommand = new StatusCommand.HistoryCommand();
        assertNotNull(historyCommand, "HistoryCommand should be instantiated");
    }

    @Test
    void testShowCommandBasicCall() throws Exception {
        StatusCommand.ShowCommand showCommand = new StatusCommand.ShowCommand();
        showCommand.projectDir = tempDir.toString();

        Integer result = showCommand.call();

        assertEquals(0, result, "Should return 0 for successful status show");
    }

    @Test
    void testShowCommandWithInvalidDirectory() throws Exception {
        StatusCommand.ShowCommand showCommand = new StatusCommand.ShowCommand();
        showCommand.projectDir = "/nonexistent/directory";

        Integer result = showCommand.call();

        assertEquals(1, result, "Should return 1 for invalid directory");
    }

    @Test
    void testShowCommandJsonOutput() throws Exception {
        StatusCommand.ShowCommand showCommand = new StatusCommand.ShowCommand();
        showCommand.projectDir = tempDir.toString();
        showCommand.jsonOutput = true;

        Integer result = showCommand.call();

        assertEquals(0, result, "Should return 0 for JSON output");
    }

    @Test
    void testShowCommandCompactOutput() throws Exception {
        StatusCommand.ShowCommand showCommand = new StatusCommand.ShowCommand();
        showCommand.projectDir = tempDir.toString();
        showCommand.compact = true;

        Integer result = showCommand.call();

        assertEquals(0, result, "Should return 0 for compact output");
    }

    @Test
    void testShowCommandWithAllAgents() throws Exception {
        StatusCommand.ShowCommand showCommand = new StatusCommand.ShowCommand();
        showCommand.projectDir = tempDir.toString();
        showCommand.showAll = true;

        Integer result = showCommand.call();

        assertEquals(0, result, "Should return 0 when showing all agents");
    }

    @Test
    void testListCommandBasicCall() throws Exception {
        StatusCommand.ListCommand listCommand = new StatusCommand.ListCommand();
        listCommand.projectDir = tempDir.toString();

        Integer result = listCommand.call();

        assertEquals(0, result, "Should return 0 for successful list");
    }

    @Test
    void testListCommandWithLongFormat() throws Exception {
        StatusCommand.ListCommand listCommand = new StatusCommand.ListCommand();
        listCommand.projectDir = tempDir.toString();
        listCommand.longFormat = true;

        Integer result = listCommand.call();

        assertEquals(0, result, "Should return 0 for long format");
    }

    @Test
    void testListCommandWithFilter() throws Exception {
        StatusCommand.ListCommand listCommand = new StatusCommand.ListCommand();
        listCommand.projectDir = tempDir.toString();
        listCommand.filter = "worker";

        Integer result = listCommand.call();

        assertEquals(0, result, "Should return 0 for filtered list");
    }

    @Test
    void testListCommandWithSort() throws Exception {
        StatusCommand.ListCommand listCommand = new StatusCommand.ListCommand();
        listCommand.projectDir = tempDir.toString();
        listCommand.sortBy = "name";

        Integer result = listCommand.call();

        assertEquals(0, result, "Should return 0 for sorted list");
    }

    @Test
    void testListCommandWithInvalidDirectory() throws Exception {
        StatusCommand.ListCommand listCommand = new StatusCommand.ListCommand();
        listCommand.projectDir = "/nonexistent/directory";

        Integer result = listCommand.call();

        assertEquals(1, result, "Should return 1 for invalid directory");
    }

    @Test
    void testHistoryCommandBasicCall() throws Exception {
        StatusCommand.HistoryCommand historyCommand = new StatusCommand.HistoryCommand();
        historyCommand.projectDir = tempDir.toString();

        Integer result = historyCommand.call();

        assertEquals(0, result, "Should return 0 for successful history");
    }

    @Test
    void testHistoryCommandWithCount() throws Exception {
        StatusCommand.HistoryCommand historyCommand = new StatusCommand.HistoryCommand();
        historyCommand.projectDir = tempDir.toString();
        historyCommand.count = 5;

        Integer result = historyCommand.call();

        assertEquals(0, result, "Should return 0 with count limit");
    }

    @Test
    void testHistoryCommandWithAgentFilter() throws Exception {
        StatusCommand.HistoryCommand historyCommand = new StatusCommand.HistoryCommand();
        historyCommand.projectDir = tempDir.toString();
        historyCommand.agentId = "worker-1";

        Integer result = historyCommand.call();

        assertEquals(0, result, "Should return 0 with agent filter");
    }

    @Test
    void testHistoryCommandWithInvalidDirectory() throws Exception {
        StatusCommand.HistoryCommand historyCommand = new StatusCommand.HistoryCommand();
        historyCommand.projectDir = "/nonexistent/directory";

        Integer result = historyCommand.call();

        assertEquals(1, result, "Should return 1 for invalid directory");
    }

    @Test
    void testAgentStatusCollector() {
        StatusCommand.AgentStatusCollector collector = new StatusCommand.AgentStatusCollector(tempDir);

        var statuses = collector.collectStatuses(true);

        assertNotNull(statuses, "Should return status list");
        assertFalse(statuses.isEmpty(), "Should return at least mock statuses");
    }

    @Test
    void testAgentStatusCollectorInactiveOnly() {
        StatusCommand.AgentStatusCollector collector = new StatusCommand.AgentStatusCollector(tempDir);

        var statuses = collector.collectStatuses(false);

        assertNotNull(statuses, "Should return status list");
        // Only active agents should be returned when includeInactive is false
    }

    @Test
    void testAgentStatusRecord() {
        var details = java.util.Map.<String, Object>of("uptime", "1h 23m", "tasks", "15");

        StatusCommand.AgentStatus status = new StatusCommand.AgentStatus(
            "test-id",
            "Test Agent",
            "worker",
            "running",
            true,
            java.time.LocalDateTime.now(),
            "healthy",
            details
        );

        assertEquals("test-id", status.id());
        assertEquals("Test Agent", status.name());
        assertEquals("worker", status.type());
        assertEquals("running", status.status());
        assertTrue(status.isActive());
        assertEquals("healthy", status.health());
        assertEquals(2, status.details().size());
    }

    @Test
    void testAgentStatusWithNulls() {
        StatusCommand.AgentStatus status = new StatusCommand.AgentStatus(
            null,
            null,
            null,
            null,
            true,
            null,
            null,
            null
        );

        assertEquals("", status.id());
        assertEquals("", status.name());
        assertEquals("", status.type());
        assertEquals("unknown", status.status());
        assertEquals("unknown", status.health());
        assertTrue(status.details().isEmpty());
    }

    @Test
    void testStatusHistoryEntryRecord() {
        StatusCommand.StatusHistoryEntry entry = new StatusCommand.StatusHistoryEntry(
            java.time.LocalDateTime.now(),
            "agent-1",
            "Agent One",
            "idle",
            "running",
            "Task started"
        );

        assertNotNull(entry.timestamp());
        assertEquals("agent-1", entry.agentId());
        assertEquals("Agent One", entry.agentName());
        assertEquals("idle", entry.oldStatus());
        assertEquals("running", entry.newStatus());
        assertEquals("Task started", entry.reason());
    }

    @Test
    void testStatusHistoryEntryWithNulls() {
        StatusCommand.StatusHistoryEntry entry = new StatusCommand.StatusHistoryEntry(
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertEquals("", entry.agentId());
        assertEquals("", entry.agentName());
        assertEquals("", entry.oldStatus());
        assertEquals("", entry.newStatus());
        assertEquals("", entry.reason());
    }

    @Test
    void testCommandAnnotationPresence() {
        picocli.CommandLine.Command commandAnnotation =
            StatusCommand.ShowCommand.class.getAnnotation(picocli.CommandLine.Command.class);

        assertNotNull(commandAnnotation, "ShowCommand should have @Command annotation");
        assertEquals("show", commandAnnotation.name());

        commandAnnotation = StatusCommand.ListCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "ListCommand should have @Command annotation");
        assertEquals("list", commandAnnotation.name());

        commandAnnotation = StatusCommand.HistoryCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "HistoryCommand should have @Command annotation");
        assertEquals("history", commandAnnotation.name());
    }

    /**
     * Integration test for command structure
     */
    @Test
    void testStatusCommandIntegration() {
        StatusCommand statusCommand = new StatusCommand();
        assertNotNull(statusCommand, "StatusCommand should be properly instantiated");

        picocli.CommandLine.Command commandAnnotation =
            statusCommand.getClass().getAnnotation(picocli.CommandLine.Command.class);

        assertNotNull(commandAnnotation, "StatusCommand should have @Command annotation");
        assertEquals("status", commandAnnotation.name());
    }

    /**
     * Test collector with mock data
     */
    @Test
    void testCollectorWithMockData() {
        StatusCommand.AgentStatusCollector collector = new StatusCommand.AgentStatusCollector(tempDir);

        var statuses = collector.collectStatuses(true);

        assertTrue(statuses.size() >= 3, "Should have at least 3 mock agents");

        boolean hasWorker = statuses.stream().anyMatch(s -> s.type().equals("worker"));
        assertTrue(hasWorker, "Should have worker agent");

        boolean hasReviewer = statuses.stream().anyMatch(s -> s.type().equals("reviewer"));
        assertTrue(hasReviewer, "Should have reviewer agent");

        boolean hasAdvisor = statuses.stream().anyMatch(s -> s.type().equals("advisor"));
        assertTrue(hasAdvisor, "Should have advisor agent");
    }

    /**
     * Test history functionality
     */
    @Test
    void testHistoryCollection() {
        StatusCommand.AgentStatusCollector collector = new StatusCommand.AgentStatusCollector(tempDir);

        var history = collector.getHistory(null, null, 10);

        assertNotNull(history, "Should return history list");
        assertFalse(history.isEmpty(), "Should have mock history entries");
    }

    /**
     * Test history filtering by agent
     */
    @Test
    void testHistoryFilteringByAgent() {
        StatusCommand.AgentStatusCollector collector = new StatusCommand.AgentStatusCollector(tempDir);

        var allHistory = collector.getHistory(null, null, 10);
        var filteredHistory = collector.getHistory("worker-1", null, 10);

        assertTrue(filteredHistory.size() <= allHistory.size(),
            "Filtered history should be smaller or equal to all history");

        assertTrue(filteredHistory.stream().allMatch(e -> e.agentId().equals("worker-1")),
            "All entries should be for worker-1");
    }

    /**
     * Test history count limit
     */
    @Test
    void testHistoryCountLimit() {
        StatusCommand.AgentStatusCollector collector = new StatusCommand.AgentStatusCollector(tempDir);

        var limitedHistory = collector.getHistory(null, null, 1);

        assertTrue(limitedHistory.size() <= 1, "History should be limited to 1 entry");
    }
}
