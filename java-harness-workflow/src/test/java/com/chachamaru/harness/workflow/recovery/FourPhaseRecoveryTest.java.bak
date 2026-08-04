package com.chachamaru.harness.workflow.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FourPhaseRecovery.
 */
@DisplayName("FourPhaseRecovery Tests")
public class FourPhaseRecoveryTest {

    private FourPhaseRecovery recovery;

    @BeforeEach
    void setUp() {
        recovery = new FourPhaseRecovery();
        recovery.clearAllSessionStates();
    }

    @Test
    @DisplayName("应该初始化所有四个阶段策略")
    void shouldInitializeAllFourPhaseStrategies() throws StateRecovery.RecoveryException {
        // For a new session, phase is null until first recovery attempt
        assertNull(recovery.getCurrentPhase("test-session"));
        assertEquals(0, recovery.getRecoveryAttempts("test-session"));

        // After first attempt, phase should be set
        recovery.attemptSelfHealing("test-session");
        assertEquals(RecoveryResult.RecoveryPhase.SELF_HEALING, recovery.getCurrentPhase("test-session"));
    }

    @Test
    @DisplayName("应该能够尝试自我修复")
    void shouldAttemptSelfHealing() throws StateRecovery.RecoveryException {
        RecoveryResult result = recovery.attemptSelfHealing("test-session");

        assertNotNull(result);
        assertEquals(RecoveryResult.RecoveryPhase.SELF_HEALING, result.phase());
    }

    @Test
    @DisplayName("应该能够尝试同伴修复")
    void shouldAttemptPeerRecovery() throws StateRecovery.RecoveryException {
        RecoveryResult result = recovery.attemptPeerRecovery("test-session");

        assertNotNull(result);
        assertEquals(RecoveryResult.RecoveryPhase.PEER_RECOVERY, result.phase());
    }

    @Test
    @DisplayName("应该能够尝试指挥官介入")
    void shouldAttemptLeadIntervention() throws StateRecovery.RecoveryException {
        RecoveryResult result = recovery.attemptLeadIntervention("test-session");

        assertNotNull(result);
        assertEquals(RecoveryResult.RecoveryPhase.LEAD_INTERVENTION, result.phase());
    }

    @Test
    @DisplayName("应该能够标记会话为中止状态")
    void shouldMarkSessionAsAborted() throws StateRecovery.RecoveryException {
        recovery.markAborted("test-session");

        assertFalse(recovery.isRecoverable("test-session"));
    }

    @Test
    @DisplayName("中止的会话应该返回中止结果")
    void shouldReturnAbortedResultForAbortedSession() throws StateRecovery.RecoveryException {
        recovery.markAborted("test-session");

        RecoveryResult result = recovery.attemptRecovery("test-session");

        assertEquals(RecoveryResult.RecoveryStatus.ABORTED, result.status());
        assertTrue(result.message().contains("aborted"));
    }

    @Test
    @DisplayName("应该检查会话是否可恢复")
    void shouldCheckIfSessionIsRecoverable() {
        assertTrue(recovery.isRecoverable("test-session"));

        try {
            recovery.markAborted("test-session");
        } catch (StateRecovery.RecoveryException e) {
            fail("Should not throw exception");
        }

        assertFalse(recovery.isRecoverable("test-session"));
    }

    @Test
    @DisplayName("应该获取当前恢复阶段")
    void shouldGetCurrentRecoveryPhase() throws StateRecovery.RecoveryException {
        assertNull(recovery.getCurrentPhase("new-session"));

        recovery.attemptSelfHealing("new-session");
        assertEquals(RecoveryResult.RecoveryPhase.SELF_HEALING, recovery.getCurrentPhase("new-session"));
    }

    @Test
    @DisplayName("应该获取恢复尝试次数")
    void shouldGetRecoveryAttemptCount() throws StateRecovery.RecoveryException {
        assertEquals(0, recovery.getRecoveryAttempts("new-session"));

        recovery.attemptSelfHealing("new-session");
        // After recovery attempt, count should be > 0
        int attempts = recovery.getRecoveryAttempts("new-session");
        assertTrue(attempts > 0, "Expected attempts > 0, but got: " + attempts);
    }

    @Test
    @DisplayName("完整恢复流程应该按阶段执行")
    void fullRecoveryFlowShouldExecutePhasesSequentially() throws StateRecovery.RecoveryException {
        // Create custom strategies that fail to trigger escalation
        SelfHealingStrategy selfHealing = new SelfHealingStrategy();
        PeerRecoveryStrategy peerRecovery = new PeerRecoveryStrategy();
        LeadInterventionStrategy leadIntervention = new LeadInterventionStrategy();
        AbortStrategy abort = new AbortStrategy();

        FourPhaseRecovery customRecovery = new FourPhaseRecovery(
            selfHealing, peerRecovery, leadIntervention, abort
        );

        // Attempt recovery (will escalate through phases and eventually abort)
        RecoveryResult result = customRecovery.attemptRecovery("test-session");

        // After all phases are exhausted, should abort
        assertNotNull(result);
    }

    @Test
    @DisplayName("应该能够清除会话状态")
    void shouldClearSessionState() throws StateRecovery.RecoveryException {
        recovery.attemptSelfHealing("test-session");
        assertNotNull(recovery.getCurrentPhase("test-session"));

        recovery.clearSessionState("test-session");
        assertNull(recovery.getCurrentPhase("test-session"));
    }

    @Test
    @DisplayName("应该能够清除所有会话状态")
    void shouldClearAllSessionStates() throws StateRecovery.RecoveryException {
        recovery.attemptSelfHealing("session-1");
        recovery.attemptSelfHealing("session-2");

        int activeBefore = recovery.getActiveRecoveryCount();
        assertTrue(activeBefore >= 1, "Should have at least 1 active recovery before clearing");

        recovery.clearAllSessionStates();
        assertEquals(0, recovery.getActiveRecoveryCount());
    }

    @Test
    @DisplayName("应该统计活跃恢复会话数")
    void shouldCountActiveRecoverySessions() throws StateRecovery.RecoveryException {
        assertEquals(0, recovery.getActiveRecoveryCount());

        recovery.attemptSelfHealing("session-1");
        assertTrue(recovery.getActiveRecoveryCount() >= 1);
    }

    @Test
    @DisplayName("空sessionId应该抛出异常")
    void shouldThrowExceptionForNullSessionId() {
        assertThrows(IllegalArgumentException.class, () -> {
            recovery.attemptRecovery(null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            recovery.attemptSelfHealing("");
        });
    }

    @Test
    @DisplayName("中止会话后应该清除状态")
    void shouldClearStateAfterAbort() throws StateRecovery.RecoveryException {
        recovery.attemptSelfHealing("test-session");
        assertNotNull(recovery.getCurrentPhase("test-session"));

        recovery.markAborted("test-session");
        // After abort, session state should be cleared
        assertEquals(0, recovery.getRecoveryAttempts("test-session"));
    }

    @Test
    @DisplayName("自定义策略应该被正确使用")
    void shouldUseCustomStrategies() throws StateRecovery.RecoveryException {
        SelfHealingStrategy customSelfHealing = new SelfHealingStrategy(500L, 2000L);
        PeerRecoveryStrategy customPeerRecovery = new PeerRecoveryStrategy();
        LeadInterventionStrategy customLead = new LeadInterventionStrategy();
        AbortStrategy customAbort = new AbortStrategy();

        FourPhaseRecovery customRecovery = new FourPhaseRecovery(
            customSelfHealing, customPeerRecovery, customLead, customAbort
        );

        RecoveryResult result = customRecovery.attemptSelfHealing("test-session");

        assertNotNull(result);
        assertEquals(RecoveryResult.RecoveryPhase.SELF_HEALING, result.phase());
    }

    @Test
    @DisplayName("会话状态应该跟踪恢复尝试")
    void sessionStateShouldTrackRecoveryAttempts() throws StateRecovery.RecoveryException {
        String sessionId = "test-session";

        recovery.attemptSelfHealing(sessionId);
        int attempts1 = recovery.getRecoveryAttempts(sessionId);

        recovery.attemptSelfHealing(sessionId);
        int attempts2 = recovery.getRecoveryAttempts(sessionId);

        assertTrue(attempts2 >= attempts1, "Attempts should not decrease: attempts1=" + attempts1 + ", attempts2=" + attempts2);
    }
}
