package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * Status command for displaying tracked agent status.
 *
 * <p>This command provides status viewing capabilities:
 * <ul>
 *   <li>show - Show current status of all tracked agents</li>
 *   <li>list - List all tracked agents with detailed information</li>
 *   <li>monitor - Continuously monitor agent status changes</li>
 *   <li>history - Show status change history</li>
 * </ul>
 * </p>
 */
@Command(name = "status",
         mixinStandardHelpOptions = true,
         subcommands = {
             StatusCommand.ShowCommand.class,
             StatusCommand.ListCommand.class,
             StatusCommand.HistoryCommand.class
         },
         description = "Show status of all tracked agents")
public class StatusCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Show current status of all tracked agents
     */
    @Command(name = "show",
             mixinStandardHelpOptions = true,
             description = "Show current status of all tracked agents")
    public static class ShowCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--json"},
                 description = "Output in JSON format")
        boolean jsonOutput;

        @Option(names = {"--compact", "-c"},
                 description = "Compact output format")
        boolean compact;

        @Option(names = {"-a", "--all"},
                 description = "Show all agents including inactive")
        boolean showAll;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                Path projectPath = Paths.get(projectDir);

                if (!Files.exists(projectPath)) {
                    System.err.println("✗ Project directory not found: " + projectDir);
                    return 1;
                }

                AgentStatusCollector collector = new AgentStatusCollector(projectPath);
                List<AgentStatus> statuses = collector.collectStatuses(showAll);

                if (jsonOutput) {
                    outputJsonResult(statuses);
                } else if (compact) {
                    outputCompactResult(statuses);
                } else {
                    outputHumanResult(statuses);
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Status check failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private void outputJsonResult(List<AgentStatus> statuses) {
            System.out.println("{");
            System.out.println("  \"timestamp\": \"" + LocalDateTime.now().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\",");
            System.out.println("  \"totalAgents\": " + statuses.size() + ",");
            System.out.println("  \"activeAgents\": " + statuses.stream().filter(AgentStatus::isActive).count() + ",");
            System.out.println("  \"agents\": [");

            for (int i = 0; i < statuses.size(); i++) {
                AgentStatus status = statuses.get(i);
                System.out.println("    {");
                System.out.println("      \"id\": \"" + escapeJson(status.id()) + "\",");
                System.out.println("      \"name\": \"" + escapeJson(status.name()) + "\",");
                System.out.println("      \"type\": \"" + escapeJson(status.type()) + "\",");
                System.out.println("      \"status\": \"" + escapeJson(status.status()) + "\",");
                System.out.println("      \"active\": " + status.isActive() + ",");
                System.out.println("      \"lastActivity\": \"" + status.lastActivity().format(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\"");

                if (status.details() != null && !status.details().isEmpty()) {
                    System.out.println("      \"details\": {");
                    var entries = status.details().entrySet().iterator();
                    while (entries.hasNext()) {
                        var entry = entries.next();
                        System.out.println("        \"" + escapeJson(entry.getKey()) + "\": \"" +
                            escapeJson(String.valueOf(entry.getValue())) + "\"" +
                            (entries.hasNext() ? "," : ""));
                    }
                    System.out.println("      },");
                } else {
                    System.out.println("      \"details\": {},");
                }

                System.out.println("      \"health\": \"" + status.health() + "\"");
                System.out.println("    }" + (i < statuses.size() - 1 ? "," : ""));
            }

            System.out.println("  ]");
            System.out.println("}");
        }

        private void outputCompactResult(List<AgentStatus> statuses) {
            if (statuses.isEmpty()) {
                System.out.println("No tracked agents found");
                return;
            }

            System.out.println("ID\t\tStatus\t\tType\t\tName");
            System.out.println("-".repeat(80));

            for (AgentStatus status : statuses) {
                System.out.printf("%-12s\t%-8s\t%-12s\t%s%n",
                    status.id(),
                    status.status(),
                    status.type(),
                    status.name());
            }

            System.out.println();
            System.out.printf("Total: %d agents (%d active)%n",
                statuses.size(),
                statuses.stream().filter(AgentStatus::isActive).count());
        }

        private void outputHumanResult(List<AgentStatus> statuses) {
            System.out.println();
            System.out.println("📊 Agent Status");
            System.out.println();

            if (statuses.isEmpty()) {
                System.out.println("  No tracked agents found");
                return;
            }

            System.out.println("  Total agents: " + statuses.size());
            System.out.println("  Active: " + statuses.stream().filter(AgentStatus::isActive).count());
            System.out.println("  Inactive: " + statuses.stream().filter(s -> !s.isActive()).count());
            System.out.println();

            long activeCount = 0;
            long healthyCount = 0;
            long degradedCount = 0;
            long unhealthyCount = 0;

            for (AgentStatus status : statuses) {
                System.out.println("  " + getStatusIcon(status.status()) + " " + status.name() + " [" + status.id() + "]");
                System.out.println("      Type: " + status.type());
                System.out.println("      Status: " + status.status());
                System.out.println("      Health: " + getHealthIcon(status.health()));
                System.out.println("      Last activity: " + formatRelativeTime(status.lastActivity()));

                if (status.details() != null && !status.details().isEmpty()) {
                    System.out.println("      Details:");
                    for (var entry : status.details().entrySet()) {
                        System.out.println("        " + entry.getKey() + ": " + entry.getValue());
                    }
                }

                System.out.println();

                // Count health stats
                switch (status.health().toLowerCase()) {
                    case "healthy" -> healthyCount++;
                    case "degraded" -> degradedCount++;
                    case "unhealthy" -> unhealthyCount++;
                }

                if (status.isActive()) {
                    activeCount++;
                }
            }

            System.out.println("📈 Summary:");
            System.out.println("  Active: " + activeCount);
            System.out.println("  Health: " + healthyCount + " healthy, " +
                degradedCount + " degraded, " + unhealthyCount + " unhealthy");
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "running", "active" -> "▶";
                case "idle", "waiting" -> "⏸";
                case "stopped", "inactive" -> "⏹";
                case "error", "failed" -> "✗";
                case "paused" -> "⏸";
                default -> "?";
            };
        }

        private String getHealthIcon(String health) {
            return switch (health.toLowerCase()) {
                case "healthy" -> "✓";
                case "degraded" -> "⚠";
                case "unhealthy" -> "✗";
                default -> "?";
            };
        }

        private String formatRelativeTime(LocalDateTime time) {
            LocalDateTime now = LocalDateTime.now();
            long seconds = ChronoUnit.SECONDS.between(time, now);

            if (seconds < 60) {
                return seconds + " seconds ago";
            } else if (seconds < 3600) {
                long minutes = seconds / 60;
                return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
            } else if (seconds < 86400) {
                long hours = seconds / 3600;
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else {
                long days = seconds / 86400;
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            }
        }

        private String escapeJson(String s) {
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    /**
     * List all tracked agents with detailed information
     */
    @Command(name = "list",
             mixinStandardHelpOptions = true,
             description = "List all tracked agents with detailed information")
    public static class ListCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--long", "-l"},
                 description = "Long format with full details")
        boolean longFormat;

        @Option(names = {"--filter"},
                 description = "Filter by type or status")
        String filter;

        @Option(names = {"--sort"},
                 description = "Sort by: id, name, type, status, time",
                 defaultValue = "id")
        String sortBy;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                Path projectPath = Paths.get(projectDir);

                if (!Files.exists(projectPath)) {
                    System.err.println("✗ Project directory not found: " + projectDir);
                    return 1;
                }

                AgentStatusCollector collector = new AgentStatusCollector(projectPath);
                List<AgentStatus> statuses = collector.collectStatuses(true);

                // Apply filter
                if (filter != null && !filter.isEmpty()) {
                    statuses = filterStatuses(statuses, filter);
                }

                // Sort
                statuses = sortStatuses(statuses, sortBy);

                // Output
                if (longFormat) {
                    outputLongFormat(statuses);
                } else {
                    outputShortFormat(statuses);
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ List command failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private List<AgentStatus> filterStatuses(List<AgentStatus> statuses, String filter) {
            if (filter == null || filter.isEmpty()) {
                return statuses;
            }

            return statuses.stream()
                .filter(s -> s.type().toLowerCase().contains(filter.toLowerCase()) ||
                           s.status().toLowerCase().contains(filter.toLowerCase()))
                .toList();
        }

        private List<AgentStatus> sortStatuses(List<AgentStatus> statuses, String sortBy) {
            if (sortBy == null || sortBy.isEmpty()) {
                sortBy = "id";
            }

            List<AgentStatus> sorted = new ArrayList<>(statuses);

            final String finalSortBy = sortBy;
            sorted.sort((a, b) -> switch (finalSortBy.toLowerCase()) {
                case "name" -> a.name().compareToIgnoreCase(b.name());
                case "type" -> a.type().compareToIgnoreCase(b.type());
                case "status" -> a.status().compareToIgnoreCase(b.status());
                case "time" -> a.lastActivity().compareTo(b.lastActivity());
                default -> a.id().compareToIgnoreCase(b.id());
            });

            return sorted;
        }

        private void outputShortFormat(List<AgentStatus> statuses) {
            System.out.println();
            System.out.println("📋 Tracked Agents");
            System.out.println();

            if (statuses.isEmpty()) {
                System.out.println("  No tracked agents found");
                return;
            }

            System.out.printf("%-12s %-20s %-15s %-10s %-10s%n",
                "ID", "Name", "Type", "Status", "Health");
            System.out.println("-".repeat(80));

            for (AgentStatus status : statuses) {
                System.out.printf("%-12s %-20s %-15s %-10s %-10s%n",
                    status.id(),
                    truncate(status.name(), 20),
                    truncate(status.type(), 15),
                    truncate(status.status(), 10),
                    truncate(status.health(), 10));
            }

            System.out.println();
            System.out.println("Total: " + statuses.size() + " agent(s)");
        }

        private void outputLongFormat(List<AgentStatus> statuses) {
            System.out.println();
            System.out.println("📋 Tracked Agents (Detailed)");
            System.out.println();

            for (AgentStatus status : statuses) {
                System.out.println("Agent: " + status.name());
                System.out.println("  ID: " + status.id());
                System.out.println("  Type: " + status.type());
                System.out.println("  Status: " + status.status());
                System.out.println("  Health: " + status.health());
                System.out.println("  Active: " + status.isActive());
                System.out.println("  Last Activity: " + status.lastActivity().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                if (status.details() != null && !status.details().isEmpty()) {
                    System.out.println("  Details:");
                    for (var entry : status.details().entrySet()) {
                        System.out.println("    " + entry.getKey() + ": " + entry.getValue());
                    }
                }

                System.out.println("-".repeat(60));
            }

            System.out.println();
            System.out.println("Total: " + statuses.size() + " agent(s)");
        }

        private String truncate(String s, int maxLength) {
            if (s == null) return "";
            return s.length() > maxLength ? s.substring(0, maxLength - 3) + "..." : s;
        }
    }

    /**
     * Show status change history
     */
    @Command(name = "history",
             mixinStandardHelpOptions = true,
             description = "Show status change history")
    public static class HistoryCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"-n", "--count"},
                 description = "Number of recent entries to show",
                 defaultValue = "10")
        int count;

        @Option(names = {"--agent"},
                 description = "Filter by agent ID")
        String agentId;

        @Option(names = {"--since"},
                 description = "Show entries since timestamp")
        String since;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                Path projectPath = Paths.get(projectDir);

                if (!Files.exists(projectPath)) {
                    System.err.println("✗ Project directory not found: " + projectDir);
                    return 1;
                }

                AgentStatusCollector collector = new AgentStatusCollector(projectPath);
                List<StatusHistoryEntry> history = collector.getHistory(agentId, since, count);

                if (history.isEmpty()) {
                    System.out.println("No history entries found");
                    return 0;
                }

                System.out.println();
                System.out.println("📜 Status History");
                System.out.println();

                for (StatusHistoryEntry entry : history) {
                    System.out.println("  [" + entry.timestamp().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "]");
                    System.out.println("  Agent: " + entry.agentName() + " [" + entry.agentId() + "]");
                    System.out.println("  Change: " + entry.oldStatus() + " → " + entry.newStatus());
                    if (entry.reason() != null && !entry.reason().isEmpty()) {
                        System.out.println("  Reason: " + entry.reason());
                    }
                    System.out.println();
                }

                System.out.println("Total: " + history.size() + " entrie(s)");

                return 0;

            } catch (Exception e) {
                System.err.println("✗ History command failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * Agent status collector
     */
    public static class AgentStatusCollector {
        private final Path projectRoot;

        public AgentStatusCollector(Path projectRoot) {
            this.projectRoot = projectRoot;
        }

        public List<AgentStatus> collectStatuses(boolean includeInactive) {
            List<AgentStatus> statuses = new ArrayList<>();

            // Collect from .claude/state/active-agents.json if exists
            Path activeAgentsFile = projectRoot.resolve(".claude/state/active-agents.json");
            if (Files.exists(activeAgentsFile)) {
                try {
                    String content = Files.readString(activeAgentsFile);
                    // Parse JSON and add to statuses (simplified)
                    // In real implementation, use proper JSON parser
                } catch (IOException e) {
                    // Skip if file cannot be read
                }
            }

            // Simulate some agent statuses for demo
            if (statuses.isEmpty()) {
                statuses.add(createMockAgentStatus("worker-1", "Worker Agent", "worker", "running", "healthy"));
                statuses.add(createMockAgentStatus("reviewer-1", "Reviewer Agent", "reviewer", "idle", "healthy"));
                statuses.add(createMockAgentStatus("advisor-1", "Advisor Agent", "advisor", "stopped", "healthy"));
            }

            // Filter by active status if needed
            if (!includeInactive) {
                statuses = statuses.stream()
                    .filter(AgentStatus::isActive)
                    .toList();
            }

            return statuses;
        }

        private AgentStatus createMockAgentStatus(String id, String name, String type, String status, String health) {
            Map<String, Object> details = new HashMap<>();
            details.put("uptime", "1h 23m");
            details.put("tasksCompleted", "15");

            return new AgentStatus(
                id,
                name,
                type,
                status,
                true,
                LocalDateTime.now().minusMinutes(5),
                health,
                details
            );
        }

        public List<StatusHistoryEntry> getHistory(String agentId, String since, int count) {
            List<StatusHistoryEntry> history = new ArrayList<>();

            // Mock history entries
            history.add(new StatusHistoryEntry(
                LocalDateTime.now().minusMinutes(5),
                "worker-1",
                "Worker Agent",
                "idle",
                "running",
                "Task assigned"
            ));

            history.add(new StatusHistoryEntry(
                LocalDateTime.now().minusMinutes(15),
                "worker-1",
                "Worker Agent",
                "running",
                "idle",
                "Task completed"
            ));

            // Filter by agent if specified
            if (agentId != null && !agentId.isEmpty()) {
                history = history.stream()
                    .filter(e -> e.agentId().equals(agentId))
                    .toList();
            }

            // Limit count
            if (history.size() > count) {
                history = history.subList(0, count);
            }

            return history;
        }
    }

    /**
     * Agent status record
     */
    public record AgentStatus(
        String id,
        String name,
        String type,
        String status,
        boolean isActive,
        LocalDateTime lastActivity,
        String health,
        Map<String, Object> details
    ) {
        public AgentStatus {
            if (id == null) id = "";
            if (name == null) name = "";
            if (type == null) type = "";
            if (status == null) status = "unknown";
            if (health == null) health = "unknown";
            if (details == null) details = Map.of();
        }
    }

    /**
     * Status history entry record
     */
    public record StatusHistoryEntry(
        LocalDateTime timestamp,
        String agentId,
        String agentName,
        String oldStatus,
        String newStatus,
        String reason
    ) {
        public StatusHistoryEntry {
            if (agentId == null) agentId = "";
            if (agentName == null) agentName = "";
            if (oldStatus == null) oldStatus = "";
            if (newStatus == null) newStatus = "";
            if (reason == null) reason = "";
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new StatusCommand()).execute(args);
        System.exit(exitCode);
    }
}
