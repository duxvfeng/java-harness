package com.chachamaru.harness.cli.command;

import com.chachamaru.harness.tools.validation.DoctorTool;
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
import java.util.concurrent.Callable;

/**
 * Doctor command for system diagnosis and repair.
 *
 * <p>This command provides diagnostic capabilities:
 * <ul>
 *   <li>check - Run health checks and diagnose issues</li>
 *   <li>fix - Attempt to fix common issues automatically</li>
 *   <li>report - Generate detailed health report</li>
 *   <li>analyze - Analyze specific component or subsystem</li>
 * </ul>
 * </p>
 */
@Command(name = "doctor",
         mixinStandardHelpOptions = true,
         subcommands = {
             DoctorCommand.CheckCommand.class,
             DoctorCommand.FixCommand.class,
             DoctorCommand.ReportCommand.class,
             DoctorCommand.AnalyzeCommand.class
         },
         description = "Diagnose and fix system issues")
public class DoctorCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Run health checks and diagnose issues
     */
    @Command(name = "check",
             mixinStandardHelpOptions = true,
             description = "Run health checks and diagnose issues")
    public static class CheckCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory to check (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--json"},
                 description = "Output in JSON format")
        boolean jsonOutput;

        @Option(names = {"--quiet", "-q"},
                 description = "Suppress output, only exit code")
        boolean quiet;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                Path projectPath = Paths.get(projectDir);

                if (!Files.exists(projectPath)) {
                    if (!quiet) {
                        System.err.println("✗ Project directory not found: " + projectDir);
                    }
                    return 1;
                }

                DoctorTool doctor = new DoctorTool(projectPath);
                DoctorTool.HealthReport report = doctor.generateReport();

                if (jsonOutput) {
                    outputJsonResult(report);
                } else if (!quiet) {
                    outputHumanResult(report);
                }

                // Exit code based on health status
                return report.overallStatus() == com.chachamaru.harness.tools.validation.HealthCheck.HealthStatus.HEALTHY
                    ? 0 : 1;

            } catch (Exception e) {
                if (!quiet) {
                    System.err.println("✗ Health check failed: " + e.getMessage());
                    if (verbose) {
                        e.printStackTrace();
                    }
                }
                return 2;
            }
        }

        private void outputJsonResult(DoctorTool.HealthReport report) {
            System.out.println("{");
            System.out.println("  \"projectPath\": \"" + escapeJson(report.projectPath()) + "\",");
            System.out.println("  \"generatedAt\": \"" + report.generatedAt().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\",");
            System.out.println("  \"overallStatus\": \"" + report.overallStatus() + "\",");
            System.out.println("  \"healthy\": " + report.isHealthy() + ",");
            System.out.println("  \"hasIssues\": " + report.hasIssues() + ",");
            System.out.println("  \"healthCheckResults\": " + report.healthCheckResults().size() + ",");
            System.out.println("  \"validationErrors\": " + report.validationResult().errorCount() + ",");
            System.out.println("  \"validationWarnings\": " + report.validationResult().warningCount() + ",");
            System.out.println("  \"summary\": \"" + escapeJson(report.summary()) + "\"");
            System.out.println("}");
        }

        private void outputHumanResult(DoctorTool.HealthReport report) {
            System.out.println();
            System.out.println("🏥 Health Check Results");
            System.out.println();

            System.out.println("  Overall Status: " + getStatusIcon(report.overallStatus()) + " " + report.overallStatus());
            System.out.println("  Project: " + report.projectPath());
            System.out.println("  Checked: " + report.generatedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.println();

            System.out.println("📋 Health Checks:");
            for (var result : report.healthCheckResults()) {
                System.out.println("  " + getStatusIcon(result.status()) + " " + result.name() + ": " + result.message());
            }
            System.out.println();

            System.out.println("🔍 Validation:");
            System.out.println("  Valid: " + (report.validationResult().valid() ? "✓" : "✗"));
            System.out.println("  Errors: " + report.validationResult().errorCount());
            System.out.println("  Warnings: " + report.validationResult().warningCount());
            System.out.println();

            if (report.validationResult().errorCount() > 0 || report.validationResult().warningCount() > 0) {
                System.out.println("⚠️  Issues:");
                for (var issue : report.validationResult().issues()) {
                    System.out.println("  [" + issue.severity() + "] " + issue.category() + ": " + issue.message());
                    if (!issue.recommendation().isEmpty()) {
                        System.out.println("      💡 " + issue.recommendation());
                    }
                }
                System.out.println();
            }

            System.out.println("📝 Summary: " + report.summary());
        }

        private String getStatusIcon(com.chachamaru.harness.tools.validation.HealthCheck.HealthStatus status) {
            return switch (status) {
                case HEALTHY -> "✓";
                case DEGRADED -> "⚠";
                case UNHEALTHY -> "✗";
                default -> "?";
            };
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
     * Attempt to fix common issues automatically
     */
    @Command(name = "fix",
             mixinStandardHelpOptions = true,
             description = "Attempt to fix common issues automatically")
    public static class FixCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory to fix (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--dry-run"},
                 description = "Show what would be fixed without making changes")
        boolean dryRun;

        @Option(names = {"--auto"},
                 description = "Apply fixes without confirmation")
        boolean autoFix;

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

                System.out.println("🔧 Analyzing project for fixable issues...");

                DoctorTool doctor = new DoctorTool(projectPath);
                DoctorTool.HealthReport report = doctor.generateReport();

                // Count fixable issues
                int fixableCount = countFixableIssues(report);

                if (fixableCount == 0) {
                    System.out.println("✓ No fixable issues found");
                    return 0;
                }

                System.out.println();
                System.out.println("Found " + fixableCount + " potentially fixable issue(s):");
                listFixableIssues(report);

                if (!autoFix && !dryRun) {
                    System.out.println();
                    System.out.print("Apply fixes? [y/N]: ");
                    String response = System.console().readLine();
                    if (!response.equalsIgnoreCase("y") && !response.equalsIgnoreCase("yes")) {
                        System.out.println("✗ Fix cancelled");
                        return 1;
                    }
                }

                if (dryRun) {
                    System.out.println();
                    System.out.println("⚠️  DRY RUN - No actual changes would be made");
                    return 0;
                }

                // Apply fixes
                System.out.println();
                System.out.println("Applying fixes...");
                int fixed = applyFixes(projectPath, report);

                System.out.println();
                System.out.println("✓ Applied " + fixed + " fix(es)");

                // Re-check after fixes
                System.out.println();
                System.out.println("Re-running health check...");
                DoctorTool.HealthReport newReport = doctor.generateReport();

                if (newReport.isHealthy()) {
                    System.out.println("✓ All issues resolved!");
                } else {
                    System.out.println("⚠️  Some issues remain. Run 'doctor check' for details.");
                }

                return newReport.isHealthy() ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Fix failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private int countFixableIssues(DoctorTool.HealthReport report) {
            int count = 0;
            for (var issue : report.validationResult().issues()) {
                if (isFixable(issue)) {
                    count++;
                }
            }
            return count;
        }

        private boolean isFixable(com.chachamaru.harness.tools.validation.ValidateTool.ValidationIssue issue) {
            // Add fixable issue detection logic
            return issue.category().equals("Directory Structure") ||
                   issue.category().equals("File Permissions") ||
                   issue.severity().equals("warning");
        }

        private void listFixableIssues(DoctorTool.HealthReport report) {
            int index = 1;
            for (var issue : report.validationResult().issues()) {
                if (isFixable(issue)) {
                    System.out.println("  " + index + ". [" + issue.severity() + "] " + issue.message());
                    System.out.println("     Category: " + issue.category());
                    System.out.println("     Fix: " + issue.recommendation());
                    index++;
                }
            }
        }

        private int applyFixes(Path projectPath, DoctorTool.HealthReport report) {
            int fixed = 0;

            for (var issue : report.validationResult().issues()) {
                if (!isFixable(issue)) {
                    continue;
                }

                try {
                    if (issue.category().equals("Directory Structure")) {
                        // Create missing directory
                        String dirPath = extractDirectoryPath(issue.recommendation());
                        if (dirPath != null) {
                            Path dir = projectPath.resolve(dirPath);
                            Files.createDirectories(dir);
                            System.out.println("  ✓ Created directory: " + dirPath);
                            fixed++;
                        }
                    } else if (issue.category().equals("File Permissions")) {
                        // Fix permissions (simplified)
                        System.out.println("  ⚠️  Permission fixes require manual intervention");
                    } else {
                        System.out.println("  ⚠️  Issue '" + issue.message() + "' requires manual fix");
                    }
                } catch (Exception e) {
                    System.out.println("  ✗ Failed to fix: " + issue.message() + " - " + e.getMessage());
                }
            }

            return fixed;
        }

        private String extractDirectoryPath(String recommendation) {
            // Extract directory path from recommendation like "Create directory: mkdir -p src/main/java"
            if (recommendation != null && recommendation.contains("mkdir")) {
                String[] parts = recommendation.split("mkdir -p\\s+");
                if (parts.length > 1) {
                    return parts[1].trim();
                }
            }
            return null;
        }
    }

    /**
     * Generate detailed health report
     */
    @Command(name = "report",
             mixinStandardHelpOptions = true,
             description = "Generate detailed health report")
    public static class ReportCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"-o", "--output"},
                 description = "Report output file",
                 defaultValue = "health-report.md")
        String outputFile;

        @Option(names = {"-f", "--format"},
                 description = "Report format: md, json, html",
                 defaultValue = "md")
        String format;

        @Option(names = {"--detailed"},
                 description = "Include detailed diagnostic information")
        boolean detailed;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                Path projectPath = Paths.get(projectDir);
                Path reportPath = Paths.get(outputFile);

                if (!Files.exists(projectPath)) {
                    System.err.println("✗ Project directory not found: " + projectDir);
                    return 1;
                }

                System.out.println("📝 Generating health report...");
                System.out.println("  Project: " + projectPath.toAbsolutePath());
                System.out.println("  Output: " + reportPath.toAbsolutePath());
                System.out.println("  Format: " + format);

                DoctorTool doctor = new DoctorTool(projectPath);
                DoctorTool.HealthReport report = doctor.generateReport();

                String reportContent = switch (format.toLowerCase()) {
                    case "json" -> generateJsonReport(report);
                    case "html" -> generateHtmlReport(report, detailed);
                    default -> report.toFormattedString();
                };

                Files.createDirectories(reportPath.getParent());
                Files.write(reportPath, reportContent.getBytes(StandardCharsets.UTF_8));

                System.out.println();
                System.out.println("✓ Report generated successfully");

                if (verbose) {
                    System.out.println();
                    System.out.println("📊 Report Summary:");
                    System.out.println("  Overall Status: " + report.overallStatus());
                    System.out.println("  Health Checks: " + report.healthCheckResults().size());
                    System.out.println("  Validation Errors: " + report.validationResult().errorCount());
                    System.out.println("  Validation Warnings: " + report.validationResult().warningCount());
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Report generation failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }

        private String generateJsonReport(DoctorTool.HealthReport report) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"projectPath\": \"").append(escapeJson(report.projectPath())).append("\",\n");
            sb.append("  \"generatedAt\": \"").append(report.generatedAt().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
            sb.append("  \"overallStatus\": \"").append(report.overallStatus()).append("\",\n");
            sb.append("  \"healthy\": ").append(report.isHealthy()).append(",\n");
            sb.append("  \"hasIssues\": ").append(report.hasIssues()).append(",\n");
            sb.append("  \"summary\": \"").append(escapeJson(report.summary())).append("\",\n");
            sb.append("  \"healthCheckResults\": [\n");

            for (int i = 0; i < report.healthCheckResults().size(); i++) {
                var result = report.healthCheckResults().get(i);
                sb.append("    {\n");
                sb.append("      \"name\": \"").append(escapeJson(result.name())).append("\",\n");
                sb.append("      \"status\": \"").append(result.status()).append("\",\n");
                sb.append("      \"message\": \"").append(escapeJson(result.message())).append("\"\n");
                sb.append("    }");
                if (i < report.healthCheckResults().size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ]\n");
            sb.append("}");
            return sb.toString();
        }

        private String generateHtmlReport(DoctorTool.HealthReport report, boolean detailed) {
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html>\n");
            sb.append("<html>\n");
            sb.append("<head>\n");
            sb.append("  <meta charset=\"UTF-8\">\n");
            sb.append("  <title>Java Harness Health Report</title>\n");
            sb.append("  <style>\n");
            sb.append("    body { font-family: Arial, sans-serif; margin: 40px; }\n");
            sb.append("    h1 { color: #333; }\n");
            sb.append("    .status-healthy { color: #28a745; }\n");
            sb.append("    .status-degraded { color: #ffc107; }\n");
            sb.append("    .status-unhealthy { color: #dc3545; }\n");
            sb.append("    table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
            sb.append("    th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
            sb.append("    th { background-color: #f2f2f2; }\n");
            sb.append("  </style>\n");
            sb.append("</head>\n");
            sb.append("<body>\n");
            sb.append("  <h1>🏥 Java Harness Health Report</h1>\n");
            sb.append("  <p><strong>Project:</strong> ").append(escapeHtml(report.projectPath())).append("</p>\n");
            sb.append("  <p><strong>Generated:</strong> ").append(report.generatedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n");
            sb.append("  <p><strong>Overall Status:</strong> <span class=\"status-").append(
                report.overallStatus().toString().toLowerCase()).append("\">").append(
                report.overallStatus()).append("</span></p>\n");

            sb.append("  <h2>Health Check Results</h2>\n");
            sb.append("  <table>\n");
            sb.append("    <tr><th>Status</th><th>Name</th><th>Message</th></tr>\n");
            for (var result : report.healthCheckResults()) {
                sb.append("    <tr>\n");
                sb.append("      <td class=\"status-").append(result.status().toString().toLowerCase()).append("\">").append(
                    result.status()).append("</td>\n");
                sb.append("      <td>").append(escapeHtml(result.name())).append("</td>\n");
                sb.append("      <td>").append(escapeHtml(result.message())).append("</td>\n");
                sb.append("    </tr>\n");
            }
            sb.append("  </table>\n");

            sb.append("  <p><strong>Summary:</strong> ").append(escapeHtml(report.summary())).append("</p>\n");
            sb.append("</body>\n");
            sb.append("</html>\n");
            return sb.toString();
        }

        private String escapeJson(String s) {
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }

        private String escapeHtml(String s) {
            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
        }
    }

    /**
     * Analyze specific component or subsystem
     */
    @Command(name = "analyze",
             mixinStandardHelpOptions = true,
             description = "Analyze specific component or subsystem")
    public static class AnalyzeCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Component to analyze",
                 arity = "1")
        String component;

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--deep"},
                 description = "Perform deep analysis")
        boolean deepAnalysis;

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

                System.out.println("🔬 Analyzing component: " + component);
                System.out.println("  Directory: " + projectPath.toAbsolutePath());
                System.out.println("  Deep analysis: " + deepAnalysis);

                // Component-specific analysis
                int result = analyzeComponent(component, projectPath, deepAnalysis);

                if (result == 0) {
                    System.out.println();
                    System.out.println("✓ Analysis complete");
                } else {
                    System.out.println();
                    System.out.println("⚠️  Analysis found issues");
                }

                return result;

            } catch (Exception e) {
                System.err.println("✗ Analysis failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private int analyzeComponent(String component, Path projectPath, boolean deep) {
            return switch (component.toLowerCase()) {
                case "configuration", "config" -> analyzeConfiguration(projectPath, deep);
                case "dependencies", "deps" -> analyzeDependencies(projectPath, deep);
                case "structure", "project" -> analyzeProjectStructure(projectPath, deep);
                case "skills" -> analyzeSkills(projectPath, deep);
                case "workflows" -> analyzeWorkflows(projectPath, deep);
                default -> {
                    System.err.println("✗ Unknown component: " + component);
                    System.err.println("  Available: configuration, dependencies, structure, skills, workflows");
                    yield 2;
                }
            };
        }

        private int analyzeConfiguration(Path projectPath, boolean deep) {
            System.out.println();
            System.out.println("📋 Configuration Analysis:");

            Path claudeDir = projectPath.resolve(".claude");
            if (!Files.exists(claudeDir)) {
                System.out.println("  ✗ .claude directory not found");
                return 1;
            }

            System.out.println("  ✓ .claude directory exists");

            Path settingsFile = claudeDir.resolve("settings.json");
            if (Files.exists(settingsFile)) {
                System.out.println("  ✓ settings.json exists");
                if (deep) {
                    System.out.println("    Size: " + settingsFile.toFile().length() + " bytes");
                }
            }

            Path harnessConfig = claudeDir.resolve("harness.yaml");
            if (Files.exists(harnessConfig)) {
                System.out.println("  ✓ harness.yaml exists");
            }

            return 0;
        }

        private int analyzeDependencies(Path projectPath, boolean deep) {
            System.out.println();
            System.out.println("📦 Dependencies Analysis:");

            Path pomFile = projectPath.resolve("pom.xml");
            if (!Files.exists(pomFile)) {
                System.out.println("  ✗ pom.xml not found");
                return 1;
            }

            System.out.println("  ✓ pom.xml exists");

            if (deep) {
                try {
                    String pomContent = Files.readString(pomFile);
                    long depCount = pomContent.lines()
                        .filter(line -> line.trim().startsWith("<dependency>"))
                        .count();

                    System.out.println("  Dependencies found: " + depCount);
                } catch (IOException e) {
                    System.out.println("  ⚠️  Could not analyze dependencies: " + e.getMessage());
                }
            }

            return 0;
        }

        private int analyzeProjectStructure(Path projectPath, boolean deep) {
            System.out.println();
            System.out.println("🏗️  Project Structure Analysis:");

            String[] requiredDirs = {"src", "target", ".claude"};
            int issues = 0;

            for (String dir : requiredDirs) {
                Path dirPath = projectPath.resolve(dir);
                if (Files.exists(dirPath)) {
                    System.out.println("  ✓ " + dir + " exists");
                } else {
                    System.out.println("  ✗ " + dir + " missing");
                    issues++;
                }
            }

            if (deep) {
                try {
                    long dirCount = Files.walk(projectPath, 2)
                        .filter(Files::isDirectory)
                        .count();

                    System.out.println("  Total directories (depth 2): " + dirCount);
                } catch (IOException e) {
                    System.out.println("  ⚠️  Could not count directories: " + e.getMessage());
                }
            }

            return issues > 0 ? 1 : 0;
        }

        private int analyzeSkills(Path projectPath, boolean deep) {
            System.out.println();
            System.out.println("🎯 Skills Analysis:");

            Path skillsDir = projectPath.resolve(".claude/skills");
            if (!Files.exists(skillsDir)) {
                System.out.println("  ⚠️  Skills directory not found (optional)");
                return 0;
            }

            try {
                long skillCount = Files.walk(skillsDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md") || p.toString().endsWith(".skill"))
                    .count();

                System.out.println("  ✓ Found " + skillCount + " skill file(s)");

                if (deep && skillCount > 0) {
                    System.out.println("  Skills:");
                    Files.walk(skillsDir)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".md") || p.toString().endsWith(".skill"))
                        .forEach(p -> System.out.println("    - " + p.getFileName()));
                }

            } catch (IOException e) {
                System.out.println("  ⚠️  Could not analyze skills: " + e.getMessage());
            }

            return 0;
        }

        private int analyzeWorkflows(Path projectPath, boolean deep) {
            System.out.println();
            System.out.println("⚙️  Workflows Analysis:");

            Path workflowsDir = projectPath.resolve(".claude/workflows");
            if (!Files.exists(workflowsDir)) {
                System.out.println("  ⚠️  Workflows directory not found (optional)");
                return 0;
            }

            try {
                long workflowCount = Files.walk(workflowsDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md") || p.toString().endsWith(".workflow"))
                    .count();

                System.out.println("  ✓ Found " + workflowCount + " workflow file(s)");

                if (deep && workflowCount > 0) {
                    System.out.println("  Workflows:");
                    Files.walk(workflowsDir)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".md") || p.toString().endsWith(".workflow"))
                        .forEach(p -> System.out.println("    - " + p.getFileName()));
                }

            } catch (IOException e) {
                System.out.println("  ⚠️  Could not analyze workflows: " + e.getMessage());
            }

            return 0;
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new DoctorCommand()).execute(args);
        System.exit(exitCode);
    }
}
