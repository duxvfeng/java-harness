package com.chachamaru.harness.tools.validation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Validation tool for Java Harness configuration and setup.
 *
 * <p>Performs comprehensive validation of:
 * <ul>
 *   <li>Configuration files (settings.json, harness.yaml)</li>
 *   <li>Skill system setup</li>
 *   <li>Agent system setup</li>
 *   <li>Project structure</li>
 *   <li>Dependency integrity</li>
 * </ul>
 *
 * @spec_reference spec.md#Validation System
 */
public class ValidateTool {

    private final Path projectRoot;
    private final List<ValidationIssue> issues;

    /**
     * Creates a validation tool for the current directory.
     */
    public ValidateTool() {
        this(Paths.get("").toAbsolutePath());
    }

    /**
     * Creates a validation tool for a specific project root.
     *
     * @param projectRoot Project root directory
     */
    public ValidateTool(Path projectRoot) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("projectRoot cannot be null");
        }
        if (!Files.exists(projectRoot)) {
            throw new IllegalArgumentException("Project root does not exist: " + projectRoot);
        }
        this.projectRoot = projectRoot;
        this.issues = new ArrayList<>();
    }

    /**
     * Validates all aspects of the Java Harness setup.
     *
     * @return Validation result
     */
    public ValidationResult validateAll() {
        issues.clear();

        System.out.println("[ValidateTool] Starting comprehensive validation...");

        validateConfiguration();
        validateProjectStructure();
        validateSkillSystem();
        validateAgentSystem();
        validateDependencies();

        return new ValidationResult(
            issues.isEmpty(),
            new ArrayList<>(issues),
            String.format("Validation complete: %d issues found", issues.size())
        );
    }

    /**
     * Validates configuration files.
     */
    private void validateConfiguration() {
        System.out.println("[ValidateTool] Validating configuration files...");

        // Check .claude directory
        Path claudeDir = projectRoot.resolve(".claude");
        if (!Files.exists(claudeDir)) {
            issues.add(new ValidationIssue(
                ValidationIssueSeverity.WARNING,
                "configuration",
                ".claude directory not found",
                "Run initialization: mkdir -p .claude"
            ));
        } else {
            // Check settings.json
            Path settingsFile = claudeDir.resolve("settings.json");
            if (!Files.exists(settingsFile)) {
                issues.add(new ValidationIssue(
                    ValidationIssueSeverity.INFO,
                    "configuration",
                    "settings.json not found",
                    "Generate settings using ConfigSyncTool"
                ));
            } else {
                validateJsonFile(settingsFile);
            }

            // Check harness.yaml
            Path harnessConfig = claudeDir.resolve("harness.yaml");
            if (!Files.exists(harnessConfig)) {
                issues.add(new ValidationIssue(
                    ValidationIssueSeverity.INFO,
                    "configuration",
                    "harness.yaml not found",
                    "Generate config using ConfigSyncTool"
                ));
            } else {
                validateYamlFile(harnessConfig);
            }
        }

        // Check for pom.xml
        Path pomFile = projectRoot.resolve("pom.xml");
        if (!Files.exists(pomFile)) {
            issues.add(new ValidationIssue(
                ValidationIssueSeverity.ERROR,
                "project",
                "pom.xml not found",
                "Ensure this is a valid Maven project"
            ));
        }
    }

    /**
     * Validates project structure.
     */
    private void validateProjectStructure() {
        System.out.println("[ValidateTool] Validating project structure...");

        String[] requiredModules = {
            "java-harness-foundation",
            "java-harness-protocol",
            "java-harness-security",
            "java-harness-workflow",
            "java-harness-collaboration",
            "java-harness-tools"
        };

        for (String module : requiredModules) {
            Path modulePath = projectRoot.resolve(module);
            if (!Files.exists(modulePath)) {
                issues.add(new ValidationIssue(
                    ValidationIssueSeverity.ERROR,
                    "structure",
                    String.format("Required module not found: %s", module),
                    "Ensure all required modules are present"
                ));
            }
        }

        // Check for src directories
        Path srcDir = projectRoot.resolve("src");
        if (!Files.exists(srcDir)) {
            issues.add(new ValidationIssue(
                ValidationIssueSeverity.WARNING,
                "structure",
                "src directory not found in project root",
                "Expected structure: src/main/java, src/test/java"
            ));
        }
    }

    /**
     * Validates skill system setup.
     */
    private void validateSkillSystem() {
        System.out.println("[ValidateTool] Validating skill system...");

        Path skillsDir = projectRoot.resolve(".claude/skills");
        if (!Files.exists(skillsDir)) {
            issues.add(new ValidationIssue(
                ValidationIssueSeverity.INFO,
                "skills",
                "Skills directory not found",
                "Create skills directory: mkdir -p .claude/skills"
            ));
        } else {
            // Count skill files
            long skillCount = listFiles(skillsDir, ".md", ".skill").stream()
                .filter(Files::isRegularFile)
                .count();

            if (skillCount == 0) {
                issues.add(new ValidationIssue(
                    ValidationIssueSeverity.INFO,
                    "skills",
                    "No skill files found",
                    "Add skill files to enable skill system"
                ));
            }
        }
    }

    /**
     * Validates agent system setup.
     */
    private void validateAgentSystem() {
        System.out.println("[ValidateTool] Validating agent system...");

        Path agentsDir = projectRoot.resolve(".claude/agents");
        if (!Files.exists(agentsDir)) {
            issues.add(new ValidationIssue(
                ValidationIssueSeverity.INFO,
                "agents",
                "Agents directory not found",
                "Create agents directory: mkdir -p .claude/agents"
            ));
        }
    }

    /**
     * Validates dependency integrity.
     */
    private void validateDependencies() {
        System.out.println("[ValidateTool] Validating dependencies...");

        // This is a simplified check - in real implementation would parse pom.xml
        Path pomFile = projectRoot.resolve("pom.xml");
        if (Files.exists(pomFile)) {
            try {
                String pomContent = Files.readString(pomFile);

                // Check for essential dependencies
                String[] essentialDeps = {
                    "jackson-databind",
                    "junit-jupiter",
                    "slf4j-api"
                };

                for (String dep : essentialDeps) {
                    if (!pomContent.contains(dep)) {
                        issues.add(new ValidationIssue(
                            ValidationIssueSeverity.WARNING,
                            "dependencies",
                            String.format("Dependency may be missing: %s", dep),
                            "Verify pom.xml includes all required dependencies"
                        ));
                    }
                }

            } catch (IOException e) {
                issues.add(new ValidationIssue(
                    ValidationIssueSeverity.ERROR,
                    "dependencies",
                    "Failed to read pom.xml",
                    e.getMessage()
                ));
            }
        }
    }

    /**
     * Validates a JSON file.
     */
    private void validateJsonFile(Path jsonFile) {
        try {
            String content = Files.readString(jsonFile);
            // Basic JSON validation
            if (!content.trim().startsWith("{") && !content.trim().startsWith("[")) {
                issues.add(new ValidationIssue(
                    ValidationIssueSeverity.ERROR,
                    "configuration",
                    String.format("Invalid JSON format: %s", jsonFile.getFileName()),
                    "Fix JSON syntax errors"
                ));
            }
        } catch (IOException e) {
            issues.add(new ValidationIssue(
                ValidationIssueSeverity.ERROR,
                "configuration",
                String.format("Failed to read %s", jsonFile.getFileName()),
                e.getMessage()
            ));
        }
    }

    /**
     * Validates a YAML file.
     */
    private void validateYamlFile(Path yamlFile) {
        try {
            String content = Files.readString(yamlFile);
            // Basic YAML validation (very simplified)
            if (content.trim().isEmpty()) {
                issues.add(new ValidationIssue(
                    ValidationIssueSeverity.WARNING,
                    "configuration",
                    String.format("Empty YAML file: %s", yamlFile.getFileName()),
                    "Add configuration content"
                ));
            }
        } catch (IOException e) {
            issues.add(new ValidationIssue(
                ValidationIssueSeverity.ERROR,
                "configuration",
                String.format("Failed to read %s", yamlFile.getFileName()),
                e.getMessage()
            ));
        }
    }

    /**
     * Lists files in a directory with specific extensions.
     */
    private List<Path> listFiles(Path directory, String... extensions) {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return List.of();
        }

        try {
            return Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    for (String ext : extensions) {
                        if (fileName.endsWith(ext)) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * Gets the validation issues found.
     *
     * @return List of validation issues
     */
    public List<ValidationIssue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    /**
     * Clears all validation issues.
     */
    public void clearIssues() {
        issues.clear();
    }

    /**
     * Result of a validation operation.
     */
    public record ValidationResult(
        boolean valid,
        List<ValidationIssue> issues,
        String summary
    ) {
        public ValidationResult {
            if (issues == null) {
                issues = List.of();
            }
        }

        /**
         * Gets the number of error-level issues.
         */
        public int errorCount() {
            return (int) issues.stream()
                .filter(issue -> issue.severity() == ValidationIssueSeverity.ERROR)
                .count();
        }

        /**
         * Gets the number of warning-level issues.
         */
        public int warningCount() {
            return (int) issues.stream()
                .filter(issue -> issue.severity() == ValidationIssueSeverity.WARNING)
                .count();
        }

        /**
         * Gets issues by severity.
         */
        public List<ValidationIssue> getIssuesBySeverity(ValidationIssueSeverity severity) {
            return issues.stream()
                .filter(issue -> issue.severity() == severity)
                .toList();
        }
    }

    /**
     * A validation issue found during validation.
     */
    public record ValidationIssue(
        ValidationIssueSeverity severity,
        String category,
        String message,
        String recommendation
    ) {
        public ValidationIssue {
            if (severity == null) {
                severity = ValidationIssueSeverity.INFO;
            }
            if (category == null || category.isBlank()) {
                category = "general";
            }
            if (message == null || message.isBlank()) {
                message = "No message provided";
            }
            if (recommendation == null) {
                recommendation = "";
            }
        }
    }

    /**
     * Severity levels for validation issues.
     */
    public enum ValidationIssueSeverity {
        /** Critical issue that must be fixed */
        ERROR,

        /** Warning that should be addressed */
        WARNING,

        /** Informational message */
        INFO
    }
}
