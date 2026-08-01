package com.chachamaru.harness.collaboration.backend;

import com.chachamaru.harness.collaboration.agent.AgentExecutionException;
import com.chachamaru.harness.collaboration.agent.model.AgentContext;
import com.chachamaru.harness.collaboration.agent.model.AgentResult;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for backend executors that can run agents on different platforms.
 *
 * <p>Backend executors provide the ability to run agents on:
 * <ul>
 *   <li>Native Java (Claude subagents)</li>
 *   <li>Cursor (cursor-agent with composer-2.5-fast)</li>
 *   <li>Codex (Codex CLI via App Server Protocol)</li>
 * </ul>
 *
 * @spec_reference spec.md#Backend Selection
 * @since 4.2.0
 */
public interface BackendExecutor {

    /**
     * Gets the backend name.
     *
     * @return the backend identifier
     */
    String getBackendName();

    /**
     * Gets the backend type.
     *
     * @return the backend type
     */
    BackendType getBackendType();

    /**
     * Checks if this backend is available.
     *
     * @return true if the backend can be used
     */
    boolean isAvailable();

    /**
     * Executes an agent task synchronously.
     *
     * @param context the agent context
     * @return the execution result
     * @throws AgentExecutionException if execution fails
     */
    AgentResult execute(AgentContext context) throws AgentExecutionException;

    /**
     * Executes an agent task asynchronously.
     *
     * @param context the agent context
     * @return future containing the execution result
     */
    CompletableFuture<AgentResult> executeAsync(AgentContext context);

    /**
     * Backend type enumeration.
     */
    enum BackendType {
        /** Native Java subagent execution */
        NATIVE,
        /** External companion process (Cursor/Codex) */
        EXTERNAL,
        /** Hybrid execution (mixed native and external) */
        HYBRID
    }
}