package com.chachamaru.harness.workflow.recovery;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StateRecovery interface contract.
 */
class StateRecoveryTest {

    @Test
    void testInterfaceExists() {
        StateRecovery recovery = new StateRecovery() {
            @Override
            public RecoveryResult attemptRecovery(String sessionId) throws RecoveryException {
                return RecoveryResult.success(sessionId, RecoveryResult.RecoveryPhase.SELF_HEALING, 1);
            }

            @Override
            public RecoveryResult attemptSelfHealing(String sessionId) throws RecoveryException {
                return RecoveryResult.success(sessionId, RecoveryResult.RecoveryPhase.SELF_HEALING, 1);
            }

            @Override
            public RecoveryResult attemptPeerRecovery(String sessionId) throws RecoveryException {
                return RecoveryResult.success(sessionId, RecoveryResult.RecoveryPhase.PEER_RECOVERY, 1);
            }

            @Override
            public RecoveryResult attemptLeadIntervention(String sessionId) throws RecoveryException {
                return RecoveryResult.success(sessionId, RecoveryResult.RecoveryPhase.LEAD_INTERVENTION, 1);
            }

            @Override
            public void markAborted(String sessionId) throws RecoveryException {
            }

            @Override
            public boolean isRecoverable(String sessionId) {
                return true;
            }

            @Override
            public RecoveryResult.RecoveryPhase getCurrentPhase(String sessionId) {
                return RecoveryResult.RecoveryPhase.SELF_HEALING;
            }

            @Override
            public int getRecoveryAttempts(String sessionId) {
                return 1;
            }
        };

        assertNotNull(recovery);
        assertTrue(recovery.isRecoverable("session-1"));
    }

    @Test
    void testFourPhaseMethods() {
        // Test that all 4 phase methods are present and callable
        StateRecovery recovery = new StateRecovery() {
            @Override
            public RecoveryResult attemptRecovery(String sessionId) throws RecoveryException {
                return null;
            }

            @Override
            public RecoveryResult attemptSelfHealing(String sessionId) throws RecoveryException {
                return RecoveryResult.success(sessionId, RecoveryResult.RecoveryPhase.SELF_HEALING, 1);
            }

            @Override
            public RecoveryResult attemptPeerRecovery(String sessionId) throws RecoveryException {
                return RecoveryResult.success(sessionId, RecoveryResult.RecoveryPhase.PEER_RECOVERY, 1);
            }

            @Override
            public RecoveryResult attemptLeadIntervention(String sessionId) throws RecoveryException {
                return RecoveryResult.success(sessionId, RecoveryResult.RecoveryPhase.LEAD_INTERVENTION, 1);
            }

            @Override
            public void markAborted(String sessionId) throws RecoveryException {
            }

            @Override
            public boolean isRecoverable(String sessionId) {
                return true;
            }

            @Override
            public RecoveryResult.RecoveryPhase getCurrentPhase(String sessionId) {
                return null;
            }

            @Override
            public int getRecoveryAttempts(String sessionId) {
                return 0;
            }
        };

        assertDoesNotThrow(() -> recovery.attemptSelfHealing("session-1"));
        assertDoesNotThrow(() -> recovery.attemptPeerRecovery("session-1"));
        assertDoesNotThrow(() -> recovery.attemptLeadIntervention("session-1"));
        assertDoesNotThrow(() -> recovery.markAborted("session-1"));
    }

    @Test
    void testRecoveryException() {
        var ex = new StateRecovery.RecoveryException("Recovery failed", "session-1");

        assertEquals("Recovery failed", ex.getMessage());
        assertEquals("session-1", ex.getSessionId());
    }

    @Test
    void testRecoveryException_WithCause() {
        Throwable cause = new RuntimeException("Inner error");
        var ex = new StateRecovery.RecoveryException("Recovery failed", cause, "session-1");

        assertEquals(cause, ex.getCause());
        assertEquals("session-1", ex.getSessionId());
    }

    @Test
    void testMethodSignatures() {
        // Verify method signatures match the interface contract
        assertDoesNotThrow(() -> {
            StateRecovery recovery = new StateRecovery() {
                @Override
                public RecoveryResult attemptRecovery(String sessionId) {
                    return RecoveryResult.success(sessionId, RecoveryResult.RecoveryPhase.SELF_HEALING, 1);
                }

                @Override
                public RecoveryResult attemptSelfHealing(String sessionId) {
                    return RecoveryResult.success(sessionId, RecoveryResult.RecoveryPhase.SELF_HEALING, 1);
                }

                @Override
                public RecoveryResult attemptPeerRecovery(String sessionId) {
                    return RecoveryResult.success(sessionId, RecoveryResult.RecoveryPhase.PEER_RECOVERY, 1);
                }

                @Override
                public RecoveryResult attemptLeadIntervention(String sessionId) {
                    return RecoveryResult.success(sessionId, RecoveryResult.RecoveryPhase.LEAD_INTERVENTION, 1);
                }

                @Override
                public void markAborted(String sessionId) {
                }

                @Override
                public boolean isRecoverable(String sessionId) {
                    return true;
                }

                @Override
                public RecoveryResult.RecoveryPhase getCurrentPhase(String sessionId) {
                    return RecoveryResult.RecoveryPhase.SELF_HEALING;
                }

                @Override
                public int getRecoveryAttempts(String sessionId) {
                    return 1;
                }
            };

            // Test all methods are callable
            recovery.attemptRecovery("s1");
            recovery.attemptSelfHealing("s2");
            recovery.attemptPeerRecovery("s3");
            recovery.attemptLeadIntervention("s4");
            recovery.markAborted("s5");
            assertTrue(recovery.isRecoverable("s6"));
            recovery.getCurrentPhase("s7");
            recovery.getRecoveryAttempts("s8");
        });
    }
}
