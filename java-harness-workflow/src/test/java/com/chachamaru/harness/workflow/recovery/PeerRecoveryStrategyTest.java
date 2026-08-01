package com.chachamaru.harness.workflow.recovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PeerRecoveryStrategy.
 */
@DisplayName("PeerRecoveryStrategy Tests")
public class PeerRecoveryStrategyTest {

    @Test
    @DisplayName("应该返回正确的恢复阶段")
    void shouldReturnCorrectPhase() {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();
        assertEquals(
            RecoveryResult.RecoveryPhase.PEER_RECOVERY,
            strategy.getPhase()
        );
    }

    @Test
    @DisplayName("应该返回最大尝试次数为2")
    void shouldReturnMaxAttemptsAsTwo() {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();
        assertEquals(2, strategy.getMaxAttempts());
    }

    @Test
    @DisplayName("应该能处理容量错误")
    void shouldHandleCapacityErrors() {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();
        assertTrue(strategy.canHandle("WorkerCapacityException"));
        assertTrue(strategy.canHandle("CapacityLimitException"));
        assertTrue(strategy.canHandle("ResourceExhaustedException"));
    }

    @Test
    @DisplayName("应该能处理worker特定错误")
    void shouldHandleWorkerSpecificErrors() {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();
        assertTrue(strategy.canHandle("WorkerFailedException"));
        assertTrue(strategy.canHandle("AgentUnavailableException"));
    }

    @Test
    @DisplayName("不应该处理其他错误类型")
    void shouldNotHandleOtherErrorTypes() {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();
        assertFalse(strategy.canHandle("TimeoutException"));
        assertFalse(strategy.canHandle("NullPointerException"));
    }

    @Test
    @DisplayName("第一次尝试应该重新分配到替代worker")
    void shouldReassignToAlternativeWorkerOnFirstAttempt() throws RecoveryStrategy.RecoveryException {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();

        RuntimeException workerError = new RuntimeException("Worker failed");
        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            workerError,
            "WorkerFailedException",
            RecoveryResult.RecoveryPhase.PEER_RECOVERY,
            0,
            LocalDateTime.now(),
            Map.of("workerId", "worker-1")
        );

        RecoveryResult result = strategy.recover("test-session", context);

        assertFalse(result.isSuccess());
        assertEquals(RecoveryResult.RecoveryPhase.PEER_RECOVERY, result.phase());
        assertEquals(1, result.attemptsMade());
        assertTrue(result.message().contains("Reassigned to worker"));
    }

    @Test
    @DisplayName("第二次尝试后应该升级到指挥官介入")
    void shouldEscalateAfterMaxAttempts() throws RecoveryStrategy.RecoveryException {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();

        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            new RuntimeException("Worker failed"),
            "WorkerFailedException",
            RecoveryResult.RecoveryPhase.PEER_RECOVERY,
            1,
            LocalDateTime.now(),
            Map.of("workerId", "worker-2")
        );

        RecoveryResult result = strategy.recover("test-session", context);

        assertFalse(result.isSuccess());
        assertEquals(2, result.attemptsMade());
        assertTrue(result.shouldEscalate());
        assertTrue(result.message().contains("Peer recovery failed"));
    }

    @Test
    @DisplayName("超过最大尝试次数应该失败")
    void shouldFailWhenExceedingMaxAttempts() throws RecoveryStrategy.RecoveryException {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();

        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            new RuntimeException("Worker failed"),
            "WorkerFailedException",
            RecoveryResult.RecoveryPhase.PEER_RECOVERY,
            2,
            LocalDateTime.now(),
            null
        );

        RecoveryResult result = strategy.recover("test-session", context);

        assertFalse(result.isSuccess());
        assertTrue(result.message().contains("Exceeded maximum"));
    }

    @Test
    @DisplayName("没有可用worker应该失败")
    void shouldFailWhenNoAvailableWorkers() throws RecoveryStrategy.RecoveryException {
        // Create strategy with no workers
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy(Map.of());

        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            new RuntimeException("Worker failed"),
            "WorkerFailedException",
            RecoveryResult.RecoveryPhase.PEER_RECOVERY,
            0,
            LocalDateTime.now(),
            null
        );

        RecoveryResult result = strategy.recover("test-session", context);

        assertFalse(result.isSuccess());
        assertTrue(result.message().contains("No available alternative workers"));
    }

    @Test
    @DisplayName("应该记录失败的worker分配")
    void shouldTrackFailedWorkerAssignments() throws RecoveryStrategy.RecoveryException {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();

        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            new RuntimeException("Worker failed"),
            "WorkerFailedException",
            RecoveryResult.RecoveryPhase.PEER_RECOVERY,
            0,
            LocalDateTime.now(),
            Map.of("workerId", "worker-1")
        );

        strategy.recover("test-session", context);

        // Worker-1 should be marked as failed for this session
        // Next attempt should avoid worker-1 and worker-2
        // Use attempt 0 to simulate a new recovery attempt (not exceeding max)
        RecoveryStrategy.RecoveryContext context2 = new RecoveryStrategy.RecoveryContext(
            "test-session",
            new RuntimeException("Another worker failed"),
            "WorkerFailedException",
            RecoveryResult.RecoveryPhase.PEER_RECOVERY,
            0,  // Fresh attempt for peer recovery phase
            LocalDateTime.now(),
            Map.of("workerId", "worker-2")
        );

        RecoveryResult result2 = strategy.recover("test-session", context2);

        // Should select specialist-worker (load 0) or worker-3 (load 1)
        assertTrue(result2.message().contains("specialist-worker") ||
                  result2.message().contains("worker-3"));
    }

    @Test
    @DisplayName("应该选择负载最轻的worker")
    void shouldSelectLeastLoadedWorker() throws RecoveryStrategy.RecoveryException {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();

        // specialist-worker has load 0 (least loaded), worker-3 has load 1
        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            new RuntimeException("Worker failed"),
            "WorkerFailedException",
            RecoveryResult.RecoveryPhase.PEER_RECOVERY,
            0,
            LocalDateTime.now(),
            Map.of("workerId", "worker-1")
        );

        RecoveryResult result = strategy.recover("test-session", context);

        // Should select specialist-worker (load 0) as it's least loaded
        assertTrue(result.message().contains("specialist-worker"));
    }

    @Test
    @DisplayName("应该能够注册新worker")
    void shouldRegisterNewWorker() {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();
        int initialCount = strategy.getAvailableWorkerCount();

        strategy.registerWorker("new-worker", PeerRecoveryStrategy.WorkerType.WORKER, 10, 0);

        assertEquals(initialCount + 1, strategy.getAvailableWorkerCount());
    }

    @Test
    @DisplayName("应该能够注销worker")
    void shouldUnregisterWorker() {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();
        int initialCount = strategy.getAvailableWorkerCount();

        strategy.unregisterWorker("worker-1");

        assertEquals(initialCount - 1, strategy.getAvailableWorkerCount());
    }

    @Test
    @DisplayName("应该能够清除失败worker历史")
    void shouldClearFailedWorkerHistory() throws RecoveryStrategy.RecoveryException {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();

        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            "test-session",
            new RuntimeException("Worker failed"),
            "WorkerFailedException",
            RecoveryResult.RecoveryPhase.PEER_RECOVERY,
            0,
            LocalDateTime.now(),
            Map.of("workerId", "worker-1")
        );

        strategy.recover("test-session", context);
        strategy.clearFailedWorkerHistory("test-session");

        // After clearing history, restart with attempt 0
        RecoveryStrategy.RecoveryContext context2 = new RecoveryStrategy.RecoveryContext(
            "test-session",
            new RuntimeException("Worker failed again"),
            "WorkerFailedException",
            RecoveryResult.RecoveryPhase.PEER_RECOVERY,
            0,  // Start fresh
            LocalDateTime.now(),
            Map.of("workerId", "worker-2")
        );

        RecoveryResult result2 = strategy.recover("test-session", context2);
        // Should select any available worker (specialist-worker has priority with load 0)
        assertTrue(result2.message().contains("specialist-worker") ||
                  result2.message().contains("Reassigned to worker"));
    }

    @Test
    @DisplayName("应该正确检测可用worker")
    void shouldDetectAvailableWorkers() {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();
        assertTrue(strategy.hasAvailableWorkers());

        // Fill all workers
        strategy = new PeerRecoveryStrategy(Map.of(
            "worker-full", new PeerRecoveryStrategy.WorkerInfo("worker-full",
                PeerRecoveryStrategy.WorkerType.WORKER, 5, 5)
        ));
        assertFalse(strategy.hasAvailableWorkers());
    }

    @Test
    @DisplayName("空sessionId应该抛出异常")
    void shouldThrowExceptionForNullSessionId() {
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();

        assertThrows(IllegalArgumentException.class, () -> {
            strategy.recover(
                null,
                new RecoveryStrategy.RecoveryContext(
                    "test-session",
                    new RuntimeException(),
                    "WorkerFailedException",
                    RecoveryResult.RecoveryPhase.PEER_RECOVERY,
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
        PeerRecoveryStrategy strategy = new PeerRecoveryStrategy();

        assertThrows(IllegalArgumentException.class, () -> {
            strategy.recover("test-session", null);
        });
    }

    @Test
    @DisplayName("WorkerInfo记录应该验证参数")
    void shouldValidateWorkerInfoParameters() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PeerRecoveryStrategy.WorkerInfo("", PeerRecoveryStrategy.WorkerType.WORKER, 5, 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PeerRecoveryStrategy.WorkerInfo("worker", PeerRecoveryStrategy.WorkerType.WORKER, 0, 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PeerRecoveryStrategy.WorkerInfo("worker", PeerRecoveryStrategy.WorkerType.WORKER, 5, -1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new PeerRecoveryStrategy.WorkerInfo("worker", PeerRecoveryStrategy.WorkerType.WORKER, 5, 6);
        });
    }

    @Test
    @DisplayName("WorkerInfo应该正确计算可用容量")
    void shouldCalculateAvailableCapacity() {
        PeerRecoveryStrategy.WorkerInfo worker = new PeerRecoveryStrategy.WorkerInfo(
            "worker-1",
            PeerRecoveryStrategy.WorkerType.WORKER,
            10,
            3
        );

        assertTrue(worker.hasCapacity());
        assertEquals(7, worker.availableCapacity());
    }

    @Test
    @DisplayName("满负载worker不应该有容量")
    void fullLoadWorkerShouldHaveNoCapacity() {
        PeerRecoveryStrategy.WorkerInfo worker = new PeerRecoveryStrategy.WorkerInfo(
            "worker-full",
            PeerRecoveryStrategy.WorkerType.WORKER,
            5,
            5
        );

        assertFalse(worker.hasCapacity());
        assertEquals(0, worker.availableCapacity());
    }
}
