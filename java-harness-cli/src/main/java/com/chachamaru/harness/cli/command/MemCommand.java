package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Mem command for harness-mem companion operations.
 *
 * <p>This command provides harness-mem companion management:
 * <ul>
 *   <li>health - Check harness-mem daemon health</li>
 *   <li>status - Show current status</li>
 *   <li>setup - Set up harness-mem companion</li>
 *   <li>update - Update harness-mem companion</li>
 *   <li>doctor - Run diagnostics and repair</li>
 *   <li>off - Turn off harness-mem companion</li>
 *   <li>purge - Remove harness-mem data</li>
 *   <li>record-breezing-event - Record breezing events</li>
 *   <li>search-similar - Search similar past decisions</li>
 * </ul>
 * </p>
 */
@Command(name = "mem",
         mixinStandardHelpOptions = true,
         subcommands = {
             MemCommand.HealthCommand.class,
             MemCommand.StatusCommand.class,
             MemCommand.SetupCommand.class,
             MemCommand.UpdateCommand.class,
             MemCommand.DoctorCommand.class,
             MemCommand.OffCommand.class,
             MemCommand.PurgeCommand.class,
             MemCommand.RecordBreezingEventCommand.class,
             MemCommand.SearchSimilarCommand.class
         },
         description = "Manage harness-mem companion operations")
public class MemCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Health check command
     */
    @Command(name = "health",
             mixinStandardHelpOptions = true,
             description = "Check harness-mem daemon health")
    public static class HealthCommand implements Callable<Integer> {

        @Option(names = {"--json"},
                 description = "Output in JSON format")
        boolean jsonOutput;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                MemHealthChecker checker = new MemHealthChecker(verbose);
                MemHealthOutput result = checker.checkHealth();

                if (jsonOutput) {
                    outputJsonHealth(result);
                } else {
                    outputHumanHealth(result);
                }

                return result.healthy() ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Health check failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private void outputJsonHealth(MemHealthOutput result) {
            System.out.println("{");
            System.out.println("  \"healthy\": " + result.healthy() + ",");
            System.out.println("  \"reason\": \"" + escapeJson(result.reason()) + "\"");
            System.out.println("}");
        }

        private void outputHumanHealth(MemHealthOutput result) {
            System.out.println();
            System.out.println("🏥 Harness-Mem Health");
            System.out.println();
            System.out.println("Status: " + (result.healthy() ? "✓ Healthy" : "✗ Unhealthy"));

            if (result.reason() != null && !result.reason().isEmpty()) {
                System.out.println("Reason: " + result.reason());
            }

            System.out.println();
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    /**
     * Status command
     */
    @Command(name = "status",
             mixinStandardHelpOptions = true,
             description = "Show harness-mem companion status")
    public static class StatusCommand implements Callable<Integer> {

        @Option(names = {"--json"},
                 description = "Output in JSON format")
        boolean jsonOutput;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                MemStatusCollector collector = new MemStatusCollector(verbose);
                MemStatusReport report = collector.collectStatus();

                if (jsonOutput) {
                    outputJsonStatus(report);
                } else {
                    outputHumanStatus(report);
                }

                return report.allGreen() ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Status check failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private void outputJsonStatus(MemStatusReport report) {
            System.out.println("{");
            System.out.println("  \"status\": \"" + report.status() + "\",");
            System.out.println("  \"installed\": " + report.installed() + ",");
            System.out.println("  \"all_green\": " + report.allGreen() + ",");
            System.out.println("  \"failed_count\": " + report.failedCount() + ",");
            System.out.println("  \"backend\": \"" + report.backendMode() + "\",");

            if (report.fixCommand() != null && !report.fixCommand().isEmpty()) {
                System.out.println("  \"fix_command\": \"" + escapeJson(report.fixCommand()) + "\"");
            } else {
                System.out.println("  \"fix_command\": \"\"");
            }

            System.out.println("}");
        }

        private void outputHumanStatus(MemStatusReport report) {
            System.out.println();
            System.out.println("📊 Harness-Mem Status");
            System.out.println();

            if (!report.installed()) {
                System.out.println("Status: ℹ Not configured");
                System.out.println();
                System.out.println("Run: harness mem setup");
                return;
            }

            System.out.println("Status: " + (report.allGreen() ? "✓ Ready" : "⚠ Degraded"));
            System.out.println("Mode: " + report.backendMode());
            System.out.println("Failed checks: " + report.failedCount());

            if (report.fixCommand() != null && !report.fixCommand().isEmpty() && !report.allGreen()) {
                System.out.println();
                System.out.println("Fix: " + report.fixCommand());
            }

            System.out.println();
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    /**
     * Setup command
     */
    @Command(name = "setup",
             mixinStandardHelpOptions = true,
             description = "Set up harness-mem companion")
    public static class SetupCommand implements Callable<Integer> {

        @Option(names = {"--platform"},
                 description = "Target platform (default: auto-detect)")
        String platform;

        @Option(names = {"--skip-quality"},
                 description = "Skip quality checks")
        boolean skipQuality;

        @Option(names = {"--auto-update"},
                 description = "Enable auto-update")
        boolean autoUpdate;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                System.out.println();
                System.out.println("🔧 Setting up Harness-Mem Companion");
                System.out.println();

                MemSetupManager setupManager = new MemSetupManager(verbose);
                SetupResult result = setupManager.setup(platform, skipQuality, autoUpdate);

                if (result.success()) {
                    System.out.println("✓ Setup completed successfully");
                    return 0;
                } else {
                    System.out.println("✗ Setup failed: " + result.error());
                    return 1;
                }

            } catch (Exception e) {
                System.err.println("✗ Setup failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * Update command
     */
    @Command(name = "update",
             mixinStandardHelpOptions = true,
             description = "Update harness-mem companion")
    public static class UpdateCommand implements Callable<Integer> {

        @Option(names = {"--platform"},
                 description = "Target platform (default: auto-detect)")
        String platform;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                System.out.println();
                System.out.println("🔄 Updating Harness-Mem Companion");
                System.out.println();

                MemUpdateManager updateManager = new MemUpdateManager(verbose);
                UpdateResult result = updateManager.update(platform);

                if (result.success()) {
                    System.out.println("✓ Update completed successfully");
                    return 0;
                } else {
                    System.out.println("✗ Update failed: " + result.error());
                    return 1;
                }

            } catch (Exception e) {
                System.err.println("✗ Update failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * Doctor command
     */
    @Command(name = "doctor",
             mixinStandardHelpOptions = true,
             description = "Run diagnostics and repair")
    public static class DoctorCommand implements Callable<Integer> {

        @Option(names = {"--platform"},
                 description = "Target platform (default: auto-detect)")
        String platform;

        @Option(names = {"--fix"},
                 description = "Automatically fix issues")
        boolean autoFix;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                MemDoctor doctor = new MemDoctor(verbose);
                DoctorResult result = doctor.diagnoseAndFix(platform, autoFix);

                System.out.println();
                System.out.println("🩺 Harness-Mem Doctor");
                System.out.println();

                if (result.issues().isEmpty()) {
                    System.out.println("✓ No issues found");
                } else {
                    System.out.println("Issues found: " + result.issues().size());
                    System.out.println();

                    for (String issue : result.issues()) {
                        System.out.println("  • " + issue);
                    }

                    if (result.fixed() > 0) {
                        System.out.println();
                        System.out.println("Fixed: " + result.fixed() + " issue(s)");
                    }
                }

                System.out.println();
                return result.issues().isEmpty() ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Doctor failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * Off command
     */
    @Command(name = "off",
             mixinStandardHelpOptions = true,
             description = "Turn off harness-mem companion")
    public static class OffCommand implements Callable<Integer> {

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                System.out.println();
                System.out.println("🔌 Turning Off Harness-Mem Companion");
                System.out.println();

                MemController controller = new MemController(verbose);
                boolean success = controller.turnOff();

                if (success) {
                    System.out.println("✓ Harness-mem companion turned off");
                    return 0;
                } else {
                    System.out.println("✗ Failed to turn off companion");
                    return 1;
                }

            } catch (Exception e) {
                System.err.println("✗ Turn off failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * Purge command
     */
    @Command(name = "purge",
             mixinStandardHelpOptions = true,
             description = "Remove harness-mem data")
    public static class PurgeCommand implements Callable<Integer> {

        @Option(names = {"--confirm-purge"},
                 description = "Confirm purge operation (required)")
        boolean confirmPurge;

        @Option(names = {"--platform"},
                 description = "Target platform (default: auto-detect)")
        String platform;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                if (!confirmPurge && System.getenv("CLAUDE_CODE_HARNESS_MEM_CONFIRM_PURGE") == null) {
                    System.err.println("✗ Refusing to purge harness-mem data without explicit confirmation.");
                    System.err.println("Run: harness mem purge --confirm-purge");
                    return 2;
                }

                System.out.println();
                System.out.println("🗑️  Purging Harness-Mem Data");
                System.out.println();

                MemController controller = new MemController(verbose);
                boolean success = controller.purge();

                if (success) {
                    System.out.println("✓ Harness-mem data purged");
                    return 0;
                } else {
                    System.out.println("✗ Failed to purge data");
                    return 1;
                }

            } catch (Exception e) {
                System.err.println("✗ Purge failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * Record breezing event command
     */
    @Command(name = "record-breezing-event",
             mixinStandardHelpOptions = true,
             description = "Record breezing workflow event")
    public static class RecordBreezingEventCommand implements Callable<Integer> {

        @Option(names = {"--type"},
                 description = "Event type (required)",
                 required = true)
        String type;

        @Option(names = {"--project"},
                 description = "Project name (required)",
                 required = true)
        String project;

        @Option(names = {"--session"},
                 description = "Session ID (required)",
                 required = true)
        String session;

        @Option(names = {"--content"},
                 description = "Event content (required)",
                 required = true)
        String content;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                // Validate event type
                if (!isValidBreezingEventType(type)) {
                    System.err.println("✗ Unknown breezing event type: " + type);
                    System.err.println("Valid types: brief-confirmed, run-started, worker-result, aggregation-done");
                    return 1;
                }

                BreezingEventRecorder recorder = new BreezingEventRecorder(verbose);
                boolean success = recorder.recordEvent(type, project, session, content);

                if (success) {
                    if (verbose) {
                        System.out.println("✓ Breezing event recorded");
                    }
                    return 0;
                } else {
                    System.err.println("✗ Failed to record event");
                    return 1;
                }

            } catch (Exception e) {
                System.err.println("✗ Event recording failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private boolean isValidBreezingEventType(String type) {
            return type.equals("brief-confirmed") ||
                   type.equals("run-started") ||
                   type.equals("worker-result") ||
                   type.equals("aggregation-done");
        }
    }

    /**
     * Search similar command
     */
    @Command(name = "search-similar",
             mixinStandardHelpOptions = true,
             description = "Search similar past decisions")
    public static class SearchSimilarCommand implements Callable<Integer> {

        @Option(names = {"--project"},
                 description = "Project name (required)")
        String project;

        @Option(names = {"--query"},
                 description = "Search query (required)")
        String query;

        @Option(names = {"--format"},
                 description = "Output format (default: json)",
                 defaultValue = "json")
        String format;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                // Fail-open if required parameters are missing
                if (project == null || project.isEmpty() || query == null || query.isEmpty()) {
                    System.out.println("[]");
                    return 0;
                }

                if (!format.equals("json")) {
                    System.err.println("✗ Unsupported format: " + format);
                    return 1;
                }

                SimilarDecisionSearcher searcher = new SimilarDecisionSearcher(verbose);
                List<SimilarDecision> results = searcher.search(project, query);

                if (results == null) {
                    results = List.of();
                }

                // Output as JSON array
                System.out.println("[");
                for (int i = 0; i < results.size(); i++) {
                    SimilarDecision decision = results.get(i);
                    System.out.println("  {");
                    System.out.println("    \"id\": \"" + decision.id() + "\",");
                    System.out.println("    \"summary\": \"" + escapeJson(decision.summary()) + "\",");
                    System.out.println("    \"similarity\": " + decision.similarity() + ",");
                    System.out.println("    \"timestamp\": \"" + decision.timestamp().format(
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\"");
                    System.out.println("  }" + (i < results.size() - 1 ? "," : ""));
                }
                System.out.println("]");

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Search failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    /**
     * Records
     */
    public record MemHealthOutput(
        boolean healthy,
        String reason
    ) {
        public MemHealthOutput {
            if (reason == null) reason = "";
        }
    }

    public record MemStatusReport(
        String status,
        boolean installed,
        boolean allGreen,
        int failedCount,
        String backendMode,
        String fixCommand
    ) {
        public MemStatusReport {
            if (status == null) status = "";
            if (backendMode == null) backendMode = "";
            if (fixCommand == null) fixCommand = "";
        }
    }

    public record SetupResult(
        boolean success,
        String error
    ) {
        public SetupResult {
            if (error == null) error = "";
        }
    }

    public record UpdateResult(
        boolean success,
        String error
    ) {
        public UpdateResult {
            if (error == null) error = "";
        }
    }

    public record DoctorResult(
        List<String> issues,
        int fixed
    ) {
        public DoctorResult {
            if (issues == null) issues = List.of();
        }
    }

    public record SimilarDecision(
        String id,
        String summary,
        double similarity,
        LocalDateTime timestamp
    ) {
        public SimilarDecision {
            if (id == null) id = "";
            if (summary == null) summary = "";
            if (timestamp == null) timestamp = LocalDateTime.now();
        }
    }

    /**
     * Mem health checker - validates harness-mem installation and daemon reachability
     */
    public static class MemHealthChecker {
        private final boolean verbose;
        private static final String DEFAULT_MEM_HOST = "127.0.0.1";
        private static final String DEFAULT_MEM_PORT = "37888";
        private static final int CONNECTION_TIMEOUT_MS = 500;

        public MemHealthChecker(boolean verbose) {
            this.verbose = verbose;
        }

        public MemHealthOutput checkHealth() {
            try {
                String home = System.getProperty("user.home");
                String harnessMemHome = System.getenv().getOrDefault("HARNESS_MEM_HOME",
                    Paths.get(home, ".harness-mem").toString());
                String claudeMem = Paths.get(home, ".claude-mem").toString();

                // Check if harness-mem is configured
                Path harnessMemPath = Paths.get(harnessMemHome);
                Path claudeMemPath = Paths.get(claudeMem);

                boolean harnessMemExists = Files.exists(harnessMemPath);
                boolean claudeMemExists = Files.exists(claudeMemPath);

                if (!harnessMemExists && !claudeMemExists) {
                    return new MemHealthOutput(true, "not-configured");
                }

                // Determine which installation to check
                Path targetPath = harnessMemExists ? harnessMemPath : claudeMemPath;

                // Check if it looks like a proper installation
                if (!looksConfiguredHarnessMem(targetPath)) {
                    // For legacy .claude-mem, check specific files
                    if (targetPath.equals(claudeMemPath)) {
                        Path settingsPath = targetPath.resolve("settings.json");
                        Path supervisorPath = targetPath.resolve("supervisor.json");

                        boolean settingsOK = Files.exists(settingsPath);
                        boolean supervisorOK = Files.exists(supervisorPath);

                        if (!settingsOK && !supervisorOK) {
                            return new MemHealthOutput(false, "corrupted");
                        }
                    } else {
                        return new MemHealthOutput(true, "not-configured");
                    }
                }

                // Check daemon reachability
                if (!probeDaemon()) {
                    return new MemHealthOutput(false, "daemon-unreachable");
                }

                return new MemHealthOutput(true, "");

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Health check exception: " + e.getMessage());
                }
                // Home directory resolution failure - treat as not configured
                return new MemHealthOutput(true, "not-configured");
            }
        }

        private boolean looksConfiguredHarnessMem(Path root) {
            Path configPath = root.resolve("config.json");
            Path runtimeCLI = root.resolve("runtime").resolve("harness-mem")
                .resolve("scripts").resolve("harness-mem");
            Path dbPath = root.resolve("harness-mem.db");

            return Files.exists(configPath) ||
                   Files.exists(runtimeCLI) ||
                   Files.exists(dbPath);
        }

        private boolean probeDaemon() {
            String host = System.getenv().getOrDefault("HARNESS_MEM_HOST", DEFAULT_MEM_HOST);
            String port = System.getenv().getOrDefault("HARNESS_MEM_PORT", DEFAULT_MEM_PORT);

            if (verbose) {
                System.out.println("Probing daemon at " + host + ":" + port);
            }

            try (Socket socket = new Socket(host, Integer.parseInt(port))) {
                socket.setSoTimeout(CONNECTION_TIMEOUT_MS);
                return true;
            } catch (Exception e) {
                if (verbose) {
                    System.out.println("Daemon probe failed: " + e.getMessage());
                }
                return false;
            }
        }
    }

    /**
     * Mem status collector - gets current status from harness-mem daemon
     */
    public static class MemStatusCollector {
        private final boolean verbose;
        private static final String DEFAULT_MEM_HOST = "127.0.0.1";
        private static final String DEFAULT_MEM_PORT = "37888";
        private static final int HTTP_TIMEOUT_MS = 2000;

        public MemStatusCollector(boolean verbose) {
            this.verbose = verbose;
        }

        public MemStatusReport collectStatus() {
            try {
                String home = System.getProperty("user.home");
                String harnessMemHome = System.getenv().getOrDefault("HARNESS_MEM_HOME",
                    Paths.get(home, ".harness-mem").toString());
                String claudeMem = Paths.get(home, ".claude-mem").toString();

                boolean harnessMemExists = Files.exists(Paths.get(harnessMemHome));
                boolean claudeMemExists = Files.exists(Paths.get(claudeMem));

                if (!harnessMemExists && !claudeMemExists) {
                    return new MemStatusReport("not_configured", false, false, 0, "", "harness mem setup");
                }

                // Try to get status from daemon via HTTP
                String host = System.getenv().getOrDefault("HARNESS_MEM_HOST", DEFAULT_MEM_HOST);
                String port = System.getenv().getOrDefault("HARNESS_MEM_PORT", DEFAULT_MEM_PORT);
                String statusUrl = "http://" + host + ":" + port + "/api/status";

                try {
                    HttpURLConnection conn = createConnection(statusUrl);
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(HTTP_TIMEOUT_MS);
                    conn.setReadTimeout(HTTP_TIMEOUT_MS);

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        String response = readResponse(conn.getInputStream());
                        // Parse JSON response (simplified - in real implementation use proper JSON parser)
                        return parseStatusResponse(response);
                    } else {
                        // Daemon not responding - degraded status
                        return new MemStatusReport("degraded", true, false, 1, "node", "harness mem doctor");
                    }
                } catch (Exception e) {
                    if (verbose) {
                        System.out.println("Failed to reach daemon: " + e.getMessage());
                    }
                    return new MemStatusReport("degraded", true, false, 1, "node", "harness mem doctor");
                }

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Status collection failed: " + e.getMessage());
                }
                return new MemStatusReport("error", true, false, 1, "", "harness mem doctor");
            }
        }

        private HttpURLConnection createConnection(String urlString) throws IOException {
            URL url = new URL(urlString);
            return (HttpURLConnection) url.openConnection();
        }

        private String readResponse(InputStream inputStream) throws IOException {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        }

        private MemStatusReport parseStatusResponse(String response) {
            // Simplified JSON parsing - in real implementation use Jackson or similar
            // Extract status, backend_mode, failed_count from response
            // For now return defaults
            return new MemStatusReport("ready", true, true, 0, "node", "");
        }
    }

    /**
     * Mem setup manager - manages harness-mem companion setup
     */
    public static class MemSetupManager {
        private final boolean verbose;

        public MemSetupManager(boolean verbose) {
            this.verbose = verbose;
        }

        public SetupResult setup(String platform, boolean skipQuality, boolean autoUpdate) {
            try {
                // Check if already set up
                MemHealthChecker checker = new MemHealthChecker(verbose);
                MemHealthOutput health = checker.checkHealth();

                if (health.healthy() && health.reason().equals("")) {
                    System.out.println("harness-mem companion already ready");
                    return new SetupResult(true, "");
                }

                // Build setup command
                List<String> command = buildSetupCommand(platform, skipQuality, autoUpdate);

                if (verbose) {
                    System.out.println("Running setup: " + String.join(" ", command));
                }

                // Execute setup command (npx harness-mem setup)
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);

                Process process = pb.start();
                String output = readProcessOutput(process);
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    return new SetupResult(true, "");
                } else {
                    return new SetupResult(false, "Setup failed with exit code " + exitCode);
                }

            } catch (Exception e) {
                return new SetupResult(false, e.getMessage());
            }
        }

        private List<String> buildSetupCommand(String platform, boolean skipQuality, boolean autoUpdate) {
            List<String> command = new ArrayList<>();

            // Detect platform
            String detectedPlatform = (platform != null && !platform.isEmpty()) ?
                platform : detectPlatform();

            command.add("npx");
            command.add("-y");
            command.add("harness-mem");
            command.add("setup");
            command.add("--platform=" + detectedPlatform);

            if (skipQuality) {
                command.add("--skip-quality");
            }

            if (autoUpdate) {
                command.add("--auto-update=enable");
            }

            return command;
        }

        private String detectPlatform() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                return "win32";
            } else if (os.contains("mac")) {
                return "darwin";
            } else {
                return "linux";
            }
        }

        private String readProcessOutput(Process process) throws IOException {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (verbose) {
                        System.out.println(line);
                    }
                }
                return output.toString();
            }
        }
    }

    /**
     * Mem update manager - manages harness-mem companion updates
     */
    public static class MemUpdateManager {
        private final boolean verbose;

        public MemUpdateManager(boolean verbose) {
            this.verbose = verbose;
        }

        public UpdateResult update(String platform) {
            try {
                List<String> command = buildUpdateCommand(platform);

                if (verbose) {
                    System.out.println("Running update: " + String.join(" ", command));
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);

                Process process = pb.start();
                String output = readProcessOutput(process);
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    return new UpdateResult(true, "");
                } else {
                    return new UpdateResult(false, "Update failed with exit code " + exitCode);
                }

            } catch (Exception e) {
                return new UpdateResult(false, e.getMessage());
            }
        }

        private List<String> buildUpdateCommand(String platform) {
            List<String> command = new ArrayList<>();
            String detectedPlatform = (platform != null && !platform.isEmpty()) ?
                platform : detectPlatform();

            command.add("npx");
            command.add("-y");
            command.add("harness-mem");
            command.add("update");
            command.add("--platform=" + detectedPlatform);

            return command;
        }

        private String detectPlatform() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                return "win32";
            } else if (os.contains("mac")) {
                return "darwin";
            } else {
                return "linux";
            }
        }

        private String readProcessOutput(Process process) throws IOException {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (verbose) {
                        System.out.println(line);
                    }
                }
                return output.toString();
            }
        }
    }

    /**
     * Mem doctor - runs diagnostics and repair
     */
    public static class MemDoctor {
        private final boolean verbose;

        public MemDoctor(boolean verbose) {
            this.verbose = verbose;
        }

        public DoctorResult diagnoseAndFix(String platform, boolean autoFix) {
            List<String> issues = new ArrayList<>();
            int fixed = 0;

            try {
                List<String> command = buildDoctorCommand(platform);

                if (verbose) {
                    System.out.println("Running doctor: " + String.join(" ", command));
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);

                Process process = pb.start();
                String output = readProcessOutput(process);
                int exitCode = process.waitFor();

                // Parse output for issues
                if (exitCode != 0) {
                    issues.add("harness-mem doctor reported errors");
                    if (autoFix) {
                        // Attempt auto-fix
                        fixed = attemptAutoFix();
                    }
                }

                return new DoctorResult(issues, fixed);

            } catch (Exception e) {
                issues.add("Doctor execution failed: " + e.getMessage());
                return new DoctorResult(issues, fixed);
            }
        }

        private List<String> buildDoctorCommand(String platform) {
            List<String> command = new ArrayList<>();
            String detectedPlatform = (platform != null && !platform.isEmpty()) ?
                platform : detectPlatform();

            command.add("npx");
            command.add("-y");
            command.add("harness-mem");
            command.add("doctor");
            command.add("--platform=" + detectedPlatform);

            return command;
        }

        private String detectPlatform() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                return "win32";
            } else if (os.contains("mac")) {
                return "darwin";
            } else {
                return "linux";
            }
        }

        private String readProcessOutput(Process process) throws IOException {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    if (verbose) {
                        System.out.println(line);
                    }
                }
                return output.toString();
            }
        }

        private int attemptAutoFix() {
            try {
                MemSetupManager setupManager = new MemSetupManager(verbose);
                SetupResult result = setupManager.setup(null, true, true);
                return result.success() ? 1 : 0;
            } catch (Exception e) {
                return 0;
            }
        }
    }

    /**
     * Mem controller - controls harness-mem daemon lifecycle
     */
    public static class MemController {
        private final boolean verbose;

        public MemController(boolean verbose) {
            this.verbose = verbose;
        }

        public boolean turnOff() {
            try {
                // Send "recall off" command to harness-mem
                List<String> command = Arrays.asList(
                    "npx", "-y", "harness-mem", "recall", "off"
                );

                if (verbose) {
                    System.out.println("Turning off: " + String.join(" ", command));
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);

                Process process = pb.start();
                int exitCode = process.waitFor();

                return exitCode == 0;

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Turn off failed: " + e.getMessage());
                }
                return false;
            }
        }

        public boolean purge() {
            try {
                // Run uninstall with --purge-db
                List<String> command = Arrays.asList(
                    "npx", "-y", "harness-mem", "uninstall", "--purge-db"
                );

                if (verbose) {
                    System.out.println("Purging: " + String.join(" ", command));
                }

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);

                Process process = pb.start();
                int exitCode = process.waitFor();

                return exitCode == 0;

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Purge failed: " + e.getMessage());
                }
                return false;
            }
        }
    }

    /**
     * Breezing event recorder - records breezing workflow events to harness-mem
     */
    public static class BreezingEventRecorder {
        private final boolean verbose;
        private static final String DEFAULT_MEM_HOST = "127.0.0.1";
        private static final String DEFAULT_MEM_PORT = "37888";
        private static final int HTTP_TIMEOUT_MS = 5000;

        public BreezingEventRecorder(boolean verbose) {
            this.verbose = verbose;
        }

        public boolean recordEvent(String type, String project, String session, String content) {
            try {
                String host = System.getenv().getOrDefault("HARNESS_MEM_HOST", DEFAULT_MEM_HOST);
                String port = System.getenv().getOrDefault("HARNESS_MEM_PORT", DEFAULT_MEM_PORT);
                String eventUrl = "http://" + host + ":" + port + "/api/breezing-events";

                // Build event payload
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", type);
                payload.put("project", project);
                payload.put("session", session);
                payload.put("content", content);
                payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                String jsonPayload = buildJsonPayload(payload);

                if (verbose) {
                    System.out.println("Recording event: " + type + " for project: " + project);
                }

                HttpURLConnection conn = createConnection(eventUrl);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(HTTP_TIMEOUT_MS);
                conn.setReadTimeout(HTTP_TIMEOUT_MS);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                return responseCode == 200 || responseCode == 201;

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Event recording failed: " + e.getMessage());
                }
                return false;
            }
        }

        private HttpURLConnection createConnection(String urlString) throws IOException {
            URL url = new URL(urlString);
            return (HttpURLConnection) url.openConnection();
        }

        private String buildJsonPayload(Map<String, Object> data) {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(entry.getKey()).append("\":");
                Object value = entry.getValue();
                if (value instanceof String) {
                    json.append("\"").append(escapeJson((String) value)).append("\"");
                } else if (value instanceof Boolean) {
                    json.append(value);
                } else {
                    json.append("\"").append(value).append("\"");
                }
                first = false;
            }
            json.append("}");
            return json.toString();
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    /**
     * Similar decision searcher - searches for similar past decisions in harness-mem
     */
    public static class SimilarDecisionSearcher {
        private final boolean verbose;
        private static final String DEFAULT_MEM_HOST = "127.0.0.1";
        private static final String DEFAULT_MEM_PORT = "37888";
        private static final int HTTP_TIMEOUT_MS = 5000;

        public SimilarDecisionSearcher(boolean verbose) {
            this.verbose = verbose;
        }

        public List<SimilarDecision> search(String project, String query) {
            try {
                String host = System.getenv().getOrDefault("HARNESS_MEM_HOST", DEFAULT_MEM_HOST);
                String port = System.getenv().getOrDefault("HARNESS_MEM_PORT", DEFAULT_MEM_PORT);
                String searchUrl = "http://" + host + ":" + port + "/api/similar-decisions" +
                    "?project=" + java.net.URLEncoder.encode(project, "UTF-8") +
                    "&query=" + java.net.URLEncoder.encode(query, "UTF-8");

                if (verbose) {
                    System.out.println("Searching for similar decisions in project: " + project);
                }

                HttpURLConnection conn = createConnection(searchUrl);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(HTTP_TIMEOUT_MS);
                conn.setReadTimeout(HTTP_TIMEOUT_MS);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    String response = readResponse(conn.getInputStream());
                    return parseSearchResponse(response);
                } else {
                    return List.of();
                }

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Search failed: " + e.getMessage());
                }
                return List.of();
            }
        }

        private HttpURLConnection createConnection(String urlString) throws IOException {
            URL url = new URL(urlString);
            return (HttpURLConnection) url.openConnection();
        }

        private String readResponse(InputStream inputStream) throws IOException {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        }

        private List<SimilarDecision> parseSearchResponse(String response) {
            // Simplified JSON parsing - in real implementation use Jackson
            // For now return empty list
            return List.of();
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new MemCommand()).execute(args);
        System.exit(exitCode);
    }
}
