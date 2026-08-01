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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * CI Status command for querying CI status.
 *
 * <p>This command provides CI status querying capabilities:
 * <ul>
 *   <li>show - Show current CI status</li>
 *   <li>query - Query CI status with filters</li>
 *   <li>history - Show CI status history</li>
 *   <li>watch - Watch CI status changes</li>
 * </ul>
 * </p>
 */
@Command(name = "ci-status",
         mixinStandardHelpOptions = true,
         subcommands = {
             CiStatusCommand.ShowCommand.class,
             CiStatusCommand.QueryCommand.class,
             CiStatusCommand.HistoryCommand.class
         },
         description = "Query CI status and history")
public class CiStatusCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Show current CI status
     */
    @Command(name = "show",
             mixinStandardHelpOptions = true,
             description = "Show current CI status")
    public static class ShowCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"-b", "--branch"},
                 description = "Branch to show status for",
                 defaultValue = "master")
        String branch;

        @Option(names = {"--platform"},
                 description = "CI platform: github, gitlab, auto",
                 defaultValue = "auto")
        String platform;

        @Option(names = {"--format"},
                 description = "Output format: table, json, detailed",
                 defaultValue = "table")
        String format;

        @Option(names = {"--compact", "-c"},
                 description = "Compact output format")
        boolean compact;

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

                // Detect platform if auto
                String detectedPlatform = platform;
                if ("auto".equals(platform)) {
                    detectedPlatform = detectPlatform(projectPath);
                    if (verbose) {
                        System.out.println("Detected platform: " + detectedPlatform);
                    }
                }

                CiStatusQuerier querier = createQuerier(detectedPlatform, projectPath, verbose);
                CiStatusDetail status = querier.getCurrentStatus(branch);

                // Output result
                if ("json".equals(format)) {
                    outputJsonResult(status);
                } else if ("detailed".equals(format)) {
                    outputDetailedResult(status);
                } else if (compact) {
                    outputCompactResult(status);
                } else {
                    outputTableResult(status);
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Status show failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private String detectPlatform(Path projectPath) {
            Path gitlabFile = projectPath.resolve(".gitlab-ci.yml");
            return Files.exists(gitlabFile) ? "gitlab" : "github";
        }

        private CiStatusQuerier createQuerier(String platform, Path projectPath, boolean verbose) {
            return switch (platform.toLowerCase()) {
                case "gitlab" -> new GitLabStatusQuerier(projectPath, verbose);
                default -> new GitHubStatusQuerier(projectPath, verbose);
            };
        }

        private void outputJsonResult(CiStatusDetail status) {
            System.out.println("{");
            System.out.println("  \"platform\": \"" + status.platform() + "\",");
            System.out.println("  \"branch\": \"" + status.branch() + "\",");
            System.out.println("  \"status\": \"" + status.status() + "\",");
            System.out.println("  \"state\": \"" + status.state() + "\",");
            System.out.println("  \"lastUpdate\": \"" + status.lastUpdate().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\",");

            if (!status.pipelines().isEmpty()) {
                System.out.println("  \"pipelines\": [");
                for (int i = 0; i < status.pipelines().size(); i++) {
                    PipelineInfo pipeline = status.pipelines().get(i);
                    System.out.println("    {");
                    System.out.println("      \"id\": \"" + pipeline.id() + "\",");
                    System.out.println("      \"name\": \"" + escapeJson(pipeline.name()) + "\",");
                    System.out.println("      \"status\": \"" + pipeline.status() + "\",");
                    System.out.println("      \"duration\": " + pipeline.duration() + ",");
                    System.out.println("      \"url\": \"" + escapeJson(pipeline.url()) + "\"");
                    System.out.println("    }" + (i < status.pipelines().size() - 1 ? "," : ""));
                }
                System.out.println("  ]");
            }

            System.out.println("}");
        }

        private void outputDetailedResult(CiStatusDetail status) {
            System.out.println();
            System.out.println("📊 CI Status Detail");
            System.out.println();
            System.out.println("Platform: " + status.platform());
            System.out.println("Branch: " + status.branch());
            System.out.println("Status: " + getStatusIcon(status.status()) + " " + status.status());
            System.out.println("State: " + status.state());
            System.out.println("Last Update: " + formatRelativeTime(status.lastUpdate()));

            if (!status.pipelines().isEmpty()) {
                System.out.println();
                System.out.println("Pipelines:");
                for (PipelineInfo pipeline : status.pipelines()) {
                    System.out.println("  " + getStatusIcon(pipeline.status()) + " " +
                        pipeline.name() + " [" + pipeline.id() + "]");
                    System.out.println("      Status: " + pipeline.status());
                    System.out.println("      Duration: " + formatDuration(pipeline.duration()));
                    System.out.println("      URL: " + pipeline.url());
                    System.out.println();
                }
            }

            if (status.pendingApprovals() > 0) {
                System.out.println();
                System.out.println("⚠️  Pending Approvals: " + status.pendingApprovals());
            }
        }

        private void outputCompactResult(CiStatusDetail status) {
            System.out.printf("%-15s %-15s %-10s%n",
                "Platform", "Branch", "Status");
            System.out.println("-".repeat(50));

            System.out.printf("%-15s %-15s %-10s%n",
                truncate(status.platform(), 15),
                truncate(status.branch(), 15),
                getStatusIcon(status.status()) + " " + truncate(status.status(), 8));
        }

        private void outputTableResult(CiStatusDetail status) {
            System.out.println();
            System.out.println("📊 Current CI Status");
            System.out.println();
            System.out.printf("%-15s %-15s %-15s %-15s%n",
                "Platform", "Branch", "Status", "State");
            System.out.println("-".repeat(70));

            System.out.printf("%-15s %-15s %-15s %-15s%n",
                truncate(status.platform(), 15),
                truncate(status.branch(), 15),
                getStatusIcon(status.status()) + " " + truncate(status.status(), 13),
                truncate(status.state(), 15));

            if (!status.pipelines().isEmpty()) {
                System.out.println();
                System.out.println("Pipelines:");
                for (PipelineInfo pipeline : status.pipelines()) {
                    System.out.println("  " + getStatusIcon(pipeline.status()) + " " +
                        pipeline.name() + " [" + pipeline.id() + "] - " + pipeline.status());
                }
            }

            System.out.println();
            System.out.println("Last update: " + formatRelativeTime(status.lastUpdate()));
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "success", "passed", "completed" -> "✓";
                case "failure", "failed", "error" -> "✗";
                case "pending", "waiting" -> "⏳";
                case "running", "in_progress" -> "▶";
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

        private String formatDuration(long seconds) {
            if (seconds < 60) return seconds + "s";
            if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
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
     * Query CI status with filters
     */
    @Command(name = "query",
             mixinStandardHelpOptions = true,
             description = "Query CI status with filters")
    public static class QueryCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--branch"},
                 description = "Filter by branch")
        String branch;

        @Option(names = {"--status"},
                 description = "Filter by status")
        String status;

        @Option(names = {"--state"},
                 description = "Filter by state")
        String state;

        @Option(names = {"--since"},
                 description = "Show entries since timestamp")
        String since;

        @Option(names = {"--limit"},
                 description = "Limit number of results",
                 defaultValue = "20")
        int limit;

        @Option(names = {"--sort"},
                 description = "Sort by: time, status, branch, duration",
                 defaultValue = "time")
        String sortBy;

        @Option(names = {"--format"},
                 description = "Output format: table, json, csv",
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

                String platform = detectPlatform(projectPath);
                CiStatusQuerier querier = createQuerier(platform, projectPath, verbose);
                List<CiStatusDetail> results = querier.queryStatus(branch, status, state, since, limit, sortBy);

                if ("json".equals(format)) {
                    outputJsonResults(results);
                } else if ("csv".equals(format)) {
                    outputCsvResults(results);
                } else {
                    outputTableResults(results);
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Query failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private String detectPlatform(Path projectPath) {
            Path gitlabFile = projectPath.resolve(".gitlab-ci.yml");
            return Files.exists(gitlabFile) ? "gitlab" : "github";
        }

        private CiStatusQuerier createQuerier(String platform, Path projectPath, boolean verbose) {
            return switch (platform.toLowerCase()) {
                case "gitlab" -> new GitLabStatusQuerier(projectPath, verbose);
                default -> new GitHubStatusQuerier(projectPath, verbose);
            };
        }

        private void outputJsonResults(List<CiStatusDetail> results) {
            System.out.println("[");
            for (int i = 0; i < results.size(); i++) {
                CiStatusDetail status = results.get(i);
                System.out.println("  {");
                System.out.println("    \"platform\": \"" + status.platform() + "\",");
                System.out.println("    \"branch\": \"" + status.branch() + "\",");
                System.out.println("    \"status\": \"" + status.status() + "\",");
                System.out.println("    \"state\": \"" + status.state() + "\"");
                System.out.println("  }" + (i < results.size() - 1 ? "," : ""));
            }
            System.out.println("]");
        }

        private void outputCsvResults(List<CiStatusDetail> results) {
            System.out.println("platform,branch,status,state,lastUpdate");
            for (CiStatusDetail status : results) {
                System.out.println(status.platform() + "," +
                    status.branch() + "," +
                    status.status() + "," +
                    status.state() + "," +
                    status.lastUpdate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        }

        private void outputTableResults(List<CiStatusDetail> results) {
            System.out.println();
            System.out.println("📋 Query Results");
            System.out.println();
            System.out.printf("%-15s %-15s %-15s %-15s %-20s%n",
                "Platform", "Branch", "Status", "State", "Last Update");
            System.out.println("-".repeat(90));

            for (CiStatusDetail status : results) {
                System.out.printf("%-15s %-15s %-15s %-15s %-20s%n",
                    truncate(status.platform(), 15),
                    truncate(status.branch(), 15),
                    getStatusIcon(status.status()) + " " + truncate(status.status(), 13),
                    truncate(status.state(), 15),
                    status.lastUpdate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }

            System.out.println();
            System.out.println("Total: " + results.size() + " result(s)");
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "success", "passed", "completed" -> "✓";
                case "failure", "failed", "error" -> "✗";
                case "pending", "waiting" -> "⏳";
                case "running", "in_progress" -> "▶";
                default -> "?";
            };
        }

        private String truncate(String s, int maxLength) {
            if (s == null) return "";
            return s.length() > maxLength ? s.substring(0, maxLength - 3) + "..." : s;
        }
    }

    /**
     * Show CI status history
     */
    @Command(name = "history",
             mixinStandardHelpOptions = true,
             description = "Show CI status history")
    public static class HistoryCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"-n", "--count"},
                 description = "Number of history entries to show",
                 defaultValue = "10")
        int count;

        @Option(names = {"--branch"},
                 description = "Filter by branch")
        String branch;

        @Option(names = {"--type"},
                 description = "Type of history: all, failures, successes",
                 defaultValue = "all")
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

                String platform = detectPlatform(projectPath);
                CiStatusQuerier querier = createQuerier(platform, projectPath, verbose);
                List<StatusHistoryEntry> history = querier.getHistory(branch, type, count);

                if ("json".equals(format)) {
                    outputJsonHistory(history);
                } else if ("detailed".equals(format)) {
                    outputDetailedHistory(history);
                } else {
                    outputTableHistory(history);
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ History command failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private String detectPlatform(Path projectPath) {
            Path gitlabFile = projectPath.resolve(".gitlab-ci.yml");
            return Files.exists(gitlabFile) ? "gitlab" : "github";
        }

        private CiStatusQuerier createQuerier(String platform, Path projectPath, boolean verbose) {
            return switch (platform.toLowerCase()) {
                case "gitlab" -> new GitLabStatusQuerier(projectPath, verbose);
                default -> new GitHubStatusQuerier(projectPath, verbose);
            };
        }

        private void outputJsonHistory(List<StatusHistoryEntry> history) {
            System.out.println("[");
            for (int i = 0; i < history.size(); i++) {
                StatusHistoryEntry entry = history.get(i);
                System.out.println("  {");
                System.out.println("    \"timestamp\": \"" + entry.timestamp().format(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\",");
                System.out.println("    \"branch\": \"" + entry.branch() + "\",");
                System.out.println("    \"status\": \"" + entry.status() + "\",");
                System.out.println("    \"changeType\": \"" + entry.changeType() + "\"");
                System.out.println("  }" + (i < history.size() - 1 ? "," : ""));
            }
            System.out.println("]");
        }

        private void outputDetailedHistory(List<StatusHistoryEntry> history) {
            System.out.println();
            System.out.println("📜 CI Status History");
            System.out.println();

            for (StatusHistoryEntry entry : history) {
                System.out.println("[" + entry.timestamp().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "]");
                System.out.println("  Branch: " + entry.branch());
                System.out.println("  Status: " + getStatusIcon(entry.status()) + " " + entry.status());
                System.out.println("  Change: " + entry.changeType());
                System.out.println("-".repeat(60));
            }

            System.out.println();
            System.out.println("Total: " + history.size() + " entr" + (history.size() == 1 ? "y" : "ies"));
        }

        private void outputTableHistory(List<StatusHistoryEntry> history) {
            System.out.println();
            System.out.println("📜 CI Status History");
            System.out.println();
            System.out.printf("%-20s %-15s %-15s %-15s%n",
                "Time", "Branch", "Status", "Change");
            System.out.println("-".repeat(70));

            for (StatusHistoryEntry entry : history) {
                System.out.printf("%-20s %-15s %-15s %-15s%n",
                    entry.timestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    truncate(entry.branch(), 15),
                    getStatusIcon(entry.status()) + " " + truncate(entry.status(), 13),
                    truncate(entry.changeType(), 15));
            }

            System.out.println();
            System.out.println("Total: " + history.size() + " entr" + (history.size() == 1 ? "y" : "ies"));
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "success", "passed", "completed" -> "✓";
                case "failure", "failed", "error" -> "✗";
                case "pending", "waiting" -> "⏳";
                case "running", "in_progress" -> "▶";
                default -> "?";
            };
        }

        private String truncate(String s, int maxLength) {
            if (s == null) return "";
            return s.length() > maxLength ? s.substring(0, maxLength - 3) + "..." : s;
        }
    }

    /**
     * CI status querier interface
     */
    public interface CiStatusQuerier {
        CiStatusDetail getCurrentStatus(String branch) throws Exception;
        List<CiStatusDetail> queryStatus(String branch, String status, String state,
                                         String since, int limit, String sortBy) throws Exception;
        List<StatusHistoryEntry> getHistory(String branch, String type, int count) throws Exception;
    }

    /**
     * GitHub status querier implementation
     */
    public static class GitHubStatusQuerier implements CiStatusQuerier {
        private final Path projectRoot;
        private final boolean verbose;

        public GitHubStatusQuerier(Path projectRoot, boolean verbose) {
            this.projectRoot = projectRoot;
            this.verbose = verbose;
        }

        @Override
        public CiStatusDetail getCurrentStatus(String branch) throws Exception {
            List<PipelineInfo> pipelines = new ArrayList<>();
            pipelines.add(new PipelineInfo(
                "gh-1234",
                "CI Pipeline",
                "success",
                300,
                "https://github.com/test/repo/actions/runs/1234"
            ));

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("workflow", "main.yml");
            metadata.put("trigger", "push");

            return new CiStatusDetail(
                "github",
                branch,
                "success",
                "active",
                LocalDateTime.now().minusMinutes(5),
                pipelines,
                0,
                metadata
            );
        }

        @Override
        public List<CiStatusDetail> queryStatus(String branch, String status, String state,
                                                 String since, int limit, String sortBy) throws Exception {
            List<CiStatusDetail> results = new ArrayList<>();

            // Mock data
            for (int i = 1; i <= Math.min(limit, 5); i++) {
                results.add(new CiStatusDetail(
                    "github",
                    branch != null ? branch : "main",
                    i % 3 == 0 ? "failure" : "success",
                    "active",
                    LocalDateTime.now().minusHours(i),
                    List.of(),
                    0,
                    Map.of()
                ));
            }

            return results;
        }

        @Override
        public List<StatusHistoryEntry> getHistory(String branch, String type, int count) throws Exception {
            List<StatusHistoryEntry> history = new ArrayList<>();

            for (int i = 1; i <= Math.min(count, 10); i++) {
                boolean isFailure = i % 4 == 0;
                if ("failures".equals(type) && !isFailure) continue;
                if ("successes".equals(type) && isFailure) continue;

                history.add(new StatusHistoryEntry(
                    LocalDateTime.now().minusHours(i),
                    branch != null ? branch : "main",
                    isFailure ? "failure" : "success",
                    isFailure ? "status_changed" : "completed"
                ));
            }

            return history;
        }
    }

    /**
     * GitLab status querier implementation
     */
    public static class GitLabStatusQuerier implements CiStatusQuerier {
        private final Path projectRoot;
        private final boolean verbose;

        public GitLabStatusQuerier(Path projectRoot, boolean verbose) {
            this.projectRoot = projectRoot;
            this.verbose = verbose;
        }

        @Override
        public CiStatusDetail getCurrentStatus(String branch) throws Exception {
            List<PipelineInfo> pipelines = new ArrayList<>();
            pipelines.add(new PipelineInfo(
                "gl-5678",
                "Build",
                "success",
                240,
                "https://gitlab.com/test/repo/-/pipelines/5678"
            ));

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("project", "test/repo");
            metadata.put("ref", branch);

            return new CiStatusDetail(
                "gitlab",
                branch,
                "success",
                "active",
                LocalDateTime.now().minusMinutes(3),
                pipelines,
                0,
                metadata
            );
        }

        @Override
        public List<CiStatusDetail> queryStatus(String branch, String status, String state,
                                                 String since, int limit, String sortBy) throws Exception {
            List<CiStatusDetail> results = new ArrayList<>();

            for (int i = 1; i <= Math.min(limit, 5); i++) {
                results.add(new CiStatusDetail(
                    "gitlab",
                    branch != null ? branch : "main",
                    i % 5 == 0 ? "failed" : "success",
                    "active",
                    LocalDateTime.now().minusHours(i),
                    List.of(),
                    0,
                    Map.of()
                ));
            }

            return results;
        }

        @Override
        public List<StatusHistoryEntry> getHistory(String branch, String type, int count) throws Exception {
            List<StatusHistoryEntry> history = new ArrayList<>();

            for (int i = 1; i <= Math.min(count, 10); i++) {
                boolean isFailure = i % 5 == 0;
                if ("failures".equals(type) && !isFailure) continue;
                if ("successes".equals(type) && isFailure) continue;

                history.add(new StatusHistoryEntry(
                    LocalDateTime.now().minusHours(i),
                    branch != null ? branch : "main",
                    isFailure ? "failed" : "success",
                    "pipeline_completed"
                ));
            }

            return history;
        }
    }

    /**
     * CI status detail record
     */
    public record CiStatusDetail(
        String platform,
        String branch,
        String status,
        String state,
        LocalDateTime lastUpdate,
        List<PipelineInfo> pipelines,
        int pendingApprovals,
        Map<String, Object> metadata
    ) {
        public CiStatusDetail {
            if (platform == null) platform = "";
            if (branch == null) branch = "";
            if (status == null) status = "";
            if (state == null) state = "";
            if (pipelines == null) pipelines = List.of();
            if (metadata == null) metadata = Map.of();
        }
    }

    /**
     * Pipeline info record
     */
    public record PipelineInfo(
        String id,
        String name,
        String status,
        long duration,
        String url
    ) {
        public PipelineInfo {
            if (id == null) id = "";
            if (name == null) name = "";
            if (status == null) status = "";
            if (url == null) url = "";
        }
    }

    /**
     * Status history entry record
     */
    public record StatusHistoryEntry(
        LocalDateTime timestamp,
        String branch,
        String status,
        String changeType
    ) {
        public StatusHistoryEntry {
            if (branch == null) branch = "";
            if (status == null) status = "";
            if (changeType == null) changeType = "";
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new CiStatusCommand()).execute(args);
        System.exit(exitCode);
    }
}
