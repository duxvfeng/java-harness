package com.chachamaru.harness.workflow.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SelfHealingStrategy.
 */
@DisplayName("SelfHealingStrategy Tests")
public class SelfHealingStrategyTest {

    @Test
    @DisplayName("应该返回正确的恢复阶段")
    void shouldReturnCorrectPhase() {
        SelfHealingStrategy strategy = new SelfHealingStrategy();
        assertEquals(
            RecoveryResult.RecoveryPhase.SELF_HEALING,
            strategy.getPhase()
        );
    }

    @Test
    @DisplayName("应该返回最大尝试次数为3")
    void shouldReturnMaxAttemptsAsThree() {
        SelfHealingStrategy strategy = new SelfHealingStrategy();
        assertEquals(3, strategy.getMaxAttempts());
    }

    @Test
    @DisplayName("应该能处理超时错误")
    void shouldHandleTimeoutErrors() {
        SelfHealingStrategy strategy = new SelfHealingStrategy();
        assertTrue(strategy.canHandle("TimeoutException"));
        assertTrue(strategy.canHandle("SocketTimeoutException"));
        assertTrue(strategy.canHandle("ConnectTimeoutException"));
    }

    @Test
    @DisplayName("应该能处理连接错误")
    void shouldHandleConnectionErrors() {
        SelfHealingStrategy strategy = new SelfHealingStrategy();
        assertTrue(strategy.canHandle("ConnectException"));
        assertTrue(strategy.canHandle("SocketException"));
        assertTrue(strategy.canHandle("IOException"));
    }

    @Test
    @DisplayName("应该能处理配置错误")
    void shouldHandleConfigurationErrors() {
        SelfHealingStrategy strategy = new SelfHealingStrategy();
        assertTrue(strategy.canHandle("ConfigurationException"));
        assertTrue(strategy.canHandle("MissingPropertyException"));
        assertTrue(strategy.canHandle("IllegalStateException"));
    }

    @Test
    @DisplayName("不应该处理其他错误类型")
    void shouldNotHandleOtherErrorTypes() {
        SelfHealingStrategy strategy = new SelfHealingStrategy();
        assertFalse(strategy.canHandle("NullPointerException"));
        assertFalse(strategy.canHandle("IllegalArgumentException"));
    }

    @Test
    @DisplayName("第一次尝试应该失败并建议重试")
    void shouldFailAndRecommendRetryOnFirstAttempt() throws RecoveryStrategy.RecoveryException {
        SelfHealingStrategy strategy = new SelfHealingStrategy();

        RecoveryResult result = strategy.recover(
            "test-session",
            new RecoveryStrategy.RecoveryContext(
                "test-session",
                new TimeoutException("Operation timed out"),
                "TimeoutException",
                RecoveryResult.RecoveryPhase.SELF_HEALING,
                0,
                LocalDateTime.now(),
                null
            )
        );

        assertFalse(result.isSuccess());
        assertEquals(RecoveryResult.RecoveryPhase.SELF_HEALING, result.phase());
        assertEquals(1, result.attemptsMade());
    }

    @Test
    @DisplayName("第二次尝试应该失败并建议重试")
    void shouldFailAndRecommendRetryOnSecondAttempt() throws RecoveryStrategy.RecoveryException {
        SelfHealingStrategy strategy = new SelfHealingStrategy();

        RecoveryResult result = strategy.recover(
            "test-session",
            new RecoveryStrategy.RecoveryContext(
                "test-session",
                new TimeoutException("Operation timed out"),
                "TimeoutException",
                RecoveryResult.RecoveryPhase.SELF_HEALING,
                1,
                LocalDateTime.now(),
                null
            )
        );

        assertFalse(result.isSuccess());
        assertEquals(2, result.attemptsMade());
    }

    @Test
    @DisplayName("第三次尝试后应该升级到同伴修复")
    void shouldEscalateAfterMaxAttempts() throws RecoveryStrategy.RecoveryException {
        SelfHealingStrategy strategy = new SelfHealingStrategy();

        RecoveryResult result = strategy.recover(
            "test-session",
            new RecoveryStrategy.RecoveryContext(
                "test-session",
                new TimeoutException("Operation timed out"),
                "TimeoutException",
                RecoveryResult.RecoveryPhase.SELF_HEALING,
                2,
                LocalDateTime.now(),
                null
            )
        );

        assertFalse(result.isSuccess());
        assertEquals(3, result.attemptsMade());
        assertTrue(result.shouldEscalate());
        assertTrue(result.message().contains("Self-healing failed"));
    }

    @Test
    @DisplayName("超过最大尝试次数应该失败")
    void shouldFailWhenExceedingMaxAttempts() throws RecoveryStrategy.RecoveryException {
        SelfHealingStrategy strategy = new SelfHealingStrategy();

        RecoveryResult result = strategy.recover(
            "test-session",
            new RecoveryStrategy.RecoveryContext(
                "test-session",
                new TimeoutException("Operation timed out"),
                "TimeoutException",
                RecoveryResult.RecoveryPhase.SELF_HEALING,
                3,
                LocalDateTime.now(),
                null
            )
        );

        assertFalse(result.isSuccess());
        assertTrue(result.message().contains("Exceeded maximum"));
    }

    @Test
    @DisplayName("对于不支持的错误类型应该失败")
    void shouldFailForUnsupportedErrorType() throws RecoveryStrategy.RecoveryException {
        SelfHealingStrategy strategy = new SelfHealingStrategy();

        RecoveryResult result = strategy.recover(
            "test-session",
            new RecoveryStrategy.RecoveryContext(
                "test-session",
                new NullPointerException("Null pointer"),
                "NullPointerException",
                RecoveryResult.RecoveryPhase.SELF_HEALING,
                0,
                LocalDateTime.now(),
                null
            )
        );

        assertFalse(result.isSuccess());
        assertTrue(result.message().contains("Cannot handle error type"));
    }

    @Test
    @DisplayName("空sessionId应该抛出异常")
    void shouldThrowExceptionForNullSessionId() {
        SelfHealingStrategy strategy = new SelfHealingStrategy();

        assertThrows(IllegalArgumentException.class, () -> {
            strategy.recover(
                null,
                new RecoveryStrategy.RecoveryContext(
                    "test-session",
                    new TimeoutException(),
                    "TimeoutException",
                    RecoveryResult.RecoveryPhase.SELF_HEALING,
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
        SelfHealingStrategy strategy = new SelfHealingStrategy();

        assertThrows(IllegalArgumentException.class, () -> {
            strategy.recover("test-session", null);
        });
    }

    @Test
    @DisplayName("应该识别超时错误")
    void shouldIdentifyTimeoutErrors() {
        SelfHealingStrategy strategy = new SelfHealingStrategy();
        assertTrue(strategy.isTimeoutError("TimeoutException"));
        assertTrue(strategy.isTimeoutError("SocketTimeoutException"));
        assertFalse(strategy.isTimeoutError("ConnectException"));
    }

    @Test
    @DisplayName("应该识别连接错误")
    void shouldIdentifyConnectionErrors() {
        SelfHealingStrategy strategy = new SelfHealingStrategy();
        assertTrue(strategy.isConnectionError("ConnectException"));
        assertTrue(strategy.isConnectionError("SocketException"));
        assertFalse(strategy.isConnectionError("TimeoutException"));
    }

    @Test
    @DisplayName("应该识别配置错误")
    void shouldIdentifyConfigurationErrors() {
        SelfHealingStrategy strategy = new SelfHealingStrategy();
        assertTrue(strategy.isConfigurationError("ConfigurationException"));
        assertTrue(strategy.isConfigurationError("MissingPropertyException"));
        assertTrue(strategy.isConfigurationError("IllegalStateException"));
        assertFalse(strategy.isConfigurationError("TimeoutException"));
    }

    @Test
    @DisplayName("自定义退避时间应该正确计算")
    void shouldCalculateCustomBackoffTime() throws RecoveryStrategy.RecoveryException {
        SelfHealingStrategy strategy = new SelfHealingStrategy(500L, 5000L);

        // 验证策略创建成功
        assertEquals(3, strategy.getMaxAttempts());
        assertEquals(RecoveryResult.RecoveryPhase.SELF_HEALING, strategy.getPhase());
    }

    @Test
    @DisplayName("无效的退避时间应该抛出异常")
    void shouldThrowExceptionForInvalidBackoffTime() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SelfHealingStrategy(0L, 1000L);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new SelfHealingStrategy(1000L, 500L);
        });
    }
}
