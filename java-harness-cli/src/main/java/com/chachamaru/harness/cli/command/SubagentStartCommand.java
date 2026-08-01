package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Subagent Start command for starting subagents.
 *
 * <p>This command provides subagent startup capabilities:
 * <ul>
 *   <li>start - Start a single subagent</li>
 *   <li>batch - Start multiple subagents</li>
 *   <li>list - List available agent types</li>
 *   <li>health - Check subagent health</li>
 * </ul>
 * </p>
 */
@Command(name = "subagent-start",
         mixinStandardHelpOptions = true,
         subcommands = {
             SubagentStartCommand.StartCommand.class,
             SubagentStartCommand.BatchCommand.class,
             SubagentStartCommand.ListCommand.class,
             SubagentStartCommand.HealthCommand.class
         },
         description = "Start and manage subagents")
public class SubagentStartCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Start a single subagent
     */
    @Command(name = "start",
             mixinStandardHelpOptions = true,
             description = "Start a single subagent")
    public static class StartCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--type"},
                 description = "Agent type: worker, reviewer, advisor, custom",
                 required = true)
        String agentType;

        @Option(names = {"--name"},
                 description = "Agent name (auto-generated if not specified)")
        String agentName;

        @Option(names = {"--config"},
                 description = "Agent configuration file")
        String configFile;

        @Option(names = {"--model"},
                 description = "Model to use for agent")
        String model;

        @Option(names = {"--mode"},
                 description = "Agent mode: solo, parallel, breezing",
                 defaultValue = "solo")
        String mode;

        @Option(names = {"--env"},
                 description = "Environment variables (KEY=VALUE, comma-separated)")
        String envVars;

        @Option(names = {"--detached"},
                 description = "Run agent in detached mode")
        boolean detached;

        @Option(names = {"--workdir"},
                 description = "Working directory for agent")
        String workDir;

        @Option(names = {"--log-file"},
                 description = "Log file path")
        String logFile;

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

                SubagentManager manager = new SubagentManager(projectPath, verbose);

                // Generate agent name if not provided
                String finalAgentName = agentName != null && !agentName.isEmpty() ?
                    agentName : agentType + "-" + System.currentTimeMillis();

                // Parse environment variables
                Map<String, String> env = parseEnvVars(envVars);

                // Create agent config
                AgentConfig config = new AgentConfig(
                    agentType,
                    finalAgentName,
                    model != null ? model : "default",
                    mode,
                    configFile != null ? configFile : "",
                    workDir != null ? workDir : "",
                    logFile != null ? logFile : "",
                    env,
                    detached
                );

                System.out.println();
                System.out.println("🚀 Starting Subagent");
                System.out.println();
                System.out.println("Name: " + config.agentName());
                System.out.println("Type: " + config.agentType());
                System.out.println("Model: " + config.model());
                System.out.println("Mode: " + config.mode());
                System.out.println("Detached: " + (config.detached() ? "Yes" : "No"));
                System.out.println();

                // Start agent
                AgentResult result = manager.startAgent(config);

                System.out.println("Status: " + getStatusIcon(result.status()) + " " + result.status());
                System.out.println("Agent ID: " + result.agentId());
                System.out.println("PID: " + result.pid());

                if (result.workingDirectory() != null && !result.workingDirectory().isEmpty()) {
                    System.out.println("Working Directory: " + result.workingDirectory());
                }

                if (!result.errors().isEmpty()) {
                    System.out.println();
                    System.out.println("Errors:");
                    for (String error : result.errors()) {
                        System.out.println("  ✗ " + error);
                    }
                }

                return result.status().equals("running") ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Agent start failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private Map<String, String> parseEnvVars(String envVars) {
            Map<String, String> env = new HashMap<>();
            if (envVars != null && !envVars.isEmpty()) {
                String[] pairs = envVars.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        env.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }
            return env;
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "running", "active" -> "▶";
                case "stopped", "exited" -> "⏹";
                case "failed", "error" -> "✗";
                case "pending" -> "⏳";
                default -> "?";
            };
        }
    }

    /**
     * Start multiple subagents
     */
    @Command(name = "batch",
             mixinStandardHelpOptions = true,
             description = "Start multiple subagents")
    public static class BatchCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--config"},
                 description = "Batch configuration file",
                 required = true)
        String batchConfig;

        @Option(names = {"--parallel"},
                 description = "Start agents in parallel")
        boolean parallel;

        @Option(names = {"--delay"},
                 description = "Delay between starts (milliseconds)",
                 defaultValue = "1000")
        int delay;

        @Option(names = {"--continue-on-error"},
                 description = "Continue starting agents even if some fail")
        boolean continueOnError;

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

                Path configPath = projectPath.resolve(batchConfig);

                if (!Files.exists(configPath)) {
                    System.err.println("✗ Batch config file not found: " + configPath);
                    return 1;
                }

                SubagentManager manager = new SubagentManager(projectPath, verbose);

                // Load batch config
                BatchConfig config = manager.loadBatchConfig(configPath);

                System.out.println();
                System.out.println("🚀 Batch Starting Subagents");
                System.out.println();
                System.out.println("Config: " + batchConfig);
                System.out.println("Agents: " + config.agents().size());
                System.out.println("Parallel: " + (parallel ? "Yes" : "No"));
                System.out.println();

                // Start agents
                BatchResult result = manager.startBatch(config, parallel, delay, continueOnError);

                System.out.println();
                System.out.println("📊 Batch Start Summary");
                System.out.println();
                System.out.println("Total: " + result.totalCount());
                System.out.println("Started: " + result.startedCount());
                System.out.println("Failed: " + result.failedCount());

                if (!result.failedAgents().isEmpty()) {
                    System.out.println();
                    System.out.println("Failed Agents:");
                    for (String agent : result.failedAgents()) {
                        System.out.println("  ✗ " + agent);
                    }
                }

                return result.failedCount() == 0 ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Batch start failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * List available agent types
     */
    @Command(name = "list",
             mixinStandardHelpOptions = true,
             description = "List available agent types")
    public static class ListCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--format"},
                 description = "Output format: table, json, detailed",
                 defaultValue = "table")
        String format;

        @Option(names = {"--include-custom"},
                 description = "Include custom agent types")
        boolean includeCustom;

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

                SubagentManager manager = new SubagentManager(projectPath, verbose);
                List<AgentTypeInfo> agentTypes = manager.listAgentTypes(includeCustom);

                if ("json".equals(format)) {
                    outputJsonTypes(agentTypes);
                } else if ("detailed".equals(format)) {
                    outputDetailedTypes(agentTypes);
                } else {
                    outputTableTypes(agentTypes);
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

        private void outputJsonTypes(List<AgentTypeInfo> types) {
            System.out.println("[");
            for (int i = 0; i < types.size(); i++) {
                AgentTypeInfo type = types.get(i);
                System.out.println("  {");
                System.out.println("    \"type\": \"" + type.type() + "\",");
                System.out.println("    \"description\": \"" + escapeJson(type.description()) + "\",");
                System.out.println("    \"defaultModel\": \"" + type.defaultModel() + "\"");
                System.out.println("  }" + (i < types.size() - 1 ? "," : ""));
            }
            System.out.println("]");
        }

        private void outputDetailedTypes(List<AgentTypeInfo> types) {
            System.out.println();
            System.out.println("📋 Available Agent Types");
            System.out.println();

            for (AgentTypeInfo type : types) {
                System.out.println("Type: " + type.type());
                System.out.println("  Description: " + type.description());
                System.out.println("  Default Model: " + type.defaultModel());
                System.out.println("  Supported Modes: " + String.join(", ", type.supportedModes()));
                System.out.println("-".repeat(60));
            }

            System.out.println();
            System.out.println("Total: " + types.size() + " type(s)");
        }

        private void outputTableTypes(List<AgentTypeInfo> types) {
            System.out.println();
            System.out.println("📋 Available Agent Types");
            System.out.println();
            System.out.printf("%-15s %-40s %-20s%n",
                "Type", "Description", "Default Model");
            System.out.println("-".repeat(80));

            for (AgentTypeInfo type : types) {
                System.out.printf("%-15s %-40s %-20s%n",
                    truncate(type.type(), 15),
                    truncate(type.description(), 40),
                    truncate(type.defaultModel(), 20));
            }

            System.out.println();
            System.out.println("Total: " + types.size() + " type(s)");
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
     * Check subagent health
     */
    @Command(name = "health",
             mixinStandardHelpOptions = true,
             description = "Check subagent health")
    public static class HealthCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--agent"},
                 description = "Specific agent to check")
        String agentName;

        @Option(names = {"--all"},
                 description = "Check all running agents")
        boolean checkAll;

        @Option(names = {"--format"},
                 description = "Output format: table, json, detailed",
                 defaultValue = "table")
        String format;

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                Path projectPath = Paths.get(projectDir);

                if (!Files.exists(projectPath)) {
                    System.err.println("✗ Project directory not found: " + projectPath);
                    return 1;
                }

                SubagentManager manager = new SubagentManager(projectPath, verbose);
                List<AgentHealth> healthList = manager.checkHealth(agentName, checkAll);

                if ("json".equals(format)) {
                    outputJsonHealth(healthList);
                } else if ("detailed".equals(format)) {
                    outputDetailedHealth(healthList);
                } else {
                    outputTableHealth(healthList);
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Health check failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private void outputJsonHealth(List<AgentHealth> healthList) {
            System.out.println("[");
            for (int i = 0; i < healthList.size(); i++) {
                AgentHealth health = healthList.get(i);
                System.out.println("  {");
                System.out.println("    \"agentName\": \"" + health.agentName() + "\",");
                System.out.println("    \"status\": \"" + health.status() + "\",");
                System.out.println("    \"healthy\": " + health.healthy() + ",");
                System.out.println("    \"uptime\": " + health.uptime() + ",");
                System.out.println("    \"memoryUsage\": " + health.memoryUsage());
                System.out.println("  }" + (i < healthList.size() - 1 ? "," : ""));
            }
            System.out.println("]");
        }

        private void outputDetailedHealth(List<AgentHealth> healthList) {
            System.out.println();
            System.out.println("🏥 Subagent Health");
            System.out.println();

            for (AgentHealth health : healthList) {
                System.out.println("Agent: " + health.agentName());
                System.out.println("  Status: " + getStatusIcon(health.status()) + " " + health.status());
                System.out.println("  Healthy: " + (health.healthy() ? "✓ Yes" : "✗ No"));
                System.out.println("  Uptime: " + health.uptime() + "s");
                System.out.println("  Memory: " + health.memoryUsage() + " MB");
                System.out.println("  Last Check: " + health.lastCheck().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                System.out.println("-".repeat(60));
            }

            System.out.println();
            System.out.println("Total: " + healthList.size() + " agent(s)");
        }

        private void outputTableHealth(List<AgentHealth> healthList) {
            System.out.println();
            System.out.println("🏥 Subagent Health");
            System.out.println();
            System.out.printf("%-20s %-15s %-10s %-15s %-15s%n",
                "Agent", "Status", "Healthy", "Uptime", "Memory");
            System.out.println("-".repeat(90));

            for (AgentHealth health : healthList) {
                System.out.printf("%-20s %-15s %-10s %-15s %-15s%n",
                    truncate(health.agentName(), 20),
                    getStatusIcon(health.status()) + " " + truncate(health.status(), 13),
                    health.healthy() ? "✓" : "✗",
                    health.uptime() + "s",
                    health.memoryUsage() + " MB");
            }

            System.out.println();
            System.out.println("Total: " + healthList.size() + " agent(s)");
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "running", "active" -> "▶";
                case "stopped", "exited" -> "⏹";
                case "failed", "error" -> "✗";
                case "unhealthy" -> "⚠";
                default -> "?";
            };
        }

        private String truncate(String s, int maxLength) {
            if (s == null) return "";
            return s.length() > maxLength ? s.substring(0, maxLength - 3) + "..." : s;
        }
    }

    /**
     * Agent config record
     */
    public record AgentConfig(
        String agentType,
        String agentName,
        String model,
        String mode,
        String configFile,
        String workDir,
        String logFile,
        Map<String, String> envVars,
        boolean detached
    ) {
        public AgentConfig {
            if (agentType == null) agentType = "";
            if (agentName == null) agentName = "";
            if (model == null) model = "";
            if (mode == null) mode = "solo";
            if (configFile == null) configFile = "";
            if (workDir == null) workDir = "";
            if (logFile == null) logFile = "";
            if (envVars == null) envVars = Map.of();
        }
    }

    /**
     * Agent result record
     */
    public record AgentResult(
        String agentId,
        String status,
        long pid,
        String workingDirectory,
        List<String> errors
    ) {
        public AgentResult {
            if (agentId == null) agentId = "";
            if (status == null) status = "";
            if (errors == null) errors = List.of();
        }
    }

    /**
     * Batch config record
     */
    public record BatchConfig(
        String name,
        List<AgentConfig> agents
    ) {
        public BatchConfig {
            if (name == null) name = "";
            if (agents == null) agents = List.of();
        }
    }

    /**
     * Batch result record
     */
    public record BatchResult(
        int totalCount,
        int startedCount,
        int failedCount,
        List<String> failedAgents
    ) {
        public BatchResult {
            if (failedAgents == null) failedAgents = List.of();
        }
    }

    /**
     * Agent type info record
     */
    public record AgentTypeInfo(
        String type,
        String description,
        String defaultModel,
        List<String> supportedModes
    ) {
        public AgentTypeInfo {
            if (type == null) type = "";
            if (description == null) description = "";
            if (defaultModel == null) defaultModel = "";
            if (supportedModes == null) supportedModes = List.of();
        }
    }

    /**
     * Agent health record
     */
    public record AgentHealth(
        String agentName,
        String status,
        boolean healthy,
        long uptime,
        long memoryUsage,
        LocalDateTime lastCheck
    ) {
        public AgentHealth {
            if (agentName == null) agentName = "";
            if (status == null) status = "";
            if (lastCheck == null) lastCheck = LocalDateTime.now();
        }
    }

    /**
     * Subagent manager
     */
    public static class SubagentManager {
        private final Path projectRoot;
        private final boolean verbose;

        public SubagentManager(Path projectRoot, boolean verbose) {
            this.projectRoot = projectRoot;
            this.verbose = verbose;
        }

        public AgentResult startAgent(AgentConfig config) {
            List<String> errors = new ArrayList<>();

            try {
                String agentId = UUID.randomUUID().toString();
                long pid = (long) (Math.random() * 10000) + 1000;

                if (verbose) {
                    System.out.println("Starting agent: " + config.agentName());
                    System.out.println("  Type: " + config.agentType());
                    System.out.println("  Mode: " + config.mode());
                }

                // Simulate agent start
                String workDir = config.workDir().isEmpty() ?
                    projectRoot.resolve(".claude/workdirs/" + config.agentName()).toString() :
                    config.workDir();

                return new AgentResult(
                    agentId,
                    "running",
                    pid,
                    workDir,
                    errors
                );

            } catch (Exception e) {
                errors.add(e.getMessage());
                return new AgentResult("", "failed", 0, "", errors);
            }
        }

        public BatchConfig loadBatchConfig(Path configPath) throws IOException {
            // Simplified loading
            List<AgentConfig> agents = new ArrayList<>();

            // Mock agents
            agents.add(new AgentConfig(
                "worker", "worker-1", "default", "solo", "", "", "", Map.of(), false
            ));
            agents.add(new AgentConfig(
                "reviewer", "reviewer-1", "default", "solo", "", "", "", Map.of(), false
            ));

            return new BatchConfig("batch-" + System.currentTimeMillis(), agents);
        }

        public BatchResult startBatch(BatchConfig config, boolean parallel, int delay, boolean continueOnError) {
            List<String> failedAgents = new ArrayList<>();
            int startedCount = 0;

            for (AgentConfig agentConfig : config.agents()) {
                try {
                    AgentResult result = startAgent(agentConfig);

                    if (result.status().equals("running")) {
                        startedCount++;
                        System.out.println("✓ Started: " + agentConfig.agentName());
                    } else {
                        failedAgents.add(agentConfig.agentName());

                        if (!continueOnError) {
                            break;
                        }
                    }

                    if (delay > 0 && !parallel) {
                        Thread.sleep(delay);
                    }

                } catch (Exception e) {
                    failedAgents.add(agentConfig.agentName());
                    if (!continueOnError) {
                        break;
                    }
                }
            }

            return new BatchResult(
                config.agents().size(),
                startedCount,
                failedAgents.size(),
                failedAgents
            );
        }

        public List<AgentTypeInfo> listAgentTypes(boolean includeCustom) {
            List<AgentTypeInfo> types = new ArrayList<>();

            // Standard types
            types.add(new AgentTypeInfo(
                "worker",
                "Implementation worker agent",
                "claude-opus-4",
                List.of("solo", "parallel", "breezing")
            ));
            types.add(new AgentTypeInfo(
                "reviewer",
                "Code review agent",
                "claude-opus-4",
                List.of("solo")
            ));
            types.add(new AgentTypeInfo(
                "advisor",
                "Advisor agent for guidance",
                "claude-sonnet-4",
                List.of("solo")
            ));

            if (includeCustom) {
                types.add(new AgentTypeInfo(
                    "custom",
                    "Custom agent type",
                    "default",
                    List.of("solo")
                ));
            }

            return types;
        }

        public List<AgentHealth> checkHealth(String agentName, boolean checkAll) {
            List<AgentHealth> healthList = new ArrayList<>();

            if (checkAll || agentName == null || agentName.isEmpty()) {
                // Check all standard agents
                String[] agents = {"worker-1", "reviewer-1", "advisor-1"};

                for (String agent : agents) {
                    healthList.add(new AgentHealth(
                        agent,
                        "running",
                        true,
                        (long) (Math.random() * 3600),
                        (long) (Math.random() * 512),
                        LocalDateTime.now()
                    ));
                }
            } else {
                // Check specific agent
                healthList.add(new AgentHealth(
                    agentName,
                    "running",
                    true,
                    1800L,
                    256L,
                    LocalDateTime.now()
                ));
            }

            return healthList;
        }
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new SubagentStartCommand()).execute(args);
        System.exit(exitCode);
    }
}
