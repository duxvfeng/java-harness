package com.chachamaru.harness.workflow.model;

/**
 * Task status in the workflow lifecycle.
 *
 * <p>Represents the state of a task from project management request to completion or withdrawal.
 * Status transitions follow the workflow: PM_REQUESTED → PM_APPROVED → CC_TODO → CC_WIP → CC_DONE.</p>
 *
 * @spec_reference spec.md#Data Models
 */
public enum Status {
    /** Requested by project management, not yet approved */
    PM_REQUESTED("pm-requested"),

    /** Approved by project management, ready to implement */
    PM_APPROVED("pm-approved"),

    /** TODO in Claude Code, not yet started */
    CC_TODO("cc:TODO"),

    /** Work in progress in Claude Code */
    CC_WIP("cc:WIP"),

    /** Completed in Claude Code */
    CC_DONE("cc:DONE"),

    /** Withdrawn or cancelled */
    CC_WITHDRAWN("cc:WITHDRAWN");

    private final String marker;

    Status(String marker) {
        this.marker = marker;
    }

    /**
     * Gets the Plans.md marker representation.
     */
    public String getMarker() {
        return marker;
    }

    /**
     * Checks if this is a completed status.
     */
    public boolean isCompleted() {
        return this == CC_DONE;
    }

    /**
     * Checks if this is an active status.
     */
    public boolean isActive() {
        return this == CC_TODO || this == CC_WIP;
    }

    /**
     * Parses a marker string to Status.
     */
    public static Status fromMarker(String marker) {
        for (Status status : values()) {
            if (status.marker.equals(marker)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status marker: " + marker);
    }
}
