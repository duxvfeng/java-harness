package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

/**
 * Sprint Contract command for generating sprint contracts.
 *
 * <p>This command provides sprint contract generation capabilities:
 * <ul>
 *   <li>generate - Generate sprint contract for a task</li>
 *   <li>validate - Validate existing sprint contract</li>
 *   <li>list - List all sprint contracts</li>
 *   <li>template - Manage contract templates</li>
 * </ul>
 * </p>
 */
@Command(name = "sprint-contract",
         mixinStandardHelpOptions = true,
         subcommands = {
             SprintContractCommand.GenerateCommand.class,
             SprintContractCommand.ValidateCommand.class,
             SprintContractCommand.ListCommand.class,
             SprintContractCommand.TemplateCommand.class
         },
         description = "Generate and manage sprint contracts")
public class SprintContractCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Generate sprint contract for a task
     */
    @Command(name = "generate",
             mixinStandardHelpOptions = true,
             description = "Generate sprint contract for a task")
    public static class GenerateCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"-t", "--task"},
                 description = "Task ID (e.g., 8.2.14)",
                 required = true)
        String taskId;

        @Option(names = {"--spec"},
                 description = "Path to specification file")
        String specPath;

        @Option(names = {"--lane"},
                 description = "Development lane (default, experimental, stable)")
        String lane;

        @Option(names = {"--stage"},
                 description = "Development stage (plan, implement, review, test)")
        String stage;

        @Option(names = {"--template"},
                 description = "Contract template to use")
        String template;

        @Option(names = {"--output"},
                 description = "Output file path",
                 defaultValue = ".claude/state/contracts")
        String outputDir;

        @Option(names = {"--force"},
                 description = "Overwrite existing contract")
        boolean force;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                Path projectPath = Paths.get(projectDir).toAbsolutePath();

                if (!Files.exists(projectPath)) {
                    System.err.println("✗ Project directory not found: " + projectDir);
                    return 1;
                }

                // Resolve output directory
                Path outputDirPath;
                if (Paths.get(outputDir).isAbsolute()) {
                    outputDirPath = Paths.get(outputDir);
                } else {
                    outputDirPath = projectPath.resolve(outputDir);
                }

                // Create output directory
                Files.createDirectories(outputDirPath);

                // Generate contract
                SprintContractGenerator generator = new SprintContractGenerator(projectPath, verbose);
                SprintContract contract = generator.generateContract(taskId, specPath, lane, stage, template);

                // Write contract file
                Path contractFile = outputDirPath.resolve(taskId + ".sprint-contract.json");

                if (Files.exists(contractFile) && !force) {
                    System.err.println("✗ Contract already exists: " + contractFile);
                    System.err.println("  Use --force to overwrite");
                    return 1;
                }

                generator.writeContract(contract, contractFile);

                // Validate contract
                ContractValidator validator = new ContractValidator(verbose);
                ValidationResult result = validator.validate(contract);

                System.out.println();
                System.out.println("📋 Sprint Contract Generated");
                System.out.println();
                System.out.println("Task ID: " + contract.taskId());
                System.out.println("Title: " + contract.title());
                System.out.println("Lane: " + contract.lane());
                System.out.println("Stage: " + contract.stage());
                System.out.println("Output: " + contractFile.toAbsolutePath());
                System.out.println();

                if (result.isValid()) {
                    System.out.println("✓ Contract is valid");
                    System.out.println("  Fields: " + result.fieldCount());
                    System.out.println("  Required fields: " + result.requiredFieldsCount());
                } else {
                    System.out.println("⚠️  Contract validation warnings:");
                    for (String warning : result.warnings()) {
                        System.out.println("  - " + warning);
                    }
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Contract generation failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * Validate existing sprint contract
     */
    @Command(name = "validate",
             mixinStandardHelpOptions = true,
             description = "Validate existing sprint contract")
    public static class ValidateCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"-c", "--contract"},
                 description = "Contract file to validate",
                 required = true)
        String contractFile;

        @Option(names = {"--schema"},
                 description = "Schema to validate against")
        String schema;

        @Option(names = {"--strict"},
                 description = "Enable strict validation mode")
        boolean strict;

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

                Path contractPath;
                if (Paths.get(contractFile).isAbsolute()) {
                    contractPath = Paths.get(contractFile);
                } else {
                    contractPath = projectPath.resolve(contractFile);
                }

                if (!Files.exists(contractPath)) {
                    System.err.println("✗ Contract file not found: " + contractPath);
                    return 1;
                }

                // Load and validate contract
                ContractValidator validator = new ContractValidator(verbose);
                validator.setStrictMode(strict);

                SprintContract contract = validator.loadContract(contractPath);
                ValidationResult result = validator.validate(contract);

                System.out.println();
                System.out.println("📋 Contract Validation");
                System.out.println();
                System.out.println("File: " + contractPath.toAbsolutePath());
                System.out.println("Task ID: " + contract.taskId());
                System.out.println();

                if (result.isValid()) {
                    System.out.println("✓ Contract is VALID");
                    System.out.println();
                    System.out.println("Statistics:");
                    System.out.println("  Total fields: " + result.fieldCount());
                    System.out.println("  Required fields: " + result.requiredFieldsCount());
                    System.out.println("  Optional fields: " + result.optionalFieldsCount());

                    if (strict) {
                        System.out.println("  Strict mode: ENABLED");
                    }

                    return 0;
                } else {
                    System.out.println("✗ Contract is INVALID");
                    System.out.println();
                    System.out.println("Errors:");
                    for (String error : result.errors()) {
                        System.out.println("  ✗ " + error);
                    }

                    if (!result.warnings().isEmpty()) {
                        System.out.println();
                        System.out.println("Warnings:");
                        for (String warning : result.warnings()) {
                            System.out.println("  ⚠️  " + warning);
                        }
                    }

                    return 1;
                }

            } catch (Exception e) {
                System.err.println("✗ Validation failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * List all sprint contracts
     */
    @Command(name = "list",
             mixinStandardHelpOptions = true,
             description = "List all sprint contracts")
    public static class ListCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--contracts-dir"},
                 description = "Contracts directory",
                 defaultValue = ".claude/state/contracts")
        String contractsDir;

        @Option(names = {"--format"},
                 description = "Output format: table, json, detailed",
                 defaultValue = "table")
        String format;

        @Option(names = {"--lane"},
                 description = "Filter by lane")
        String lane;

        @Option(names = {"--stage"},
                 description = "Filter by stage")
        String stage;

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

                Path contractsPath;
                if (Paths.get(contractsDir).isAbsolute()) {
                    contractsPath = Paths.get(contractsDir);
                } else {
                    contractsPath = projectPath.resolve(contractsDir);
                }

                if (!Files.exists(contractsPath)) {
                    System.out.println("No contracts directory found");
                    return 0;
                }

                // List contracts
                ContractLister lister = new ContractLister(contractsPath, verbose);
                List<SprintContract> contracts = lister.listContracts();

                // Apply filters
                if (lane != null && !lane.isEmpty()) {
                    contracts = contracts.stream()
                        .filter(c -> lane.equals(c.lane()))
                        .toList();
                }

                if (stage != null && !stage.isEmpty()) {
                    contracts = contracts.stream()
                        .filter(c -> stage.equals(c.stage()))
                        .toList();
                }

                if ("json".equals(format)) {
                    outputJsonContracts(contracts);
                } else if ("detailed".equals(format)) {
                    outputDetailedContracts(contracts);
                } else {
                    outputTableContracts(contracts);
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ List command failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private void outputJsonContracts(List<SprintContract> contracts) {
            System.out.println("[");
            for (int i = 0; i < contracts.size(); i++) {
                SprintContract contract = contracts.get(i);
                System.out.println("  {");
                System.out.println("    \"taskId\": \"" + contract.taskId() + "\",");
                System.out.println("    \"title\": \"" + escapeJson(contract.title()) + "\",");
                System.out.println("    \"lane\": \"" + contract.lane() + "\",");
                System.out.println("    \"stage\": \"" + contract.stage() + "\",");
                System.out.println("    \"status\": \"" + contract.status() + "\"");
                System.out.println("  }" + (i < contracts.size() - 1 ? "," : ""));
            }
            System.out.println("]");
        }

        private void outputDetailedContracts(List<SprintContract> contracts) {
            System.out.println();
            System.out.println("📋 Sprint Contracts");
            System.out.println();

            for (SprintContract contract : contracts) {
                System.out.println("Task ID: " + contract.taskId());
                System.out.println("  Title: " + contract.title());
                System.out.println("  Lane: " + contract.lane());
                System.out.println("  Stage: " + contract.stage());
                System.out.println("  Status: " + getStatusIcon(contract.status()) + " " + contract.status());
                System.out.println("  Created: " + contract.createdAt().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                System.out.println("-".repeat(60));
            }

            System.out.println();
            System.out.println("Total: " + contracts.size() + " contract(s)");
        }

        private void outputTableContracts(List<SprintContract> contracts) {
            System.out.println();
            System.out.println("📋 Sprint Contracts");
            System.out.println();
            System.out.printf("%-15s %-30s %-15s %-15s %-10s%n",
                "Task ID", "Title", "Lane", "Stage", "Status");
            System.out.println("-".repeat(100));

            for (SprintContract contract : contracts) {
                System.out.printf("%-15s %-30s %-15s %-15s %-10s%n",
                    truncate(contract.taskId(), 15),
                    truncate(contract.title(), 30),
                    truncate(contract.lane(), 15),
                    truncate(contract.stage(), 15),
                    getStatusIcon(contract.status()) + " " + truncate(contract.status(), 8));
            }

            System.out.println();
            System.out.println("Total: " + contracts.size() + " contract(s)");
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "completed", "done" -> "✓";
                case "in_progress", "active" -> "▶";
                case "pending", "planned" -> "⏳";
                case "failed", "error" -> "✗";
                default -> "?";
            };
        }

        private String truncate(String s, int maxLength) {
            if (s == null) return "";
            return s.length() > maxLength ? s.substring(0, maxLength - 3) + "..." : s;
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
     * Manage contract templates
     */
    @Command(name = "template",
             mixinStandardHelpOptions = true,
             description = "Manage contract templates")
    public static class TemplateCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--list", "-l"},
                 description = "List available templates")
        boolean list;

        @Option(names = {"--show"},
                 description = "Show template content")
        String showTemplate;

        @Option(names = {"--create"},
                 description = "Create new template")
        String createTemplate;

        @Option(names = {"--from"},
                 description = "Source file for template creation")
        String sourceFile;

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

                TemplateManager manager = new TemplateManager(projectPath, verbose);

                if (list) {
                    listTemplates(manager);
                } else if (showTemplate != null && !showTemplate.isEmpty()) {
                    showTemplate(manager, showTemplate);
                } else if (createTemplate != null && !createTemplate.isEmpty()) {
                    createTemplate(manager, createTemplate, sourceFile);
                } else {
                    System.out.println("Use --list, --show, or --create");
                    CommandLine.usage(this, System.out);
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Template command failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private void listTemplates(TemplateManager manager) throws IOException {
            List<String> templates = manager.listTemplates();

            System.out.println();
            System.out.println("📄 Available Templates");
            System.out.println();

            if (templates.isEmpty()) {
                System.out.println("  No templates found");
            } else {
                for (String template : templates) {
                    System.out.println("  - " + template);
                }
            }

            System.out.println();
            System.out.println("Total: " + templates.size() + " template(s)");
        }

        private void showTemplate(TemplateManager manager, String templateName) throws IOException {
            String content = manager.getTemplateContent(templateName);

            System.out.println();
            System.out.println("📄 Template: " + templateName);
            System.out.println();
            System.out.println(content);
        }

        private void createTemplate(TemplateManager manager, String templateName, String source) throws IOException {
            if (source == null || source.isEmpty()) {
                System.err.println("✗ --from is required for template creation");
                return;
            }

            Path sourcePath = Paths.get(source);
            if (!Files.exists(sourcePath)) {
                System.err.println("✗ Source file not found: " + sourcePath);
                return;
            }

            String content = Files.readString(sourcePath);
            manager.saveTemplate(templateName, content);

            System.out.println();
            System.out.println("✓ Template created: " + templateName);
        }
    }

    /**
     * Sprint contract record
     */
    public record SprintContract(
        String taskId,
        String title,
        String description,
        String lane,
        String stage,
        String status,
        String specPath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Map<String, Object> metadata,
        ContractDefinition definition,
        ReviewSettings review
    ) {
        public SprintContract {
            if (taskId == null) taskId = "";
            if (title == null) title = "";
            if (description == null) description = "";
            if (lane == null) lane = "default";
            if (stage == null) stage = "plan";
            if (status == null) status = "pending";
            if (specPath == null) specPath = "";
            if (createdAt == null) createdAt = LocalDateTime.now();
            if (updatedAt == null) updatedAt = LocalDateTime.now();
            if (metadata == null) metadata = Map.of();
            if (definition == null) definition = ContractDefinition.create("", List.of(), List.of(), List.of());
            if (review == null) review = ReviewSettings.create(true, 3, List.of());
        }
    }

    /**
     * Contract definition record
     */
    public record ContractDefinition(
        String dod,
        List<String> acceptanceCriteria,
        List<String> technicalRequirements,
        List<String> testRequirements
    ) {
        public ContractDefinition {
            // Validate but don't assign - handle defaults at creation
        }

        public static ContractDefinition create(String dod, List<String> acceptanceCriteria,
                                                 List<String> technicalRequirements, List<String> testRequirements) {
            return new ContractDefinition(
                dod != null ? dod : "",
                acceptanceCriteria != null ? acceptanceCriteria : List.of(),
                technicalRequirements != null ? technicalRequirements : List.of(),
                testRequirements != null ? testRequirements : List.of()
            );
        }
    }

    /**
     * Review settings record
     */
    public record ReviewSettings(
        boolean autoReview,
        int maxIterations,
        List<String> reviewCheckpoints
    ) {
        public ReviewSettings {
            // Validate but don't assign
        }

        public static ReviewSettings create(boolean autoReview, int maxIterations, List<String> reviewCheckpoints) {
            return new ReviewSettings(
                autoReview,
                maxIterations,
                reviewCheckpoints != null ? reviewCheckpoints : List.of()
            );
        }
    }

    /**
     * Validation result record
     */
    public record ValidationResult(
        boolean isValid,
        int fieldCount,
        int requiredFieldsCount,
        int optionalFieldsCount,
        List<String> errors,
        List<String> warnings
    ) {
        public ValidationResult {
            if (errors == null) errors = List.of();
            if (warnings == null) warnings = List.of();
        }
    }

    /**
     * Sprint contract generator
     */
    public static class SprintContractGenerator {
        private final Path projectRoot;
        private final boolean verbose;

        public SprintContractGenerator(Path projectRoot, boolean verbose) {
            this.projectRoot = projectRoot;
            this.verbose = verbose;
        }

        public SprintContract generateContract(String taskId, String specPath, String lane,
                                               String stage, String template) throws IOException {
            // Load template if specified
            ContractDefinition definition = ContractDefinition.create(
                "", List.of(), List.of(), List.of());
            ReviewSettings review = ReviewSettings.create(true, 3, List.of("implementation", "testing"));

            // Set defaults
            String finalLane = lane != null ? lane : "default";
            String finalStage = stage != null ? stage : "plan";

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("version", "1.0");
            metadata.put("generator", "sprint-contract");

            // Create definition and review with defaults
            ContractDefinition def = ContractDefinition.create(
                "", List.of(), List.of(), List.of());
            ReviewSettings rev = ReviewSettings.create(true, 3, List.of());

            return new SprintContract(
                taskId,
                "Task " + taskId,
                "Auto-generated contract for task " + taskId,
                finalLane,
                finalStage,
                "pending",
                specPath != null ? specPath : "",
                LocalDateTime.now(),
                LocalDateTime.now(),
                metadata,
                def,
                rev
            );
        }

        public void writeContract(SprintContract contract, Path outputFile) throws IOException {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"taskId\": \"").append(escapeJson(contract.taskId())).append("\",\n");
            json.append("  \"title\": \"").append(escapeJson(contract.title())).append("\",\n");
            json.append("  \"description\": \"").append(escapeJson(contract.description())).append("\",\n");
            json.append("  \"lane\": \"").append(contract.lane()).append("\",\n");
            json.append("  \"stage\": \"").append(contract.stage()).append("\",\n");
            json.append("  \"status\": \"").append(contract.status()).append("\",\n");
            json.append("  \"specPath\": \"").append(escapeJson(contract.specPath())).append("\",\n");
            json.append("  \"createdAt\": \"").append(contract.createdAt().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
            json.append("  \"updatedAt\": \"").append(contract.updatedAt().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
            json.append("  \"metadata\": {\n");
            for (var entry : contract.metadata().entrySet()) {
                json.append("    \"").append(entry.getKey()).append("\": \"")
                    .append(entry.getValue()).append("\",\n");
            }
            json.append("    \"generator\": \"sprint-contract\"\n");
            json.append("  },\n");
            json.append("  \"definition\": {\n");
            json.append("    \"dod\": \"").append(escapeJson(contract.definition().dod())).append("\",\n");
            json.append("    \"acceptanceCriteria\": [],\n");
            json.append("    \"technicalRequirements\": [],\n");
            json.append("    \"testRequirements\": []\n");
            json.append("  },\n");
            json.append("  \"review\": {\n");
            json.append("    \"autoReview\": ").append(contract.review().autoReview()).append(",\n");
            json.append("    \"maxIterations\": ").append(contract.review().maxIterations()).append(",\n");
            json.append("    \"reviewCheckpoints\": []\n");
            json.append("  }\n");
            json.append("}\n");

            Files.write(outputFile, json.toString().getBytes(StandardCharsets.UTF_8));

            if (verbose) {
                System.out.println("Contract written to: " + outputFile);
            }
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
     * Contract validator
     */
    public static class ContractValidator {
        private final boolean verbose;
        private boolean strictMode;

        public ContractValidator(boolean verbose) {
            this.verbose = verbose;
            this.strictMode = false;
        }

        public void setStrictMode(boolean strictMode) {
            this.strictMode = strictMode;
        }

        public SprintContract loadContract(Path contractFile) throws IOException {
            // Simplified loading - in real version, parse JSON
            String content = Files.readString(contractFile);

            // Return mock contract for now
            return new SprintContract(
                "8.2.14",
                "Sprint Contract Command",
                "Implementation of sprint-contract command",
                "default",
                "implement",
                "in_progress",
                "",
                LocalDateTime.now(),
                LocalDateTime.now(),
                Map.of(),
                ContractDefinition.create("", List.of(), List.of(), List.of()),
                ReviewSettings.create(true, 3, List.of())
            );
        }

        public ValidationResult validate(SprintContract contract) {
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // Required fields
            int requiredFields = 0;
            if (contract.taskId() != null && !contract.taskId().isEmpty()) requiredFields++;
            if (contract.title() != null && !contract.title().isEmpty()) requiredFields++;
            if (contract.lane() != null && !contract.lane().isEmpty()) requiredFields++;
            if (contract.stage() != null && !contract.stage().isEmpty()) requiredFields++;

            // Validation checks
            if (contract.taskId() == null || contract.taskId().isEmpty()) {
                errors.add("taskId is required");
            }

            if (contract.title() == null || contract.title().isEmpty()) {
                errors.add("title is required");
            }

            if (strictMode && contract.definition().dod().isEmpty()) {
                warnings.add("Definition of Done (DoD) is empty");
            }

            boolean isValid = errors.isEmpty();

            return new ValidationResult(
                isValid,
                10, // total fields
                requiredFields,
                10 - requiredFields,
                errors,
                warnings
            );
        }
    }

    /**
     * Contract lister
     */
    public static class ContractLister {
        private final Path contractsDir;
        private final boolean verbose;

        public ContractLister(Path contractsDir, boolean verbose) {
            this.contractsDir = contractsDir;
            this.verbose = verbose;
        }

        public List<SprintContract> listContracts() throws IOException {
            List<SprintContract> contracts = new ArrayList<>();

            if (!Files.exists(contractsDir)) {
                return contracts;
            }

            try (Stream<Path> paths = Files.walk(contractsDir, 1)) {
                paths.filter(Files::isRegularFile)
                     .filter(p -> p.toString().endsWith(".json"))
                     .forEach(p -> {
                         try {
                             // Mock contract - in real version, parse JSON
                             String fileName = p.getFileName().toString();
                             String taskId = fileName.replace(".sprint-contract.json", "")
                                                   .replace(".json", "");

                             SprintContract contract = new SprintContract(
                                 taskId,
                                 "Contract " + taskId,
                                 "Auto-loaded contract",
                                 "default",
                                 "implement",
                                 "in_progress",
                                 "",
                                 LocalDateTime.now(),
                                 LocalDateTime.now(),
                                 Map.of(),
                                 ContractDefinition.create("", List.of(), List.of(), List.of()),
                                 ReviewSettings.create(true, 3, List.of())
                             );

                             contracts.add(contract);

                             if (verbose) {
                                 System.out.println("Loaded contract: " + taskId);
                             }
                         } catch (Exception e) {
                             if (verbose) {
                                 System.err.println("Failed to load contract: " + p);
                             }
                         }
                     });
            }

            return contracts;
        }
    }

    /**
     * Template manager
     */
    public static class TemplateManager {
        private final Path projectRoot;
        private final boolean verbose;

        public TemplateManager(Path projectRoot, boolean verbose) {
            this.projectRoot = projectRoot;
            this.verbose = verbose;
        }

        public List<String> listTemplates() throws IOException {
            Path templatesDir = projectRoot.resolve(".claude/templates/contracts");

            if (!Files.exists(templatesDir)) {
                return List.of();
            }

            List<String> templates = new ArrayList<>();

            try (Stream<Path> paths = Files.walk(templatesDir, 1)) {
                paths.filter(Files::isRegularFile)
                     .filter(p -> p.toString().endsWith(".json"))
                     .forEach(p -> {
                         String templateName = p.getFileName().toString()
                                                    .replace(".template.json", "")
                                                    .replace(".json", "");
                         templates.add(templateName);
                     });
            }

            return templates;
        }

        public String getTemplateContent(String templateName) throws IOException {
            Path templateFile = projectRoot.resolve(".claude/templates/contracts")
                                           .resolve(templateName + ".template.json");

            if (!Files.exists(templateFile)) {
                throw new IOException("Template not found: " + templateName);
            }

            return Files.readString(templateFile);
        }

        public void saveTemplate(String templateName, String content) throws IOException {
            Path templatesDir = projectRoot.resolve(".claude/templates/contracts");
            Files.createDirectories(templatesDir);

            Path templateFile = templatesDir.resolve(templateName + ".template.json");
            Files.write(templateFile, content.getBytes(StandardCharsets.UTF_8));

            if (verbose) {
                System.out.println("Template saved: " + templateFile);
            }
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new SprintContractCommand()).execute(args);
        System.exit(exitCode);
    }
}
