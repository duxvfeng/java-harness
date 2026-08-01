package com.chachamaru.harness.workflow.recovery;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RecoveryStrategy interface contract.
 */
class RecoveryStrategyTest {

    @Test
    void testInterfaceContract() {
        RecoveryStrategy strategy = new RecoveryStrategy() {
            @Override
            public RecoveryResult.RecoveryPhase getPhase() {
                return RecoveryResult.RecoveryPhase.SELF_HEALING;
            }

            @Override
            public int getMaxAttempts() {
                return 3;
            }

            @Override
            public boolean canHandle(String errorType) {
                return "TimeoutException".equals(errorType);
            }

            @Override
            public RecoveryResult recover(String sessionId, RecoveryContext context) throws RecoveryException {
                return RecoveryResult.success(sessionId, getPhase(), 1);
            }
        };

        assertEquals(RecoveryResult.RecoveryPhase.SELF_HEALING, strategy.getPhase());
        assertEquals(3, strategy.getMaxAttempts());
        assertTrue(strategy.canHandle("TimeoutException"));
        assertFalse(strategy.canHandle("OtherException"));
    }

    @Test
    void testDefaultCanHandle() {
        RecoveryStrategy strategy = new RecoveryStrategy() {
            @Override
            public RecoveryResult.RecoveryPhase getPhase() {
                return RecoveryResult.RecoveryPhase.SELF_HEALING;
            }

            @Override
            public int getMaxAttempts() {
                return 1;
            }

            @Override
            public boolean canHandle(String errorType) {
                return true;
            }

            @Override
            public RecoveryResult recover(String sessionId, RecoveryContext context) {
                return RecoveryResult.success(sessionId, getPhase(), 1);
            }
        };

        RuntimeException error = new RuntimeException("Test error");
        assertTrue(strategy.canHandle(error));
    }

    @Test
    void testRecoveryContextCreation() {
        Throwable error = new RuntimeException("Test error");
        var context = new RecoveryStrategy.RecoveryContext(
            "session-1",
            error,
            "RuntimeException",
            RecoveryResult.RecoveryPhase.SELF_HEALING,
            0,
            java.time.LocalDateTime.now(),
            null
        );

        assertEquals("session-1", context.sessionId());
        assertEquals(error, context.error());
        assertEquals("RuntimeException", context.errorType());
        assertEquals(0, context.previousAttempts());
    }

    @Test
    void testRecoveryContextDefaults() {
        var context = new RecoveryStrategy.RecoveryContext(
            "session-1",
            new RuntimeException("Test"),
            null,  // Will default to error class name
            RecoveryResult.RecoveryPhase.SELF_HEALING,
            1,
            null,  // Will default to now
            null
        );

        assertEquals("RuntimeException", context.errorType());
        assertNotNull(context.errorTime());
    }

    @Test
    void testRecoveryContextValidation_NullSessionId() {
        assertThrows(IllegalArgumentException.class, () ->
            new RecoveryStrategy.RecoveryContext(
                null,
                new RuntimeException("Test"),
                "RuntimeException",
                RecoveryResult.RecoveryPhase.SELF_HEALING,
                0,
                java.time.LocalDateTime.now(),
                null
            )
        );
    }

    @Test
    void testRecoveryException() {
        var ex = new RecoveryStrategy.RecoveryException("Recovery failed", "session-1", RecoveryResult.RecoveryPhase.PEER_RECOVERY);

        assertEquals("Recovery failed", ex.getMessage());
        assertEquals("session-1", ex.getSessionId());
        assertEquals(RecoveryResult.RecoveryPhase.PEER_RECOVERY, ex.getPhase());
    }

    @Test
    void testRecoveryException_WithCause() {
        Throwable cause = new RuntimeException("Inner error");
        var ex = new RecoveryStrategy.RecoveryException("Recovery failed", cause, "session-1", RecoveryResult.RecoveryPhase.SELF_HEALING);

        assertEquals(cause, ex.getCause());
        assertEquals("session-1", ex.getSessionId());
    }
}
