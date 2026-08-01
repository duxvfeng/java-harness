package com.chachamaru.harness.protocol.model;

/**
 * Task status enumeration
 * Represents the lifecycle states of a workflow task
 */
public enum Status {
    /**
     * Task is pending execution
     */
    CC_TODO("TODO"),

    /**
     * Task is currently being worked on
     */
    CC_WIP("WIP"),

    /**
     * Task has been completed
     */
    CC_DONE("DONE"),

    /**
     * Task has been blocked
     */
    CC_BLOCKED("BLOCKED"),

    /**
     * Task has been requested
     */
    CC_REQUESTED("REQUESTED"),

    /**
     * Task has been withdrawn
     */
    CC_WITHDRAWN("WITHDRAWN"),

    /**
     * Task has been approved by project manager
     */
    PM_APPROVED("PM_APPROVED"),

    /**
     * Task has been requested from project manager
     */
    PM_REQUESTED("PM_REQUESTED");

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this status represents a completed state
     */
    public boolean isCompleted() {
        return this == CC_DONE || this == PM_APPROVED;
    }

    /**
     * Check if this status represents an active state
     */
    public boolean isActive() {
        return this == CC_WIP;
    }

    /**
     * Check if this status represents a pending state
     */
    public boolean isPending() {
        return this == CC_TODO || this == CC_REQUESTED || this == PM_REQUESTED;
    }

    /**
     * Check if this status represents a withdrawn state
     */
    public boolean isWithdrawn() {
        return this == CC_WITHDRAWN;
    }
}