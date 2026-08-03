package com.chachamaru.harness.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Validate command handler.
 * Validates skills, agents, or configuration.
 */
public class ValidateHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(ValidateHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            String targetType = "all";
            String projectRoot = ".";

            // Parse arguments
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("skills") || arg.equals("agents") || arg.equals("all")) {
                    targetType = arg;
                } else if (!arg.startsWith("--")) {
                    projectRoot = arg;
                }
            }

            logger.info("Validating: {} in {}", targetType, projectRoot);

            System.out.println("🔍 Java Harness Validation");
            System.out.println("==========================");

            int errors = 0;
            int warnings = 0;

            if (targetType.equals("skills") || targetType.equals("all")) {
                ValidationResult result = validateSkills(projectRoot);
                errors += result.errors;
                warnings += result.warnings;
            }

            if (targetType.equals("agents") || targetType.equals("all")) {
                ValidationResult result = validateAgents(projectRoot);
                errors += result.errors;
                warnings += result.warnings;
            }

            if (targetType.equals("all")) {
                ValidationResult result = validateConfig(projectRoot);
                errors += result.errors;
                warnings += result.warnings;
            }

            // Summary
            System.out.println("\n📊 Validation Summary:");
            System.out.println("  Warnings: " + warnings);
            System.out.println("  Errors: " + errors);

            if (errors > 0) {
                System.out.println("\n✗ Validation failed with " + errors + " errors");
                System.exit(1);
            } else if (warnings > 0) {
                System.out.println("\n⚠️  Validation passed with " + warnings + " warnings");
            } else {
                System.out.println("\n✓ Validation passed successfully");
            }

        } catch (Exception e) {
            logger.error("Validation failed", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private ValidationResult validateSkills(String projectRoot) {
        System.out.println("\n🎯 Validating Skills:");
        ValidationResult result = new ValidationResult();

        String skillsDir = projectRoot + "/.claude-plugin/skills";
        Path skillsPath = Paths.get(skillsDir);

        if (!Files.exists(skillsPath)) {
            System.out.println("  ⚠️  Skills directory not found: " + skillsDir);
            result.warnings++;
            return result;
        }

        try {
            List<Path> skillDirs = Files.list(skillsPath)
                    .filter(Files::isDirectory)
                    .toList();

            System.out.println("  Found " + skillDirs.size() + " skill directories");

            for (Path skillDir : skillDirs) {
                String skillName = skillDir.getFileName().toString();
                Path skillFile = skillDir.resolve("SKILL.md");

                if (Files.exists(skillFile)) {
                    // Validate skill file format
                    String content = Files.readString(skillFile);

                    // Check for required frontmatter fields
                    if (!content.contains("---")) {
                        System.out.println("  ✗ " + skillName + ": Missing frontmatter");
                        result.errors++;
                    } else if (!content.contains("name:") || !content.contains("description:")) {
                        System.out.println("  ⚠️  " + skillName + ": Missing required fields");
                        result.warnings++;
                    } else {
                        System.out.println("  ✓ " + skillName);
                    }
                } else {
                    System.out.println("  ✗ " + skillName + ": Missing SKILL.md");
                    result.errors++;
                }
            }

        } catch (IOException e) {
            System.out.println("  ✗ Error reading skills directory: " + e.getMessage());
            result.errors++;
        }

        return result;
    }

    private ValidationResult validateAgents(String projectRoot) {
        System.out.println("\n🤖 Validating Agents:");
        ValidationResult result = new ValidationResult();

        // Check for agent configuration
        String agentsFile = projectRoot + "/.claude/agents.json";
        Path agentsPath = Paths.get(agentsFile);

        if (Files.exists(agentsPath)) {
            System.out.println("  ✓ Agents configuration found");
        } else {
            System.out.println("  ⚠️  No agents configuration (optional)");
            result.warnings++;
        }

        return result;
    }

    private ValidationResult validateConfig(String projectRoot) {
        System.out.println("\n⚙️  Validating Configuration:");
        ValidationResult result = new ValidationResult();

        String configFile = projectRoot + "/harness.toml";
        Path configPath = Paths.get(configFile);

        if (!Files.exists(configPath)) {
            System.out.println("  ✗ Configuration file not found: " + configFile);
            result.errors++;
        } else {
            try {
                String content = Files.readString(configPath);

                // Basic validation
                if (content.contains("[harness]") && content.contains("version")) {
                    System.out.println("  ✓ Configuration file is valid");
                } else {
                    System.out.println("  ⚠️  Configuration file may be incomplete");
                    result.warnings++;
                }

            } catch (IOException e) {
                System.out.println("  ✗ Error reading configuration: " + e.getMessage());
                result.errors++;
            }
        }

        return result;
    }

    private static class ValidationResult {
        int errors = 0;
        int warnings = 0;
    }
}
