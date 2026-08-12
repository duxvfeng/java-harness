package com.chachamaru.harness.foundation.monitoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class NightWatchReportTest {

    @AfterEach
    void clearNightWatchEnvironment() {
        System.clearProperty("NIGHT_WATCH_ENABLED");
        System.clearProperty("HARNESS_NIGHT_WATCH_HOME");
        System.clearProperty("HARNESS_BRIDGE_HOME");
    }

    @Test
    void reportsStaleWorkInProgressTasksAndKeepsUnconfiguredHealthHealthy() throws Exception {
        Path root = Files.createTempDirectory("night-watch");
        Path plans = root.resolve("Plans.md");
        Files.writeString(plans, "| 99.1.1 | stale task | DoD | deps | cc:WIP |\n");
        Instant now = Instant.parse("2026-08-12T00:00:00Z");
        Files.setLastModifiedTime(plans, FileTime.from(now.minus(80, ChronoUnit.HOURS)));
        System.setProperty("NIGHT_WATCH_ENABLED", "false");

        NightWatchReport.Report report = NightWatchReport.build(root, false, now);

        assertEquals(NightWatchReport.SCHEMA_VERSION, report.schemaVersion());
        assertTrue(report.health().healthy());
        assertEquals(NightWatchReport.REASON_NOT_CONFIGURED, report.health().reason());
        assertEquals(1, report.staleTasks().size());
        assertEquals("99.1.1", report.staleTasks().get(0).taskId());
        assertTrue(report.unresolvedLoops().isEmpty());
        assertTrue(report.openDecisions().isEmpty());
    }

    @Test
    void reportsCorruptedEnabledBridgeConfiguration() throws Exception {
        Path root = Files.createTempDirectory("night-watch-corrupt");
        Path bridge = root.resolve("bridge");
        Files.createDirectories(bridge);
        Files.writeString(bridge.resolve("channels.json"), "{bad json");
        System.setProperty("NIGHT_WATCH_ENABLED", "true");
        System.setProperty("HARNESS_BRIDGE_HOME", bridge.toString());

        NightWatchReport.HealthResult result = NightWatchReport.checkHealth();

        assertFalse(result.healthy());
        assertEquals(NightWatchReport.REASON_CORRUPTED, result.reason());
    }

    @Test
    void reportsUnresolvedMailboxRequestAfterOneHour() throws Exception {
        Path root = Files.createTempDirectory("night-watch-mailbox");
        Path bridge = root.resolve("bridge");
        Files.createDirectories(bridge);
        Path mailbox = bridge.resolve("mailbox.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + mailbox);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE bridge_events (event_id TEXT PRIMARY KEY, source TEXT NOT NULL, event_type TEXT NOT NULL, lane TEXT NOT NULL, payload_json TEXT NOT NULL, ts INTEGER NOT NULL)");
            long timestamp = Instant.parse("2026-08-11T21:00:00Z").toEpochMilli() * 1_000_000L;
            statement.executeUpdate("INSERT INTO bridge_events VALUES ('evt-req', 'cc', 'advisor-request', 'fast', '{\"task_id\":\"t1\",\"trigger_hash\":\"abc\"}', " + timestamp + ")");
        }
        Files.writeString(bridge.resolve("channels.json"), "{\"socket_path\":\"unused\",\"mailbox_db\":\""
            + mailbox.toString().replace("\\", "\\\\") + "\"}");
        System.setProperty("NIGHT_WATCH_ENABLED", "false");
        System.setProperty("HARNESS_BRIDGE_HOME", bridge.toString());

        NightWatchReport.Report report = NightWatchReport.build(
            root, true, Instant.parse("2026-08-12T00:00:00Z"));

        assertEquals(1, report.unresolvedLoops().size());
        assertEquals("evt-req", report.unresolvedLoops().get(0).eventId());
        assertEquals("t1", report.unresolvedLoops().get(0).taskId());
    }

    @Test
    void usesDecisionHeaderDateAndChineseOpenStatus() throws Exception {
        Path root = Files.createTempDirectory("night-watch-decision");
        Path decisions = root.resolve(".claude/memory/decisions.md");
        Files.createDirectories(decisions.getParent());
        Files.writeString(decisions, "## 2026-08-01: Pending provider choice\n\n**\u72b6\u6001**: \u5f00\u653e\n");
        System.setProperty("NIGHT_WATCH_ENABLED", "false");

        NightWatchReport.Report report = NightWatchReport.build(
            root, false, Instant.parse("2026-08-12T00:00:00Z"));

        assertEquals(1, report.openDecisions().size());
        assertEquals("2026-08-01", report.openDecisions().get(0).decisionId());
    }
}
