package com.chachamaru.harness.foundation.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.nio.channels.SocketChannel;
import java.net.UnixDomainSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds the side-effect-free night-watch patrol report. */
public final class NightWatchReport {
    public static final String SCHEMA_VERSION = "night-watch-report.v1";
    public static final String REASON_NOT_CONFIGURED = "not-configured";
    public static final String REASON_DAEMON_UNREACHABLE = "daemon-unreachable";
    public static final String REASON_CORRUPTED = "corrupted";

    private static final int DEFAULT_STALE_TASK_HOURS = 72;
    private static final int DEFAULT_OPEN_DECISION_HOURS = 168;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern TABLE_ROW = Pattern.compile("^\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|.*$");
    private static final Pattern DECISION_HEADER = Pattern.compile("^##\\s+(D[0-9]+|[0-9]{4}-[0-9]{2}-[0-9]{2}):\\s*(.+)$");
    private static final Pattern DECISION_DATE = Pattern.compile("\\*\\*(?:Date|日期|日付)\\*\\*:?\\s*([0-9]{4}-[0-9]{2}-[0-9]{2})");

    private NightWatchReport() {
    }

    public record HealthResult(boolean healthy, String reason) {
    }

    public record UnresolvedLoop(
        @com.fasterxml.jackson.annotation.JsonProperty("event_id") String eventId,
        @com.fasterxml.jackson.annotation.JsonProperty("event_type") String eventType,
        String source,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        @com.fasterxml.jackson.annotation.JsonProperty("task_id") String taskId,
        @com.fasterxml.jackson.annotation.JsonProperty("age_hours") double ageHours) {
    }

    public record StaleTask(
        @com.fasterxml.jackson.annotation.JsonProperty("task_id") String taskId,
        String status,
        @com.fasterxml.jackson.annotation.JsonProperty("age_hours") double ageHours) {
    }

    public record OpenDecision(
        @com.fasterxml.jackson.annotation.JsonProperty("decision_id") String decisionId,
        String title,
        @com.fasterxml.jackson.annotation.JsonProperty("age_hours") double ageHours) {
    }

    public record Report(
        @com.fasterxml.jackson.annotation.JsonProperty("schema_version") String schemaVersion,
        @com.fasterxml.jackson.annotation.JsonProperty("generated_at") String generatedAt,
        @com.fasterxml.jackson.annotation.JsonProperty("dry_run") boolean dryRun,
        HealthResult health,
        @com.fasterxml.jackson.annotation.JsonProperty("unresolved_loops") List<UnresolvedLoop> unresolvedLoops,
        @com.fasterxml.jackson.annotation.JsonProperty("stale_tasks") List<StaleTask> staleTasks,
        @com.fasterxml.jackson.annotation.JsonProperty("open_decisions") List<OpenDecision> openDecisions) {
        public Report {
            unresolvedLoops = unresolvedLoops == null ? List.of() : List.copyOf(unresolvedLoops);
            staleTasks = staleTasks == null ? List.of() : List.copyOf(staleTasks);
            openDecisions = openDecisions == null ? List.of() : List.copyOf(openDecisions);
        }
    }

    public static HealthResult checkHealth() {
        if (!isEnabled()) {
            return new HealthResult(true, REASON_NOT_CONFIGURED);
        }
        Path bridgeHome = configuredPath("HARNESS_BRIDGE_HOME", Path.of(System.getProperty("user.home"), ".harness-bridge"));
        Path config = bridgeHome.resolve("channels.json");
        if (!Files.exists(config)) {
            return new HealthResult(true, REASON_NOT_CONFIGURED);
        }
        try {
            JsonNode node = MAPPER.readTree(Files.readString(config));
            String socket = text(node, "socket_path");
            String mailbox = text(node, "mailbox_db");
            if (socket == null || mailbox == null) {
                return new HealthResult(false, REASON_CORRUPTED);
            }
            if (!probeSocket(socket)) {
                return new HealthResult(false, REASON_DAEMON_UNREACHABLE);
            }
            return Files.exists(Path.of(mailbox))
                ? new HealthResult(true, "")
                : new HealthResult(false, REASON_CORRUPTED);
        } catch (Exception e) {
            return new HealthResult(false, REASON_CORRUPTED);
        }
    }

    public static Report build(Path repoRoot, boolean dryRun, Instant now) throws IOException {
        Path root = repoRoot.toAbsolutePath().normalize();
        Instant timestamp = now == null ? Instant.now() : now;
        HealthResult health = checkHealth();
        int staleHours = loadThreshold(root.resolve("scripts/templates/night-watch-config.yaml"), "stale_task_hours", DEFAULT_STALE_TASK_HOURS);
        int decisionHours = loadThreshold(root.resolve("scripts/templates/night-watch-config.yaml"), "open_decision_hours", DEFAULT_OPEN_DECISION_HOURS);
        List<UnresolvedLoop> unresolvedLoops = detectUnresolvedLoops(resolveMailboxPath(), timestamp);
        List<StaleTask> staleTasks = detectStaleTasks(root.resolve("Plans.md"), staleHours, timestamp);
        List<OpenDecision> decisions = detectOpenDecisions(root.resolve(".claude/memory/decisions.md"), decisionHours, timestamp);
        return new Report(SCHEMA_VERSION, timestamp.toString(), dryRun, health, unresolvedLoops, staleTasks, decisions);
    }

    private static Path resolveMailboxPath() {
        Path bridgeHome = configuredPath("HARNESS_BRIDGE_HOME", Path.of(System.getProperty("user.home"), ".harness-bridge"));
        Path config = bridgeHome.resolve("channels.json");
        if (!Files.isRegularFile(config)) {
            return null;
        }
        try {
            String mailbox = text(MAPPER.readTree(Files.readString(config)), "mailbox_db");
            return mailbox == null ? null : Path.of(mailbox);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static List<UnresolvedLoop> detectUnresolvedLoops(Path mailbox, Instant now) throws IOException {
        if (mailbox == null || !Files.isRegularFile(mailbox)) {
            return List.of();
        }
        Map<String, MailboxEvent> requests = new HashMap<>();
        Set<String> responses = new HashSet<>();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + mailbox);
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT event_id, source, event_type, payload_json, ts FROM bridge_events ORDER BY ts ASC");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                String eventType = rows.getString("event_type");
                JsonNode payload;
                try {
                    payload = MAPPER.readTree(rows.getString("payload_json"));
                } catch (IOException e) {
                    continue;
                }
                String key = loopKey(eventType, payload);
                if (key == null) {
                    continue;
                }
                if (isLoopResponse(eventType)) {
                    responses.add(key);
                } else if (isLoopRequest(eventType)) {
                    requests.put(key, new MailboxEvent(
                        rows.getString("event_id"), eventType, rows.getString("source"),
                        text(payload, "task_id"), rows.getLong("ts")));
                }
            }
        } catch (SQLException e) {
            throw new IOException("read mailbox events: " + e.getMessage(), e);
        }

        List<UnresolvedLoop> result = new ArrayList<>();
        for (Map.Entry<String, MailboxEvent> entry : requests.entrySet()) {
            MailboxEvent event = entry.getValue();
            if (responses.contains(entry.getKey())) {
                continue;
            }
            double age = ageHours(Instant.ofEpochSecond(0, event.timestamp()), now);
            if (age >= 1.0) {
                result.add(new UnresolvedLoop(event.eventId(), event.eventType(), event.source(), event.taskId(), age));
            }
        }
        result.sort(Comparator.comparing(UnresolvedLoop::eventId));
        return result;
    }

    private record MailboxEvent(String eventId, String eventType, String source, String taskId, long timestamp) {
    }

    private static String loopKey(String eventType, JsonNode payload) {
        String taskId = text(payload, "task_id");
        String triggerHash = text(payload, "trigger_hash");
        if (taskId == null && triggerHash == null) {
            String conversationId = text(payload, "conversation_id");
            return conversationId == null ? null : eventType + ":" + conversationId;
        }
        return (taskId == null ? "" : taskId) + ":" + (triggerHash == null ? "" : triggerHash);
    }

    private static boolean isLoopRequest(String eventType) {
        return "advisor-request".equals(eventType)
            || "review-request".equals(eventType)
            || "worker-report".equals(eventType)
            || eventType.endsWith("-request");
    }

    private static boolean isLoopResponse(String eventType) {
        return "advisor-response".equals(eventType)
            || "review-result".equals(eventType)
            || eventType.endsWith("-response")
            || eventType.endsWith("-result");
    }

    private static boolean isEnabled() {
        String enabled = configuredValue("NIGHT_WATCH_ENABLED");
        if ("true".equalsIgnoreCase(enabled)) {
            return true;
        }
        if ("false".equalsIgnoreCase(enabled)) {
            return false;
        }
        Path home = configuredPath("HARNESS_NIGHT_WATCH_HOME", Path.of(System.getProperty("user.home"), ".harness-night-watch"));
        try {
            JsonNode node = MAPPER.readTree(Files.readString(home.resolve("night-watch.json")));
            return node.path("enabled").asBoolean(false);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean probeSocket(String path) {
        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            channel.connect(UnixDomainSocketAddress.of(path));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static List<StaleTask> detectStaleTasks(Path plans, int threshold, Instant now) throws IOException {
        if (!Files.isRegularFile(plans)) {
            return List.of();
        }
        double age = ageHours(Files.getLastModifiedTime(plans).toInstant(), now);
        if (age <= threshold) {
            return List.of();
        }
        List<StaleTask> result = new ArrayList<>();
        for (String line : Files.readAllLines(plans)) {
            Matcher matcher = TABLE_ROW.matcher(line.trim());
            if (!matcher.matches()) {
                continue;
            }
            String status = matcher.group(5).trim();
            String lower = status.toLowerCase(Locale.ROOT);
            if (lower.contains("done") || lower.contains("complete") || lower.contains("完成")) {
                continue;
            }
            if (lower.contains("wip") || lower.contains("todo") || lower.contains("blocked")
                || lower.contains("open") || lower.contains("进行") || lower.contains("待")) {
                result.add(new StaleTask(matcher.group(1).trim(), status, age));
            }
        }
        return result;
    }

    private static List<OpenDecision> detectOpenDecisions(Path decisions, int threshold, Instant now) throws IOException {
        if (!Files.isRegularFile(decisions)) {
            return List.of();
        }
        List<OpenDecision> result = new ArrayList<>();
        String currentId = null;
        String title = null;
        Instant date = null;
        boolean open = false;
        for (String line : Files.readAllLines(decisions)) {
            Matcher header = DECISION_HEADER.matcher(line.trim());
            if (header.matches()) {
                if (open && currentId != null && ageHours(date == null ? Files.getLastModifiedTime(decisions).toInstant() : date, now) > threshold) {
                    result.add(new OpenDecision(currentId, title, ageHours(date == null ? Files.getLastModifiedTime(decisions).toInstant() : date, now)));
                }
                currentId = header.group(1);
                title = header.group(2).trim();
                date = parseHeaderDate(currentId);
                open = false;
                continue;
            }
            String trimmed = line.trim();
            if (isOpenStatus(trimmed)) {
                open = true;
            }
            Matcher dateMatcher = DECISION_DATE.matcher(trimmed);
            if (dateMatcher.find()) {
                date = LocalDate.parse(dateMatcher.group(1)).atStartOfDay().toInstant(ZoneOffset.UTC);
            }
        }
        if (open && currentId != null) {
            Instant base = date == null ? Files.getLastModifiedTime(decisions).toInstant() : date;
            double age = ageHours(base, now);
            if (age > threshold) {
                result.add(new OpenDecision(currentId, title, age));
            }
        }
        return result;
    }

    private static Instant parseHeaderDate(String decisionId) {
        try {
            return decisionId.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
                ? LocalDate.parse(decisionId).atStartOfDay().toInstant(ZoneOffset.UTC)
                : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean isOpenStatus(String line) {
        Matcher matcher = Pattern.compile("(?i)^\\*\\*([^*]+)\\*\\*:?\\s*(.+)$").matcher(line);
        if (!matcher.matches()) {
            return false;
        }
        String field = matcher.group(1).trim();
        if (!field.equalsIgnoreCase("status")
            && !field.equals("\u72b6\u6001")
            && !field.equals("\u72b6\u614b")) {
            return false;
        }
        String status = matcher.group(2).trim().toLowerCase(Locale.ROOT);
        return status.startsWith("open") || status.startsWith("pending") || status.startsWith("todo")
            || status.startsWith("unresolved") || status.startsWith("开放")
            || status.startsWith("未解决") || status.startsWith("未決")
            || status.startsWith("待定");
    }

    private static int loadThreshold(Path config, String key, int fallback) throws IOException {
        if (!Files.isRegularFile(config)) {
            return fallback;
        }
        String prefix = key + ":";
        for (String line : Files.readAllLines(config)) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                try {
                    return Integer.parseInt(trimmed.substring(prefix.length()).trim());
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private static double ageHours(Instant base, Instant now) {
        return Math.max(0, Duration.between(base, now).toMinutes() / 60.0);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field) || node.get(field).asText().isBlank()) {
            return null;
        }
        return node.get(field).asText();
    }

    private static String configuredValue(String key) {
        String property = System.getProperty(key);
        return property != null ? property : System.getenv(key);
    }

    private static Path configuredPath(String key, Path fallback) {
        String value = configuredValue(key);
        return value == null || value.isBlank() ? fallback : Path.of(value);
    }
}
