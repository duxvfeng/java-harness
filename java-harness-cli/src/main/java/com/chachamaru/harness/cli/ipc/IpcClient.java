package com.chachamaru.harness.cli.ipc;

import com.chachamaru.harness.shared.dto.HookEvent;
import com.chachamaru.harness.shared.dto.GuardrailDecision;
import java.util.concurrent.CompletableFuture;

/**
 * IPC client for communicating with Spring Boot Service
 * Handles asynchronous communication for complex logic delegation
 */
public interface IpcClient {
    /**
     * Send hook event to Spring Boot Service asynchronously
     *
     * @param event the hook event to send
     * @return CompletableFuture containing the guardrail decision
     */
    CompletableFuture<GuardrailDecision> sendHookEvent(HookEvent event);

    /**
     * Check if the Spring Boot Service is available
     *
     * @return true if service is available, false otherwise
     */
    boolean isServiceAvailable();

    /**
     * Close the IPC client and release resources
     */
    void close();
}
