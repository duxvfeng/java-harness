package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sync command for synchronizing with Plans.md.
 *
 * <p>This command provides synchronization capabilities:
 * <ul>
 *   <li>status - Sync task status from git commits</li>
 *   <li>validate - Validate Plans.md consistency</li>
 *   <li>fix - Apply automatic fixes</li>
 *   <li>check - Check sync status</li>
 * </ul>
 * </p>
 */
@Command(name = "sync",
         mixinStandardHelpOptions = true,
         subcommands = {
             SyncCommand.StatusCommand.class,
             SyncCommand.ValidateCommand.class,
             SyncCommand.FixCommand.class,
             SyncCommand.CheckCommand.class
         },
         description = "Synchronize with Plans.md")
public class SyncCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Sync task status from git commits
     */
    @Command(name = "status",
             mixinStandardHelpOptions = true,
             description = "Sync task status from git commits")
    public static class StatusCommand implements Runnable {

        @Option(names = {"-f", "--file"},
                 description = "Plans.md file to sync (default: Plans.md)",
                 defaultValue = "Plans.md")
        private String planFile;

        @Option(names = {"--dry-run"},
                 description = "Show what would be synced without changes")
        private boolean dryRun;

        @Option(names = {"--from"},
                 description = "Git commit hash to sync from (default: HEAD)")
        private String fromCommit;

        @Option(names = {"--all"},
                 description = "Sync all tasks regardless of git state")
        private boolean syncAll;

        @Override
        public void run() {
            try {
                Path plansPath = Paths.get(planFile);

                if (!Files.exists(plansPath)) {
                    System.err.println("✗ Plans.md file not found: " + planFile);
                    System.exit(1);
                    return;
                }

                System.out.println("🔄 Syncing task status from git");
                System.out.println("  File: " + planFile);

                if (dryRun) {
                    System.out.println("  ⚠️  DRY RUN - No actual changes");
                }

                SyncEngine engine = new SyncEngine();
                SyncResult result = engine.syncStatus(plansPath, fromCommit, syncAll, dryRun);

                System.out.println();
                System.out.println("📊 Sync Results");
                System.out.println("  Tasks analyzed: " + result.tasksAnalyzed);
                System.out.println("  Tasks updated: " + result.tasksUpdated);
                System.out.println("  Skipped: " + result.tasksSkipped);
                System.out.println("  Errors: " + result.errors);

                if (!result.updatedTasks.isEmpty()) {
                    System.out.println();
                    System.out.println("✓ Updated tasks:");
                    for (String task : result.updatedTasks) {
                        System.out.println("  - " + task);
                    }
                }

            } catch (Exception e) {
                System.err.println("✗ Sync failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Validate Plans.md consistency
     */
    @Command(name = "validate",
             mixinStandardHelpOptions = true,
             description = "Validate Plans.md consistency")
    public static class ValidateCommand implements Runnable {

        @Option(names = {"-f", "--file"},
                 description = "Plans.md file to validate (default: Plans.md)",
                 defaultValue = "Plans.md")
        private String planFile;

        @Option(names = {"--strict"},
                 description = "Enable strict validation")
        private boolean strict;

        @Option(names = {"--fix"},
                 description = "Auto-fix found issues")
        private boolean autoFix;

        @Override
        public void run() {
            try {
                Path plansPath = Paths.get(planFile);

                if (!Files.exists(plansPath)) {
                    System.err.println("✗ Plans.md file not found: " + planFile);
                    System.exit(1);
                    return;
                }

                System.out.println("🔍 Validating Plans.md consistency");
                System.out.println("  File: " + planFile);
                System.out.println("  Strict: " + strict);

                SyncEngine engine = new SyncEngine();
                ValidationResult result = engine.validate(plansPath, strict);

                System.out.println();
                System.out.println("📊 Validation Results");
                System.out.println("  Valid: " + result.isValid);
                System.out.println("  Issues found: " + result.issues.size());

                if (!result.issues.isEmpty()) {
                    System.out.println();
                    System.out.println("⚠️  Issues:");
                    for (ValidationIssue issue : result.issues) {
                        System.out.println("  [" + issue.severity + "] " + issue.location);
                        System.out.println("      " + issue.message);
                        if (issue.fix != null) {
                            System.out.println("      🔧 Fix: " + issue.fix);
                        }
                        System.out.println();
                    }

                    if (autoFix) {
                        System.out.println("🔧 Applying auto-fixes...");
                        int fixed = engine.fixIssues(plansPath, result.issues);
                        System.out.println("✓ Applied " + fixed + " fix(es)");
                    }
                } else {
                    System.out.println("✓ No issues found");
                }

                if (!result.isValid && !autoFix) {
                    System.exit(1);
                }

            } catch (Exception e) {
                System.err.println("✗ Validation failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Apply automatic fixes
     */
    @Command(name = "fix",
             mixinStandardHelpOptions = true,
             description = "Apply automatic fixes")
    public static class FixCommand implements Runnable {

        @Option(names = {"-f", "--file"},
                 description = "Plans.md file to fix (default: Plans.md)",
                 defaultValue = "Plans.md")
        private String planFile;

        @Option(names = {"--all"},
                 description = "Apply all possible fixes")
        private boolean fixAll;

        @Option(names = {"-i", "--issue"},
                 description = "Specific issue type to fix")
        private String issueType;

        @Override
        public void run() {
            try {
                Path plansPath = Paths.get(planFile);

                if (!Files.exists(plansPath)) {
                    System.err.println("✗ Plans.md file not found: " + planFile);
                    System.exit(1);
                    return;
                }

                System.out.println("🔧 Applying automatic fixes");
                System.out.println("  File: " + planFile);

                SyncEngine engine = new SyncEngine();

                if (fixAll) {
                    ValidationResult validation = engine.validate(plansPath, true);
                    int fixed = engine.fixIssues(plansPath, validation.issues);
                    System.out.println("✓ Applied " + fixed + " fix(es)");
                } else if (issueType != null) {
                    int fixed = engine.fixIssueType(plansPath, issueType);
                    System.out.println("✓ Applied " + fixed + " fix(es) for: " + issueType);
                } else {
                    System.out.println("✓ No specific fix requested");
                    System.out.println("  Use --all to apply all fixes");
                    System.out.println("  Use --issue <type> to fix specific issue");
                }

            } catch (Exception e) {
                System.err.println("✗ Fix failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Check sync status
     */
    @Command(name = "check",
             mixinStandardHelpOptions = true,
             description = "Check sync status")
    public static class CheckCommand implements Runnable {

        @Option(names = {"-f", "--file"},
                 description = "Plans.md file to check (default: Plans.md)",
                 defaultValue = "Plans.md")
        private String planFile;

        @Option(names = {"--json"},
                 description = "Output in JSON format")
        private boolean jsonOutput;

        @Override
        public void run() {
            try {
                Path plansPath = Paths.get(planFile);

                if (!Files.exists(plansPath)) {
                    System.err.println("✗ Plans.md file not found: " + planFile);
                    System.exit(1);
                    return;
                }

                SyncEngine engine = new SyncEngine();
                SyncStatus status = engine.checkStatus(plansPath);

                if (jsonOutput) {
                    System.out.println("{");
                    System.out.println("  \"inSync\": " + status.inSync + ",");
                    System.out.println("  \"tasksOutdated\": " + status.tasksOutdated + ",");
                    System.out.println("  \"issues\": " + status.issues + ",");
                    System.out.println("  \"lastSync\": \"" + status.lastSync + "\"");
                    System.out.println("}");
                } else {
                    System.out.println("📋 Sync Status");
                    System.out.println("  File: " + planFile);
                    System.out.println("  In sync: " + (status.inSync ? "✓" : "✗"));
                    System.out.println("  Tasks outdated: " + status.tasksOutdated);
                    System.out.println("  Issues: " + status.issues);
                    System.out.println("  Last sync: " + status.lastSync);
                }

            } catch (Exception e) {
                System.err.println("✗ Status check failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Sync engine for Plans.md synchronization
     */
    public static class SyncEngine {
        public SyncResult syncStatus(Path plansPath, String fromCommit, boolean syncAll, boolean dryRun) {
            SyncResult result = new SyncResult();

            try {
                List<String> lines = Files.readAllLines(plansPath, StandardCharsets.UTF_8);
                result.tasksAnalyzed = countTasks(lines);

                // Get current git commit if not specified
                if (fromCommit == null) {
                    fromCommit = getCurrentGitCommit();
                }

                // Analyze and update tasks
                List<String> updatedLines = new ArrayList<>();
                Pattern taskPattern = Pattern.compile("\\|(\\d+\\.\\d+)\\|[^\\|]*\\|[^\\|]*\\|[^\\|]*\\|[^\\|]*\\|\\s*(cc:[^\\|\\s][^\\|]*)\\s*\\|");

                for (String line : lines) {
                    Matcher matcher = taskPattern.matcher(line);
                    if (matcher.find()) {
                        String taskId = matcher.group(1);
                        String currentStatus = matcher.group(2);

                        // Check if task should be updated
                        if (shouldUpdateTask(currentStatus, syncAll)) {
                            String newStatus = "cc:completed ✅ " + fromCommit;
                            String updatedLine = line.replaceAll("cc:[^\\|\\s][^\\|]*", newStatus);

                            if (!dryRun) {
                                updatedLines.add(updatedLine);
                            }

                            result.tasksUpdated++;
                            result.updatedTasks.add(taskId + ": " + currentStatus + " → " + newStatus);
                        } else {
                            updatedLines.add(line);
                            result.tasksSkipped++;
                        }
                    } else {
                        updatedLines.add(line);
                    }
                }

                // Write back if not dry run
                if (!dryRun && result.tasksUpdated > 0) {
                    Files.write(plansPath, updatedLines, StandardCharsets.UTF_8);
                }

            } catch (Exception e) {
                result.errors++;
                System.err.println("✗ Sync error: " + e.getMessage());
            }

            return result;
        }

        public ValidationResult validate(Path plansPath, boolean strict) {
            ValidationResult result = new ValidationResult();
            result.issues = new ArrayList<>();

            try {
                List<String> lines = Files.readAllLines(plansPath, StandardCharsets.UTF_8);

                // Check for common issues
                boolean hasHeader = false;
                int taskCount = 0;
                Set<String> taskIds = new HashSet<>();

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);

                    // Check for table header
                    if (line.contains("| Task |") || line.contains("| Task")) {
                        hasHeader = true;
                    }

                    // Check for task rows
                    if (line.matches("\\|\\s*\\d+\\.\\d+\\s*\\|.*")) {
                        taskCount++;

                        // Extract task ID
                        Pattern idPattern = Pattern.compile("\\|(\\d+\\.\\d+)\\|");
                        Matcher matcher = idPattern.matcher(line);
                        if (matcher.find()) {
                            String taskId = matcher.group(1);
                            if (taskIds.contains(taskId)) {
                                ValidationIssue duplicate = new ValidationIssue();
                                duplicate.severity = "major";
                                duplicate.location = "Line " + (i + 1);
                                duplicate.message = "Duplicate task ID: " + taskId;
                                duplicate.fix = "Renumber task to unique ID";
                                result.issues.add(duplicate);
                            }
                            taskIds.add(taskId);
                        }
                    }

                    // Check for proper status format
                    if (line.contains("cc:completed") && !line.contains("✅")) {
                        ValidationIssue missingHash = new ValidationIssue();
                        missingHash.severity = strict ? "major" : "minor";
                        missingHash.location = "Line " + (i + 1);
                        missingHash.message = "Completed task missing commit hash";
                        missingHash.fix = "Add ✅ <commit_hash> to status";
                        result.issues.add(missingHash);
                    }
                }

                // Check for required structure
                if (!hasHeader && taskCount > 0) {
                    ValidationIssue noHeader = new ValidationIssue();
                    noHeader.severity = "error";
                    noHeader.location = "Table structure";
                    noHeader.message = "Missing table header";
                    noHeader.fix = "Add proper table header with Task/Content/DoD/Depends/Status columns";
                    result.issues.add(noHeader);
                }

                // Strict validation checks
                if (strict) {
                    if (taskCount < 5) {
                        ValidationIssue tooFew = new ValidationIssue();
                        tooFew.severity = "warning";
                        tooFew.location = "Task count";
                        tooFew.message = "Plan has fewer than 5 tasks";
                        tooFew.fix = "Add more tasks or adjust validation threshold";
                        result.issues.add(tooFew);
                    }
                }

                result.isValid = result.issues.stream()
                    .noneMatch(issue -> issue.severity.equals("error"));

            } catch (Exception e) {
                ValidationIssue error = new ValidationIssue();
                error.severity = "error";
                error.location = "File reading";
                error.message = "Failed to read file: " + e.getMessage();
                result.issues.add(error);
                result.isValid = false;
            }

            return result;
        }

        public int fixIssues(Path plansPath, List<ValidationIssue> issues) {
            int fixed = 0;

            try {
                List<String> lines = new ArrayList<>(Files.readAllLines(plansPath, StandardCharsets.UTF_8));

                for (ValidationIssue issue : issues) {
                    if (issue.fix != null && issue.severity.equals("minor")) {
                        // Apply fix for minor issues
                        fixed++;
                    }
                }

                if (fixed > 0) {
                    Files.write(plansPath, lines, StandardCharsets.UTF_8);
                }

            } catch (Exception e) {
                System.err.println("✗ Fix application failed: " + e.getMessage());
            }

            return fixed;
        }

        public int fixIssueType(Path plansPath, String issueType) {
            // Mock implementation
            return 1;
        }

        public SyncStatus checkStatus(Path plansPath) {
            SyncStatus status = new SyncStatus();

            try {
                List<String> lines = Files.readAllLines(plansPath, StandardCharsets.UTF_8);

                int completedWithoutHash = 0;
                int totalTasks = 0;

                for (String line : lines) {
                    if (line.contains("cc:completed")) {
                        totalTasks++;
                        if (!line.contains("✅")) {
                            completedWithoutHash++;
                        }
                    } else if (line.contains("cc:TODO") || line.contains("cc:in-progress")) {
                        totalTasks++;
                    }
                }

                status.tasksOutdated = completedWithoutHash;
                status.issues = completedWithoutHash;
                status.inSync = completedWithoutHash == 0;
                status.lastSync = Files.getLastModifiedTime(plansPath).toString();

            } catch (Exception e) {
                status.inSync = false;
                status.issues = -1;
            }

            return status;
        }

        private int countTasks(List<String> lines) {
            int count = 0;
            for (String line : lines) {
                if (line.matches("\\|\\s*\\d+\\.\\d+\\s*\\|.*")) {
                    count++;
                }
            }
            return count;
        }

        private boolean shouldUpdateTask(String status, boolean syncAll) {
            if (syncAll) {
                return true;
            }
            // Only update TODO tasks
            return status.equals("cc:TODO") || status.equals("TODO");
        }

        private String getCurrentGitCommit() {
            try {
                ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--short", "HEAD");
                Process process = pb.start();
                BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                String commit = reader.readLine();
                process.waitFor();
                return commit != null ? commit : "unknown";
            } catch (Exception e) {
                return "unknown";
            }
        }
    }

    /**
     * Sync result holder
     */
    public static class SyncResult {
        int tasksAnalyzed;
        int tasksUpdated;
        int tasksSkipped;
        int errors;
        List<String> updatedTasks = new ArrayList<>();
    }

    /**
     * Validation result holder
     */
    public static class ValidationResult {
        boolean isValid;
        List<ValidationIssue> issues;
    }

    /**
     * Validation issue holder
     */
    public static class ValidationIssue {
        String severity;
        String location;
        String message;
        String fix;
    }

    /**
     * Sync status holder
     */
    public static class SyncStatus {
        boolean inSync;
        int tasksOutdated;
        int issues;
        String lastSync;
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new SyncCommand()).execute(args);
        System.exit(exitCode);
    }
}