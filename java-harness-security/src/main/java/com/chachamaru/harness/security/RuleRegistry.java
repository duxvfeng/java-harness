package com.chachamaru.harness.security;

import com.chachamaru.harness.foundation.dto.GuardrailResult;
import com.chachamaru.harness.protocol.HookEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing and evaluating guardrail rules.
 *
 * <p>This implementation maintains a prioritized list of rules and provides
 * both synchronous and asynchronous evaluation methods.</p>
 *
 * @since 4.1.0
 */
public class RuleRegistry implements GuardrailEngine {

    private static final Logger logger = LoggerFactory.getLogger(RuleRegistry.class);
    private final Map<String, GuardrailRule> rules = new ConcurrentHashMap<>();

    @Override
    public void registerRule(GuardrailRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Rule cannot be null");
        }
        if (rule.getId() == null || rule.getId().isBlank()) {
            throw new IllegalArgumentException("Rule ID cannot be null or blank");
        }
        rules.put(rule.getId(), rule);
        logger.info("Registered guardrail rule: {} - {}", rule.getId(), rule.getDescription());
    }

    @Override
    public boolean unregisterRule(String ruleId) {
        if (ruleId == null) {
            return false;
        }
        GuardrailRule removed = rules.remove(ruleId);
        if (removed != null) {
            logger.info("Unregistered guardrail rule: {}", ruleId);
            return true;
        }
        return false;
    }

    @Override
    public GuardrailResult evaluate(HookEventType eventType, String toolName, Map<String, Object> toolInput) {
        List<GuardrailRule> applicableRules = getApplicableRules(eventType);

        logger.debug("Evaluating {} rules for event: {}, tool: {}", applicableRules.size(), eventType, toolName);

        for (GuardrailRule rule : applicableRules) {
            try {
                GuardrailResult result = rule.evaluate(eventType, toolName, toolInput);
                if (result.isBlocked()) {
                    logger.info("Rule {} denied tool '{}' for event {}: {}",
                            rule.getId(), toolName, eventType, result.reason());
                    return result;
                }
            } catch (Exception e) {
                logger.error("Error evaluating rule {}: {}", rule.getId(), e.getMessage(), e);
                // On error, default to allow to prevent breaking the workflow
            }
        }

        logger.debug("All rules allowed for tool '{}' on event {}", toolName, eventType);
        return GuardrailResult.allow("registry");
    }

    @Override
    public CompletableFuture<GuardrailResult> evaluateAsync(HookEventType eventType, String toolName, Map<String, Object> toolInput) {
        return CompletableFuture.supplyAsync(() -> evaluate(eventType, toolName, toolInput));
    }

    @Override
    public int getRuleCount() {
        return rules.size();
    }

    @Override
    public void clearRules() {
        rules.clear();
        logger.info("Cleared all guardrail rules");
    }

    @Override
    public void shutdown() {
        clearRules();
        logger.info("RuleRegistry shut down");
    }

    /**
     * Gets applicable rules for the given event type, sorted by priority (highest first).
     */
    private List<GuardrailRule> getApplicableRules(HookEventType eventType) {
        return rules.values().stream()
                .filter(rule -> rule.isEnabled() && rule.matches(eventType))
                .sorted(Comparator.comparingInt(GuardrailRule::getPriority).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Gets all registered rules (for testing/inspection).
     */
    protected List<GuardrailRule> getAllRules() {
        return new ArrayList<>(rules.values());
    }
}
