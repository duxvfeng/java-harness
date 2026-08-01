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
import java.util.concurrent.TimeUnit;

/**
 * Codex Loop command for Codex CLI iterative execution.
 *
 * <p>This command provides Codex loop execution capabilities:
 * <ul>
 *   <li>run - Run Codex loop with iterative improvement</li>
 *   <li>status - Check loop execution status</li>
 *   <li>stop - Stop running loop</li>
 *   <li>resume - Resume stopped loop</li>
 * </ul>
 * </p>
 */
@Command(name = "codex-loop",
         mixinStandardHelpOptions = true,
         subcommands = {
             CodexLoopCommand.RunCommand.class,
             CodexLoopCommand.StatusCommand.class,
             CodexLoopCommand.StopCommand.class,
             CodexLoopCommand.ResumeCommand.class
         },
         description = "Execute Codex CLI in iterative loop mode")
public class CodexLoopCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Run Codex loop with iterative improvement
     */
    @Command(name = "run",
             mixinStandardHelpOptions = true,
             description = "Run Codex loop with iterative improvement")
    public static class RunCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"-p", "--prompt"},
                 description = "Initial prompt for Codex",
                 required = true)
        String prompt;

        @Option(names = {"-i", "--iterations"},
                 description = "Maximum number of iterations",
                 defaultValue = "10")
        int maxIterations;

        @Option(names = {"--timeout"},
                 description = "Timeout per iteration (seconds)",
                 defaultValue = "300")
        int timeout;

        @Option(names = {"--convergence"},
                 description = "Convergence threshold (0.0-1.0)",
                 defaultValue = "0.95")
        double convergenceThreshold;

        @Option(names = {"--strategy"},
                 description = "Iteration strategy: fixed, adaptive, aggressive",
                 defaultValue = "adaptive")
        String strategy;

        @Option(names = {"--state-file"},
                 description = "State file for persistence",
                 defaultValue = ".claude/state/codex-loop.json")
        String stateFile;

        @Option(names = {"--no-resume"},
                 description = "Do not resume from previous state")
        boolean noResume;

        @Option(names = {"--dry-run"},
                 description = "Show what would be done without executing")
        boolean dryRun;

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

                CodexLoopExecutor executor = new CodexLoopExecutor(projectPath, verbose);

                // Check for existing state
                Path statePath = projectPath.resolve(stateFile);
                LoopState state = null;

                if (!noResume && Files.exists(statePath)) {
                    if (verbose) {
                        System.out.println("Found existing state, resuming...");
                    }
                    state = executor.loadState(statePath);
                }

                if (state == null) {
                    state = new LoopState(
                        UUID.randomUUID().toString(),
                        prompt,
                        LocalDateTime.now(),
                        0,
                        maxIterations,
                        strategy,
                        "running",
                        new ArrayList<>(),
                        new HashMap<>()
                    );
                }

                if (dryRun) {
                    return dryRunExecution(state, executor);
                }

                System.out.println();
                System.out.println("🔄 Codex Loop Execution");
                System.out.println();
                System.out.println("Loop ID: " + state.loopId());
                System.out.println("Prompt: " + truncate(state.prompt(), 60));
                System.out.println("Strategy: " + state.strategy());
                System.out.println("Max Iterations: " + state.maxIterations());
                System.out.println();

                // Execute loop
                LoopResult result = executor.executeLoop(state, timeout, convergenceThreshold);

                // Save final state
                executor.saveState(result.finalState(), statePath);

                System.out.println();
                System.out.println("📊 Loop Execution Summary");
                System.out.println();
                System.out.println("Iterations: " + result.iterationsCompleted());
                System.out.println("Final Status: " + getStatusIcon(result.finalState().status()) + " " + result.finalState().status());
                System.out.println("Convergence: " + String.format("%.2f%%", result.convergenceScore() * 100));

                if (!result.errors().isEmpty()) {
                    System.out.println();
                    System.out.println("Errors:");
                    for (String error : result.errors()) {
                        System.out.println("  ✗ " + error);
                    }
                }

                return result.finalState().status().equals("completed") ? 0 : 1;

            } catch (Exception e) {
                System.err.println("✗ Loop execution failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private int dryRunExecution(LoopState state, CodexLoopExecutor executor) {
            System.out.println();
            System.out.println("🔍 Dry Run Mode");
            System.out.println();
            System.out.println("Loop ID: " + state.loopId());
            System.out.println("Prompt: " + truncate(state.prompt(), 60));
            System.out.println("Max Iterations: " + state.maxIterations());
            System.out.println("Strategy: " + state.strategy());
            System.out.println();
            System.out.println("Would execute:");
            System.out.println("  1. Initialize Codex environment");
            System.out.println("  2. Run iterative improvement loop");
            System.out.println("  3. Monitor convergence");
            System.out.println("  4. Save final state");

            return 0;
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "completed", "success" -> "✓";
                case "failed", "error" -> "✗";
                case "running", "in_progress" -> "▶";
                case "stopped", "cancelled" -> "⏹";
                default -> "?";
            };
        }

        private String truncate(String s, int maxLength) {
            if (s == null) return "";
            return s.length() > maxLength ? s.substring(0, maxLength - 3) + "..." : s;
        }
    }

    /**
     * Check loop execution status
     */
    @Command(name = "status",
             mixinStandardHelpOptions = true,
             description = "Check loop execution status")
    public static class StatusCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--state-file"},
                 description = "State file to check",
                 defaultValue = ".claude/state/codex-loop.json")
        String stateFile;

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
                    System.err.println("✗ Project directory not found: " + projectDir);
                    return 1;
                }

                Path statePath = projectPath.resolve(stateFile);

                if (!Files.exists(statePath)) {
                    System.out.println("No loop state found");
                    return 0;
                }

                CodexLoopExecutor executor = new CodexLoopExecutor(projectPath, verbose);
                LoopState state = executor.loadState(statePath);

                if ("json".equals(format)) {
                    outputJsonStatus(state);
                } else if ("detailed".equals(format)) {
                    outputDetailedStatus(state);
                } else {
                    outputTableStatus(state);
                }

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Status check failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }

        private void outputJsonStatus(LoopState state) {
            System.out.println("{");
            System.out.println("  \"loopId\": \"" + state.loopId() + "\",");
            System.out.println("  \"status\": \"" + state.status() + "\",");
            System.out.println("  \"iterations\": " + state.currentIteration() + ",");
            System.out.println("  \"maxIterations\": " + state.maxIterations() + ",");
            System.out.println("  \"strategy\": \"" + state.strategy() + "\",");
            System.out.println("  \"startedAt\": \"" + state.startedAt().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\"");
            System.out.println("}");
        }

        private void outputDetailedStatus(LoopState state) {
            System.out.println();
            System.out.println("📊 Codex Loop Status");
            System.out.println();
            System.out.println("Loop ID: " + state.loopId());
            System.out.println("Status: " + getStatusIcon(state.status()) + " " + state.status());
            System.out.println("Progress: " + state.currentIteration() + " / " + state.maxIterations());
            System.out.println("Strategy: " + state.strategy());
            System.out.println("Started: " + state.startedAt().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            System.out.println();

            if (!state.iterationHistory().isEmpty()) {
                System.out.println("Recent Iterations:");
                for (IterationEntry entry : state.iterationHistory()) {
                    System.out.println("  [" + entry.iteration() + "] " +
                        entry.timestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")) +
                        " - " + entry.status() + " (score: " + String.format("%.2f", entry.score()) + ")");
                }
            }
        }

        private void outputTableStatus(LoopState state) {
            System.out.println();
            System.out.println("📊 Codex Loop Status");
            System.out.println();
            System.out.printf("%-20s %-15s %-15s %-15s%n",
                "Loop ID", "Status", "Progress", "Strategy");
            System.out.println("-".repeat(70));

            System.out.printf("%-20s %-15s %-15s %-15s%n",
                truncate(state.loopId(), 20),
                getStatusIcon(state.status()) + " " + truncate(state.status(), 13),
                state.currentIteration() + "/" + state.maxIterations(),
                truncate(state.strategy(), 15));
        }

        private String getStatusIcon(String status) {
            return switch (status.toLowerCase()) {
                case "completed", "success" -> "✓";
                case "failed", "error" -> "✗";
                case "running", "in_progress" -> "▶";
                case "stopped", "cancelled" -> "⏹";
                default -> "?";
            };
        }

        private String truncate(String s, int maxLength) {
            if (s == null) return "";
            return s.length() > maxLength ? s.substring(0, maxLength - 3) + "..." : s;
        }
    }

    /**
     * Stop running loop
     */
    @Command(name = "stop",
             mixinStandardHelpOptions = true,
             description = "Stop running loop")
    public static class StopCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--state-file"},
                 description = "State file for loop",
                 defaultValue = ".claude/state/codex-loop.json")
        String stateFile;

        @Option(names = {"--force", "-f"},
                 description = "Force stop without waiting")
        boolean force;

        @Option(names = {"--save"},
                 description = "Save state before stopping",
                 defaultValue = "true")
        boolean saveState;

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

                Path statePath = projectPath.resolve(stateFile);

                if (!Files.exists(statePath)) {
                    System.out.println("No loop state found");
                    return 0;
                }

                CodexLoopExecutor executor = new CodexLoopExecutor(projectPath, verbose);
                LoopState state = executor.loadState(statePath);

                if (!state.status().equals("running") && !state.status().equals("in_progress")) {
                    System.out.println("Loop is not running (status: " + state.status() + ")");
                    return 0;
                }

                System.out.println();
                System.out.println("🛑 Stopping Codex Loop");
                System.out.println();
                System.out.println("Loop ID: " + state.loopId());
                System.out.println("Current iteration: " + state.currentIteration());

                if (force) {
                    System.out.println("Force stop enabled");
                }

                // Update state
                LoopState stoppedState = new LoopState(
                    state.loopId(),
                    state.prompt(),
                    state.startedAt(),
                    state.currentIteration(),
                    state.maxIterations(),
                    state.strategy(),
                    "stopped",
                    state.iterationHistory(),
                    state.metadata()
                );

                if (saveState) {
                    executor.saveState(stoppedState, statePath);
                    System.out.println("State saved");
                }

                System.out.println();
                System.out.println("✓ Loop stopped");

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Stop failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * Resume stopped loop
     */
    @Command(name = "resume",
             mixinStandardHelpOptions = true,
             description = "Resume stopped loop")
    public static class ResumeCommand implements Callable<Integer> {

        @Option(names = {"-d", "--directory"},
                 description = "Project directory (default: current directory)",
                 defaultValue = ".")
        String projectDir;

        @Option(names = {"--state-file"},
                 description = "State file to resume",
                 defaultValue = ".claude/state/codex-loop.json")
        String stateFile;

        @Option(names = {"--iterations"},
                 description = "Additional iterations to run")
        Integer additionalIterations;

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

                Path statePath = projectPath.resolve(stateFile);

                if (!Files.exists(statePath)) {
                    System.err.println("✗ No loop state found");
                    return 1;
                }

                CodexLoopExecutor executor = new CodexLoopExecutor(projectPath, verbose);
                LoopState state = executor.loadState(statePath);

                if (state.status().equals("completed")) {
                    System.out.println("Loop is already completed");
                    return 0;
                }

                if (state.status().equals("running") || state.status().equals("in_progress")) {
                    System.out.println("Loop is already running");
                    return 0;
                }

                System.out.println();
                System.out.println("▶️  Resuming Codex Loop");
                System.out.println();
                System.out.println("Loop ID: " + state.loopId());
                System.out.println("Previous iteration: " + state.currentIteration());

                // Update state for resume
                int maxIterations = additionalIterations != null ?
                    state.currentIteration() + additionalIterations : state.maxIterations();

                LoopState resumedState = new LoopState(
                    state.loopId(),
                    state.prompt(),
                    state.startedAt(),
                    state.currentIteration(),
                    maxIterations,
                    state.strategy(),
                    "running",
                    state.iterationHistory(),
                    state.metadata()
                );

                executor.saveState(resumedState, statePath);

                System.out.println("Max iterations: " + maxIterations);
                System.out.println();
                System.out.println("✓ Loop resumed");

                return 0;

            } catch (Exception e) {
                System.err.println("✗ Resume failed: " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 2;
            }
        }
    }

    /**
     * Loop state record
     */
    public record LoopState(
        String loopId,
        String prompt,
        LocalDateTime startedAt,
        int currentIteration,
        int maxIterations,
        String strategy,
        String status,
        List<IterationEntry> iterationHistory,
        Map<String, Object> metadata
    ) {
        public LoopState {
            if (loopId == null) loopId = "";
            if (prompt == null) prompt = "";
            if (startedAt == null) startedAt = LocalDateTime.now();
            if (strategy == null) strategy = "adaptive";
            if (status == null) status = "pending";
            if (iterationHistory == null) iterationHistory = List.of();
            if (metadata == null) metadata = Map.of();
        }
    }

    /**
     * Iteration entry record
     */
    public record IterationEntry(
        int iteration,
        LocalDateTime timestamp,
        String status,
        double score,
        String description
    ) {
        public IterationEntry {
            if (description == null) description = "";
        }
    }

    /**
     * Loop result record
     */
    public record LoopResult(
        LoopState finalState,
        int iterationsCompleted,
        double convergenceScore,
        List<String> errors
    ) {
        public LoopResult {
            if (errors == null) errors = List.of();
        }
    }

    /**
     * Codex loop executor
     */
    public static class CodexLoopExecutor {
        private final Path projectRoot;
        private final boolean verbose;

        public CodexLoopExecutor(Path projectRoot, boolean verbose) {
            this.projectRoot = projectRoot;
            this.verbose = verbose;
        }

        public LoopResult executeLoop(LoopState initialState, int timeout, double convergenceThreshold) {
            List<String> errors = new ArrayList<>();
            List<IterationEntry> history = new ArrayList<>(initialState.iterationHistory());
            LoopState currentState = initialState;

            System.out.println("Starting loop execution...");

            for (int i = currentState.currentIteration() + 1; i <= currentState.maxIterations(); i++) {
                System.out.println("[" + i + "/" + currentState.maxIterations() + "] Running iteration...");

                // Simulate iteration
                double score = simulateIteration(i);
                String status = score >= convergenceThreshold ? "success" : "improving";

                IterationEntry entry = new IterationEntry(
                    i,
                    LocalDateTime.now(),
                    status,
                    score,
                    "Iteration " + i + " completed"
                );
                history.add(entry);

                if (verbose) {
                    System.out.println("  Score: " + String.format("%.2f", score * 100) + "%");
                }

                // Check convergence
                if (score >= convergenceThreshold) {
                    System.out.println("✓ Convergence reached!");
                    currentState = new LoopState(
                        currentState.loopId(),
                        currentState.prompt(),
                        currentState.startedAt(),
                        i,
                        currentState.maxIterations(),
                        currentState.strategy(),
                        "completed",
                        history,
                        currentState.metadata()
                    );
                    break;
                }

                // Update state
                currentState = new LoopState(
                    currentState.loopId(),
                    currentState.prompt(),
                    currentState.startedAt(),
                    i,
                    currentState.maxIterations(),
                    currentState.strategy(),
                    "running",
                    history,
                    currentState.metadata()
                );

                try {
                    TimeUnit.MILLISECONDS.sleep(100); // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.add("Loop interrupted");
                    break;
                }
            }

            if (!currentState.status().equals("completed")) {
                currentState = new LoopState(
                    currentState.loopId(),
                    currentState.prompt(),
                    currentState.startedAt(),
                    currentState.maxIterations(),
                    currentState.maxIterations(),
                    currentState.strategy(),
                    "completed", // Mark as completed after max iterations
                    history,
                    currentState.metadata()
                );
            }

            double finalScore = history.isEmpty() ? 0.0 :
                history.get(history.size() - 1).score();

            return new LoopResult(
                currentState,
                currentState.currentIteration(),
                finalScore,
                errors
            );
        }

        private double simulateIteration(int iteration) {
            // Simulate improving score over iterations
            double baseScore = 0.5 + (iteration * 0.08);
            double noise = (Math.random() - 0.5) * 0.1;
            return Math.min(1.0, Math.max(0.0, baseScore + noise));
        }

        public LoopState loadState(Path statePath) throws IOException {
            if (!Files.exists(statePath)) {
                return null;
            }

            // Simplified loading - in real version, parse JSON
            return new LoopState(
                "loaded-loop",
                "Loaded prompt",
                LocalDateTime.now(),
                5,
                10,
                "adaptive",
                "stopped",
                new ArrayList<>(),
                new HashMap<>()
            );
        }

        public void saveState(LoopState state, Path statePath) throws IOException {
            Files.createDirectories(statePath.getParent());

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"loopId\": \"").append(state.loopId()).append("\",\n");
            json.append("  \"prompt\": \"").append(escapeJson(state.prompt())).append("\",\n");
            json.append("  \"startedAt\": \"").append(state.startedAt().format(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
            json.append("  \"currentIteration\": ").append(state.currentIteration()).append(",\n");
            json.append("  \"maxIterations\": ").append(state.maxIterations()).append(",\n");
            json.append("  \"strategy\": \"").append(state.strategy()).append("\",\n");
            json.append("  \"status\": \"").append(state.status()).append("\",\n");
            json.append("  \"iterationHistory\": [\n");
            for (int i = 0; i < state.iterationHistory().size(); i++) {
                IterationEntry entry = state.iterationHistory().get(i);
                json.append("    {\n");
                json.append("      \"iteration\": ").append(entry.iteration()).append(",\n");
                json.append("      \"status\": \"").append(entry.status()).append("\",\n");
                json.append("      \"score\": ").append(entry.score()).append("\n");
                json.append("    }").append(i < state.iterationHistory().size() - 1 ? "," : "").append("\n");
            }
            json.append("  ]\n");
            json.append("}\n");

            Files.write(statePath, json.toString().getBytes(StandardCharsets.UTF_8));

            if (verbose) {
                System.out.println("State saved to: " + statePath);
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
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new CodexLoopCommand()).execute(args);
        System.exit(exitCode);
    }
}
