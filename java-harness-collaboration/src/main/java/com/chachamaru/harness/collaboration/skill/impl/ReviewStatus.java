package com.chachamaru.harness.collaboration.skill.impl;

/**
 * Review status enumeration.
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public enum ReviewStatus {
    /** Review approved */
    APPROVED,
    /** Review approved with suggestions */
    APPROVED_WITH_SUGGESTIONS,
    /** Review requested changes */
    REQUEST_CHANGES,
    /** Review failed */
    FAILED
}
