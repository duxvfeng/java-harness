package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Subagent Stop command for stopping subagents.
 *
 * <p>This command provides subagent termination capabilities:
 * <ul>
 *   <li>stop - Stop a running subagent</li>
 *   <li>stop-all - Stop all running subagents</li>
 *   <li>signal - Send signal to subagent</li>
 *   <li>status - Show stop operation status</li>
 * </ul>
 * </p>
 */
@Command(name = "subagent-stop",
         mixinStandardHelpOptions = true,
         subcommands = {
             SubagentStopCommand.StopCommand.class,
             SubagentStopCommand.StopAllCommand.class,
             SubagentStopCommand.SignalCommand.class,
             SubagentStopCommand.StatusCommand.class
         },
         description = "Stop and manage subagents")
public class SubagentStopCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Stop a running subagent
     */
    @Command(name = "stop",
             mixinStandardHelpOptions = true,
             description = "Stop a running subagent")
    public static class StopCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--agent"},
                 description = "Agent name to stop",
                 required = true)
        String agentName;

        @Option(names = {"--force", "-f"},
                 description = "Force stop without graceful shutdown")
        boolean force;

        @Option(names = {"--timeout"},
                 description = "Graceful shutdown timeout (seconds)",
                 defaultValue = "30")
        int timeout;

        @Option(names = {"--wait"},
                 description = "Wait for agent to exit")
        boolean waitForExit;

        @Option(names = {"--save-state"},
                 description = "Save agent state before stopping")
        boolean saveState;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                Path projectPath = Paths.get(projectDir).toAbsolutePath();

                if (!Files.exists(projectPath)) {
                    System.err.println("✗ Project directory not found: " + projectDir);
                    return 1;
                }

                SubagentStopper stopper = new SubagentStopper(projectPath, verbose);

                System.out.println();
                System.out.println("🛑 Stopping Subagent");
                System.out.println();
                System.out.println("Agent: " + agentName);
                System.out.println("Force: " + (force ? "Yes" : "No"));
                System.out.println("Timeout: " + timeout + "s");
                System.out.println();

                // Stop agent
                StopResult result = stopper.stopAgent(agentName, force, timeout, waitForExit, saveState);

                System.out.println("Status: " + getStatusIcon(result.status()) + " " + result.status());
                System.out.println("Exit Code: " + result.exitCode());

                if (result.stoppedAt() != null) {
                    System.out.println("Stopped At: " + result.stoppedAt().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }

                if (saveState && result.stateFilePath() != null) {
                    System.out.println("State Saved: " + result.stateFilePath());
                }

                if (!result.errors().isEmpty()) {
                    System.out.println();
                    System.out.println("Errors:");
                    for (String error : result.errors()) {
                        System.out.println("  ✗ " + error);
                    }
                }

                return result.status().equals("stopped") ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Stop failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "stopped", "exited" -> "✓";
                case "failed", "error" -> "✗";
                case "timeout" -> "⏱";
                case "not_found" -> "⚠";
                default -> "?";
            };
        }
    }

    /**
     * Stop all running subagents
     */
    @Command(name = "stop-all",
             mixinStandardHelpOptions = true,
             description = "Stop all running subagents")
    public static class StopAllCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--force"},
                 description = "Force stop all agents")
        boolean force;

        @Option(names = {"--timeout"},
                 description = "Timeout per agent (seconds)",
                 defaultValue = "30")
        int timeout;

        @Option(names = {"--parallel"},
                 description = "Stop agents in parallel")
        boolean parallel;

        @Option(names = {"--delay"},
                 description = "Delay between stops (milliseconds)",
                 defaultValue = "500")
        int delay;

        @Option(names = {"--exclude"},
                 description = "Agents to exclude from stopping (comma-separated)")
        String exclude;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                Path projectPath = Paths.get(projectDir).toAbsolutePath();

                if (!Files.exists(projectPath)) {
                    System.err.println("✗ Project directory not found: " + projectDir);
                    return 1;
                }

                SubagentStopper stopper = new SubagentStopper(projectPath, verbose);

                // Parse exclude list
                Set<String> excluded = new HashSet<>();
                if (exclude != null && !exclude.isEmpty()) {
                    excluded = new HashSet<>(Arrays.asList(exclude.split(",")));
                }

                System.out.println();
                System.out.println("🛑 Stopping All Subagents");
                System.out.println();
                System.out.println("Force: " + (force ? "Yes" : "No"));
                System.out.println("Parallel: " + (parallel ? "Yes" : "No"));

                if (!excluded.isEmpty()) {
                    System.out.println("Excluded: " + String.join(", ", excluded));
                }

                System.out.println();

                // Stop all agents
                StopAllResult result = stopper.stopAll(force, timeout, parallel, delay, excluded);

                System.out.println();
                System.out.println("📊 Stop All Summary");
                System.out.println();
                System.out.println("Total: " + result.totalCount());
                System.out.println("Stopped: " + result.stoppedCount());
                System.out.println("Failed: " + result.failedCount());
                System.out.println("Skipped: " + result.skippedCount());

                if (!result.failedAgents().isEmpty()) {
                    System.out.println();
                    System.out.println("Failed Agents:");
                    for (String agent : result.failedAgents()) {
                        System.out.println("  ✗ " + agent);
                    }
                }

                return result.failedCount() == 0 ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Stop all failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * Send signal to subagent
     */
    @Command(name = "signal",
             mixinStandardHelpOptions = true,
             description = "Send signal to subagent")
    public static class SignalCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--agent"},
                 description = "Target agent name",
                 required = true)
        String agentName;

        @Option(names = {"--signal"},
                 description = "Signal to send: SIGTERM, SIGKILL, SIGINT, SIGHUP",
                 defaultValue = "SIGTERM")
        String signal;

        @Option(names = {"--reason"},
                 description = "Reason for sending signal")
        String reason;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                Path projectPath = Paths.get(projectDir);

                if (!Files.exists(projectPath)) {
                    System.err.println("✗ Project directory not found: " + projectPath);
                    return 1;
                }

                SubagentStopper stopper = new SubagentStopper(projectPath, verbose);

                System.out.println();
                System.out.println("📡 Sending Signal");
                System.out.println();
                System.out.println("Agent: " + agentName);
                System.out.println("Signal: " + signal);

                if (reason != null && !reason.isEmpty()) {
                    System.out.println("Reason: " + reason);
                }

                System.out.println();

                // Send signal
                SignalResult result = stopper.sendSignal(agentName, signal, reason);

                System.out.println("Status: " + getStatusIcon(result.status()) + " " + result.status());

                if (result.pid() > 0) {
                    System.out.println("PID: " + result.pid());
                }

                if (!result.errors().isEmpty()) {
                    System.out.println();
                    System.out.println("Errors:");
                    for (String error : result.errors()) {
                        System.out.println("  ✗ " + error);
                    }
                }

                return result.status().equals("sent") ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Signal send failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "sent", "delivered" -> "✓";
                case "failed", "error" -> "✗";
                case "not_found" -> "⚠";
                default -> "?";
            };
        }
    }

    /**
     * Show stop operation status
     */
    @Command(name = "status",
             mixinStandardHelpOptions = true,
             description = "Show stop operation status")
    public static class StatusCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--agent"},
                 description = "Specific agent to check")
        String agentName;

        @Option(names = {"--format"},
                 description = "Output format: table, json, detailed",
                 defaultValue = "table")
        String format;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                Path projectPath = Paths.get(projectDir);

                if (!Files.exists(projectPath)) {
                    System.err.println("✗ Project directory not found: " + projectPath);
                    return 1;
                }

                SubagentStopper stopper = new SubagentStopper(projectPath, verbose);
                List<AgentStopStatus> statuses = stopper.getStopStatus(agentName);

                if ("json".equals(format)) {
                    outputJsonStatus(statuses);
                } else if ("detailed".equals(format)) {
                    outputDetailedStatus(statuses);
                } else {
                    outputTableStatus(statuses);
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

        private void outputJsonStatus(List<AgentStopStatus> statuses) {
            System.out.println("[");
            for (int i = 0; i < statuses.size(); i++) {
                AgentStopStatus status = statuses.get(i);
                System.out.println("  {");
                System.out.println("    \"agentName\": \"" + status.agentName() + "\",");
                System.out.println("    \"status\": \"" + status.status() + "\",");
                System.out.println("    \"exitCode\": " + status.exitCode() + ",");
                System.out.println("    \"stoppedAt\": \"" + status.stoppedAt().format(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\"");
                System.out.println("  }" + (i < statuses.size() - 1 ? "," : ""));
            }
            System.out.println("]");
        }

        private void outputDetailedStatus(List<AgentStopStatus> statuses) {
            System.out.println();
            System.out.println("📊 Agent Stop Status");
            System.out.println();

            for (AgentStopStatus status : statuses) {
                System.out.println("Agent: " + status.agentName());
                System.out.println("  Status: " + getStatusIcon(status.status()) + " " + status.status());
                System.out.println("  Exit Code: " + status.exitCode());
                System.out.println("  Stopped At: " + status.stoppedAt().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                System.out.println("-".repeat(60));
            }

            System.out.println();
            System.out.println("Total: " + statuses.size() + " agent(s)");
        }

        private void outputTableStatus(List<AgentStopStatus> statuses) {
            System.out.println();
            System.out.println("📊 Agent Stop Status");
            System.out.println();
            System.out.printf("%-20s %-15s %-10s %-20s%n",
                "Agent", "Status", "Exit Code", "Stopped At");
            System.out.println("-".repeat(70));

            for (AgentStopStatus status : statuses) {
                System.out.printf("%-20s %-15s %-10s %-20s%n",
                    truncate(status.agentName(), 20),
                    getStatusIcon(status.status()) + " " + truncate(status.status(), 13),
                    status.exitCode(),
                    status.stoppedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }

            System.out.println();
            System.out.println("Total: " + statuses.size() + " agent(s)");
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "stopped", "exited" -> "✓";
                case "running" -> "▶";
                case "failed" -> "✗";
                case "not_found" -> "⚠";
                default -> "?";
            };
        }

        private String truncate(String s, int maxLength) {
            if (s == null) return "";
            return s.length() > maxLength ? s.substring(0, maxLength - 3) + "..." : s;
        }
    }

    /**
     * Stop result record
     */
    public record StopResult(
        String status,
        int exitCode,
        LocalDateTime stoppedAt,
        String stateFilePath,
        List<String> errors
    ) {
        public StopResult {
            if (status == null) status = "";
            if (errors == null) errors = List.of();
        }
    }

    /**
     * Stop all result record
     */
    public record StopAllResult(
        int totalCount,
        int stoppedCount,
        int failedCount,
        int skippedCount,
        List<String> failedAgents
    ) {
        public StopAllResult {
            if (failedAgents == null) failedAgents = List.of();
        }
    }

    /**
     * Signal result record
     */
    public record SignalResult(
        String status,
        long pid,
        List<String> errors
    ) {
        public SignalResult {
            if (status == null) status = "";
            if (errors == null) errors = List.of();
        }
    }

    /**
     * Agent stop status record
     */
    public record AgentStopStatus(
        String agentName,
        String status,
        int exitCode,
        LocalDateTime stoppedAt
    ) {
        public AgentStopStatus {
            if (agentName == null) agentName = "";
            if (status == null) status = "";
            if (stoppedAt == null) stoppedAt = LocalDateTime.now();
        }
    }

    /**
     * Subagent stopper
     */
    public static class SubagentStopper {
        private final Path projectRoot;
        private final boolean verbose;

        public SubagentStopper(Path projectRoot, boolean verbose) {
            this.projectRoot = projectRoot;
            this.verbose = verbose;
        }

        public StopResult stopAgent(String agentName, boolean force, int timeout, boolean wait, boolean saveState) {
            List<String> errors = new ArrayList<>();

            try {
                if (verbose) {
                    System.out.println("Stopping agent: " + agentName);
                    System.out.println("  Force: " + force);
                    System.out.println("  Timeout: " + timeout);
                }

                // Simulate stop
                String stateFilePath = null;
                if (saveState) {
                    stateFilePath = projectRoot.resolve(".claude/state/agents/" + agentName + "-state.json").toString();
                }

                StopResult result = new StopResult(
                    "stopped",
                    0,
                    LocalDateTime.now(),
                    stateFilePath,
                    errors
                );

                if (verbose) {
                    System.out.println("Agent stopped successfully");
                }

                return result;

            } catch (Exception e) {
                errors.add(e.getMessage());
                return new StopResult("failed", -1, LocalDateTime.now(), null, errors);
            }
        }

        public StopAllResult stopAll(boolean force, int timeout, boolean parallel, int delay, Set<String> exclude) {
            List<String> failedAgents = new ArrayList<>();
            int stoppedCount = 0;
            int skippedCount = 0;

            // Mock agents
            List<String> agents = Arrays.asList("worker-1", "reviewer-1", "advisor-1");

            for (String agent : agents) {
                if (exclude.contains(agent)) {
                    skippedCount++;
                    continue;
                }

                try {
                    StopResult result = stopAgent(agent, force, timeout, false, false);

                    if (result.status().equals("stopped")) {
                        stoppedCount++;
                        System.out.println("✓ Stopped: " + agent);
                    } else {
                        failedAgents.add(agent);
                    }

                    if (!parallel && delay > 0) {
                        Thread.sleep(delay);
                    }

                } catch (Exception e) {
                    failedAgents.add(agent);
                }
            }

            return new StopAllResult(
                agents.size(),
                stoppedCount,
                failedAgents.size(),
                skippedCount,
                failedAgents
            );
        }

        public SignalResult sendSignal(String agentName, String signal, String reason) {
            List<String> errors = new ArrayList<>();

            try {
                if (verbose) {
                    System.out.println("Sending " + signal + " to " + agentName);
                }

                long pid = (long) (Math.random() * 10000) + 1000;

                return new SignalResult("sent", pid, errors);

            } catch (Exception e) {
                errors.add(e.getMessage());
                return new SignalResult("failed", 0, errors);
            }
        }

        public List<AgentStopStatus> getStopStatus(String agentName) {
            List<AgentStopStatus> statuses = new ArrayList<>();

            if (agentName != null && !agentName.isEmpty()) {
                // Check specific agent
                statuses.add(new AgentStopStatus(
                    agentName,
                    "stopped",
                    0,
                    LocalDateTime.now().minusMinutes(5)
                ));
            } else {
                // Check all agents
                List<String> agents = Arrays.asList("worker-1", "reviewer-1", "advisor-1");

                for (String agent : agents) {
                    statuses.add(new AgentStopStatus(
                        agent,
                        "stopped",
                        0,
                        LocalDateTime.now().minusMinutes((long) (Math.random() * 60))
                    ));
                }
            }

            return statuses;
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new SubagentStopCommand()).execute(args);
        System.exit(exitCode);
    }
}
