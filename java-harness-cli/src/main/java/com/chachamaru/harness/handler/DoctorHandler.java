package com.chachamaru.harness.handler;

import com.chachamaru.harness.config.ConfigSync;
import com.chachamaru.harness.state.StatePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Doctor command handler.
 * Performs health check and diagnostics on the harness project.
 */
public class DoctorHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(DoctorHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            String projectRoot = ".";
            boolean migrationMode = false;

            // Parse arguments
            for (String arg : args) {
                if (arg.equals("--migration")) {
                    migrationMode = true;
                } else if (!arg.startsWith("--")) {
                    projectRoot = arg;
                }
            }

            logger.info("Running health check for: {}", projectRoot);

            System.out.println("🏥 Java Harness Health Check");
            System.out.println("========================");

            // Check configuration file
            checkConfigFile(projectRoot);

            // Check state directory
            checkStateDirectory(projectRoot);

            // Check skill files
            checkSkillFiles(projectRoot);

            // Check logs directory
            checkLogsDirectory(projectRoot);

            // Migration checks
            if (migrationMode) {
                checkMigration(projectRoot);
            }

            System.out.println("\n✓ Health check complete!");

        } catch (Exception e) {
            logger.error("Health check failed", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void checkConfigFile(String projectRoot) {
        System.out.println("\n📄 Configuration File:");
        String configPath = projectRoot + "/harness.toml";

        try {
            Map<String, Object> config = ConfigSync.loadConfigFromFile(configPath);
            if (config.isEmpty()) {
                System.out.println("  ⚠️  Missing or empty: " + configPath);
            } else {
                System.out.println("  ✓ Found: " + configPath);

                // Validate configuration
                if (ConfigSync.validateConfig(Files.readString(Paths.get(configPath)))) {
                    System.out.println("  ✓ Configuration is valid");
                } else {
                    System.out.println("  ⚠️  Configuration has validation issues");
                }
            }
        } catch (IOException e) {
            System.out.println("  ✗ Error reading config: " + e.getMessage());
        }
    }

    private void checkStateDirectory(String projectRoot) {
        System.out.println("\n💾 State Directory:");
        String stateDir = projectRoot + "/.claude/state";

        if (Files.exists(Paths.get(stateDir))) {
            System.out.println("  ✓ Found: " + stateDir);

            // Check state files
            String[] stateFiles = {"session.jsonl", "work.jsonl"};
            for (String file : stateFiles) {
                Path filePath = Paths.get(stateDir, file);
                if (Files.exists(filePath)) {
                    try {
                        long size = Files.size(filePath);
                        System.out.println("  ✓ " + file + " (" + size + " bytes)");
                    } catch (IOException e) {
                        System.out.println("  ⚠️  " + file + " (error reading size)");
                    }
                } else {
                    System.out.println("  ⚠️  Missing: " + file);
                }
            }
        } else {
            System.out.println("  ⚠️  Missing: " + stateDir);
            System.out.println("  Run: java-harness init");
        }
    }

    private void checkSkillFiles(String projectRoot) {
        System.out.println("\n🎯 Skill Files:");
        String skillsDir = projectRoot + "/.claude-plugin/skills";

        if (Files.exists(Paths.get(skillsDir))) {
            System.out.println("  ✓ Found: " + skillsDir);

            try {
                long skillCount = Files.list(Paths.get(skillsDir)).count();
                System.out.println("  ✓ Total skills: " + skillCount);
            } catch (IOException e) {
                System.out.println("  ⚠️  Error counting skills: " + e.getMessage());
            }
        } else {
            System.out.println("  ⚠️  Missing: " + skillsDir);
        }
    }

    private void checkLogsDirectory(String projectRoot) {
        System.out.println("\n📋 Logs Directory:");
        String logsDir = projectRoot + "/.claude/logs";

        if (Files.exists(Paths.get(logsDir))) {
            System.out.println("  ✓ Found: " + logsDir);
        } else {
            System.out.println("  ⚠️  Missing: " + logsDir);
        }
    }

    private void checkMigration(String projectRoot) {
        System.out.println("\n🔄 Migration Status:");

        // Check for legacy picocli files
        String[] legacyFiles = {
                "pom.xml",
                "src/main/java/com/chachamaru/harness/cli/command/HarnessCLI.java"
        };

        boolean hasLegacy = false;
        for (String file : legacyFiles) {
            Path filePath = Paths.get(projectRoot, file);
            if (Files.exists(filePath)) {
                hasLegacy = true;
                System.out.println("  ⚠️  Legacy file found: " + file);
            }
        }

        if (!hasLegacy) {
            System.out.println("  ✓ No legacy files detected");
        }

        // Check for new structure
        if (Files.exists(Paths.get(projectRoot, "java-harness-cli/src/main/java/com/chachamaru/harness/Main.java"))) {
            System.out.println("  ✓ New command structure detected");
        }
    }
}
