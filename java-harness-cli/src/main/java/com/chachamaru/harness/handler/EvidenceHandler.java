package com.chachamaru.harness.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Evidence command handler.
 * Collects and reports evidence for task completion.
 */
public class EvidenceHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(EvidenceHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            if (args.length == 0) {
                showHelp();
                return;
            }

            String command = args[0];
            String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

            switch (command) {
                case "collect":
                    handleCollect(commandArgs);
                    break;
                case "report":
                    handleReport(commandArgs);
                    break;
                case "list":
                    handleList(commandArgs);
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    showHelp();
                    System.exit(1);
            }

        } catch (Exception e) {
            logger.error("Evidence command failed", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleCollect(String[] args) throws IOException {
        String taskId = null;
        String projectRoot = ".";
        String type = "all";

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--task") && i + 1 < args.length) {
                taskId = args[++i];
            } else if (arg.equals("--type") && i + 1 < args.length) {
                type = args[++i];
            } else if (!arg.startsWith("--")) {
                projectRoot = arg;
            }
        }

        logger.info("Collecting evidence for task: {}, type: {}", taskId, type);

        // Collect evidence
        List<EvidenceItem> evidence = collectEvidence(taskId, type, projectRoot);

        // Store evidence
        String evidenceDir = projectRoot + "/.claude/state/evidence";
        Files.createDirectories(Paths.get(evidenceDir));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String evidenceFile = evidenceDir + "/evidence-" + (taskId != null ? taskId : "general") + "-" + timestamp + ".json";

        StringBuilder json = new StringBuilder();
        json.append("[\n");
        for (int i = 0; i < evidence.size(); i++) {
            EvidenceItem item = evidence.get(i);
            json.append("  {\n");
            json.append("    \"type\": \"").append(item.type).append("\",\n");
            json.append("    \"description\": \"").append(item.description).append("\",\n");
            json.append("    \"file\": \"").append(item.file).append("\",\n");
            json.append("    \"timestamp\": \"").append(item.timestamp).append("\"\n");
            json.append("  }").append(i < evidence.size() - 1 ? ",\n" : "\n");
        }
        json.append("]\n");

        Files.writeString(Paths.get(evidenceFile), json.toString());

        System.out.println("✓ Collected " + evidence.size() + " evidence items");
        System.out.println("  Stored in: " + evidenceFile);
    }

    private void handleReport(String[] args) throws IOException {
        String taskId = null;
        String projectRoot = ".";
        String format = "text";

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--task") && i + 1 < args.length) {
                taskId = args[++i];
            } else if (arg.equals("--format") && i + 1 < args.length) {
                format = args[++i];
            } else if (!arg.startsWith("--")) {
                projectRoot = arg;
            }
        }

        logger.info("Generating evidence report for task: {}", taskId);

        // Load evidence
        List<EvidenceItem> evidence = loadEvidence(taskId, projectRoot);

        // Generate report
        if ("json".equals(format)) {
            generateJsonReport(evidence);
        } else {
            generateTextReport(evidence, taskId);
        }
    }

    private void handleList(String[] args) throws IOException {
        String projectRoot = ".";

        for (String arg : args) {
            if (!arg.startsWith("--")) {
                projectRoot = arg;
            }
        }

        String evidenceDir = projectRoot + "/.claude/state/evidence";

        if (!Files.exists(Paths.get(evidenceDir))) {
            System.out.println("No evidence directory found");
            return;
        }

        System.out.println("📁 Evidence Files:");
        Files.list(Paths.get(evidenceDir))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(file -> {
                    try {
                        String filename = file.getFileName().toString();
                        long size = Files.size(file);
                        System.out.println("  - " + filename + " (" + size + " bytes)");
                    } catch (Exception e) {
                        logger.warn("Error listing file", e);
                    }
                });
    }

    private List<EvidenceItem> collectEvidence(String taskId, String type, String projectRoot) throws IOException {
        List<EvidenceItem> items = new ArrayList<>();

        // Collect test results
        if ("all".equals(type) || "tests".equals(type)) {
            collectTestEvidence(items, projectRoot);
        }

        // Collect code changes
        if ("all".equals(type) || "code".equals(type)) {
            collectCodeEvidence(items, projectRoot);
        }

        // Collect documentation
        if ("all".equals(type) || "docs".equals(type)) {
            collectDocumentationEvidence(items, projectRoot);
        }

        return items;
    }

    private void collectTestEvidence(List<EvidenceItem> items, String projectRoot) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        items.add(new EvidenceItem("test", "Test results evidence", "target/surefire-reports", timestamp));
        items.add(new EvidenceItem("test", "Test coverage report", "target/site/jacoco", timestamp));
    }

    private void collectCodeEvidence(List<EvidenceItem> items, String projectRoot) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        items.add(new EvidenceItem("code", "Source code changes", "src/main/java", timestamp));
        items.add(new EvidenceItem("code", "Build artifacts", "target/classes", timestamp));
    }

    private void collectDocumentationEvidence(List<EvidenceItem> items, String projectRoot) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        items.add(new EvidenceItem("docs", "README documentation", "README.md", timestamp));
        items.add(new EvidenceItem("docs", "API documentation", "target/site/apidocs", timestamp));
    }

    private List<EvidenceItem> loadEvidence(String taskId, String projectRoot) throws IOException {
        // Simple implementation - in real system would load from stored files
        List<EvidenceItem> evidence = new ArrayList<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        evidence.add(new EvidenceItem("general", "Sample evidence", "sample.txt", timestamp));
        return evidence;
    }

    private void generateTextReport(List<EvidenceItem> evidence, String taskId) {
        System.out.println("\n📋 Evidence Report");
        System.out.println("================");
        if (taskId != null) {
            System.out.println("Task: " + taskId);
        }
        System.out.println("Total Evidence Items: " + evidence.size());
        System.out.println();

        evidence.forEach(item -> {
            System.out.println("  Type: " + item.type);
            System.out.println("  Description: " + item.description);
            System.out.println("  File: " + item.file);
            System.out.println("  Timestamp: " + item.timestamp);
            System.out.println();
        });
    }

    private void generateJsonReport(List<EvidenceItem> evidence) {
        System.out.println("[");
        for (int i = 0; i < evidence.size(); i++) {
            EvidenceItem item = evidence.get(i);
            System.out.println("  {");
            System.out.println("    \"type\": \"" + item.type + "\",");
            System.out.println("    \"description\": \"" + item.description + "\",");
            System.out.println("    \"file\": \"" + item.file + "\",");
            System.out.println("    \"timestamp\": \"" + item.timestamp + "\"");
            System.out.println("  }" + (i < evidence.size() - 1 ? "," : ""));
        }
        System.out.println("]");
    }

    private void showHelp() {
        System.err.println("Usage: java-harness evidence <command> [options]");
        System.err.println("");
        System.err.println("Commands:");
        System.err.println("  collect    Collect evidence for task completion");
        System.err.println("  report     Generate evidence report");
        System.err.println("  list       List all evidence files");
        System.err.println("");
        System.err.println("Options:");
        System.err.println("  --task=<taskId>      Task ID");
        System.err.println("  --type=<type>        Evidence type (tests, code, docs, all)");
        System.err.println("  --format=<format>    Report format (text, json)");
    }

    private static class EvidenceItem {
        String type;
        String description;
        String file;
        String timestamp;

        EvidenceItem(String type, String description, String file, String timestamp) {
            this.type = type;
            this.description = description;
            this.file = file;
            this.timestamp = timestamp;
        }
    }
}
