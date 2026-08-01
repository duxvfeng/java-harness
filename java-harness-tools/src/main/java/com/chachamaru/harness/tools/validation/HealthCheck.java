package com.chachamaru.harness.tools.validation;

import java.util.*;

/**
 * Health check interface for system components.
 *
 * <p>Defines the contract for health checks that can be performed
 * on various components of the Java Harness system.</p>
 *
 * @spec_reference spec.md#Health Check System
 */
public interface HealthCheck {

    /**
     * Performs a health check on this component.
     *
     * @return Health check result
     */
    HealthCheckResult check();

    /**
     * Gets the name of this health check.
     *
     * @return Health check name
     */
    String getName();

    /**
     * Gets the description of what this health check verifies.
     *
     * @return Description
     */
    String getDescription();

    /**
     * Checks if this health check is enabled.
     *
     * @return true if enabled
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Result of a health check operation.
     */
    record HealthCheckResult(
        String name,
        HealthStatus status,
        String message,
        Map<String, Object> details,
        long checkDurationMs
    ) {
        public HealthCheckResult {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name cannot be null or blank");
            }
            if (status == null) {
                status = HealthStatus.UNKNOWN;
            }
            if (message == null) {
                message = "";
            }
            if (details == null) {
                details = Map.of();
            }
            if (checkDurationMs < 0) {
                throw new IllegalArgumentException("checkDurationMs cannot be negative");
            }
        }

        /**
         * Creates a healthy result.
         */
        public static HealthCheckResult healthy(String name, String message) {
            return new HealthCheckResult(
                name,
                HealthStatus.HEALTHY,
                message,
                Map.of(),
                0L
            );
        }

        /**
         * Creates a degraded result.
         */
        public static HealthCheckResult degraded(String name, String message) {
            return new HealthCheckResult(
                name,
                HealthStatus.DEGRADED,
                message,
                Map.of(),
                0L
            );
        }

        /**
         * Creates an unhealthy result.
         */
        public static HealthCheckResult unhealthy(String name, String message) {
            return new HealthCheckResult(
                name,
                HealthStatus.UNHEALTHY,
                message,
                Map.of(),
                0L
            );
        }

        /**
         * Checks if the component is healthy.
         */
        public boolean isHealthy() {
            return status == HealthStatus.HEALTHY;
        }

        /**
         * Checks if the component is degraded or unhealthy.
         */
        public boolean hasIssues() {
            return status == HealthStatus.DEGRADED || status == HealthStatus.UNHEALTHY;
        }
    }

    /**
     * Health status enumeration.
     */
    public enum HealthStatus {
        /** Component is functioning normally */
        HEALTHY,

        /** Component is functioning but with reduced capacity */
        DEGRADED,

        /** Component is not functioning properly */
        UNHEALTHY,

        /** Component status is unknown */
        UNKNOWN
    }
}
