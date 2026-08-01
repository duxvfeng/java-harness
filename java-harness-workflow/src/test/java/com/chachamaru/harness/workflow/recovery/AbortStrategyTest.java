package com.chachamaru.harness.workflow.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AbortStrategy.
 */
@DisplayName("AbortStrategy Tests")
public class AbortStrategyTest {

    private AbortStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new AbortStrategy();
        strategy.clearAllAbortRecords(); // Clean state for each test
    }

    @Test
    @DisplayName("应该返回正确的恢复阶段")
    void shouldReturnCorrectPhase() {
        assertEquals(
            RecoveryResult.RecoveryPhase.ABORT,
            strategy.getPhase()
        );
    }

    @Test
    @DisplayName("应该返回最大尝试次数为0")
    void shouldReturnMaxAttemptsAsZero() {
        assertEquals(0, strategy.getMaxAttempts());
    }

    @Test
    @DisplayName("应该能处理所有错误类型")
    void shouldHandleAllErrorTypes() {
        assertTrue(strategy.canHandle("AnyException"));
        assertTrue(strategy.canHandle("UnknownError"));
        assertTrue(strategy.canHandle("NullPointerException"));
    }

    @Test
    @DisplayName("应该标记会话为中止状态")
    void shouldMarkSessionAsAborted() throws RecoveryStrategy.RecoveryException {
        RuntimeException error = new RuntimeException("Fatal error");
        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            error,
            "FatalException",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            3,
            LocalDateTime.now(),
            null
        );

        RecoveryResult result = strategy.recover("test-session", context);

        assertEquals(RecoveryResult.RecoveryStatus.ABORTED, result.status());
        assertEquals(RecoveryResult.RecoveryPhase.ABORT, result.phase());
        assertTrue(result.message().contains("aborted"));
    }

    @Test
    @DisplayName("应该记录中止信息")
    void shouldRecordAbortInformation() throws RecoveryStrategy.RecoveryException {
        RuntimeException error = new RuntimeException("Fatal error");
        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            error,
            "FatalException",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            3,
            LocalDateTime.now(),
            null
        );

        strategy.recover("test-session", context);

        assertTrue(strategy.isAborted("test-session"));

        AbortStrategy.AbortRecord record = strategy.getAbortRecord("test-session");
        assertNotNull(record);
        assertEquals("test-session", record.sessionId());
        assertEquals("FatalException", record.errorType());
        assertEquals(3, record.totalAttempts());
    }

    @Test
    @DisplayName("应该能够获取所有中止的会话")
    void shouldGetAllAbortedSessions() throws RecoveryStrategy.RecoveryException {
        // Abort two sessions
        RecoveryStrategy.RecoveryContext context1 = new RecoveryStrategy.RecoveryContext(
            "session-1",
            new RuntimeException("Error 1"),
            "Error1",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            3,
            LocalDateTime.now(),
            null
        );

        RecoveryStrategy.RecoveryContext context2 = new RecoveryStrategy.RecoveryContext(
            "session-2",
            new RuntimeException("Error 2"),
            "Error2",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            2,
            LocalDateTime.now(),
            null
        );

        strategy.recover("session-1", context1);
        strategy.recover("session-2", context2);

        var abortedSessions = strategy.getAbortedSessions();
        assertEquals(2, abortedSessions.size());
        assertTrue(abortedSessions.containsKey("session-1"));
        assertTrue(abortedSessions.containsKey("session-2"));
    }

    @Test
    @DisplayName("应该正确统计中止的会话数量")
    void shouldCountAbortedSessions() throws RecoveryStrategy.RecoveryException {
        assertEquals(0, strategy.getAbortedSessionCount());

        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            new RuntimeException("Error"),
            "Error",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            1,
            LocalDateTime.now(),
            null
        );

        strategy.recover("test-session", context);
        assertEquals(1, strategy.getAbortedSessionCount());
    }

    @Test
    @DisplayName("应该能够清除中止记录")
    void shouldClearAbortRecord() throws RecoveryStrategy.RecoveryException {
        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            new RuntimeException("Error"),
            "Error",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            1,
            LocalDateTime.now(),
            null
        );

        strategy.recover("test-session", context);
        assertTrue(strategy.isAborted("test-session"));

        assertTrue(strategy.clearAbortRecord("test-session"));
        assertFalse(strategy.isAborted("test-session"));
    }

    @Test
    @DisplayName("清除不存在的记录应该返回false")
    void shouldReturnFalseWhenClearingNonExistentRecord() {
        assertFalse(strategy.clearAbortRecord("non-existent"));
    }

    @Test
    @DisplayName("应该能够清除所有中止记录")
    void shouldClearAllAbortRecords() throws RecoveryStrategy.RecoveryException {
        RecoveryStrategy.RecoveryContext context1 = new RecoveryStrategy.RecoveryContext(
            "session-1",
            new RuntimeException("Error 1"),
            "Error1",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            1,
            LocalDateTime.now(),
            null
        );

        RecoveryStrategy.RecoveryContext context2 = new RecoveryStrategy.RecoveryContext(
            "session-2",
            new RuntimeException("Error 2"),
            "Error2",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            1,
            LocalDateTime.now(),
            null
        );

        strategy.recover("session-1", context1);
        strategy.recover("session-2", context2);

        assertEquals(2, strategy.getAbortedSessionCount());

        strategy.clearAllAbortRecords();
        assertEquals(0, strategy.getAbortedSessionCount());
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
                    "Error",
                    RecoveryResult.RecoveryPhase.ABORT,
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
    @DisplayName("AbortRecord应该验证参数")
    void shouldValidateAbortRecordParameters() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AbortStrategy.AbortRecord(
                "",
                "Error",
                "Reason",
                LocalDateTime.now(),
                RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
                1
            );
        });

        // Null reason should default to "Unknown reason"
        AbortStrategy.AbortRecord record = new AbortStrategy.AbortRecord(
            "session",
            "Error",
            null,
            null,
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            1
        );
        assertEquals("Unknown reason", record.abortReason());
        assertNotNull(record.abortedAt());
    }

    @Test
    @DisplayName("中止消息应该包含尝试次数")
    void shouldIncludeAttemptCountInAbortMessage() throws RecoveryStrategy.RecoveryException {
        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            new RuntimeException("Error"),
            "Error",
            RecoveryResult.RecoveryPhase.LEAD_INTERVENTION,
            5,
            LocalDateTime.now(),
            null
        );

        RecoveryResult result = strategy.recover("test-session", context);
        assertTrue(result.message().contains("5"));
    }
}
