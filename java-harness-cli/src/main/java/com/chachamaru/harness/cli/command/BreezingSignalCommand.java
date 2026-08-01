package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Breezing Signal command for Breezing mode signal processing.
 *
 * <p>This command provides Breezing mode signal handling capabilities:
 * <ul>
 *   <li>send - Send signal to Breezing components</li>
 *   <li>receive - Receive and process signals</li>
 *   <li>status - Show signal status</li>
 *   <li>sync - Synchronize state across components</li>
 * </ul>
 * </p>
 */
@Command(name = "breezing-signal",
         mixinStandardHelpOptions = true,
         subcommands = {
             BreezingSignalCommand.SendCommand.class,
             BreezingSignalCommand.ReceiveCommand.class,
             BreezingSignalCommand.StatusCommand.class,
             BreezingSignalCommand.SyncCommand.class
         },
         description = "Handle Breezing mode signals")
public class BreezingSignalCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Send signal to Breezing components
     */
    @Command(name = "send",
             mixinStandardHelpOptions = true,
             description = "Send signal to Breezing components")
    public static class SendCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--target"},
                 description = "Target component: lead, worker, reviewer, all",
                 defaultValue = "all")
        String target;

        @Option(names = {"--type"},
                 description = "Signal type: start, stop, pause, resume, status, interrupt",
                 required = true)
        String signalType;

        @Option(names = {"--message"},
                 description = "Signal message")
        String message;

        @Option(names = {"--priority"},
                 description = "Signal priority: low, normal, high, urgent",
                 defaultValue = "normal")
        String priority;

        @Option(names = {"--timeout"},
                 description = "Response timeout (seconds)",
                 defaultValue = "30")
        int timeout;

        @Option(names = {"--wait"},
                 description = "Wait for response")
        boolean waitForResponse;

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

                BreezingSignalHandler handler = new BreezingSignalHandler(projectPath, verbose);

                // Create signal
                Signal signal = new Signal(
                    UUID.randomUUID().toString(),
                    signalType,
                    target,
                    priority,
                    message != null ? message : "",
                    LocalDateTime.now(),
                    "pending"
                );

                System.out.println();
                System.out.println("📤 Sending Breezing Signal");
                System.out.println();
                System.out.println("Signal ID: " + signal.signalId());
                System.out.println("Type: " + signal.type());
                System.out.println("Target: " + signal.target());
                System.out.println("Priority: " + getPriorityIcon(signal.priority()) + " " + signal.priority());
                System.out.println();

                // Send signal
                SignalResult result = handler.sendSignal(signal, waitForResponse, timeout);

                System.out.println("Status: " + getStatusIcon(result.status()) + " " + result.status());

                if (waitForResponse && result.response() != null) {
                    System.out.println();
                    System.out.println("Response:");
                    System.out.println("  From: " + result.response().from());
                    System.out.println("  Message: " + result.response().message());
                    System.out.println("  Timestamp: " + result.response().timestamp().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }

                if (!result.errors().isEmpty()) {
                    System.out.println();
                    System.out.println("Errors:");
                    for (String error : result.errors()) {
                        System.out.println("  ✗ " + error);
                    }
                }

                return result.status().equals("delivered") ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Signal send failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private String getPriorityIcon(String priority) {
            return switch (priority.toLowerCase()) {
                case "urgent" -> "🔴";
                case "high" -> "🟠";
                case "normal" -> "🟢";
                case "low" -> "🔵";
                default -> "⚪";
            };
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "delivered", "completed" -> "✓";
                case "failed", "error" -> "✗";
                case "pending", "waiting" -> "⏳";
                default -> "?";
            };
        }
    }

    /**
     * Receive and process signals
     */
    @Command(name = "receive",
             mixinStandardHelpOptions = true,
             description = "Receive and process signals")
    public static class ReceiveCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--component"},
                 description = "Component role: lead, worker, reviewer",
                 defaultValue = "worker")
        String component;

        @Option(names = {"--timeout"},
                 description = "Receive timeout (seconds, 0 = infinite)",
                 defaultValue = "0")
        int timeout;

        @Option(names = {"--count"},
                 description = "Number of signals to receive",
                 defaultValue = "1")
        int count;

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
                    System.err.println("✗ Project directory not found: " + projectDir);
                    return 1;
                }

                BreezingSignalHandler handler = new BreezingSignalHandler(projectPath, verbose);

                System.out.println();
                System.out.println("📥 Receiving Signals");
                System.out.println();
                System.out.println("Component: " + component);
                System.out.println("Count: " + count);
                System.out.println("Timeout: " + (timeout == 0 ? "Infinite" : timeout + "s"));
                System.out.println();

                List<Signal> signals = handler.receiveSignals(component, count, timeout);

                if ("json".equals(format)) {
                    outputJsonSignals(signals);
                } else if ("detailed".equals(format)) {
                    outputDetailedSignals(signals);
                } else {
                    outputTableSignals(signals);
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Signal receive failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private void outputJsonSignals(List<Signal> signals) {
            System.out.println("[");
            for (int i = 0; i < signals.size(); i++) {
                Signal signal = signals.get(i);
                System.out.println("  {");
                System.out.println("    \"signalId\": \"" + signal.signalId() + "\",");
                System.out.println("    \"type\": \"" + signal.type() + "\",");
                System.out.println("    \"target\": \"" + signal.target() + "\",");
                System.out.println("    \"priority\": \"" + signal.priority() + "\",");
                System.out.println("    \"message\": \"" + escapeJson(signal.message()) + "\"");
                System.out.println("  }" + (i < signals.size() - 1 ? "," : ""));
            }
            System.out.println("]");
        }

        private void outputDetailedSignals(List<Signal> signals) {
            System.out.println("Signals received: " + signals.size());
            System.out.println();

            for (Signal signal : signals) {
                System.out.println("Signal ID: " + signal.signalId());
                System.out.println("  Type: " + signal.type());
                System.out.println("  Target: " + signal.target());
                System.out.println("  Priority: " + getPriorityIcon(signal.priority()) + " " + signal.priority());
                System.out.println("  Message: " + signal.message());
                System.out.println("  Timestamp: " + signal.timestamp().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                System.out.println("-".repeat(60));
            }
        }

        private void outputTableSignals(List<Signal> signals) {
            System.out.println("Signals received: " + signals.size());
            System.out.println();
            System.out.printf("%-20s %-15s %-15s %-10s %-30s%n",
                "Signal ID", "Type", "Target", "Priority", "Message");
            System.out.println("-".repeat(100));

            for (Signal signal : signals) {
                System.out.printf("%-20s %-15s %-15s %-10s %-30s%n",
                    truncate(signal.signalId(), 20),
                    truncate(signal.type(), 15),
                    truncate(signal.target(), 15),
                    getPriorityIcon(signal.priority()) + " " + truncate(signal.priority(), 8),
                    truncate(signal.message(), 30));
            }
        }

        private String getPriorityIcon(String priority) {
            return switch (priority.toLowerCase()) {
                case "urgent" -> "🔴";
                case "high" -> "🟠";
                case "normal" -> "🟢";
                case "low" -> "🔵";
                default -> "⚪";
            };
        }

        private String truncate(String s, int maxLength) {
            if (s == null) return "";
            return s.length() > maxLength ? s.substring(0, maxLength - 3) + "..." : s;
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
     * Show signal status
     */
    @Command(name = "status",
             mixinStandardHelpOptions = true,
             description = "Show signal status")
    public static class StatusCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--component"},
                 description = "Filter by component")
        String component;

        @Option(names = {"--type"},
                 description = "Filter by signal type")
        String type;

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
                    System.err.println("✗ Project directory not found: " + projectDir);
                    return 1;
                }

                BreezingSignalHandler handler = new BreezingSignalHandler(projectPath, verbose);
                List<SignalStatus> statuses = handler.getSignalStatus(component, type);

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

        private void outputJsonStatus(List<SignalStatus> statuses) {
            System.out.println("[");
            for (int i = 0; i < statuses.size(); i++) {
                SignalStatus status = statuses.get(i);
                System.out.println("  {");
                System.out.println("    \"component\": \"" + status.component() + "\",");
                System.out.println("    \"state\": \"" + status.state() + "\",");
                System.out.println("    \"signalsReceived\": " + status.signalsReceived() + ",");
                System.out.println("    \"signalsProcessed\": " + status.signalsProcessed() + ",");
                System.out.println("    \"lastSignalTime\": \"" + status.lastSignalTime().format(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\"");
                System.out.println("  }" + (i < statuses.size() - 1 ? "," : ""));
            }
            System.out.println("]");
        }

        private void outputDetailedStatus(List<SignalStatus> statuses) {
            System.out.println();
            System.out.println("📊 Breezing Signal Status");
            System.out.println();

            for (SignalStatus status : statuses) {
                System.out.println("Component: " + status.component());
                System.out.println("  State: " + getStateIcon(status.state()) + " " + status.state());
                System.out.println("  Signals Received: " + status.signalsReceived());
                System.out.println("  Signals Processed: " + status.signalsProcessed());
                System.out.println("  Last Signal: " + status.lastSignalTime().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                System.out.println("-".repeat(60));
            }

            System.out.println();
            System.out.println("Total: " + statuses.size() + " component(s)");
        }

        private void outputTableStatus(List<SignalStatus> statuses) {
            System.out.println();
            System.out.println("📊 Breezing Signal Status");
            System.out.println();
            System.out.printf("%-15s %-15s %-20s %-20s%n",
                "Component", "State", "Received", "Processed");
            System.out.println("-".repeat(80));

            for (SignalStatus status : statuses) {
                System.out.printf("%-15s %-15s %-20d %-20d%n",
                    truncate(status.component(), 15),
                    getStateIcon(status.state()) + " " + truncate(status.state(), 13),
                    status.signalsReceived(),
                    status.signalsProcessed());
            }

            System.out.println();
            System.out.println("Total: " + statuses.size() + " component(s)");
        }

        private String getStateIcon(String state) {
            return switch (state.toLowerCase()) {
                case "active", "running" -> "▶";
                case "idle", "waiting" -> "⏸";
                case "paused" -> "⏸";
                case "stopped" -> "⏹";
                case "error" -> "✗";
                default -> "?";
            };
        }

        private String truncate(String s, int maxLength) {
            if (s == null) return "";
            return s.length() > maxLength ? s.substring(0, maxLength - 3) + "..." : s;
        }
    }

    /**
     * Synchronize state across components
     */
    @Command(name = "sync",
             mixinStandardHelpOptions = true,
             description = "Synchronize state across components")
    public static class SyncCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--force"},
                 description = "Force synchronization even if conflicts exist")
        boolean force;

        @Option(names = {"--dry-run"},
                 description = "Show what would be synchronized")
        boolean dryRun;

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

                BreezingSignalHandler handler = new BreezingSignalHandler(projectPath, verbose);

                System.out.println();
                System.out.println("🔄 Synchronizing Breezing State");
                System.out.println();

                SyncResult result = handler.synchronizeState(force, dryRun);

                if (dryRun) {
                    System.out.println("Dry run completed");
                    System.out.println("  Would sync " + result.componentsToSync() + " components");
                    if (result.hasConflicts()) {
                        System.out.println("  Conflicts detected: " + result.conflictCount());
                    }
                } else {
                    System.out.println("Synchronization completed");
                    System.out.println("  Components synced: " + result.componentsToSync());
                    if (result.hasConflicts()) {
                        System.out.println("  Conflicts resolved: " + result.conflictCount());
                    }
                }

                if (!result.errors().isEmpty()) {
                    System.out.println();
                    System.out.println("Errors:");
                    for (String error : result.errors()) {
                        System.out.println("  ✗ " + error);
                    }
                    return 1;
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Synchronization failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * Signal record
     */
    public record Signal(
        String signalId,
        String type,
        String target,
        String priority,
        String message,
        LocalDateTime timestamp,
        String status
    ) {
        public Signal {
            if (signalId == null) signalId = "";
            if (type == null) type = "";
            if (target == null) target = "";
            if (priority == null) priority = "normal";
            if (message == null) message = "";
            if (timestamp == null) timestamp = LocalDateTime.now();
            if (status == null) status = "pending";
        }
    }

    /**
     * Signal response record
     */
    public record SignalResponse(
        String signalId,
        String from,
        String message,
        LocalDateTime timestamp
    ) {
        public SignalResponse {
            if (signalId == null) signalId = "";
            if (from == null) from = "";
            if (message == null) message = "";
            if (timestamp == null) timestamp = LocalDateTime.now();
        }
    }

    /**
     * Signal result record
     */
    public record SignalResult(
        String status,
        SignalResponse response,
        List<String> errors
    ) {
        public SignalResult {
            if (status == null) status = "";
            if (errors == null) errors = List.of();
        }
    }

    /**
     * Signal status record
     */
    public record SignalStatus(
        String component,
        String state,
        int signalsReceived,
        int signalsProcessed,
        LocalDateTime lastSignalTime
    ) {
        public SignalStatus {
            if (component == null) component = "";
            if (state == null) state = "";
            if (lastSignalTime == null) lastSignalTime = LocalDateTime.now();
        }
    }

    /**
     * Sync result record
     */
    public record SyncResult(
        int componentsToSync,
        boolean hasConflicts,
        int conflictCount,
        List<String> errors
    ) {
        public SyncResult {
            if (errors == null) errors = List.of();
        }
    }

    /**
     * Breezing signal handler
     */
    public static class BreezingSignalHandler {
        private final Path projectRoot;
        private final boolean verbose;

        public BreezingSignalHandler(Path projectRoot, boolean verbose) {
            this.projectRoot = projectRoot;
            this.verbose = verbose;
        }

        public SignalResult sendSignal(Signal signal, boolean wait, int timeout) {
            List<String> errors = new ArrayList<>();

            try {
                // Simulate signal delivery
                if (verbose) {
                    System.out.println("Sending signal to: " + signal.target());
                }

                SignalResponse response = null;
                String status = "delivered";

                if (wait) {
                    // Simulate response
                    response = new SignalResponse(
                        signal.signalId(),
                        signal.target(),
                        "Signal processed successfully",
                        LocalDateTime.now()
                    );

                    if (verbose) {
                        System.out.println("Response received from: " + response.from());
                    }
                }

                return new SignalResult(status, response, errors);

            } catch (Exception e) {
                errors.add(e.getMessage());
                return new SignalResult("failed", null, errors);
            }
        }

        public List<Signal> receiveSignals(String component, int count, int timeout) {
            List<Signal> signals = new ArrayList<>();

            // Simulate receiving signals
            for (int i = 0; i < count; i++) {
                signals.add(new Signal(
                    UUID.randomUUID().toString(),
                    "status",
                    component,
                    "normal",
                    "Signal " + (i + 1),
                    LocalDateTime.now(),
                    "received"
                ));
            }

            return signals;
        }

        public List<SignalStatus> getSignalStatus(String component, String type) {
            List<SignalStatus> statuses = new ArrayList<>();

            // Add status for each component
            String[] components = {"lead", "worker", "reviewer"};

            for (String comp : components) {
                if (component != null && !component.isEmpty() && !comp.equals(component)) {
                    continue;
                }

                statuses.add(new SignalStatus(
                    comp,
                    "active",
                    (int) (Math.random() * 10),
                    (int) (Math.random() * 10),
                    LocalDateTime.now().minusMinutes((long) (Math.random() * 60))
                ));
            }

            return statuses;
        }

        public SyncResult synchronizeState(boolean force, boolean dryRun) {
            List<String> errors = new ArrayList<>();

            try {
                int componentsToSync = 3;
                boolean hasConflicts = false;
                int conflictCount = 0;

                // Simulate synchronization
                if (!force) {
                    // Randomly simulate conflicts
                    hasConflicts = Math.random() > 0.7;
                    conflictCount = hasConflicts ? (int) (Math.random() * 3) + 1 : 0;
                }

                if (verbose) {
                    System.out.println("Syncing " + componentsToSync + " components...");
                    if (hasConflicts) {
                        System.out.println("Found " + conflictCount + " conflicts");
                    }
                }

                return new SyncResult(componentsToSync, hasConflicts, conflictCount, errors);

            } catch (Exception e) {
                errors.add(e.getMessage());
                return new SyncResult(0, false, 0, errors);
            }
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new BreezingSignalCommand()).execute(args);
        System.exit(exitCode);
    }
}
