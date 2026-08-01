package com.chachamaru.harness.collaboration.agent.message;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result from code review.
 *
 * <p>Message returned by Reviewer agent containing review findings,
 * verdict (APPROVE/REQUEST_CHANGES), and severity classification.</p>
 *
 * @spec_reference spec.md#Breezing Mode Reviewer Protocol
 * @since 4.2.0
 */
public record ReviewResultV1(
    String requestId,
    String verdict,
    List<ReviewFinding> findings,
    String summary,
    int criticalCount,
    int majorCount,
    Map<String, Object> metadata,
    Instant timestamp
) {
    /**
     * Creates a review result.
     */
    public ReviewResultV1 {
        if (verdict == null) {
            verdict = Verdict.REQUEST_CHANGES.name();
        }
        if (findings == null) {
            findings = List.of();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        // Recalculate critical and major counts from findings
        int tempCriticalCount = 0;
        int tempMajorCount = 0;
        for (ReviewFinding finding : findings) {
            try {
                ReviewSeverity severity = ReviewSeverity.valueOf(finding.severity());
                if (severity == ReviewSeverity.CRITICAL) {
                    tempCriticalCount++;
                } else if (severity == ReviewSeverity.MAJOR) {
                    tempMajorCount++;
                }
            } catch (IllegalArgumentException e) {
                // Invalid severity, skip
            }
        }
        criticalCount = tempCriticalCount;
        majorCount = tempMajorCount;
    }

    /**
     * Creates an APPROVE result.
     */
    public static ReviewResultV1 approve(String requestId, List<ReviewFinding> findings, String summary) {
        return new ReviewResultV1(
            requestId,
            Verdict.APPROVE.name(),
            findings,
            summary,
            0,
            0,
            Map.of("verdictReason", "All critical and major issues resolved"),
            Instant.now()
        );
    }

    /**
     * Creates a REQUEST_CHANGES result.
     */
    public static ReviewResultV1 requestChanges(String requestId, List<ReviewFinding> findings, String summary) {
        long criticalCount = 0;
        long majorCount = 0;

        for (ReviewFinding finding : findings) {
            try {
                ReviewSeverity severity = ReviewSeverity.valueOf(finding.severity());
                if (severity == ReviewSeverity.CRITICAL) {
                    criticalCount++;
                } else if (severity == ReviewSeverity.MAJOR) {
                    majorCount++;
                }
            } catch (IllegalArgumentException e) {
                // Invalid severity, skip
            }
        }

        return new ReviewResultV1(
            requestId,
            Verdict.REQUEST_CHANGES.name(),
            findings,
            summary,
            (int) criticalCount,
            (int) majorCount,
            Map.of("verdictReason", "Found " + criticalCount + " critical and " + majorCount + " major issues"),
            Instant.now()
        );
    }

    /**
     * Gets the verdict as enum.
     */
    public Verdict getVerdictEnum() {
        try {
            return Verdict.valueOf(verdict);
        } catch (IllegalArgumentException e) {
            return Verdict.REQUEST_CHANGES; // Default
        }
    }

    /**
     * Checks if this is an APPROVE verdict.
     */
    public boolean isApproved() {
        return getVerdictEnum() == Verdict.APPROVE;
    }

    /**
     * Checks if this is a REQUEST_CHANGES verdict.
     */
    public boolean isRequestChanges() {
        return getVerdictEnum() == Verdict.REQUEST_CHANGES;
    }

    /**
     * Checks if there are any critical issues.
     */
    public boolean hasCriticalIssues() {
        return criticalCount > 0;
    }

    /**
     * Checks if there are any major issues.
     */
    public boolean hasMajorIssues() {
        return majorCount > 0;
    }

    /**
     * Verdict enumeration.
     */
    public enum Verdict {
        /** Changes are approved */
        APPROVE,
        /** Changes need revision */
        REQUEST_CHANGES
    }

    /**
     * Review finding record.
     */
    public record ReviewFinding(
        String category,
        String severity,
        String title,
        String description,
        String file,
        int line,
        String failureScenario
    ) {
        public ReviewFinding {
            if (category == null) category = "general";
            if (severity == null) severity = "MINOR";
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("title cannot be null or blank");
            }
        }

        /**
         * Gets the severity as enum.
         */
        public ReviewSeverity getSeverityEnum() {
            try {
                return ReviewSeverity.valueOf(severity);
            } catch (IllegalArgumentException e) {
                return ReviewSeverity.MINOR; // Default
            }
        }
    }

    /**
     * Review severity enumeration.
     */
    public enum ReviewSeverity {
        CRITICAL,
        MAJOR,
        MINOR,
        INFO
    }
}