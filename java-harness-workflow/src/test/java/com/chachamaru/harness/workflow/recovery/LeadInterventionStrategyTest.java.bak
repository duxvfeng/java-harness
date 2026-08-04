package com.chachamaru.harness.workflow.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LeadInterventionStrategy.
 */
@DisplayName("LeadInterventionStrategy Tests")
public class LeadInterventionStrategyTest {

    private LeadInterventionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new LeadInterventionStrategy();
    }

    @Test
    @DisplayName("应该返回正确的恢复阶段")
    void shouldReturnCorrectPhase() {
        assertEquals(
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            strategy.getPhase()
        );
    }

    @Test
    @DisplayName("应该返回最大尝试次数为1")
    void shouldReturnMaxAttemptsAsOne() {
        assertEquals(1, strategy.getMaxAttempts());
    }

    @Test
    @DisplayName("应该能处理工作流错误")
    void shouldHandleWorkflowErrors() {
        assertTrue(strategy.canHandle("WorkflowException"));
        assertTrue(strategy.canHandle("CoordinationException"));
        assertTrue(strategy.canHandle("CriticalStateException"));
    }

    @Test
    @DisplayName("应该能处理所有错误类型（作为最终fallback）")
    void shouldHandleAllErrorTypesAsFallback() {
        // Lead intervention can handle critical errors
        assertTrue(strategy.canHandle("UnrecoverableException"));
        assertTrue(strategy.canHandle("ConfigurationMismatchException"));
    }

    @Test
    @DisplayName("应该升级到人工干预")
    void shouldEscalateToIntervention() throws RecoveryStrategy.RecoveryException {
        RuntimeException error = new RuntimeException("Critical workflow failure");
        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            error,
            "WorkflowException",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            0,
            LocalDateTime.now(),
            null
        );

        RecoveryResult result = strategy.recover("test-session", context);

        assertFalse(result.isSuccess());
        assertEquals(RecoveryResult.RecoveryPhase.LEAD_INTERVENTION, result.phase());
        assertEquals(1, result.attemptsMade());
        assertTrue(result.message().contains("human coordinator"));
    }

    @Test
    @DisplayName("有intervention handler时应该调用它")
    void shouldCallInterventionHandlerWhenAvailable() throws RecoveryStrategy.RecoveryException {
        LeadInterventionStrategy.InterventionHandler handler =
            (sessionId, error, snapshot) ->
                LeadInterventionStrategy.InterventionResult.success("Manually resolved");

        strategy = new LeadInterventionStrategy(handler);

        RuntimeException error = new RuntimeException("Critical failure");
        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            error,
            "WorkflowException",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            0,
            LocalDateTime.now(),
            null
        );

        RecoveryResult result = strategy.recover("test-session", context);

        assertTrue(result.isSuccess());
        assertEquals(RecoveryResult.RecoveryPhase.LEAD_INTERVENTION, result.phase());
    }

    @Test
    @DisplayName("intervention handler失败时应该返回失败")
    void shouldReturnFailureWhenHandlerFails() throws RecoveryStrategy.RecoveryException {
        LeadInterventionStrategy.InterventionHandler handler =
            (sessionId, error, snapshot) ->
                LeadInterventionStrategy.InterventionResult.failure("Cannot resolve");

        strategy = new LeadInterventionStrategy(handler);

        RuntimeException error = new RuntimeException("Critical failure");
        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            error,
            "WorkflowException",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            0,
            LocalDateTime.now(),
            null
        );

        RecoveryResult result = strategy.recover("test-session", context);

        assertFalse(result.isSuccess());
        assertTrue(result.message().contains("Intervention failed"));
    }

    @Test
    @DisplayName("handler抛出异常时应该处理并返回失败")
    void shouldHandleHandlerException() throws RecoveryStrategy.RecoveryException {
        LeadInterventionStrategy.InterventionHandler handler =
            (sessionId, error, snapshot) -> {
                throw new RuntimeException("Handler error");
            };

        strategy = new LeadInterventionStrategy(handler);

        RuntimeException error = new RuntimeException("Critical failure");
        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            error,
            "WorkflowException",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            0,
            LocalDateTime.now(),
            null
        );

        RecoveryResult result = strategy.recover("test-session", context);

        assertFalse(result.isSuccess());
        assertTrue(result.message().contains("Intervention handler error"));
    }

    @Test
    @DisplayName("应该能够设置intervention handler")
    void shouldSetInterventionHandler() {
        assertFalse(strategy.hasInterventionHandler());

        LeadInterventionStrategy.InterventionHandler handler =
            (sessionId, error, snapshot) ->
                LeadInterventionStrategy.InterventionResult.success("Resolved");

        strategy.setInterventionHandler(handler);
        assertTrue(strategy.hasInterventionHandler());
    }

    @Test
    @DisplayName("空sessionId应该抛出异常")
    void shouldThrowExceptionForNullSessionId() {
        assertThrows(IllegalArgumentException.class, () -> {
            strategy.recover(
                null,
                new RecoveryStrategy.RecoveryContext(
                    "test-session",
                    new RuntimeException(),
                    "WorkflowException",
                    RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
                    0,
                    LocalDateTime.now(),
                    null
                )
            );
        });
    }

    @Test
    @DisplayName("空context应该抛出异常")
    void shouldThrowExceptionForNullContext() {
        assertThrows(IllegalArgumentException.class, () -> {
            strategy.recover("test-session", null);
        });
    }

    @Test
    @DisplayName("InterventionResult成功记录应该正确")
    void shouldCreateSuccessfulInterventionResult() {
        LeadInterventionStrategy.InterventionResult result =
            LeadInterventionStrategy.InterventionResult.success("Fixed manually");

        assertTrue(result.resolved());
        assertNull(result.reason());
        assertEquals("Fixed manually", result.resolutionDetails());
    }

    @Test
    @DisplayName("InterventionResult失败记录应该正确")
    void shouldCreateFailedInterventionResult() {
        LeadInterventionStrategy.InterventionResult result =
            LeadInterventionStrategy.InterventionResult.failure("Cannot fix");

        assertFalse(result.resolved());
        assertEquals("Cannot fix", result.reason());
        assertNull(result.resolutionDetails());
    }
}
