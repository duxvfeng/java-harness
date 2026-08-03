package com.chachamaru.harness.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Sprint Contract command handler.
 * Manages sprint contracts for task tracking and validation.
 */
public class SprintContractHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(SprintContractHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

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
                case "generate":
                    handleGenerate(commandArgs);
                    break;
                case "validate":
                    handleValidate(commandArgs);
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
            logger.error("Sprint contract command failed", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void handleGenerate(String[] args) throws IOException {
        String taskId = null;
        String projectRoot = ".";
        String lane = "default";
        String stage = "plan";
        String outputDir = null;
        boolean force = false;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--task") && i + 1 < args.length) {
                taskId = args[++i];
            } else if (arg.equals("--directory") && i + 1 < args.length) {
                projectRoot = args[++i];
            } else if (arg.equals("--lane") && i + 1 < args.length) {
                lane = args[++i];
            } else if (arg.equals("--stage") && i + 1 < args.length) {
                stage = args[++i];
            } else if (arg.equals("--output") && i + 1 < args.length) {
                outputDir = args[++i];
            } else if (arg.equals("--force")) {
                force = true;
            }
        }

        if (taskId == null) {
            System.err.println("Error: --task=<taskId> is required");
            System.exit(1);
        }

        logger.info("Generating sprint contract for task: {}", taskId);

        // Generate contract
        Map<String, Object> contract = generateContract(taskId, lane, stage, projectRoot);

        // Determine output path
        String defaultOutputDir = projectRoot + "/.claude/state/contracts";
        String outputPath = (outputDir != null ? outputDir : defaultOutputDir) + "/" + taskId + ".sprint-contract.json";

        // Check if file exists
        if (!force && Files.exists(Paths.get(outputPath))) {
            System.err.println("Contract already exists: " + outputPath);
            System.err.println("Use --force to overwrite");
            System.exit(1);
        }

        // Write contract
        Files.createDirectories(Paths.get(outputPath).getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(outputPath).toFile(), contract);

        System.out.println("✓ Sprint contract generated: " + outputPath);
    }

    private void handleValidate(String[] args) throws IOException {
        String contractFile = null;
        String projectRoot = ".";
        boolean strict = false;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--contract") && i + 1 < args.length) {
                contractFile = args[++i];
            } else if (arg.equals("--directory") && i + 1 < args.length) {
                projectRoot = args[++i];
            } else if (arg.equals("--strict")) {
                strict = true;
            }
        }

        if (contractFile == null) {
            System.err.println("Error: --contract=<contractFile> is required");
            System.exit(1);
        }

        logger.info("Validating sprint contract: {}", contractFile);

        // Validate contract
        ValidationResult result = validateContract(contractFile, strict);

        System.out.println("\n📋 Validation Results:");
        System.out.println("  Errors: " + result.errors);
        System.out.println("  Warnings: " + result.warnings);

        if (result.errors > 0) {
            System.out.println("\n✗ Validation failed");
            System.exit(1);
        } else if (result.warnings > 0) {
            System.out.println("\n⚠️  Validation passed with warnings");
        } else {
            System.out.println("\n✓ Validation passed successfully");
        }
    }

    private void handleList(String[] args) throws IOException {
        String projectRoot = ".";

        for (String arg : args) {
            if (!arg.startsWith("--")) {
                projectRoot = arg;
            }
        }

        String contractsDir = projectRoot + "/.claude/state/contracts";

        if (!Files.exists(Paths.get(contractsDir))) {
            System.out.println("No contracts directory found");
            return;
        }

        System.out.println("📁 Sprint Contracts:");
        Files.list(Paths.get(contractsDir))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(file -> {
                    try {
                        String filename = file.getFileName().toString();
                        System.out.println("  - " + filename);
                    } catch (Exception e) {
                        logger.warn("Error listing file", e);
                    }
                });
    }

    private Map<String, Object> generateContract(String taskId, String lane, String stage, String projectRoot) {
        Map<String, Object> contract = new HashMap<>();
        contract.put("task_id", taskId);
        contract.put("lane", lane);
        contract.put("stage", stage);
        contract.put("created", java.time.Instant.now().toString());
        contract.put("status", "pending");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("project_root", projectRoot);
        metadata.put("version", "1.0");
        contract.put("metadata", metadata);

        Map<String, Object> acceptance = new HashMap<>();
        acceptance.put("definition_of_done", new String[]{
                "Code implemented and tested",
                "Documentation updated",
                "Code review completed",
                "No critical issues"
        });
        acceptance.put("validation_criteria", new ArrayList<>());
        contract.put("acceptance", acceptance);

        return contract;
    }

    private ValidationResult validateContract(String contractFile, boolean strict) throws IOException {
        ValidationResult result = new ValidationResult();

        if (!Files.exists(Paths.get(contractFile))) {
            System.err.println("Contract file not found: " + contractFile);
            System.exit(1);
        }

        // Read and parse contract
        @SuppressWarnings("unchecked")
        Map<String, Object> contract = mapper.readValue(Files.readString(Paths.get(contractFile)), Map.class);

        // Required fields
        String[] requiredFields = {"task_id", "lane", "stage", "status"};
        for (String field : requiredFields) {
            if (!contract.containsKey(field)) {
                result.errors++;
                System.out.println("  ✗ Missing required field: " + field);
            } else {
                System.out.println("  ✓ Found field: " + field);
            }
        }

        // Optional fields (warnings if missing in strict mode)
        if (strict) {
            String[] optionalFields = {"metadata", "acceptance"};
            for (String field : optionalFields) {
                if (!contract.containsKey(field)) {
                    result.warnings++;
                    System.out.println("  ⚠️  Missing optional field: " + field);
                }
            }
        }

        return result;
    }

    private void showHelp() {
        System.err.println("Usage: java-harness sprint-contract <command> [options]");
        System.err.println("");
        System.err.println("Commands:");
        System.err.println("  generate    Generate a new sprint contract");
        System.err.println("  validate    Validate an existing contract");
        System.err.println("  list        List all contracts");
        System.err.println("");
        System.err.println("Options:");
        System.err.println("  --task=<taskId>      Task ID (required for generate)");
        System.err.println("  --contract=<file>   Contract file (required for validate)");
        System.err.println("  --directory=<dir>   Project directory");
        System.err.println("  --force             Overwrite existing contract");
        System.err.println("  --strict            Enable strict validation");
    }

    private static class ValidationResult {
        int errors = 0;
        int warnings = 0;
    }
}
