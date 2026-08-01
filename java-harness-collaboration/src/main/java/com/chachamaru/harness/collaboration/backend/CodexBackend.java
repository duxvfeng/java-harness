package com.chachamaru.harness.collaboration.backend;

import com.chachamaru.harness.collaboration.agent.AgentExecutionException;
import com.chachamaru.harness.collaboration.agent.model.AgentContext;
import com.chachamaru.harness.collaboration.agent.model.AgentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Codex backend for agent execution.
 *
 * <p>Implements integration with Codex CLI via the codex-companion.sh script
 * and App Server Protocol. Supports thread resume and state management.</p>
 *
 * @spec_reference spec.md#Codex Backend Support
 * @since 4.2.0
 */
public class CodexBackend implements BackendExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CodexBackend.class);
    private static final String CODEX_COMPANION_SCRIPT = "codex-companion.sh";
    private static final int CODEX_TIMEOUT_SECONDS = 600; // 10 minutes

    private final Path scriptRoot;
    private final Path worktreePath;
    private final Path codexStateFile;

    /**
     * Creates a Codex backend.
     *
     * @param scriptRoot the root directory for companion scripts
     * @param worktreePath the worktree path for isolated execution
     */
    public CodexBackend(Path scriptRoot, Path worktreePath) {
        this.scriptRoot = scriptRoot;
        this.worktreePath = worktreePath;
        this.codexStateFile = worktreePath.resolve(".claude/state/codex-primary-environment.json");
    }

    @Override
    public String getBackendName() {
        return "codex";
    }

    @Override
    public BackendType getBackendType() {
        return BackendType.EXTERNAL;
    }

    @Override
    public boolean isAvailable() {
        try {
            // Check if codex-companion.sh exists
            Path companionScript = scriptRoot.resolve("scripts").resolve(CODEX_COMPANION_SCRIPT);
            if (!Files.exists(companionScript)) {
                logger.warn("Codex companion script not found: {}", companionScript);
                return false;
            }

            // Check if codex CLI is available
            ProcessBuilder pb = new ProcessBuilder("which", "codex");
            Process process = pb.start();
            boolean available = process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;

            if (!available) {
                logger.warn("Codex CLI not available in PATH");
            }

            return available;

        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to check Codex availability", e);
            return false;
        }
    }

    @Override
    public AgentResult execute(AgentContext context) throws AgentExecutionException {
        logger.info("Executing task via Codex backend");

        try {
            // Set active task state
            setActiveTask(context);

            // Create worktree if not exists
            ensureWorktreeExists();

            // Build codex companion command
            ProcessBuilder pb = buildCodexCommand(context);

            // Execute codex companion
            CodexExecutionResult result = executeCodexCompanion(pb);

            // Verify codex produced changes
            if (result.commitHash().equals(getBaseCommit())) {
                throw new AgentExecutionException(
                    getBackendName(),
                    "Codex companion produced no commit"
                );
            }

            // Extract codex output
            String codexOutput = result.output();

            logger.info("Codex execution completed: {}", result.commitHash());

            // Return success result
            return AgentResult.success(
                getBackendName(),
                codexOutput,
                "Codex execution completed successfully",
                context.executionStartTime()
            );

        } catch (AgentExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentExecutionException(
                getBackendName(),
                "Codex execution failed: " + e.getMessage(),
                e
            );
        } finally {
            clearActiveTask();
        }
    }

    @Override
    public CompletableFuture<AgentResult> executeAsync(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(context);
            } catch (AgentExecutionException e) {
                return AgentResult.failure(
                    getBackendName(),
                    "Async Codex execution failed: " + e.getMessage(),
                    LocalDateTime.now()
                );
            }
        });
    }

    /**
     * Builds the codex companion command.
     */
    private ProcessBuilder buildCodexCommand(AgentContext context) throws IOException {
        Path companionScript = scriptRoot.resolve("scripts").resolve(CODEX_COMPANION_SCRIPT);

        // Ensure state file directory exists
        Files.createDirectories(codexStateFile.getParent());

        // Build: codex-companion.sh task --write -C <worktree> "<prompt>"
        ProcessBuilder pb = new ProcessBuilder(
            companionScript.toString(),
            "task",
            "--write",
            "-C", worktreePath.toString(),
            buildCodexPrompt(context)
        );

        // Set environment variables for state management
        pb.environment().put("HARNESS_CODEX_PRIMARY_ENV_STATE_FILE", codexStateFile.toString());
        pb.environment().put("HARNESS_ACTIVE_TASK", context.task() != null ? context.task().id() : "unknown");

        // Redirect error stream
        pb.redirectErrorStream(true);

        return pb;
    }

    /**
     * Builds the codex prompt from agent context.
     */
    private String buildCodexPrompt(AgentContext context) {
        StringBuilder prompt = new StringBuilder();

        if (context.task() != null) {
            prompt.append("Task: ").append(context.task().title()).append("\n");
            prompt.append("Description: ").append(context.task().description()).append("\n\n");
        }

        // Add advisor recommendations if present
        Object advisorResponseObj = context.getSessionState("advisorResponse", Object.class);
        if (advisorResponseObj != null) {
            prompt.append("Advisor Recommendations:\n");
            prompt.append(advisorResponseObj).append("\n\n");
        }

        // Add review findings if present
        Object reviewFindingsObj = context.getSessionState("reviewFindings", Object.class);
        if (reviewFindingsObj != null) {
            prompt.append("Review Findings to Address:\n");
            prompt.append(reviewFindingsObj).append("\n\n");
        }

        prompt.append("Please implement the required changes using Codex CLI. Create exactly one git commit in this worktree before returning.");

        return prompt.toString();
    }

    /**
     * Executes the codex companion process.
     */
    private CodexExecutionResult executeCodexCompanion(ProcessBuilder pb) throws Exception {
        logger.debug("Executing codex companion command");

        Process process = pb.start();

        // Read output
        String output = new String(process.getInputStream().readAllBytes());

        // Wait for completion with timeout
        boolean finished = process.waitFor(CODEX_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new AgentExecutionException(
                getBackendName(),
                "Codex execution timeout after " + CODEX_TIMEOUT_SECONDS + " seconds"
            );
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new AgentExecutionException(
                getBackendName(),
                "Codex companion failed with exit code " + exitCode + ": " + output
            );
        }

        // Get commit hash from worktree
        String commitHash = getWorktreeHeadCommit();

        return new CodexExecutionResult(commitHash, output);
    }

    /**
     * Ensures the worktree exists.
     */
    private void ensureWorktreeExists() throws IOException, InterruptedException {
        if (!Files.exists(worktreePath)) {
            logger.info("Creating codex worktree at: {}", worktreePath);

            Files.createDirectories(worktreePath.getParent());

            ProcessBuilder pb = new ProcessBuilder(
                "git",
                "worktree",
                "add",
                "-b", "codex-work/" + System.currentTimeMillis(),
                worktreePath.toString(),
                getBaseCommit()
            );

            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished || process.exitValue() != 0) {
                throw new IOException("Failed to create git worktree");
            }
        }
    }

    /**
     * Gets the base commit hash.
     */
    private String getBaseCommit() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
        Process process = pb.start();
        process.waitFor(10, TimeUnit.SECONDS);

        if (process.exitValue() != 0) {
            throw new IOException("Failed to get base commit");
        }

        return new String(process.getInputStream().readAllBytes()).trim();
    }

    /**
     * Gets the current HEAD commit in the worktree.
     */
    private String getWorktreeHeadCommit() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
            "git",
            "-C", worktreePath.toString(),
            "rev-parse",
            "HEAD"
        );

        Process process = pb.start();
        process.waitFor(10, TimeUnit.SECONDS);

        if (process.exitValue() != 0) {
            throw new IOException("Failed to get worktree HEAD commit");
        }

        return new String(process.getInputStream().readAllBytes()).trim();
    }

    /**
     * Sets active task state for Go guardrail integration.
     */
    private void setActiveTask(AgentContext context) {
        try {
            Path activeTaskPath = worktreePath.resolve(".claude/state/active-task.json");
            Files.createDirectories(activeTaskPath.getParent());

            String taskId = context.task() != null ? context.task().id() : "unknown";
            String content = String.format("""
                {
                  "phase": "codex-execution",
                  "task": "%s",
                  "backend": "codex",
                  "timestamp": "%s"
                }
                """,
                taskId, java.time.Instant.now()
            );

            Files.writeString(activeTaskPath, content);
            logger.debug("Set active task for codex: {}", taskId);

        } catch (IOException e) {
            logger.warn("Failed to set active task state", e);
        }
    }

    /**
     * Clears active task state.
     */
    private void clearActiveTask() {
        try {
            Path activeTaskPath = worktreePath.resolve(".claude/state/active-task.json");
            Files.deleteIfExists(activeTaskPath);
        } catch (IOException e) {
            logger.warn("Failed to clear active task state", e);
        }
    }

    /**
     * Result of Codex execution.
     */
    private record CodexExecutionResult(
        String commitHash,
        String output
    ) {
    }
}