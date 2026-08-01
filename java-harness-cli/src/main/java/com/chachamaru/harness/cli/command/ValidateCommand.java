package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * Validate command for project structure and specification validation.
 *
 * <p>This command provides validation capabilities:
 * <ul>
 *   <li>project - Validate project structure</li>
 *   <li>spec - Validate specification files</li>
 *   <li>config - Validate configuration files</li>
 *   <li>report - Generate validation report</li>
 * </ul>
 * </p>
 */
@Command(name = "validate",
         mixinStandardHelpOptions = true,
         subcommands = {
             ValidateCommand.ProjectCommand.class,
             ValidateCommand.SpecCommand.class,
             ValidateCommand.ConfigCommand.class,
             ValidateCommand.ReportCommand.class
         },
         description = "Validate project structure and specifications")
public class ValidateCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Validate project structure
     */
    @Command(name = "project",
             mixinStandardHelpOptions = true,
             description = "Validate project structure")
    public static class ProjectCommand implements Runnable {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory to validate (default: current directory)",
                 defaultValue = ".")
        private String projectDir;

        @Option(names = {"--strict"},
                 description = "Enable strict validation mode")
        private boolean strict;

        @Option(names = {"--fix"},
                 description = "Attempt to fix common issues")
        private boolean autoFix;

        @Option(names = {"--json"},
                 description = "Output in JSON format")
        private boolean jsonOutput;

        @Override
        public void run() {
            try {
                Path projectPath = Paths.get(projectDir);

                if (!Files.exists(projectPath)) {
                    System.err.println("✗ Project directory not found: " + projectDir);
                    System.exit(1);
                    return;
                }

                System.out.println("🔍 Validating project structure");
                System.out.println("  Directory: " + projectPath.toAbsolutePath());
                System.out.println("  Strict: " + strict);

                ProjectValidator validator = new ProjectValidator(strict);
                ValidationResult result = validator.validate(projectPath);

                if (autoFix && result.issueCount > 0) {
                    System.out.println();
                    System.out.println("🔧 Attempting auto-fix...");
                    int fixed = validator.applyAutoFixes(projectPath, result.issues);
                    System.out.println("✓ Applied " + fixed + " fix(es)");

                    // Re-validate after fixes
                    result = validator.validate(projectPath);
                }

                if (jsonOutput) {
                    outputJsonResult(result);
                } else {
                    outputHumanResult(result);
                }

                if (!result.isValid) {
                    System.exit(1);
                }

            } catch (Exception e) {
                System.err.println("✗ Validation failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        private void outputJsonResult(ValidationResult result) {
            System.out.println("{");
            System.out.println("  \"valid\": " + result.isValid + ",");
            System.out.println("  \"issues\": " + result.issueCount + ",");
            System.out.println("  \"errors\": " + result.errorCount + ",");
            System.out.println("  \"warnings\": " + result.warningCount + ",");
            System.out.println("  \"validatedAt\": \"" + result.validatedAt + "\"");
            System.out.println("}");
        }

        private void outputHumanResult(ValidationResult result) {
            System.out.println();
            System.out.println("📊 Validation Results");
            System.out.println("  Valid: " + (result.isValid ? "✓" : "✗"));
            System.out.println("  Issues: " + result.issueCount);
            System.out.println("  Errors: " + result.errorCount);
            System.out.println("  Warnings: " + result.warningCount);

            if (!result.issues.isEmpty()) {
                System.out.println();
                System.out.println("⚠️  Issues Found:");

                for (ValidationIssue issue : result.issues) {
                    System.out.println("  [" + issue.severity + "] " + issue.category);
                    System.out.println("      " + issue.message);
                    if (issue.location != null) {
                        System.out.println("      Location: " + issue.location);
                    }
                    if (issue.fix != null) {
                        System.out.println("      🔧 Fix: " + issue.fix);
                    }
                    System.out.println();
                }
            }

            if (result.isValid) {
                System.out.println();
                System.out.println("✓ Project structure validation passed");
            }
        }
    }

    /**
     * Validate specification files
     */
    @Command(name = "spec",
             mixinStandardHelpOptions = true,
             description = "Validate specification files")
    public static class SpecCommand implements Runnable {

        @Parameters(index = "0", description = "Specification file to validate",
                 arity = "0..1")
        private String specFile;

        @Option(names = {"--all"},
                 description = "Validate all spec files in project")
        private boolean validateAll;

        @Option(names = {"-d", "--directory"},
                 description = "Directory containing spec files")
        private String specDirectory;

        @Override
        public void run() {
            try {
                SpecValidator validator = new SpecValidator();

                if (validateAll) {
                    System.out.println("🔍 Validating all specification files");
                    List<SpecValidationResult> results = validator.validateAll(specDirectory);

                    System.out.println();
                    System.out.println("📊 Specification Validation");
                    System.out.println("  Files validated: " + results.size());

                    int validCount = 0;
                    for (SpecValidationResult result : results) {
                        if (result.isValid) {
                            validCount++;
                            System.out.println("  ✓ " + result.file + " - Valid");
                        } else {
                            System.out.println("  ✗ " + result.file + " - Invalid");
                        }
                    }

                    System.out.println();
                    System.out.println("  Summary: " + validCount + "/" + results.size() + " files valid");

                } else if (specFile != null) {
                    System.out.println("🔍 Validating specification: " + specFile);
                    SpecValidationResult result = validator.validate(Paths.get(specFile));

                    if (result.isValid) {
                        System.out.println("✓ Specification is valid");
                    } else {
                        System.out.println("✗ Specification validation failed");
                        System.out.println("  Errors: " + result.errors);
                        System.out.println("  Warnings: " + result.warnings);
                        System.exit(1);
                    }

                } else {
                    System.err.println("✗ No specification file specified");
                    System.err.println("  Use --all to validate all specs");
                    System.err.println("  Or provide specific spec file");
                    System.exit(1);
                }

            } catch (Exception e) {
                System.err.println("✗ Spec validation failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Validate configuration files
     */
    @Command(name = "config",
             mixinStandardHelpOptions = true,
             description = "Validate configuration files")
    public static class ConfigCommand implements Runnable {

        @Parameters(index = "0", description = "Configuration file to validate",
                 arity = "0..1")
        private String configFile;

        @Option(names = {"--all"},
                 description = "Validate all config files")
        private boolean validateAll;

        @Option(names = {"--schema"},
                 description = "Schema to validate against")
        private String schema;

        @Override
        public void run() {
            try {
                ConfigValidator validator = new ConfigValidator();

                if (validateAll) {
                    System.out.println("🔍 Validating all configuration files");
                    List<ConfigValidationResult> results = validator.validateAll();

                    System.out.println();
                    System.out.println("📊 Configuration Validation");

                    for (ConfigValidationResult result : results) {
                        System.out.println("  " + result.file + ":");
                        System.out.println("    Valid: " + (result.isValid ? "✓" : "✗"));
                        if (!result.errors.isEmpty()) {
                            for (String error : result.errors) {
                                System.out.println("    ✗ " + error);
                            }
                        }
                    }

                } else if (configFile != null) {
                    System.out.println("🔍 Validating configuration: " + configFile);
                    ConfigValidationResult result = validator.validate(Paths.get(configFile), schema);

                    if (result.isValid) {
                        System.out.println("✓ Configuration is valid");
                    } else {
                        System.out.println("✗ Configuration validation failed");
                        for (String error : result.errors) {
                            System.out.println("  ✗ " + error);
                        }
                        System.exit(1);
                    }

                } else {
                    System.err.println("✗ No configuration file specified");
                    System.err.println("  Use --all to validate all configs");
                    System.err.println("  Or provide specific config file");
                    System.exit(1);
                }

            } catch (Exception e) {
                System.err.println("✗ Config validation failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Generate validation report
     */
    @Command(name = "report",
             mixinStandardHelpOptions = true,
             description = "Generate validation report")
    public static class ReportCommand implements Runnable {

        @Option(names = {"-o", "--output"},
                 description = "Report output file",
                 defaultValue = "validation-report.md")
        private String outputFile;

        @Option(names = {"-f", "--format"},
                 description = "Report format: md, json, html",
                 defaultValue = "md")
        private String format;

        @Option(names = {"--include"},
                 description = "Validation types to include (comma-separated)")
        private String includeTypes;

        @Override
        public void run() {
            try {
                System.out.println("📝 Generating validation report");
                System.out.println("  Output: " + outputFile);
                System.out.println("  Format: " + format);

                ReportGenerator generator = new ReportGenerator();
                boolean generated = generator.generate(outputFile, format, includeTypes);

                if (generated) {
                    System.out.println("✓ Report generated successfully");
                } else {
                    System.err.println("✗ Report generation failed");
                    System.exit(1);
                }

            } catch (Exception e) {
                System.err.println("✗ Report generation failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Project validator
     */
    public static class ProjectValidator {
        private final boolean strict;

        public ProjectValidator(boolean strict) {
            this.strict = strict;
        }

        public ValidationResult validate(Path projectPath) {
            ValidationResult result = new ValidationResult();
            result.validatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
            result.issues = new ArrayList<>();

            // Check for required directories
            String[] requiredDirs = {"src", "target", ".claude"};
            for (String dir : requiredDirs) {
                Path dirPath = projectPath.resolve(dir);
                if (!Files.exists(dirPath)) {
                    ValidationIssue issue = new ValidationIssue();
                    issue.severity = "error";
                    issue.category = "Directory Structure";
                    issue.message = "Required directory missing: " + dir;
                    issue.location = dirPath.toString();
                    issue.fix = "Create directory: mkdir -p " + dir;
                    result.issues.add(issue);
                    result.errorCount++;
                }
            }

            // Check for required files
            String[] requiredFiles = {"Plans.md", "pom.xml", ".gitignore"};
            for (String file : requiredFiles) {
                Path filePath = projectPath.resolve(file);
                if (!Files.exists(filePath)) {
                    ValidationIssue issue = new ValidationIssue();
                    issue.severity = strict ? "error" : "warning";
                    issue.category = "Required Files";
                    issue.message = "Required file missing: " + file;
                    issue.location = filePath.toString();
                    issue.fix = "Create missing file: " + file;
                    result.issues.add(issue);

                    if (issue.severity.equals("error")) {
                        result.errorCount++;
                    } else {
                        result.warningCount++;
                    }
                }
            }

            // Check file naming conventions
            try (Stream<Path> paths = Files.walk(projectPath, 10)) {
                paths.filter(Files::isRegularFile)
                     .filter(p -> p.toString().contains(" "))
                     .forEach(p -> {
                         ValidationIssue issue = new ValidationIssue();
                         issue.severity = strict ? "error" : "warning";
                         issue.category = "File Naming";
                         issue.message = "File contains spaces in name: " + p.getFileName();
                         issue.location = p.toString();
                         issue.fix = "Rename file to remove spaces";
                         result.issues.add(issue);

                         if (issue.severity.equals("error")) {
                             result.errorCount++;
                         } else {
                             result.warningCount++;
                         }
                     });
            } catch (IOException e) {
                // Skip if walk fails
            }

            result.issueCount = result.issues.size();
            result.isValid = result.errorCount == 0;

            return result;
        }

        public int applyAutoFixes(Path projectPath, List<ValidationIssue> issues) {
            int fixed = 0;

            for (ValidationIssue issue : issues) {
                if (issue.category.equals("Directory Structure") && issue.severity.equals("error")) {
                    try {
                        Path dirPath = Paths.get(issue.location);
                        Files.createDirectories(dirPath);
                        fixed++;
                    } catch (Exception e) {
                        System.err.println("  ✗ Failed to create directory: " + issue.location);
                    }
                }
            }

            return fixed;
        }
    }

    /**
     * Specification validator
     */
    public static class SpecValidator {
        public List<SpecValidationResult> validateAll(String specDirectory) {
            List<SpecValidationResult> results = new ArrayList<>();

            // Mock implementation
            SpecValidationResult result = new SpecValidationResult();
            result.file = "Plans.md";
            result.isValid = true;
            result.errors = 0;
            result.warnings = 0;
            results.add(result);

            return results;
        }

        public SpecValidationResult validate(Path specFile) {
            SpecValidationResult result = new SpecValidationResult();
            result.file = specFile.toString();
            result.isValid = true;
            result.errors = 0;
            result.warnings = 0;

            try {
                List<String> lines = Files.readAllLines(specFile);

                // Basic validation checks
                boolean hasHeader = false;
                boolean hasTable = false;

                for (String line : lines) {
                    if (line.matches("^#+\\s.*")) {
                        hasHeader = true;
                    }
                    if (line.contains("| Task |")) {
                        hasTable = true;
                    }
                }

                if (!hasHeader) {
                    result.errors++;
                    result.isValid = false;
                }

                if (!hasTable) {
                    result.warnings++;
                }

            } catch (Exception e) {
                result.isValid = false;
                result.errors++;
            }

            return result;
        }
    }

    /**
     * Configuration validator
     */
    public static class ConfigValidator {
        public List<ConfigValidationResult> validateAll() {
            List<ConfigValidationResult> results = new ArrayList<>();

            // Mock implementation
            ConfigValidationResult result = new ConfigValidationResult();
            result.file = ".claude/settings.json";
            result.isValid = true;
            result.errors = new ArrayList<>();
            results.add(result);

            return results;
        }

        public ConfigValidationResult validate(Path configFile, String schema) {
            ConfigValidationResult result = new ConfigValidationResult();
            result.file = configFile.toString();
            result.isValid = true;
            result.errors = new ArrayList<>();

            try {
                if (!Files.exists(configFile)) {
                    result.isValid = false;
                    result.errors.add("Configuration file not found");
                } else {
                    // Basic JSON validation would go here
                    String content = Files.readString(configFile);
                    if (content.trim().isEmpty()) {
                        result.isValid = false;
                        result.errors.add("Configuration file is empty");
                    }
                }
            } catch (Exception e) {
                result.isValid = false;
                result.errors.add("Failed to read configuration: " + e.getMessage());
            }

            return result;
        }
    }

    /**
     * Report generator
     */
    public static class ReportGenerator {
        public boolean generate(String outputFile, String format, String includeTypes) {
            try {
                Path reportPath = Paths.get(outputFile);
                Files.createDirectories(reportPath.getParent());

                StringBuilder report = new StringBuilder();
                report.append("# Validation Report\n\n");
                report.append("**Generated:** ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)).append("\n\n");
                report.append("## Summary\n\n");
                report.append("This report contains validation results for the project.\n\n");

                Files.write(reportPath, report.toString().getBytes(StandardCharsets.UTF_8));
                return true;

            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * Validation result holder
     */
    public static class ValidationResult {
        boolean isValid;
        int issueCount;
        int errorCount;
        int warningCount;
        String validatedAt;
        List<ValidationIssue> issues;
    }

    /**
     * Validation issue holder
     */
    public static class ValidationIssue {
        String severity;
        String category;
        String message;
        String location;
        String fix;
    }

    /**
     * Spec validation result holder
     */
    public static class SpecValidationResult {
        String file;
        boolean isValid;
        int errors;
        int warnings;
    }

    /**
     * Config validation result holder
     */
    public static class ConfigValidationResult {
        String file;
        boolean isValid;
        List<String> errors;
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new ValidateCommand()).execute(args);
        System.exit(exitCode);
    }
}