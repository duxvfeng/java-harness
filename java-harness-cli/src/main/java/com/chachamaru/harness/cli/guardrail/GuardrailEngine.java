package com.chachamaru.harness.cli.guardrail;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.ipc.IpcClient;
import com.chachamaru.harness.cli.ipc.HttpIpcClient;
import com.chachamaru.harness.shared.dto.GuardrailDecision;
import com.chachamaru.harness.shared.dto.HookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Guardrail engine - evaluates R01-R15 rules
 * Integrates with IPC client for async communication with Spring Boot Service
 */
public class GuardrailEngine {
    private static final Logger log = LoggerFactory.getLogger(GuardrailEngine.class);
    private final List<Rule> rules = new ArrayList<>();
    private final IpcClient ipcClient;
    private static final long DEFAULT_IPC_TIMEOUT_MS = 3000; // 3 second timeout for IPC calls

    public GuardrailEngine() {
        this.ipcClient = new HttpIpcClient();
        log.info("GuardrailEngine initialized with IPC client");
    }

    public GuardrailEngine(IpcClient ipcClient) {
        this.ipcClient = ipcClient != null ? ipcClient : new HttpIpcClient();
        log.info("GuardrailEngine initialized with provided IPC client");
    }

    public void registerRule(Rule rule) {
        rules.add(rule);
        log.debug("Registered guardrail rule: {}", rule.getId());
    }

    /**
     * Evaluate input against all registered rules (synchronous)
     * Returns first deny result, or allow if no rules match/deny
     */
    public GuardrailResult evaluate(HookInput input) {
        for (Rule rule : rules) {
            if (rule.matches(input)) {
                log.debug("Rule {} matched input, evaluating", rule.getId());
                GuardrailResult result = rule.evaluate(input);
                if (result.isDenied()) {
                    return result; // Early exit on deny
                }
            }
        }
        return GuardrailResult.allowed();
    }

    /**
     * Evaluate input asynchronously with IPC delegation
     * Complex inputs are delegated to Spring Boot Service via IPC
     *
     * @param input the hook input to evaluate
     * @return CompletableFuture containing the guardrail decision
     */
    public CompletableFuture<GuardrailResult> evaluateAsync(HookInput input) {
        // First check local rules (fast path)
        GuardrailResult localResult = evaluate(input);
        if (localResult.isDenied()) {
            log.debug("Local rules denied, returning immediately");
            return CompletableFuture.completedFuture(localResult);
        }

        // If local rules allow, delegate to IPC service for complex evaluation
        log.debug("Local rules allowed, delegating to IPC service");
        return delegateToIpcService(input);
    }

    /**
     * Delegate evaluation to IPC service with timeout
     */
    private CompletableFuture<GuardrailResult> delegateToIpcService(HookInput input) {
        if (!ipcClient.isServiceAvailable()) {
            log.warn("IPC service not available, returning local allow decision");
            return CompletableFuture.completedFuture(GuardrailResult.allowed());
        }

        try {
            HookEvent event = HookEvent.create("GuardrailEvaluation", input.toMap());
            CompletableFuture<GuardrailDecision> future = ipcClient.sendHookEvent(event);

            return future.orTimeout(DEFAULT_IPC_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .handle((decision, throwable) -> {
                        if (throwable != null) {
                            if (throwable instanceof TimeoutException) {
                                log.warn("IPC call timed out, returning allow decision");
                            } else {
                                log.error("IPC call failed", throwable);
                            }
                            return GuardrailResult.allowed("IPC evaluation failed - default allow");
                        }

                        // Convert GuardrailDecision to GuardrailResult
                        if (decision.action() == GuardrailDecision.Action.DENY) {
                            return GuardrailResult.denied(decision.ruleId(), decision.reason());
                        } else {
                            return GuardrailResult.allowed(decision.reason());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to delegate to IPC service", e);
            return CompletableFuture.completedFuture(GuardrailResult.allowed("IPC delegation failed - default allow"));
        }
    }

    /**
     * Close the IPC client and release resources
     */
    public void close() {
        if (ipcClient != null) {
            ipcClient.close();
        }
    }
}
