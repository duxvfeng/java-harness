package com.chachamaru.harness.collaboration.skill.impl;

/**
 * Review finding severity enumeration.
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public enum ReviewSeverity {
    /** Critical issue that must be fixed */
    CRITICAL,
    /** Major issue that should be fixed */
    MAJOR,
    /** Minor issue or suggestion */
    MINOR,
    /** Informational note */
    INFO
}
