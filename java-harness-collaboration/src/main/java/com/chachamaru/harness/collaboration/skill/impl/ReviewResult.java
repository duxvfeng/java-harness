package com.chachamaru.harness.collaboration.skill.impl;

import java.util.List;

/**
 * Result of a review.
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public record ReviewResult(
    ReviewStatus status,
    List<ReviewFinding> findings,
    String summary
) {
    /**
     * Checks if review is approved.
     */
    public boolean isApproved() {
        return status == ReviewStatus.APPROVED || status == ReviewStatus.APPROVED_WITH_SUGGESTIONS;
    }

    /**
     * Gets critical findings count.
     */
    public long getCriticalCount() {
        return findings.stream().filter(f -> f.severity() == ReviewSeverity.CRITICAL).count();
    }

    /**
     * Gets major findings count.
     */
    public long getMajorCount() {
        return findings.stream().filter(f -> f.severity() == ReviewSeverity.MAJOR).count();
    }
}
