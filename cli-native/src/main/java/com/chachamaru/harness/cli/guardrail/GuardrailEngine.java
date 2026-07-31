package com.chachamaru.harness.cli.guardrail;

import com.chachamaru.harness.cli.hook.HookInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Guardrail engine - evaluates R01-R15 rules
 */
public class GuardrailEngine {
    private static final Logger log = LoggerFactory.getLogger(GuardrailEngine.class);
    private final List<Rule> rules = new ArrayList<>();

    public GuardrailEngine() {
        // Rules will be registered during initialization
    }

    public void registerRule(Rule rule) {
        rules.add(rule);
        log.debug("Registered guardrail rule: {}", rule.getId());
    }

    /**
     * Evaluate input against all registered rules
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
}
