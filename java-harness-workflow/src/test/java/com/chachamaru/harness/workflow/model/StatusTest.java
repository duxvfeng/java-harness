package com.chachamaru.harness.workflow.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Status enum.
 */
class StatusTest {

    @Test
    void testMarkerValues() {
        assertEquals("pm-requested", Status.PM_REQUESTED.getMarker());
        assertEquals("pm-approved", Status.PM_APPROVED.getMarker());
        assertEquals("cc:TODO", Status.CC_TODO.getMarker());
        assertEquals("cc:WIP", Status.CC_WIP.getMarker());
        assertEquals("cc:DONE", Status.CC_DONE.getMarker());
        assertEquals("cc:WITHDRAWN", Status.CC_WITHDRAWN.getMarker());
    }

    @Test
    void testIsCompleted() {
        assertTrue(Status.CC_DONE.isCompleted());
        assertFalse(Status.CC_TODO.isCompleted());
        assertFalse(Status.CC_WIP.isCompleted());
        assertFalse(Status.PM_REQUESTED.isCompleted());
    }

    @Test
    void testIsActive() {
        assertTrue(Status.CC_TODO.isActive());
        assertTrue(Status.CC_WIP.isActive());
        assertFalse(Status.CC_DONE.isActive());
        assertFalse(Status.PM_APPROVED.isActive());
    }

    @Test
    void testFromMarker() {
        assertEquals(Status.CC_TODO, Status.fromMarker("cc:TODO"));
        assertEquals(Status.CC_WIP, Status.fromMarker("cc:WIP"));
        assertEquals(Status.CC_DONE, Status.fromMarker("cc:DONE"));
    }

    @Test
    void testFromMarkerInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Status.fromMarker("invalid"));
    }
}
