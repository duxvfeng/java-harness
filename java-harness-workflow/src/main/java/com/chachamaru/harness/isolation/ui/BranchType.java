package com.chachamaru.harness.isolation.ui;

/**
 * Branch type enumeration for isolation decisions.
 */
public enum BranchType {
    MAIN("main", "Main/master branch"),
    FEATURE("feature", "Feature branch"),
    WORKTREE("worktree", "Git worktree");

    private final String value;
    private final String description;

    BranchType(String value, String description) {
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