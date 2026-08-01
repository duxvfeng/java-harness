package com.chachamaru.harness.tools.validation;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Diagnostic tool for Java Harness system health.
 *
 * <p>Generates comprehensive health reports by running all health checks
 * and aggregating results. Provides diagnosis and remediation recommendations.</p>
 *
 * @spec_reference spec.md#Diagnostic System
 */
public class DoctorTool {

    private final Path projectRoot;
    private final List<HealthCheck> healthChecks;
    private final ValidateTool validateTool;

    /**
     * Creates a doctor tool for the current directory.
     */
    public DoctorTool() {
        this(Paths.get("").toAbsolutePath());
    }

    /**
     * Creates a doctor tool for a specific project root.
     *
     * @param projectRoot Project root directory
     */
    public DoctorTool(Path projectRoot) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("projectRoot cannot be null");
        }
        this.projectRoot = projectRoot;
        this.healthChecks = new ArrayList<>();
        this.validateTool = new ValidateTool(projectRoot);
        initializeDefaultHealthChecks();
    }

    /**
     * Initializes default health checks.
     */
    private void initializeDefaultHealthChecks() {
        // Configuration health check
        addHealthCheck(new HealthCheck() {
            @Override
            public HealthCheckResult check() {
                Path claudeDir = projectRoot.resolve(".claude");
                if (!Files.exists(claudeDir)) {
                    return HealthCheck.HealthCheckResult.unhealthy(
                        "configuration",
                        ".claude directory not found"
                    );
                }

                Path settingsFile = claudeDir.resolve("settings.json");
                Path harnessConfig = claudeDir.resolve("harness.yaml");

                if (!Files.exists(settingsFile) && !Files.exists(harnessConfig)) {
                    return HealthCheck.HealthCheckResult.degraded(
                        "configuration",
                        "No configuration files found"
                    );
                }

                return HealthCheck.HealthCheckResult.healthy(
                    "configuration",
                    "Configuration files present"
                );
            }

            @Override
            public String getName() {
                return "configuration";
            }

            @Override
            public String getDescription() {
                return "Checks configuration file presence and validity";
            }
        });

        // Project structure health check
        addHealthCheck(new HealthCheck() {
            @Override
            public HealthCheckResult check() {
                Path pomFile = projectRoot.resolve("pom.xml");
                if (!Files.exists(pomFile)) {
                    return HealthCheck.HealthCheckResult.unhealthy(
                        "project-structure",
                        "pom.xml not found"
                    );
                }

                // Check for required modules
                String[] requiredModules = {
                    "java-harness-foundation",
                    "java-harness-protocol",
                    "java-harness-workflow"
                };

                for (String module : requiredModules) {
                    if (!Files.exists(projectRoot.resolve(module))) {
                        return HealthCheck.HealthCheckResult.degraded(
                            "project-structure",
                            String.format("Module not found: %s", module)
                        );
                    }
                }

                return HealthCheck.HealthCheckResult.healthy(
                    "project-structure",
                    "Project structure intact"
                );
            }

            @Override
            public String getName() {
                return "project-structure";
            }

            @Override
            public String getDescription() {
                return "Checks project structure and required modules";
            }
        });

        // Dependencies health check
        addHealthCheck(new HealthCheck() {
            @Override
            public HealthCheckResult check() {
                Path pomFile = projectRoot.resolve("pom.xml");
                if (!Files.exists(pomFile)) {
                    return HealthCheck.HealthCheckResult.unhealthy(
                        "dependencies",
                        "Cannot check dependencies - pom.xml missing"
                    );
                }

                try {
                    String pomContent = Files.readString(pomFile);
                    boolean hasJackson = pomContent.contains("jackson");
                    boolean hasJUnit = pomContent.contains("junit");

                    if (!hasJackson || !hasJUnit) {
                        return HealthCheck.HealthCheckResult.degraded(
                            "dependencies",
                            "Some dependencies may be missing"
                        );
                    }

                    return HealthCheck.HealthCheckResult.healthy(
                        "dependencies",
                        "Dependencies appear intact"
                    );
                } catch (Exception e) {
                    return HealthCheck.HealthCheckResult.unhealthy(
                        "dependencies",
                        "Failed to validate dependencies: " + e.getMessage()
                    );
                }
            }

            @Override
            public String getName() {
                return "dependencies";
            }

            @Override
            public String getDescription() {
                return "Checks dependency integrity";
            }
        });

        // Skills health check
        addHealthCheck(new HealthCheck() {
            @Override
            public HealthCheckResult check() {
                Path skillsDir = projectRoot.resolve(".claude/skills");
                if (!Files.exists(skillsDir)) {
                    return HealthCheck.HealthCheckResult.degraded(
                        "skills",
                        "Skills directory not found (optional)"
                    );
                }

                try {
                    long skillCount = Files.walk(skillsDir)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".md") || p.toString().endsWith(".skill"))
                        .count();

                    if (skillCount == 0) {
                        return HealthCheck.HealthCheckResult.degraded(
                            "skills",
                            "No skill files found (optional)"
                        );
                    }

                    Map<String, Object> details = Map.of("skillCount", skillCount);
                    return new HealthCheck.HealthCheckResult(
                        "skills",
                        HealthCheck.HealthStatus.HEALTHY,
                        String.format("Found %d skill files", skillCount),
                        details,
                        0L
                    );
                } catch (Exception e) {
                    return HealthCheck.HealthCheckResult.unhealthy(
                        "skills",
                        "Failed to check skills: " + e.getMessage()
                    );
                }
            }

            @Override
            public String getName() {
                return "skills";
            }

            @Override
            public String getDescription() {
                return "Checks skill system setup";
            }
        });
    }

    /**
     * Adds a custom health check.
     *
     * @param healthCheck Health check to add
     */
    public void addHealthCheck(HealthCheck healthCheck) {
        if (healthCheck != null) {
            healthChecks.add(healthCheck);
        }
    }

    /**
     * Runs all health checks and generates a report.
     *
     * @return Health report
     */
    public HealthReport generateReport() {
        System.out.println("[DoctorTool] Generating health report...");

        List<HealthCheck.HealthCheckResult> results = new ArrayList<>();

        // Run all health checks
        for (HealthCheck check : healthChecks) {
            if (!check.isEnabled()) {
                continue;
            }

            try {
                long startTime = System.currentTimeMillis();
                HealthCheck.HealthCheckResult result = check.check();
                long duration = System.currentTimeMillis() - startTime;

                // Create result with duration
                results.add(new HealthCheck.HealthCheckResult(
                    result.name(),
                    result.status(),
                    result.message(),
                    result.details(),
                    duration
                ));

                System.out.printf("[DoctorTool] ✓ %s: %s (%dms)%n",
                    result.name(), result.status(), duration);

            } catch (Exception e) {
                results.add(HealthCheck.HealthCheckResult.unhealthy(
                    check.getName(),
                    "Health check failed: " + e.getMessage()
                ));
            }
        }

        // Run validation tool
        ValidateTool.ValidationResult validationResult = validateTool.validateAll();

        // Calculate overall health
        HealthCheck.HealthStatus overallStatus = calculateOverallStatus(results, validationResult);

        return new HealthReport(
            projectRoot.toString(),
            LocalDateTime.now(),
            overallStatus,
            results,
            validationResult,
            generateSummary(results, validationResult)
        );
    }

    /**
     * Calculates overall system health status.
     */
    private HealthCheck.HealthStatus calculateOverallStatus(
            List<HealthCheck.HealthCheckResult> results,
            ValidateTool.ValidationResult validationResult) {

        boolean hasErrors = validationResult.errorCount() > 0 ||
            results.stream().anyMatch(r -> r.status() == HealthCheck.HealthStatus.UNHEALTHY);

        boolean hasWarnings = validationResult.warningCount() > 0 ||
            results.stream().anyMatch(r -> r.status() == HealthCheck.HealthStatus.DEGRADED);

        if (hasErrors) {
            return HealthCheck.HealthStatus.UNHEALTHY;
        } else if (hasWarnings) {
            return HealthCheck.HealthStatus.DEGRADED;
        } else {
            return HealthCheck.HealthStatus.HEALTHY;
        }
    }

    /**
     * Generates a summary of the health report.
     */
    private String generateSummary(
            List<HealthCheck.HealthCheckResult> results,
            ValidateTool.ValidationResult validationResult) {

        long healthyCount = results.stream()
            .filter(r -> r.status() == HealthCheck.HealthStatus.HEALTHY)
            .count();

        long degradedCount = results.stream()
            .filter(r -> r.status() == HealthCheck.HealthStatus.DEGRADED)
            .count();

        long unhealthyCount = results.stream()
            .filter(r -> r.status() == HealthCheck.HealthStatus.UNHEALTHY)
            .count();

        return String.format(
            "Health check complete: %d healthy, %d degraded, %d unhealthy. Validation found %d errors, %d warnings.",
            healthyCount, degradedCount, unhealthyCount,
            validationResult.errorCount(), validationResult.warningCount()
        );
    }

    /**
     * Gets the registered health checks.
     *
     * @return List of health checks
     */
    public List<HealthCheck> getHealthChecks() {
        return Collections.unmodifiableList(healthChecks);
    }

    /**
     * Gets the number of registered health checks.
     *
     * @return Number of health checks
     */
    public int getHealthCheckCount() {
        return healthChecks.size();
    }

    /**
     * Clears all registered health checks.
     */
    public void clearHealthChecks() {
        healthChecks.clear();
    }

    /**
     * Comprehensive health report.
     */
    public record HealthReport(
        String projectPath,
        LocalDateTime generatedAt,
        HealthCheck.HealthStatus overallStatus,
        List<HealthCheck.HealthCheckResult> healthCheckResults,
        ValidateTool.ValidationResult validationResult,
        String summary
    ) {
        public HealthReport {
            if (projectPath == null) {
                projectPath = "";
            }
            if (generatedAt == null) {
                generatedAt = LocalDateTime.now();
            }
            if (overallStatus == null) {
                overallStatus = HealthCheck.HealthStatus.UNKNOWN;
            }
            if (healthCheckResults == null) {
                healthCheckResults = List.of();
            }
            if (validationResult == null) {
                validationResult = new ValidateTool.ValidationResult(true, List.of(), "No validation run");
            }
        }

        /**
         * Formats the report as a human-readable string.
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            String line = "=".repeat(60);

            sb.append(line).append("\n");
            sb.append("JAVA HARNESS HEALTH REPORT\n");
            sb.append(line).append("\n\n");

            sb.append(String.format("Project: %s\n", projectPath));
            sb.append(String.format("Generated: %s\n", generatedAt.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            sb.append(String.format("Overall Status: %s\n\n", overallStatus));

            sb.append("Health Check Results:\n");
            sb.append("-".repeat(60)).append("\n");
            for (HealthCheck.HealthCheckResult result : healthCheckResults) {
                sb.append(String.format("  [%s] %s: %s\n",
                    result.status(), result.name(), result.message()));
            }
            sb.append("\n");

            sb.append("Validation Results:\n");
            sb.append("-".repeat(60)).append("\n");
            sb.append(String.format("  Valid: %s\n", validationResult.valid()));
            sb.append(String.format("  Errors: %d\n", validationResult.errorCount()));
            sb.append(String.format("  Warnings: %d\n", validationResult.warningCount()));
            sb.append("\n");

            if (!validationResult.issues().isEmpty()) {
                sb.append("Issues Found:\n");
                sb.append("-".repeat(60)).append("\n");
                for (ValidateTool.ValidationIssue issue : validationResult.issues()) {
                    sb.append(String.format("  [%s] %s: %s\n",
                        issue.severity(), issue.category(), issue.message()));
                    if (!issue.recommendation().isEmpty()) {
                        sb.append(String.format("      → %s\n", issue.recommendation()));
                    }
                }
                sb.append("\n");
            }

            sb.append(line).append("\n");
            sb.append("Summary: ").append(summary).append("\n");
            sb.append(line).append("\n");

            return sb.toString();
        }

        /**
         * Checks if the system is healthy.
         */
        public boolean isHealthy() {
            return overallStatus == HealthCheck.HealthStatus.HEALTHY;
        }

        /**
         * Checks if the system has issues.
         */
        public boolean hasIssues() {
            return overallStatus != HealthCheck.HealthStatus.HEALTHY;
        }
    }
}
