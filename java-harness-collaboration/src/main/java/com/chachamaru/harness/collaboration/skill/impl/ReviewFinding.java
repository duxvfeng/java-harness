package com.chachamaru.harness.collaboration.skill.impl;

/**
 * A single review finding.
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public record ReviewFinding(
    ReviewSeverity severity,
    String title,
    String description
) {
}
