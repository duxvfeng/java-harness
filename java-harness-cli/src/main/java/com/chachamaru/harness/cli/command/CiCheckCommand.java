package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * CI Check command for checking CI status.
 *
 * <p>This command provides CI status checking capabilities:
 * <ul>
 *   <li>check - Check current CI status</li>
 *   <li>list - List recent CI runs</li>
 *   <li>pr - Check PR CI status</li>
 *   <li>commit - Check commit CI status</li>
 * </ul>
 * </p>
 */
@Command(name = "ci-check",
         mixinStandardHelpOptions = true,
         subcommands = {
             CiCheckCommand.CheckCommand.class,
             CiCheckCommand.ListCommand.class,
             CiCheckCommand.PrCommand.class,
             CiCheckCommand.CommitCommand.class
         },
         description = "Check CI status for GitHub Actions and GitLab CI")
public class CiCheckCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Check current CI status
     */
    @Command(name = "check",
             mixinStandardHelpOptions = true,
             description = "Check current CI status")
    public static class CheckCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"-b", "--branch"},
                 description = "Branch to check",
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

                CiChecker checker = createChecker(detectedPlatform, projectPath, verbose);
                CiStatus status = checker.checkStatus(branch);

                // Output result
                if ("json".equals(format)) {
                    outputJsonResult(status);
                } else if ("detailed".equals(format)) {
                    outputDetailedResult(status);
                } else {
                    outputTableResult(status);
                }

                return status.isSuccess() ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ CI check failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private String detectPlatform(Path projectPath) {
            // Check for GitHub
            Path githubDir = projectPath.resolve(".git");
            if (Files.exists(githubDir)) {
                try {
                    URL gitRemote = new URL(getGitRemote(projectPath));
                    if (gitRemote.getHost().contains("github")) {
                        return "github";
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }

            // Check for GitLab
            Path gitlabFile = projectPath.resolve(".gitlab-ci.yml");
            if (Files.exists(gitlabFile)) {
                return "gitlab";
            }

            // Default to GitHub
            return "github";
        }

        private String getGitRemote(Path projectPath) throws IOException {
            Path gitConfig = projectPath.resolve(".git/config");
            if (Files.exists(gitConfig)) {
                List<String> lines = Files.readAllLines(gitConfig);
                for (String line : lines) {
                    if (line.contains("url =")) {
                        return line.substring(line.indexOf("url =") + 6).trim();
                    }
                }
            }
            return "";
        }

        private CiChecker createChecker(String platform, Path projectPath, boolean verbose) {
            return switch (platform.toLowerCase()) {
                case "gitlab" -> new GitLabChecker(projectPath, verbose);
                default -> new GitHubChecker(projectPath, verbose);
            };
        }

        private void outputJsonResult(CiStatus status) {
            System.out.println("{");
            System.out.println("  \"platform\": \"" + status.platform() + "\",");
            System.out.println("  \"branch\": \"" + status.branch() + "\",");
            System.out.println("  \"status\": \"" + status.status() + "\",");
            System.out.println("  \"success\": " + status.isSuccess() + ",");
            System.out.println("  \"timestamp\": \"" + status.timestamp().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\"");

            if (status.details() != null && !status.details().isEmpty()) {
                System.out.println("  \"details\": {");
                var entries = status.details().entrySet().iterator();
                while (entries.hasNext()) {
                    var entry = entries.next();
                    System.out.println("    \"" + entry.getKey() + "\": \"" +
                        escapeJson(String.valueOf(entry.getValue())) + "\"" +
                        (entries.hasNext() ? "," : ""));
                }
                System.out.println("  },");
            }

            if (!status.checks().isEmpty()) {
                System.out.println("  \"checks\": [");
                for (int i = 0; i < status.checks().size(); i++) {
                    CiCheck check = status.checks().get(i);
                    System.out.println("    {");
                    System.out.println("      \"name\": \"" + escapeJson(check.name()) + "\",");
                    System.out.println("      \"status\": \"" + check.status() + "\",");
                    System.out.println("      \"conclusion\": \"" + check.conclusion() + "\"");
                    System.out.println("    }" + (i < status.checks().size() - 1 ? "," : ""));
                }
                System.out.println("  ]");
            }

            System.out.println("}");
        }

        private void outputDetailedResult(CiStatus status) {
            System.out.println();
            System.out.println("📊 CI Status Report");
            System.out.println();
            System.out.println("Platform: " + status.platform());
            System.out.println("Branch: " + status.branch());
            System.out.println("Status: " + getStatusIcon(status.status()) + " " + status.status());
            System.out.println("Timestamp: " + status.timestamp().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            if (status.details() != null && !status.details().isEmpty()) {
                System.out.println();
                System.out.println("Details:");
                for (var entry : status.details().entrySet()) {
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue());
                }
            }

            if (!status.checks().isEmpty()) {
                System.out.println();
                System.out.println("Checks:");
                for (CiCheck check : status.checks()) {
                    System.out.println("  " + getConclusionIcon(check.conclusion()) + " " +
                        check.name() + " - " + check.status() + " (" + check.conclusion() + ")");
                }
            }

            System.out.println();
            System.out.println("Overall: " + (status.isSuccess() ? "✓ PASSED" : "✗ FAILED"));
        }

        private void outputTableResult(CiStatus status) {
            System.out.println();
            System.out.println("📊 CI Status");
            System.out.println();
            System.out.printf("%-15s %-15s %-10s%n", "Platform", "Branch", "Status");
            System.out.println("-".repeat(50));

            System.out.printf("%-15s %-15s %-10s%n",
                truncate(status.platform(), 15),
                truncate(status.branch(), 15),
                getStatusIcon(status.status()) + " " + truncate(status.status(), 8));

            System.out.println();
            System.out.println("Overall: " + (status.isSuccess() ? "✓ PASSED" : "✗ FAILED"));
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "success", "completed", "passed" -> "✓";
                case "failure", "failed", "error" -> "✗";
                case "pending", "queued", "in_progress" -> "⏳";
                case "running" -> "▶";
                default -> "?";
            };
        }

        private String getConclusionIcon(String conclusion) {
            return switch (conclusion != null ? conclusion.toLowerCase() : "") {
                case "success" -> "✓";
                case "failure" -> "✗";
                case "pending" -> "⏳";
                default -> "?";
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
     * List recent CI runs
     */
    @Command(name = "list",
             mixinStandardHelpOptions = true,
             description = "List recent CI runs")
    public static class ListCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"-n", "--count"},
                 description = "Number of recent runs to show",
                 defaultValue = "10")
        int count;

        @Option(names = {"--branch"},
                 description = "Filter by branch")
        String branch;

        @Option(names = {"--status"},
                 description = "Filter by status")
        String status;

        @Option(names = {"--format"},
                 description = "Output format: table, json",
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
                CiChecker checker = createChecker(platform, projectPath, verbose);
                List<CiRun> runs = checker.listRuns(branch, status, count);

                if ("json".equals(format)) {
                    outputJsonRuns(runs);
                } else {
                    outputTableRuns(runs);
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

        private String detectPlatform(Path projectPath) {
            // Simplified detection
            Path gitlabFile = projectPath.resolve(".gitlab-ci.yml");
            return Files.exists(gitlabFile) ? "gitlab" : "github";
        }

        private CiChecker createChecker(String platform, Path projectPath, boolean verbose) {
            return switch (platform.toLowerCase()) {
                case "gitlab" -> new GitLabChecker(projectPath, verbose);
                default -> new GitHubChecker(projectPath, verbose);
            };
        }

        private void outputJsonRuns(List<CiRun> runs) {
            System.out.println("[");
            for (int i = 0; i < runs.size(); i++) {
                CiRun run = runs.get(i);
                System.out.println("  {");
                System.out.println("    \"id\": \"" + run.id() + "\",");
                System.out.println("    \"branch\": \"" + run.branch() + "\",");
                System.out.println("    \"status\": \"" + run.status() + "\",");
                System.out.println("    \"timestamp\": \"" + run.timestamp().format(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\"");
                System.out.println("  }" + (i < runs.size() - 1 ? "," : ""));
            }
            System.out.println("]");
        }

        private void outputTableRuns(List<CiRun> runs) {
            System.out.println();
            System.out.println("📋 Recent CI Runs");
            System.out.println();
            System.out.printf("%-20s %-15s %-15s %-20s%n",
                "ID", "Branch", "Status", "Time");
            System.out.println("-".repeat(80));

            for (CiRun run : runs) {
                System.out.printf("%-20s %-15s %-15s %-20s%n",
                    truncate(run.id(), 20),
                    truncate(run.branch(), 15),
                    getStatusIcon(run.status()) + " " + truncate(run.status(), 13),
                    run.timestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }

            System.out.println();
            System.out.println("Total: " + runs.size() + " run(s)");
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "success", "completed", "passed" -> "✓";
                case "failure", "failed", "error" -> "✗";
                case "pending", "queued", "in_progress" -> "⏳";
                case "running" -> "▶";
                default -> "?";
            };
        }

        private String truncate(String s, int maxLength) {
            if (s == null) return "";
            return s.length() > maxLength ? s.substring(0, maxLength - 3) + "..." : s;
        }
    }

    /**
     * Check PR CI status
     */
    @Command(name = "pr",
             mixinStandardHelpOptions = true,
             description = "Check PR CI status")
    public static class PrCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--pr"},
                 description = "PR number",
                 required = true)
        int prNumber;

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
                CiChecker checker = createChecker(platform, projectPath, verbose);
                CiStatus status = checker.checkPrStatus(prNumber);

                if ("json".equals(format)) {
                    outputJsonResult(status);
                } else if ("detailed".equals(format)) {
                    outputDetailedResult(status);
                } else {
                    outputTableResult(status);
                }

                return status.isSuccess() ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ PR check failed: " + e.getMessage());
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

        private CiChecker createChecker(String platform, Path projectPath, boolean verbose) {
            return switch (platform.toLowerCase()) {
                case "gitlab" -> new GitLabChecker(projectPath, verbose);
                default -> new GitHubChecker(projectPath, verbose);
            };
        }

        private void outputJsonResult(CiStatus status) {
            System.out.println("{");
            System.out.println("  \"pr\": " + status.prNumber() + ",");
            System.out.println("  \"status\": \"" + status.status() + "\",");
            System.out.println("  \"success\": " + status.isSuccess() + ",");
            System.out.println("  \"checks\": " + status.checks().size());
            System.out.println("}");
        }

        private void outputDetailedResult(CiStatus status) {
            System.out.println();
            System.out.println("📊 PR #" + status.prNumber() + " CI Status");
            System.out.println();
            System.out.println("Status: " + getStatusIcon(status.status()) + " " + status.status());

            if (!status.checks().isEmpty()) {
                System.out.println();
                System.out.println("Checks:");
                for (CiCheck check : status.checks()) {
                    System.out.println("  " + getConclusionIcon(check.conclusion()) + " " +
                        check.name() + " - " + check.conclusion());
                }
            }

            System.out.println();
            System.out.println("Overall: " + (status.isSuccess() ? "✓ PASSED" : "✗ FAILED"));
        }

        private void outputTableResult(CiStatus status) {
            System.out.println();
            System.out.println("📊 PR #" + status.prNumber() + " CI Status: " +
                getStatusIcon(status.status()) + " " + status.status());
            System.out.println("Checks: " + status.checks().size() + " total, " +
                status.checks().stream().filter(c -> "success".equals(c.conclusion())).count() + " passed");
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "success", "completed", "passed" -> "✓";
                case "failure", "failed", "error" -> "✗";
                default -> "⏳";
            };
        }

        private String getConclusionIcon(String conclusion) {
            return switch (conclusion != null ? conclusion.toLowerCase() : "") {
                case "success" -> "✓";
                case "failure" -> "✗";
                default -> "?";
            };
        }
    }

    /**
     * Check commit CI status
     */
    @Command(name = "commit",
             mixinStandardHelpOptions = true,
             description = "Check commit CI status")
    public static class CommitCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--sha"},
                 description = "Commit SHA",
                 required = true)
        String sha;

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
                CiChecker checker = createChecker(platform, projectPath, verbose);
                CiStatus status = checker.checkCommitStatus(sha);

                if ("json".equals(format)) {
                    outputJsonResult(status);
                } else if ("detailed".equals(format)) {
                    outputDetailedResult(status);
                } else {
                    outputTableResult(status);
                }

                return status.isSuccess() ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Commit check failed: " + e.getMessage());
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

        private CiChecker createChecker(String platform, Path projectPath, boolean verbose) {
            return switch (platform.toLowerCase()) {
                case "gitlab" -> new GitLabChecker(projectPath, verbose);
                default -> new GitHubChecker(projectPath, verbose);
            };
        }

        private void outputJsonResult(CiStatus status) {
            System.out.println("{");
            System.out.println("  \"sha\": \"" + status.sha() + "\",");
            System.out.println("  \"status\": \"" + status.status() + "\",");
            System.out.println("  \"success\": " + status.isSuccess());
            System.out.println("}");
        }

        private void outputDetailedResult(CiStatus status) {
            System.out.println();
            System.out.println("📊 Commit " + status.sha().substring(0, 8) + " CI Status");
            System.out.println();
            System.out.println("Status: " + getStatusIcon(status.status()) + " " + status.status());

            if (!status.checks().isEmpty()) {
                System.out.println();
                System.out.println("Checks:");
                for (CiCheck check : status.checks()) {
                    System.out.println("  " + getConclusionIcon(check.conclusion()) + " " +
                        check.name() + " - " + check.conclusion());
                }
            }

            System.out.println();
            System.out.println("Overall: " + (status.isSuccess() ? "✓ PASSED" : "✗ FAILED"));
        }

        private void outputTableResult(CiStatus status) {
            System.out.println();
            System.out.println("📊 Commit " + status.sha().substring(0, 8) + " CI Status: " +
                getStatusIcon(status.status()) + " " + status.status());
            System.out.println("Checks: " + status.checks().size() + " total, " +
                status.checks().stream().filter(c -> "success".equals(c.conclusion())).count() + " passed");
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "success", "completed", "passed" -> "✓";
                case "failure", "failed", "error" -> "✗";
                default -> "⏳";
            };
        }

        private String getConclusionIcon(String conclusion) {
            return switch (conclusion != null ? conclusion.toLowerCase() : "") {
                case "success" -> "✓";
                case "failure" -> "✗";
                default -> "?";
            };
        }
    }

    /**
     * CI checker interface
     */
    public interface CiChecker {
        CiStatus checkStatus(String branch) throws Exception;
        List<CiRun> listRuns(String branch, String status, int count) throws Exception;
        CiStatus checkPrStatus(int prNumber) throws Exception;
        CiStatus checkCommitStatus(String sha) throws Exception;
    }

    /**
     * GitHub CI checker implementation
     */
    public static class GitHubChecker implements CiChecker {
        private final Path projectRoot;
        private final boolean verbose;

        public GitHubChecker(Path projectRoot, boolean verbose) {
            this.projectRoot = projectRoot;
            this.verbose = verbose;
        }

        @Override
        public CiStatus checkStatus(String branch) throws Exception {
            // Mock implementation - in real version, call GitHub API
            List<CiCheck> checks = new ArrayList<>();
            checks.add(new CiCheck("build", "completed", "success"));
            checks.add(new CiCheck("test", "completed", "success"));
            checks.add(new CiCheck("lint", "completed", "success"));

            Map<String, Object> details = new HashMap<>();
            details.put("workflow", "CI Pipeline");
            details.put("runNumber", "123");

            return new CiStatus(
                "github", branch, "success", true, LocalDateTime.now(),
                checks, details, null, null
            );
        }

        @Override
        public List<CiRun> listRuns(String branch, String status, int count) throws Exception {
            List<CiRun> runs = new ArrayList<>();

            // Mock data
            for (int i = 1; i <= Math.min(count, 5); i++) {
                runs.add(new CiRun(
                    String.valueOf(1000 + i),
                    branch != null ? branch : "main",
                    i % 3 == 0 ? "failure" : "success",
                    LocalDateTime.now().minusHours(i)
                ));
            }

            return runs;
        }

        @Override
        public CiStatus checkPrStatus(int prNumber) throws Exception {
            List<CiCheck> checks = new ArrayList<>();
            checks.add(new CiCheck("build", "completed", "success"));
            checks.add(new CiCheck("test", "completed", "success"));

            return new CiStatus(
                "github", null, "success", true, LocalDateTime.now(),
                checks, Map.of(), prNumber, null
            );
        }

        @Override
        public CiStatus checkCommitStatus(String sha) throws Exception {
            List<CiCheck> checks = new ArrayList<>();
            checks.add(new CiCheck("ci/travis", "completed", "success"));

            return new CiStatus(
                "github", null, "success", true, LocalDateTime.now(),
                checks, Map.of(), null, sha
            );
        }
    }

    /**
     * GitLab CI checker implementation
     */
    public static class GitLabChecker implements CiChecker {
        private final Path projectRoot;
        private final boolean verbose;

        public GitLabChecker(Path projectRoot, boolean verbose) {
            this.projectRoot = projectRoot;
            this.verbose = verbose;
        }

        @Override
        public CiStatus checkStatus(String branch) throws Exception {
            List<CiCheck> checks = new ArrayList<>();
            checks.add(new CiCheck("build", "completed", "success"));
            checks.add(new CiCheck("test", "completed", "success"));

            Map<String, Object> details = new HashMap<>();
            details.put("pipeline", "Main Pipeline");
            details.put("pipelineId", "456");

            return new CiStatus(
                "gitlab", branch, "success", true, LocalDateTime.now(),
                checks, details, null, null
            );
        }

        @Override
        public List<CiRun> listRuns(String branch, String status, int count) throws Exception {
            List<CiRun> runs = new ArrayList<>();

            for (int i = 1; i <= Math.min(count, 5); i++) {
                runs.add(new CiRun(
                    String.valueOf(2000 + i),
                    branch != null ? branch : "main",
                    i % 4 == 0 ? "failed" : "success",
                    LocalDateTime.now().minusHours(i)
                ));
            }

            return runs;
        }

        @Override
        public CiStatus checkPrStatus(int prNumber) throws Exception {
            List<CiCheck> checks = new ArrayList<>();
            checks.add(new CiCheck("merge", "completed", "success"));

            return new CiStatus(
                "gitlab", null, "success", true, LocalDateTime.now(),
                checks, Map.of(), prNumber, null
            );
        }

        @Override
        public CiStatus checkCommitStatus(String sha) throws Exception {
            List<CiCheck> checks = new ArrayList<>();
            checks.add(new CiCheck("pipeline", "completed", "success"));

            return new CiStatus(
                "gitlab", null, "success", true, LocalDateTime.now(),
                checks, Map.of(), null, sha
            );
        }
    }

    /**
     * CI status record
     */
    public record CiStatus(
        String platform,
        String branch,
        String status,
        boolean isSuccess,
        LocalDateTime timestamp,
        List<CiCheck> checks,
        Map<String, Object> details,
        Integer prNumber,
        String sha
    ) {
        public CiStatus {
            if (platform == null) platform = "";
            if (branch == null) branch = "";
            if (status == null) status = "unknown";
            if (checks == null) checks = List.of();
            if (details == null) details = Map.of();
        }
    }

    /**
     * CI check record
     */
    public record CiCheck(
        String name,
        String status,
        String conclusion
    ) {
        public CiCheck {
            if (name == null) name = "";
            if (status == null) status = "";
            if (conclusion == null) conclusion = "";
        }
    }

    /**
     * CI run record
     */
    public record CiRun(
        String id,
        String branch,
        String status,
        LocalDateTime timestamp
    ) {
        public CiRun {
            if (id == null) id = "";
            if (branch == null) branch = "";
            if (status == null) status = "";
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new CiCheckCommand()).execute(args);
        System.exit(exitCode);
    }
}
