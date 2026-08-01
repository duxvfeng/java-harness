package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.guardrail.PrioritizedRule;
import com.chachamaru.harness.cli.guardrail.config.CustomRuleConfig;
import com.chachamaru.harness.cli.hook.HookInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Dynamic rule implementation based on custom configuration
 */
public class DynamicRule implements PrioritizedRule {
    private static final Logger log = LoggerFactory.getLogger(DynamicRule.class);

    private final CustomRuleConfig config;

    public DynamicRule(CustomRuleConfig config) {
        this.config = config;
        log.info("Created dynamic rule: {} with priority: {}", config.getId(), config.getPriority());
    }

    @Override
    public String getId() {
        return config.getId();
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public int getPriority() {
        return config.getPriority();
    }

    @Override
    public boolean matches(HookInput input) {
        String toolType = config.getToolType();
        if ("All".equalsIgnoreCase(toolType)) {
            return true;
        }
        return toolType.equalsIgnoreCase(input.toolName());
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        List<CustomRuleConfig.ConditionConfig> conditions = config.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return GuardrailResult.allowed();
        }

        boolean allConditionsMatch = true;
        for (CustomRuleConfig.ConditionConfig condition : conditions) {
            if (!evaluateCondition(condition, input)) {
                allConditionsMatch = false;
                break;
            }
        }

        if (allConditionsMatch) {
            CustomRuleConfig.ActionConfig action = config.getAction();
            if ("deny".equalsIgnoreCase(action.getDecision())) {
                return GuardrailResult.denied(config.getId(), action.getMessage());
            } else {
                return GuardrailResult.allowed(action.getMessage());
            }
        }

        return GuardrailResult.allowed();
    }

    private boolean evaluateCondition(CustomRuleConfig.ConditionConfig condition, HookInput input) {
        String type = condition.getType();
        String value = condition.getValue();
        boolean caseSensitive = condition.isCaseSensitive();

        switch (type) {
            case "command_contains":
                return evaluateCommandContains(value, input, caseSensitive);
            case "path_contains":
                return evaluatePathContains(value, input, caseSensitive);
            case "path_ends_with":
                return evaluatePathEndsWith(value, input, caseSensitive);
            case "path_equals":
                return evaluatePathEquals(value, input, caseSensitive);
            case "tool_equals":
                return evaluateToolEquals(value, input);
            default:
                log.warn("Unknown condition type: {}", type);
                return false;
        }
    }

    private boolean evaluateCommandContains(String value, HookInput input, boolean caseSensitive) {
        Object commandObj = input.toolInput().get("command");
        if (!(commandObj instanceof String)) {
            return false;
        }
        String command = (String) commandObj;
        String compareCommand = caseSensitive ? command : command.toLowerCase();
        String compareValue = caseSensitive ? value : value.toLowerCase();
        return compareCommand.contains(compareValue);
    }

    private boolean evaluatePathContains(String value, HookInput input, boolean caseSensitive) {
        Object pathObj = input.toolInput().get("file_path");
        if (!(pathObj instanceof String)) {
            return false;
        }
        String path = (String) pathObj;
        String comparePath = caseSensitive ? path : path.toLowerCase();
        String compareValue = caseSensitive ? value : value.toLowerCase();
        return comparePath.contains(compareValue);
    }

    private boolean evaluatePathEndsWith(String value, HookInput input, boolean caseSensitive) {
        Object pathObj = input.toolInput().get("file_path");
        if (!(pathObj instanceof String)) {
            return false;
        }
        String path = (String) pathObj;
        String comparePath = caseSensitive ? path : path.toLowerCase();
        String compareValue = caseSensitive ? value : value.toLowerCase();
        return comparePath.endsWith(compareValue);
    }

    private boolean evaluatePathEquals(String value, HookInput input, boolean caseSensitive) {
        Object pathObj = input.toolInput().get("file_path");
        if (!(pathObj instanceof String)) {
            return false;
        }
        String path = (String) pathObj;
        if (caseSensitive) {
            return path.equals(value);
        }
        return path.equalsIgnoreCase(value);
    }

    private boolean evaluateToolEquals(String value, HookInput input) {
        return value.equalsIgnoreCase(input.toolName());
    }
}