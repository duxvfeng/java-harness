package com.chachamaru.harness.workflow.recovery;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RecoveryResult model.
 */
class RecoveryResultTest {

    @Test
    void testCreation() {
        var result = new RecoveryResult(
            "session-1",
            RecoveryResult.RecoveryStatus.SUCCESS,
            RecoveryResult.RecoveryPhase.SELF_HEALING,
            "Recovered successfully",
            LocalDateTime.now(),
            1,
            RecoveryResult.RecoveryAction.NONE
        );

        assertEquals("session-1", result.sessionId());
        assertEquals(RecoveryResult.RecoveryStatus.SUCCESS, result.status());
        assertEquals(RecoveryResult.RecoveryPhase.SELF_HEALING, result.phase());
        assertEquals(1, result.attemptsMade());
    }

    @Test
    void testValidation_NullSessionId() {
        assertThrows(IllegalArgumentException.class, () ->
            new RecoveryResult(null, RecoveryResult.RecoveryStatus.SUCCESS, RecoveryResult.RecoveryPhase.SELF_HEALING, "msg", LocalDateTime.now(), 1, RecoveryResult.RecoveryAction.NONE)
        );
    }

    @Test
    void testValidation_NegativeAttempts() {
        assertThrows(IllegalArgumentException.class, () ->
            new RecoveryResult("session-1", RecoveryResult.RecoveryStatus.SUCCESS, RecoveryResult.RecoveryPhase.SELF_HEALING, "msg", LocalDateTime.now(), -1, RecoveryResult.RecoveryAction.NONE)
        );
    }

    @Test
    void testDefaults() {
        var result = new RecoveryResult(
            "session-1",
            null,
            null,
            "msg",
            LocalDateTime.now(),
            0,
            null
        );

        assertEquals(RecoveryResult.RecoveryStatus.FAILED, result.status());
        assertEquals(RecoveryResult.RecoveryPhase.SELF_HEALING, result.phase());
        assertEquals(RecoveryResult.RecoveryAction.ESCALATE, result.nextAction());
    }

    @Test
    void testSuccessFactory() {
        var result = RecoveryResult.success("session-1", RecoveryResult.RecoveryPhase.PEER_RECOVERY, 2);

        assertTrue(result.isSuccess());
        assertEquals(RecoveryResult.RecoveryStatus.SUCCESS, result.status());
        assertEquals(RecoveryResult.RecoveryPhase.PEER_RECOVERY, result.phase());
        assertEquals(2, result.attemptsMade());
        assertEquals(RecoveryResult.RecoveryAction.NONE, result.nextAction());
    }

    @Test
    void testFailureFactory() {
        var result = RecoveryResult.failure("session-1", RecoveryResult.RecoveryPhase.SELF_HEALING, "Timeout", 1);

        assertFalse(result.isSuccess());
        assertEquals(RecoveryResult.RecoveryStatus.FAILED, result.status());
        assertEquals("Timeout", result.message());
        assertTrue(result.shouldEscalate());
    }

    @Test
    void testAbortedFactory() {
        var result = RecoveryResult.aborted("session-1", "Beyond recovery");

        assertEquals(RecoveryResult.RecoveryStatus.ABORTED, result.status());
        assertEquals(RecoveryResult.RecoveryPhase.ABORT, result.phase());
        assertEquals(0, result.attemptsMade());
    }

    @Test
    void testShouldRetry() {
        var result = new RecoveryResult(
            "session-1",
            RecoveryResult.RecoveryStatus.FAILED,
            RecoveryResult.RecoveryPhase.SELF_HEALING,
            "msg",
            LocalDateTime.now(),
            1,
            RecoveryResult.RecoveryAction.RETRY
        );

        assertTrue(result.shouldRetry());
    }

    @Test
    void testShouldEscalate() {
        var result = new RecoveryResult(
            "session-1",
            RecoveryResult.RecoveryStatus.FAILED,
            RecoveryResult.RecoveryPhase.SELF_HEALING,
            "msg",
            LocalDateTime.now(),
            3,
            RecoveryResult.RecoveryAction.ESCALATE
        );

        assertTrue(result.shouldEscalate());
    }

    @Test
    void testEnums() {
        assertEquals(4, RecoveryResult.RecoveryStatus.values().length);
        assertEquals(4, RecoveryResult.RecoveryPhase.values().length);
        assertEquals(5, RecoveryResult.RecoveryAction.values().length);

        assertTrue(List.of(RecoveryResult.RecoveryStatus.values()).contains(RecoveryResult.RecoveryStatus.SUCCESS));
        assertTrue(List.of(RecoveryResult.RecoveryStatus.values()).contains(RecoveryResult.RecoveryStatus.FAILED));
        assertTrue(List.of(RecoveryResult.RecoveryStatus.values()).contains(RecoveryResult.RecoveryStatus.IN_PROGRESS));
        assertTrue(List.of(RecoveryResult.RecoveryStatus.values()).contains(RecoveryResult.RecoveryStatus.ABORTED));

        assertTrue(List.of(RecoveryResult.RecoveryPhase.values()).contains(RecoveryResult.RecoveryPhase.SELF_HEALING));
        assertTrue(List.of(RecoveryResult.RecoveryPhase.values()).contains(RecoveryResult.RecoveryPhase.PEER_RECOVERY));
        assertTrue(List.of(RecoveryResult.RecoveryPhase.values()).contains(RecoveryResult.RecoveryPhase.LEAD_INTERVENTION));
        assertTrue(List.of(RecoveryResult.RecoveryPhase.values()).contains(RecoveryResult.RecoveryPhase.ABORT));
    }
}
