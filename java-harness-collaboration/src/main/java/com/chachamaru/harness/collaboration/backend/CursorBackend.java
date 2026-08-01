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
 * Cursor backend for agent execution.
 *
 * <p>Implements integration with Cursor agents via the cursor-companion.sh script.
 * Supports Mode 1 execution: Producer → Sub-Lead → Composer hierarchy.</p>
 *
 * @spec_reference spec.md#Cursor Backend Support
 * @since 4.2.0
 */
public class CursorBackend implements BackendExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CursorBackend.class);
    private static final String CURSOR_COMPANION_SCRIPT = "cursor-companion.sh";
    private static final int CURSOR_TIMEOUT_SECONDS = 300; // 5 minutes

    private final Path scriptRoot;
    private final Path worktreePath;
    private final String cursorModel;

    /**
     * Creates a Cursor backend.
     *
     * @param scriptRoot the root directory for companion scripts
     * @param worktreePath the worktree path for isolated execution
     * @param cursorModel the Cursor model to use (default: composer-2.5-fast)
     */
    public CursorBackend(Path scriptRoot, Path worktreePath, String cursorModel) {
        this.scriptRoot = scriptRoot;
        this.worktreePath = worktreePath;
        this.cursorModel = cursorModel != null ? cursorModel : "composer-2.5-fast";
    }

    @Override
    public String getBackendName() {
        return "cursor";
    }

    @Override
    public BackendType getBackendType() {
        return BackendType.EXTERNAL;
    }

    @Override
    public boolean isAvailable() {
        try {
            // Check if cursor-companion.sh exists
            Path companionScript = scriptRoot.resolve("scripts").resolve(CURSOR_COMPANION_SCRIPT);
            if (!Files.exists(companionScript)) {
                logger.warn("Cursor companion script not found: {}", companionScript);
                return false;
            }

            // Check if cursor agent is available
            ProcessBuilder pb = new ProcessBuilder("which", "cursor");
            Process process = pb.start();
            boolean available = process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;

            if (!available) {
                logger.warn("Cursor command not available in PATH");
            }

            return available;

        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to check Cursor availability", e);
            return false;
        }
    }

    @Override
    public AgentResult execute(AgentContext context) throws AgentExecutionException {
        logger.info("Executing task via Cursor backend with model: {}", cursorModel);

        try {
            // Set active task state
            setActiveTask(context);

            // Create worktree if not exists
            ensureWorktreeExists();

            // Build cursor companion command
            ProcessBuilder pb = buildCursorCommand(context);

            // Execute cursor companion
            CursorExecutionResult result = executeCursorCompanion(pb);

            // Verify cursor produced changes
            if (result.commitHash().equals(getBaseCommit())) {
                throw new AgentExecutionException(
                    getBackendName(),
                    "Cursor companion produced no commit"
                );
            }

            // Extract cursor output
            String cursorOutput = result.output();

            logger.info("Cursor execution completed: {}", result.commitHash());

            // Return success result
            return AgentResult.success(
                getBackendName(),
                cursorOutput,
                "Cursor execution completed successfully",
                context.executionStartTime()
            );

        } catch (AgentExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentExecutionException(
                getBackendName(),
                "Cursor execution failed: " + e.getMessage(),
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
                    "Async Cursor execution failed: " + e.getMessage(),
                    LocalDateTime.now()
                );
            }
        });
    }

    /**
     * Builds the cursor companion command.
     */
    private ProcessBuilder buildCursorCommand(AgentContext context) {
        Path companionScript = scriptRoot.resolve("scripts").resolve(CURSOR_COMPANION_SCRIPT);

        // Build: cursor-companion.sh task --write --workspace <worktree> "<prompt>"
        ProcessBuilder pb = new ProcessBuilder(
            companionScript.toString(),
            "task",
            "--write",
            "--workspace", worktreePath.toString(),
            buildCursorPrompt(context)
        );

        // Set environment variables
        pb.environment().put("CURSOR_MODEL", cursorModel);
        pb.environment().put("HARNESS_ACTIVE_TASK", context.task() != null ? context.task().id() : "unknown");

        // Redirect error stream
        pb.redirectErrorStream(true);

        return pb;
    }

    /**
     * Builds the cursor prompt from agent context.
     */
    private String buildCursorPrompt(AgentContext context) {
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

        prompt.append("Please implement the required changes. Create exactly one git commit in this worktree before returning.");

        return prompt.toString();
    }

    /**
     * Executes the cursor companion process.
     */
    private CursorExecutionResult executeCursorCompanion(ProcessBuilder pb) throws Exception {
        logger.debug("Executing cursor companion command");

        Process process = pb.start();

        // Read output
        String output = new String(process.getInputStream().readAllBytes());

        // Wait for completion with timeout
        boolean finished = process.waitFor(CURSOR_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new AgentExecutionException(
                getBackendName(),
                "Cursor execution timeout after " + CURSOR_TIMEOUT_SECONDS + " seconds"
            );
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new AgentExecutionException(
                getBackendName(),
                "Cursor companion failed with exit code " + exitCode + ": " + output
            );
        }

        // Get commit hash from worktree
        String commitHash = getWorktreeHeadCommit();

        return new CursorExecutionResult(commitHash, output);
    }

    /**
     * Ensures the worktree exists.
     */
    private void ensureWorktreeExists() throws IOException, InterruptedException {
        if (!Files.exists(worktreePath)) {
            logger.info("Creating cursor worktree at: {}", worktreePath);

            Files.createDirectories(worktreePath.getParent());

            ProcessBuilder pb = new ProcessBuilder(
                "git",
                "worktree",
                "add",
                "-b", "cursor-work/" + System.currentTimeMillis(),
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
                  "phase": "cursor-execution",
                  "task": "%s",
                  "backend": "cursor",
                  "timestamp": "%s"
                }
                """,
                taskId, java.time.Instant.now()
            );

            Files.writeString(activeTaskPath, content);
            logger.debug("Set active task for cursor: {}", taskId);

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
     * Result of Cursor execution.
     */
    private record CursorExecutionResult(
        String commitHash,
        String output
    ) {
    }
}