package com.chachamaru.harness.cli.guardrail;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.ipc.IpcClient;
import com.chachamaru.harness.cli.ipc.HttpIpcClient;
import com.chachamaru.harness.cli.guardrail.loader.CustomRuleLoader;
import com.chachamaru.harness.cli.guardrail.PrioritizedRule;
import com.chachamaru.harness.cli.guardrail.cache.EvaluationCache;
import com.chachamaru.harness.cli.guardrail.index.RuleIndex;
import com.chachamaru.harness.shared.dto.GuardrailDecision;
import com.chachamaru.harness.shared.dto.HookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Guardrail engine - evaluates R01-R27 rules and custom rules with performance optimization
 * Integrates with IPC client for async communication with Spring Boot Service
 */
public class GuardrailEngine {
    private static final Logger log = LoggerFactory.getLogger(GuardrailEngine.class);
    private final List<Rule> rules = new ArrayList<>();
    private final IpcClient ipcClient;
    private static final long DEFAULT_IPC_TIMEOUT_MS = 3000; // 3 second timeout for IPC calls

    // Performance optimization components
    private final EvaluationCache cache;
    private final RuleIndex ruleIndex;
    private boolean cachingEnabled = true;

    public GuardrailEngine() {
        this.ipcClient = new HttpIpcClient();
        this.cache = new EvaluationCache();
        this.ruleIndex = new RuleIndex();
        log.info("GuardrailEngine initialized with IPC client and performance optimization");
    }

    public GuardrailEngine(IpcClient ipcClient) {
        this.ipcClient = ipcClient != null ? ipcClient : new HttpIpcClient();
        this.cache = new EvaluationCache();
        this.ruleIndex = new RuleIndex();
        log.info("GuardrailEngine initialized with provided IPC client and performance optimization");
    }

    public void registerRule(Rule rule) {
        rules.add(rule);
        ruleIndex.addRule(rule);
        log.debug("Registered guardrail rule: {}", rule.getId());
    }

    /**
     * Enable or disable caching
     */
    public void setCachingEnabled(boolean enabled) {
        this.cachingEnabled = enabled;
        if (!enabled) {
            cache.clear();
        }
        log.info("Caching {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Clear the evaluation cache
     */
    public void clearCache() {
        cache.clear();
        log.debug("Evaluation cache cleared");
    }

    /**
     * Rebuild rule index (call after bulk rule updates)
     */
    public void rebuildIndex() {
        ruleIndex.rebuild();
        log.debug("Rule index rebuilt");
    }

    /**
     * Load and register custom rules from configuration files
     */
    public void loadCustomRules() {
        log.info("Loading custom guardrail rules");
        CustomRuleLoader loader = new CustomRuleLoader();
        List<Rule> customRules = loader.loadCustomRules();

        for (Rule rule : customRules) {
            registerRule(rule);
        }

        log.info("Loaded and registered {} custom rules", customRules.size());
    }

    /**
     * Evaluate input against all registered rules (synchronous)
     * Returns first deny result, or allow if no rules match/deny
     * Rules are evaluated in priority order (highest first)
     * Uses caching for performance optimization
     */
    public GuardrailResult evaluate(HookInput input) {
        // Check cache first if enabled
        if (cachingEnabled) {
            GuardrailResult cachedResult = cache.get(input);
            if (cachedResult != null) {
                log.debug("Cache hit for input: {}", input.toolName());
                return cachedResult;
            }
        }

        // Get rules for this specific tool type from index
        String toolType = input.toolName();
        List<Rule> toolRules = ruleIndex.getRulesForToolType(toolType);

        // Sort rules by priority (highest first)
        toolRules.sort(Comparator.comparingInt(rule -> {
            if (rule instanceof PrioritizedRule) {
                return -((PrioritizedRule) rule).getPriority(); // Negative for descending order
            }
            return 0; // Default priority
        }));

        // Track if we've seen an override rule
        boolean seenOverride = false;

        for (Rule rule : toolRules) {
            if (rule.matches(input)) {
                log.debug("Rule {} matched input, evaluating", rule.getId());
                GuardrailResult result = rule.evaluate(input);

                // Check if this is an override rule
                if (rule instanceof PrioritizedRule) {
                    PrioritizedRule prioritizedRule = (PrioritizedRule) rule;
                    if (prioritizedRule.isOverride()) {
                        seenOverride = true;
                        // Override rules take precedence
                        if (result.isDenied()) {
                            // Cache the denial result
                            if (cachingEnabled) {
                                cache.put(input, result);
                            }
                            return result;
                        } else {
                            // Override allow rules skip remaining lower priority rules
                            log.debug("Override rule {} allowed, skipping lower priority rules", rule.getId());
                            // Cache the allow result
                            if (cachingEnabled) {
                                cache.put(input, result);
                            }
                            return result;
                        }
                    }
                }

                // Non-override rules only deny if no override rules have been seen
                if (result.isDenied() && !seenOverride) {
                    // Cache the denial result
                    if (cachingEnabled) {
                        cache.put(input, result);
                    }
                    return result; // Early exit on deny
                }
            }
        }

        GuardrailResult result = GuardrailResult.allowed();
        // Cache the allow result
        if (cachingEnabled) {
            cache.put(input, result);
        }
        return result;
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
