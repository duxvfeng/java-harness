package com.chachamaru.harness.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Plan management command for handling multiple project plans.
 *
 * <p>This command provides functionality to:
 * <ul>
 *   <li>add - Add a new plan to the registry</li>
 *   <li>list - List all available plans</li>
 *   <li>switch - Switch the active plan</li>
 * </ul>
 * </p>
 */
@Command(name = "plan",
         mixinStandardHelpOptions = true,
         subcommands = {
             PlanCommand.AddCommand.class,
             PlanCommand.ListCommand.class,
             PlanCommand.SwitchCommand.class
         },
         description = "Manage project plans (add/list/switch)")
public class PlanCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Add a new plan to the registry
     */
    @Command(name = "add",
             mixinStandardHelpOptions = true,
             description = "Add a new plan to the registry")
    public static class AddCommand implements Runnable {

        @Parameters(index = "0", description = "Unique plan identifier (e.g., 'phase-8-completion')")
        private String planId;

        @Parameters(index = "1", description = "Human-readable plan name")
        private String planName;

        @Option(names = {"-f", "--file"},
                 description = "Path to the plan file (default: Plans.md)",
                 defaultValue = "Plans.md")
        private String planFile;

        @Option(names = {"-d", "--description"},
                 description = "Optional description of the plan")
        private String description;

        @Option(names = {"--active"},
                 description = "Set as the active plan immediately")
        private boolean makeActive;

        @Override
        public void run() {
            try {
                ManifestManager manifest = new ManifestManager();
                manifest.addPlan(planId, planName, planFile, description);

                if (makeActive) {
                    manifest.setActivePlan(planId);
                    System.out.println("✓ Added and activated plan: " + planId + " (" + planName + ")");
                } else {
                    System.out.println("✓ Added plan: " + planId + " (" + planName + ")");
                }

                System.out.println("  File: " + planFile);
                if (description != null && !description.isEmpty()) {
                    System.out.println("  Description: " + description);
                }

            } catch (Exception e) {
                System.err.println("✗ Failed to add plan: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * List all available plans
     */
    @Command(name = "list",
             mixinStandardHelpOptions = true,
             description = "List all available plans")
    public static class ListCommand implements Runnable {

        @Option(names = {"-a", "--all"},
                 description = "Show archived plans as well")
        private boolean showArchived;

        @Option(names = {"-j", "--json"},
                 description = "Output in JSON format")
        private boolean jsonOutput;

        @Override
        public void run() {
            try {
                ManifestManager manifest = new ManifestManager();

                if (jsonOutput) {
                    // JSON output
                    ObjectMapper mapper = new ObjectMapper();
                    System.out.println(mapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(manifest.getManifest()));
                } else {
                    // Human-readable output
                    printManifest(manifest, showArchived);
                }

            } catch (Exception e) {
                System.err.println("✗ Failed to list plans: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        private void printManifest(ManifestManager manifest, boolean includeArchived) {
            JsonNode manifestJson = manifest.getManifest();

            String activePlan = manifestJson.has("active_plan") ?
                manifestJson.get("active_plan").asText() : "none";

            System.out.println("=== Project Plans ===");
            System.out.println("Active Plan: " + activePlan);
            System.out.println();

            JsonNode plans = manifestJson.get("plans");
            if (plans != null && plans.size() > 0) {
                System.out.println("Available Plans:");
                plans.fields().forEachRemaining(entry -> {
                    String id = entry.getKey();
                    JsonNode plan = entry.getValue();

                    String name = plan.has("name") ? plan.get("name").asText() : id;
                    String file = plan.has("file") ? plan.get("file").asText() : "unknown";
                    String status = plan.has("status") ? plan.get("status").asText() : "unknown";

                    String isActive = id.equals(activePlan) ? " [ACTIVE]" : "";
                    String statusIcon = "active".equals(status) ? "🟢" :
                                        "archived".equals(status) ? "📦" : "⚪";

                    System.out.println("  " + statusIcon + " " + id + isActive);
                    System.out.println("      Name: " + name);
                    System.out.println("      File: " + file);
                    System.out.println("      Status: " + status);
                    if (plan.has("completion_percentage")) {
                        int completion = plan.get("completion_percentage").asInt();
                        System.out.println("      Progress: " + completion + "%");
                    }
                    System.out.println();
                });
            } else {
                System.out.println("No plans available.");
                System.out.println();
            }

            if (includeArchived) {
                JsonNode archived = manifestJson.get("archived");
                if (archived != null && archived.size() > 0) {
                    System.out.println("Archived Plans:");
                    for (JsonNode archivedPlan : archived) {
                        String name = archivedPlan.has("name") ?
                            archivedPlan.get("name").asText() : "unknown";
                        String file = archivedPlan.has("file") ?
                            archivedPlan.get("file").asText() : "unknown";
                        String archiveDate = archivedPlan.has("archived_date") ?
                            archivedPlan.get("archived_date").asText() : "unknown";
                        String reason = archivedPlan.has("reason") ?
                            archivedPlan.get("reason").asText() : "not specified";

                        System.out.println("  📦 " + name);
                        System.out.println("      File: " + file);
                        System.out.println("      Archived: " + archiveDate);
                        System.out.println("      Reason: " + reason);
                        System.out.println();
                    }
                }
            }
        }
    }

    /**
     * Switch the active plan
     */
    @Command(name = "switch",
             mixinStandardHelpOptions = true,
             description = "Switch the active plan")
    public static class SwitchCommand implements Runnable {

        @Parameters(index = "0", description = "Plan ID to switch to")
        private String planId;

        @Option(names = {"--verify"},
                 description = "Verify that the plan file exists before switching")
        private boolean verify;

        @Override
        public void run() {
            try {
                ManifestManager manifest = new ManifestManager();

                // Check if plan exists
                JsonNode manifestJson = manifest.getManifest();
                JsonNode plans = manifestJson.get("plans");

                if (plans == null || !plans.has(planId)) {
                    System.err.println("✗ Plan not found: " + planId);
                    System.err.println("Available plans:");
                    if (plans != null) {
                        plans.fields().forEachRemaining(entry ->
                            System.err.println("  - " + entry.getKey()));
                    }
                    System.exit(1);
                    return;
                }

                // Verify plan file exists if requested
                if (verify) {
                    JsonNode plan = plans.get(planId);
                    String planFile = plan.has("file") ? plan.get("file").asText() : "Plans.md";
                    Path planPath = Paths.get(planFile);

                    if (!Files.exists(planPath)) {
                        System.err.println("✗ Plan file does not exist: " + planFile);
                        System.err.println("Please verify the file path or use --no-verify to override");
                        System.exit(1);
                        return;
                    }
                }

                // Switch the active plan
                String previousActive = manifestJson.has("active_plan") ?
                    manifestJson.get("active_plan").asText() : "none";

                manifest.setActivePlan(planId);

                JsonNode plan = plans.get(planId);
                String planName = plan.has("name") ? plan.get("name").asText() : planId;

                System.out.println("✓ Switched active plan:");
                System.out.println("  Previous: " + previousActive);
                System.out.println("  Current: " + planId + " (" + planName + ")");

            } catch (Exception e) {
                System.err.println("✗ Failed to switch plan: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Manages the plans/manifest.json file
     */
    public static class ManifestManager {
        private static final String MANIFEST_PATH = "plans/manifest.json";
        private final ObjectMapper objectMapper;
        private final Path manifestPath;
        private JsonNode manifest;

        public ManifestManager() throws IOException {
            this.objectMapper = new ObjectMapper();
            this.manifestPath = Paths.get(MANIFEST_PATH);
            loadOrCreateManifest();
        }

        private void loadOrCreateManifest() throws IOException {
            if (Files.exists(manifestPath)) {
                this.manifest = objectMapper.readTree(manifestPath.toFile());
            } else {
                // Create new manifest
                ObjectNode newManifest = objectMapper.createObjectNode();
                newManifest.put("active_plan", "default");
                newManifest.set("plans", objectMapper.createObjectNode());
                newManifest.set("archived", objectMapper.createArrayNode());
                newManifest.put("last_updated", LocalDate.now().format(DateTimeFormatter.ISO_DATE));

                this.manifest = newManifest;
                saveManifest();
            }
        }

        public JsonNode getManifest() {
            return manifest;
        }

        public void addPlan(String planId, String planName, String planFile, String description)
                throws IOException {
            ObjectNode plansNode = (ObjectNode) manifest.get("plans");

            if (plansNode.has(planId)) {
                throw new IllegalArgumentException("Plan already exists: " + planId);
            }

            ObjectNode newPlan = objectMapper.createObjectNode();
            newPlan.put("name", planName);
            newPlan.put("file", planFile);
            newPlan.put("status", "active");
            newPlan.put("completion_percentage", 0);

            if (description != null && !description.isEmpty()) {
                newPlan.put("description", description);
            }

            plansNode.set(planId, newPlan);
            updateLastModified();
            saveManifest();
        }

        public void setActivePlan(String planId) throws IOException {
            ((ObjectNode) manifest).put("active_plan", planId);
            updateLastModified();
            saveManifest();
        }

        public void archivePlan(String planId, String reason) throws IOException {
            JsonNode plans = manifest.get("plans");
            if (!plans.has(planId)) {
                throw new IllegalArgumentException("Plan not found: " + planId);
            }

            JsonNode planToArchive = plans.get(planId);

            ObjectNode archivedEntry = objectMapper.createObjectNode();
            archivedEntry.set("name", planToArchive.get("name"));
            archivedEntry.set("file", planToArchive.get("file"));
            archivedEntry.put("archived_date", LocalDate.now().format(DateTimeFormatter.ISO_DATE));
            archivedEntry.put("reason", reason != null ? reason : "Manual archive");

            JsonNode archived = manifest.get("archived");
            ((com.fasterxml.jackson.databind.node.ArrayNode) archived).add(archivedEntry);

            // Remove from active plans
            ((ObjectNode) plans).remove(planId);

            // Update active plan if needed
            String currentActive = manifest.get("active_plan").asText();
            if (currentActive.equals(planId)) {
                String newActive = plans.size() > 0 ?
                    plans.fields().next().getKey() : "none";
                ((ObjectNode) manifest).put("active_plan", newActive);
            }

            updateLastModified();
            saveManifest();
        }

        private void updateLastModified() {
            ((ObjectNode) manifest).put("last_updated",
                LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        }

        private void saveManifest() throws IOException {
            // Ensure parent directory exists
            Files.createDirectories(manifestPath.getParent());

            // Write with pretty print
            objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(manifestPath.toFile(), manifest);
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new PlanCommand()).execute(args);
        System.exit(exitCode);
    }
}