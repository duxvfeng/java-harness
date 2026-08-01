package com.chachamaru.harness.cli.guardrail.config;

import java.util.List;
import java.util.Map;

/**
 * Configuration model for custom guardrail rules
 */
public class CustomRuleConfig {

    private String id;
    private String name;
    private String description;
    private String toolType; // "Bash", "Write", "Edit", "Read", "All"
    private int priority = 0; // Default priority, higher = higher priority
    private List<ConditionConfig> conditions;
    private ActionConfig action;

    public static class ConditionConfig {
        private String type; // "command_contains", "path_contains", "path_ends_with", "path_equals"
        private String value;
        private boolean caseSensitive = false;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public boolean isCaseSensitive() { return caseSensitive; }
        public void setCaseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; }
    }

    public static class ActionConfig {
        private String decision; // "deny" or "allow"
        private String message;

        public String getDecision() { return decision; }
        public void setDecision(String decision) { this.decision = decision; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getToolType() { return toolType; }
    public void setToolType(String toolType) { this.toolType = toolType; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public List<ConditionConfig> getConditions() { return conditions; }
    public void setConditions(List<ConditionConfig> conditions) { this.conditions = conditions; }

    public ActionConfig getAction() { return action; }
    public void setAction(ActionConfig action) { this.action = action; }
}