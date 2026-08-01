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
import java.util.stream.Stream;

/**
 * FailureCodifier command for automated failure re-ticketing.
 *
 * <p>This command provides failure analysis and re-ticketing capabilities:
 * <ul>
 *   <li>propose - Propose fix tasks for test/CI failures</li>
 * </ul>
 * </p>
 */
@Command(name = "failure-codifier",
         mixinStandardHelpOptions = true,
         subcommands = {
             FailureCodifierCommand.ProposeCommand.class
         },
         description = "Generate fix task proposals for failures")
public class FailureCodifierCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Propose fix tasks for failures
     */
    @Command(name = "propose",
             mixinStandardHelpOptions = true,
             description = "Propose fix tasks for test/CI failures")
    public static class ProposeCommand implements Callable<Integer> {

        @Option(names = {"--dry-run"},
                 description = "Proposal only (required; auto-promotion forbidden)",
                 required = true)
        boolean dryRun;

        @Option(names = {"--repo-root"},
                 description = "Repository root directory (default: current directory)")
        String repoRoot;

        @Option(names = {"--format"},
                 description = "Output format (default: json)",
                 defaultValue = "json")
        String format;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                // Validate dry-run requirement
                if (!dryRun) {
                    System.err.println("failure-codifier: --dry-run is required (auto-promotion forbidden)");
                    return 2;
                }

                // Resolve repository root
                String rootDir = repoRoot != null ? repoRoot : System.getProperty("user.dir");
                Path rootPath = Paths.get(rootDir).toAbsolutePath();

                if (!Files.exists(rootPath)) {
                    System.err.println("failure-codifier: repository root not found: " + rootDir);
                    return 1;
                }

                if (verbose) {
                    System.out.println("Analyzing failures in: " + rootPath);
                }

                FailureAnalyzer analyzer = new FailureAnalyzer(verbose);
                FailureProposal proposal = analyzer.proposeFixes(rootPath);

                if (!"json".equals(format)) {
                    System.err.println("failure-codifier: unsupported format " + format);
                    return 1;
                }

                // Output JSON proposal
                outputJsonProposal(proposal);

                return 0;

            } catch (Exception e) {
                System.err.println("failure-codifier: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }

        private void outputJsonProposal(FailureProposal proposal) {
            System.out.println("{");
            System.out.println("  \"repo_root\": \"" + proposal.repoRoot() + "\",");
            System.out.println("  \"analyzed_at\": \"" + proposal.analyzedAt().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\",");
            System.out.println("  \"failures\": [");

            List<FailureTask> tasks = proposal.failureTasks();
            for (int i = 0; i < tasks.size(); i++) {
                FailureTask task = tasks.get(i);
                System.out.println("    {");
                System.out.println("      \"task_id\": \"" + task.taskId() + "\",");
                System.out.println("      \"type\": \"" + task.type() + "\",");
                System.out.println("      \"category\": \"" + task.category() + "\",");
                System.out.println("      \"title\": \"" + escapeJson(task.title()) + "\",");
                System.out.println("      \"description\": \"" + escapeJson(task.description()) + "\",");

                if (task.evidenceFiles() != null && !task.evidenceFiles().isEmpty()) {
                    System.out.println("      \"evidence_files\": [");
                    for (int j = 0; j < task.evidenceFiles().size(); j++) {
                        System.out.println("        \"" + task.evidenceFiles().get(j) + "\"" +
                            (j < task.evidenceFiles().size() - 1 ? "," : ""));
                    }
                    System.out.println("      ],");
                }

                if (task.affectedTasks() != null && !task.affectedTasks().isEmpty()) {
                    System.out.println("      \"affected_tasks\": [");
                    for (int j = 0; j < task.affectedTasks().size(); j++) {
                        System.out.println("        \"" + task.affectedTasks().get(j) + "\"" +
                            (j < task.affectedTasks().size() - 1 ? "," : ""));
                    }
                    System.out.println("      ],");
                }

                System.out.println("      \"priority\": \"" + task.priority() + "\",");
                System.out.println("      \"estimated_effort\": \"" + task.estimatedEffort() + "\"");
                System.out.println("    }" + (i < tasks.size() - 1 ? "," : ""));
            }

            System.out.println("  ],");
            System.out.println("  \"summary\": {");
            System.out.println("    \"total_failures\": " + proposal.totalFailures() + ",");
            System.out.println("    \"by_category\": {");

            Map<String, Integer> byCategory = proposal.failuresByCategory();
            boolean first = true;
            for (Map.Entry<String, Integer> entry : byCategory.entrySet()) {
                if (!first) {
                    System.out.print(",");
                }
                System.out.println("      \"" + entry.getKey() + "\": " + entry.getValue());
                first = false;
            }

            System.out.println("    },");
            System.out.println("    \"recommendation\": \"" + escapeJson(proposal.recommendation()) + "\"");
            System.out.println("  }");
            System.out.println("}");
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
    public record FailureTask(
        String taskId,
        String type,
        String category,
        String title,
        String description,
        List<String> evidenceFiles,
        List<String> affectedTasks,
        String priority,
        String estimatedEffort
    ) {
        public FailureTask {
            if (taskId == null) taskId = "";
            if (type == null) type = "";
            if (category == null) category = "";
            if (title == null) title = "";
            if (description == null) description = "";
            if (evidenceFiles == null) evidenceFiles = List.of();
            if (affectedTasks == null) affectedTasks = List.of();
            if (priority == null) priority = "";
            if (estimatedEffort == null) estimatedEffort = "";
        }
    }

    public record FailureProposal(
        String repoRoot,
        LocalDateTime analyzedAt,
        List<FailureTask> failureTasks,
        int totalFailures,
        Map<String, Integer> failuresByCategory,
        String recommendation
    ) {
        public FailureProposal {
            if (repoRoot == null) repoRoot = "";
            if (analyzedAt == null) analyzedAt = LocalDateTime.now();
            if (failureTasks == null) failureTasks = List.of();
            if (failuresByCategory == null) failuresByCategory = Map.of();
            if (recommendation == null) recommendation = "";
        }
    }

    /**
     * Failure analyzer - analyzes repository for failures and proposes fixes
     */
    public static class FailureAnalyzer {
        private final boolean verbose;

        public FailureAnalyzer(boolean verbose) {
            this.verbose = verbose;
        }

        public FailureProposal proposeFixes(Path repoRoot) {
            try {
                List<FailureTask> failureTasks = new ArrayList<>();
                Map<String, Integer> failuresByCategory = new HashMap<>();

                // Analyze test failures
                List<FailureTask> testFailures = analyzeTestFailures(repoRoot);
                failureTasks.addAll(testFailures);
                failuresByCategory.put("test", testFailures.size());

                // Analyze CI failures
                List<FailureTask> ciFailures = analyzeCIFailures(repoRoot);
                failureTasks.addAll(ciFailures);
                failuresByCategory.put("ci", ciFailures.size());

                // Analyze build failures
                List<FailureTask> buildFailures = analyzeBuildFailures(repoRoot);
                failureTasks.addAll(buildFailures);
                failuresByCategory.put("build", buildFailures.size());

                // Build recommendation
                String recommendation = buildRecommendation(failureTasks, failuresByCategory);

                return new FailureProposal(
                    repoRoot.toString(),
                    LocalDateTime.now(),
                    failureTasks,
                    failureTasks.size(),
                    failuresByCategory,
                    recommendation
                );

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Failure analysis failed: " + e.getMessage());
                }
                return new FailureProposal(
                    repoRoot.toString(),
                    LocalDateTime.now(),
                    List.of(),
                    0,
                    Map.of(),
                    "Analysis failed: " + e.getMessage()
                );
            }
        }

        private List<FailureTask> analyzeTestFailures(Path repoRoot) {
            List<FailureTask> failures = new ArrayList<>();

            try {
                // Look for test result files
                Path testResultsDir = repoRoot.resolve("target").resolve("surefire-reports");
                if (!Files.exists(testResultsDir)) {
                    testResultsDir = repoRoot.resolve("build").resolve("test-results");
                }

                if (Files.exists(testResultsDir)) {
                    try (Stream<Path> stream = Files.walk(testResultsDir, 1)) {
                        stream.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".xml") ||
                                       p.toString().endsWith(".txt"))
                            .forEach(resultFile -> {
                                FailureTask task = analyzeTestResultFile(resultFile, repoRoot);
                                if (task != null) {
                                    failures.add(task);
                                }
                            });
                    }
                }

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Test failure analysis failed: " + e.getMessage());
                }
            }

            return failures;
        }

        private FailureTask analyzeTestResultFile(Path resultFile, Path repoRoot) {
            try {
                String content = Files.readString(resultFile, StandardCharsets.UTF_8);

                // Check for failure indicators
                if (content.contains("failure") || content.contains("error")) {
                    String taskId = generateTaskId("test");
                    String testName = extractTestName(content, resultFile);
                    String description = buildTestFailureDescription(content, resultFile);

                    return new FailureTask(
                        taskId,
                        "test_failure",
                        "test",
                        "Fix failing test: " + testName,
                        description,
                        List.of(resultFile.toString()),
                        extractAffectedTasks(content),
                        determinePriority(content),
                        estimateEffort(content)
                    );
                }

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Failed to analyze test result: " + resultFile);
                }
            }

            return null;
        }

        private List<FailureTask> analyzeCIFailures(Path repoRoot) {
            List<FailureTask> failures = new ArrayList<>();

            try {
                // Look for CI configuration files
                Path githubActionsDir = repoRoot.resolve(".github").resolve("workflows");
                Path gitLabCiFile = repoRoot.resolve(".gitlab-ci.yml");

                if (Files.exists(githubActionsDir)) {
                    try (Stream<Path> stream = Files.walk(githubActionsDir, 1)) {
                        stream.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".yml") ||
                                       p.toString().endsWith(".yaml"))
                            .forEach(workflowFile -> {
                                FailureTask task = analyzeCIWorkflow(workflowFile, repoRoot);
                                if (task != null) {
                                    failures.add(task);
                                }
                            });
                    }
                }

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("CI failure analysis failed: " + e.getMessage());
                }
            }

            return failures;
        }

        private FailureTask analyzeCIWorkflow(Path workflowFile, Path repoRoot) {
            try {
                String content = Files.readString(workflowFile, StandardCharsets.UTF_8);

                // Check for CI configuration issues
                if (content.contains("retry") || content.contains("continue-on-error")) {
                    String taskId = generateTaskId("ci");
                    String workflowName = workflowFile.getFileName().toString();

                    return new FailureTask(
                        taskId,
                        "ci_instability",
                        "ci",
                        "Fix CI workflow: " + workflowName,
                        "CI workflow " + workflowName + " contains retry or error suppression logic",
                        List.of(workflowFile.toString()),
                        List.of(),
                        "medium",
                        "1-2 hours"
                    );
                }

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Failed to analyze CI workflow: " + workflowFile);
                }
            }

            return null;
        }

        private List<FailureTask> analyzeBuildFailures(Path repoRoot) {
            List<FailureTask> failures = new ArrayList<>();

            try {
                // Look for build configuration files
                Path pomXml = repoRoot.resolve("pom.xml");
                Path buildGradle = repoRoot.resolve("build.gradle");

                if (Files.exists(pomXml)) {
                    FailureTask task = analyzeMavenBuild(pomXml);
                    if (task != null) {
                        failures.add(task);
                    }
                }

                if (Files.exists(buildGradle)) {
                    FailureTask task = analyzeGradleBuild(buildGradle);
                    if (task != null) {
                        failures.add(task);
                    }
                }

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Build failure analysis failed: " + e.getMessage());
                }
            }

            return failures;
        }

        private FailureTask analyzeMavenBuild(Path pomXml) {
            try {
                String content = Files.readString(pomXml, StandardCharsets.UTF_8);

                // Check for common Maven issues
                if (content.contains("SNAPSHOT") && content.contains("dependency")) {
                    String taskId = generateTaskId("build");

                    return new FailureTask(
                        taskId,
                        "maven_dependency_issue",
                        "build",
                        "Fix Maven snapshot dependencies",
                        "pom.xml contains snapshot dependencies that may cause instability",
                        List.of(pomXml.toString()),
                        List.of(),
                        "low",
                        "30-60 minutes"
                    );
                }

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Failed to analyze Maven build: " + pomXml);
                }
            }

            return null;
        }

        private FailureTask analyzeGradleBuild(Path buildGradle) {
            try {
                String content = Files.readString(buildGradle, StandardCharsets.UTF_8);

                // Check for common Gradle issues
                if (content.contains("snapshot") || content.contains("changing")) {
                    String taskId = generateTaskId("build");

                    return new FailureTask(
                        taskId,
                        "gradle_dependency_issue",
                        "build",
                        "Fix Gradle snapshot dependencies",
                        "build.gradle contains snapshot or changing dependencies",
                        List.of(buildGradle.toString()),
                        List.of(),
                        "low",
                        "30-60 minutes"
                    );
                }

            } catch (Exception e) {
                if (verbose) {
                    System.err.println("Failed to analyze Gradle build: " + buildGradle);
                }
            }

            return null;
        }

        private String buildRecommendation(List<FailureTask> failureTasks,
                                          Map<String, Integer> failuresByCategory) {
            if (failureTasks.isEmpty()) {
                return "No failures detected. System is healthy.";
            }

            StringBuilder rec = new StringBuilder();
            rec.append("Detected ").append(failureTasks.size()).append(" failure(s): ");

            for (Map.Entry<String, Integer> entry : failuresByCategory.entrySet()) {
                if (entry.getValue() > 0) {
                    rec.append(entry.getValue()).append(" ").append(entry.getKey()).append(", ");
                }
            }

            // Remove trailing comma and space
            if (rec.length() > 2) {
                rec.setLength(rec.length() - 2);
            }

            rec.append(". Review proposed tasks and run: approve fix <task_id>");

            return rec.toString();
        }

        private String generateTaskId(String prefix) {
            UUID uuid = UUID.randomUUID();
            return prefix + "-" + uuid.toString().substring(0, 8);
        }

        private String extractTestName(String content, Path resultFile) {
            // Try to extract test name from content or filename
            String filename = resultFile.getFileName().toString();
            if (filename.contains("TEST-")) {
                return filename.replace("TEST-", "").replace(".xml", "");
            }
            return "unknown_test";
        }

        private String buildTestFailureDescription(String content, Path resultFile) {
            return "Test failure detected in " + resultFile.getFileName() +
                   ". Analysis of test output reveals failure patterns requiring investigation.";
        }

        private List<String> extractAffectedTasks(String content) {
            List<String> tasks = new ArrayList<>();

            // Try to extract task IDs from content
            // This is simplified - real implementation would parse more carefully
            if (content.contains("task-")) {
                String[] parts = content.split("task-");
                for (String part : parts) {
                    String[] subParts = part.split("[^0-9.]");
                    if (subParts.length > 0 && subParts[0].matches("[0-9.]+")) {
                        tasks.add("task-" + subParts[0]);
                    }
                }
            }

            return tasks;
        }

        private String determinePriority(String content) {
            // Simple priority determination based on content
            if (content.toLowerCase().contains("critical") ||
                content.toLowerCase().contains("security")) {
                return "high";
            } else if (content.toLowerCase().contains("minor")) {
                return "low";
            }
            return "medium";
        }

        private String estimateEffort(String content) {
            // Simple effort estimation based on content
            int complexity = 0;
            if (content.contains("stack trace")) complexity++;
            if (content.contains("multiple")) complexity++;
            if (content.contains("integration")) complexity++;

            if (complexity >= 2) return "2-4 hours";
            if (complexity == 1) return "1-2 hours";
            return "30-60 minutes";
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new FailureCodifierCommand()).execute(args);
        System.exit(exitCode);
    }
}
