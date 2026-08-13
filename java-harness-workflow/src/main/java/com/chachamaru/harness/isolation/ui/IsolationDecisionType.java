package com.chachamaru.harness.isolation.ui;

/**
 * Decision type for branch isolation interaction.
 */
public enum IsolationDecisionType {
    ISOLATE("isolate", "Create isolated branch"),
    SKIP("skip", "Skip isolation, continue on current branch"),
    CONTINUE("continue", "Continue with current isolation"),
    RESET("reset", "Reset isolation state"),
    CANCEL("cancel", "Cancel execution");

    private final String value;
    private final String description;

    IsolationDecisionType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return value + " (" + description + ")";
    }
}