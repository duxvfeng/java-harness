package com.chachamaru.harness.handler;

import com.chachamaru.harness.config.ConfigSync;
import com.chachamaru.harness.state.StatePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Status command handler.
 * Shows current project status and statistics.
 */
public class StatusHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(StatusHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            String projectRoot = ".";
            boolean verbose = false;
            boolean jsonOutput = false;

            // Parse arguments
            for (String arg : args) {
                if (arg.equals("--verbose") || arg.equals("-v")) {
                    verbose = true;
                } else if (arg.equals("--json")) {
                    jsonOutput = true;
                } else if (!arg.startsWith("--")) {
                    projectRoot = arg;
                }
            }

            if (jsonOutput) {
                outputJsonStatus(projectRoot, verbose);
            } else {
                outputTextStatus(projectRoot, verbose);
            }

        } catch (Exception e) {
            logger.error("Status command failed", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void outputTextStatus(String projectRoot, boolean verbose) {
        System.out.println("📊 Java Harness Status");
        System.out.println("======================");

        // Configuration status
        showConfigStatus(projectRoot);

        // State status
        showStateStatus(projectRoot, verbose);

        // Skills status
        showSkillsStatus(projectRoot);

        // Session info
        if (verbose) {
            showSessionInfo(projectRoot);
        }
    }

    private void showConfigStatus(String projectRoot) {
        System.out.println("\n⚙️  Configuration:");
        String configPath = projectRoot + "/harness.toml";

        try {
            Map<String, Object> config = ConfigSync.loadConfigFromFile(configPath);
            if (!config.isEmpty()) {
                System.out.println("  ✓ Config loaded: " + configPath);

                Map<String, Object> harness = (Map<String, Object>) config.get("harness");
                if (harness != null) {
                    System.out.println("    Version: " + harness.get("version"));
                    System.out.println("    Backend: " + harness.get("backend"));
                }
            } else {
                System.out.println("  ⚠️  No configuration found");
            }
        } catch (IOException e) {
            System.out.println("  ✗ Error: " + e.getMessage());
        }
    }

    private void showStateStatus(String projectRoot, boolean verbose) {
        System.out.println("\n💾 State:");
        StatePersistence persistence = new StatePersistence(projectRoot);

        try {
            // Session state
            long sessionCount = persistence.getStateCount("session");
            System.out.println("  Sessions: " + sessionCount);

            // Work state
            long workCount = persistence.getStateCount("work");
            System.out.println("  Work items: " + workCount);

            if (verbose) {
                System.out.println("  Session file size: " + persistence.getStateFileSize("session") + " bytes");
                System.out.println("  Work file size: " + persistence.getStateFileSize("work") + " bytes");
            }
        } catch (IOException e) {
            System.out.println("  ⚠️  Error reading state: " + e.getMessage());
        }
    }

    private void showSkillsStatus(String projectRoot) {
        System.out.println("\n🎯 Skills:");
        String skillsDir = projectRoot + "/.claude-plugin/skills";

        try {
            if (Files.exists(Paths.get(skillsDir))) {
                long skillCount = Files.list(Paths.get(skillsDir)).count();
                System.out.println("  Total skills: " + skillCount);
            } else {
                System.out.println("  ⚠️  No skills directory");
            }
        } catch (IOException e) {
            System.out.println("  ✗ Error: " + e.getMessage());
        }
    }

    private void showSessionInfo(String projectRoot) {
        System.out.println("\n📝 Latest Session:");
        StatePersistence persistence = new StatePersistence(projectRoot);

        try {
            var latestSession = persistence.loadLatestSessionState();
            if (latestSession != null) {
                System.out.println("  Session ID: " + latestSession.getSessionId());
                System.out.println("  Active: " + latestSession.isActive());
                System.out.println("  Duration: " + latestSession.getDuration() + " seconds");

                if (latestSession.getAttributes() != null && !latestSession.getAttributes().isEmpty()) {
                    System.out.println("  Attributes:");
                    latestSession.getAttributes().forEach((key, value) ->
                            System.out.println("    " + key + ": " + value)
                    );
                }
            } else {
                System.out.println("  No session data");
            }
        } catch (IOException e) {
            System.out.println("  ✗ Error: " + e.getMessage());
        }
    }

    private void outputJsonStatus(String projectRoot, boolean verbose) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        // Add configuration info
        json.append("  \"configuration\": {\n");
        try {
            Map<String, Object> config = ConfigSync.loadConfigFromFile(projectRoot + "/harness.toml");
            if (!config.isEmpty()) {
                Map<String, Object> harness = (Map<String, Object>) config.get("harness");
                if (harness != null) {
                    json.append("    \"version\": \"").append(harness.get("version")).append("\",\n");
                    json.append("    \"backend\": \"").append(harness.get("backend")).append("\"\n");
                }
            }
        } catch (Exception e) {
            json.append("    \"error\": \"").append(e.getMessage()).append("\"\n");
        }
        json.append("  },\n");

        // Add state info
        json.append("  \"state\": {\n");
        StatePersistence persistence = new StatePersistence(projectRoot);
        try {
            json.append("    \"sessions\": ").append(persistence.getStateCount("session")).append(",\n");
            json.append("    \"work_items\": ").append(persistence.getStateCount("work")).append("\n");
        } catch (Exception e) {
            json.append("    \"error\": \"").append(e.getMessage()).append("\"\n");
        }
        json.append("  }\n");

        json.append("}\n");
        System.out.print(json.toString());
    }
}
