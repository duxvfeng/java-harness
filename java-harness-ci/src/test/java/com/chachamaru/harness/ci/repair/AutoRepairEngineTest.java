package com.chachamaru.harness.ci.repair;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Auto Repair Engine
 */
class AutoRepairEngineTest {

    private AutoRepairEngine repairEngine;

    @BeforeEach
    void setUp() {
        repairEngine = new AutoRepairEngine();
    }

    @AfterEach
    void tearDown() {
        repairEngine.shutdown();
    }

    @Test
    void testRepairEngineCreation() {
        assertNotNull(repairEngine);
        assertTrue(repairEngine.getActiveRepairs().isEmpty());
    }

    @Test
    void testHandleFailure() {
        AutoRepairEngine.FailureInfo failureInfo = new AutoRepairEngine.FailureInfo(
            "test-timeout",
            "Test timed out after 30 seconds",
            "Test execution timeout",
            "workflow-123",
            "job-456",
            Map.of("test", "timeout")
        );

        String sessionId = repairEngine.handleFailure(failureInfo);

        assertNotNull(sessionId);
        assertFalse(sessionId.isEmpty());

        // Session should be created
        var session = repairEngine.getRepairSession(sessionId);
        assertTrue(session.isPresent());
        assertEquals(sessionId, session.get().sessionId());
    }

    @Test
    void testRepairListener() throws InterruptedException {
        AutoRepairEngine.FailureInfo failureInfo = new AutoRepairEngine.FailureInfo(
            "test-flaky",
            "Flaky test detected",
            "Test intermittent failure",
            "workflow-123",
            "job-456",
            Map.of()
        );

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger eventCount = new AtomicInteger(0);

        repairEngine.addRepairListener(event -> {
            eventCount.incrementAndGet();
            if (event.eventType() == AutoRepairEngine.RepairEventType.STARTED) {
                latch.countDown();
            }
        });

        repairEngine.handleFailure(failureInfo);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(eventCount.get() >= 1);
    }

    @Test
    void testFlakyTestRepair() {
        AutoRepairEngine.FailureInfo failureInfo = new AutoRepairEngine.FailureInfo(
            "test-flaky",
            "Test is flaky and intermittent",
            "Flaky test detected",
            "workflow-123",
            "job-456",
            Map.of()
        );

        String sessionId = repairEngine.handleFailure(failureInfo);

        assertNotNull(sessionId);

        // Wait for repair to complete
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        var session = repairEngine.getRepairSession(sessionId);
        assertTrue(session.isPresent());

        // Verify the session was processed
        assertEquals(sessionId, session.get().sessionId());
        assertEquals(failureInfo.type(), session.get().failureInfo().type());
    }

    @Test
    void testTimeoutRepair() {
        AutoRepairEngine.FailureInfo failureInfo = new AutoRepairEngine.FailureInfo(
            "test-timeout",
            "Test execution timeout",
            "Timeout after 30 seconds",
            "workflow-123",
            "job-456",
            Map.of("timeout", "30s")
        );

        String sessionId = repairEngine.handleFailure(failureInfo);

        assertNotNull(sessionId);
    }

    @Test
    void testBuildFailureRepair() {
        AutoRepairEngine.FailureInfo failureInfo = new AutoRepairEngine.FailureInfo(
            "build-failure",
            "Build compilation failed",
            "Compilation error",
            "workflow-123",
            "job-456",
            Map.of("language", "java")
        );

        String sessionId = repairEngine.handleFailure(failureInfo);

        assertNotNull(sessionId);
    }

    @Test
    void testMultipleRepairs() {
        AutoRepairEngine.FailureInfo failure1 = new AutoRepairEngine.FailureInfo(
            "test-flaky",
            "Flaky test 1",
            "Test 1 failed",
            "workflow-1",
            "job-1",
            Map.of()
        );

        AutoRepairEngine.FailureInfo failure2 = new AutoRepairEngine.FailureInfo(
            "test-timeout",
            "Timeout test",
            "Test 2 timeout",
            "workflow-2",
            "job-2",
            Map.of()
        );

        String sessionId1 = repairEngine.handleFailure(failure1);
        String sessionId2 = repairEngine.handleFailure(failure2);

        assertNotNull(sessionId1);
        assertNotNull(sessionId2);
        assertNotEquals(sessionId1, sessionId2);

        var activeRepairs = repairEngine.getActiveRepairs();
        assertTrue(activeRepairs.size() >= 2);
    }

    @Test
    void testRepairSessionRetrieval() {
        AutoRepairEngine.FailureInfo failureInfo = new AutoRepairEngine.FailureInfo(
            "network-timeout",
            "Network connection timeout",
            "Connection failed",
            "workflow-123",
            "job-456",
            Map.of()
        );

        String sessionId = repairEngine.handleFailure(failureInfo);

        var session = repairEngine.getRepairSession(sessionId);
        assertTrue(session.isPresent());
        assertEquals(sessionId, session.get().sessionId());

        var nonExistentSession = repairEngine.getRepairSession("non-existent");
        assertFalse(nonExistentSession.isPresent());
    }

    @Test
    void testFailureTypeDetermination() {
        // Test timeout detection
        AutoRepairEngine.FailureInfo timeoutFailure = new AutoRepairEngine.FailureInfo(
            "generic",
            "Test execution timeout after 30 seconds",
            "Timeout detected in logs",
            "workflow-123",
            "job-456",
            Map.of()
        );

        String sessionId = repairEngine.handleFailure(timeoutFailure);
        assertNotNull(sessionId);

        // Test flaky detection
        AutoRepairEngine.FailureInfo flakyFailure = new AutoRepairEngine.FailureInfo(
            "generic",
            "Intermittent flaky test",
            "Flaky test detected in logs",
            "workflow-123",
            "job-456",
            Map.of()
        );

        sessionId = repairEngine.handleFailure(flakyFailure);
        assertNotNull(sessionId);
    }

    @Test
    void testMaxRepairAttempts() {
        repairEngine.setMaxRepairAttempts(5);

        AutoRepairEngine.FailureInfo failureInfo = new AutoRepairEngine.FailureInfo(
            "test-flaky",
            "Flaky test",
            "Test failure",
            "workflow-123",
            "job-456",
            Map.of()
        );

        String sessionId = repairEngine.handleFailure(failureInfo);
        assertNotNull(sessionId);
    }

    @Test
    void testRetryDelay() {
        repairEngine.setRetryDelay(60);

        AutoRepairEngine.FailureInfo failureInfo = new AutoRepairEngine.FailureInfo(
            "test-timeout",
            "Timeout test",
            "Test timeout",
            "workflow-123",
            "job-456",
            Map.of()
        );

        String sessionId = repairEngine.handleFailure(failureInfo);
        assertNotNull(sessionId);
    }

    @Test
    void testRemoveRepairListener() throws InterruptedException {
        AutoRepairEngine.FailureInfo failureInfo = new AutoRepairEngine.FailureInfo(
            "test-flaky",
            "Flaky test",
            "Test failure",
            "workflow-123",
            "job-456",
            Map.of()
        );

        CountDownLatch latch = new CountDownLatch(1);
        AutoRepairEngine.RepairListener listener = event -> latch.countDown();

        repairEngine.addRepairListener(listener);
        repairEngine.handleFailure(failureInfo);

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Remove listener
        repairEngine.removeRepairListener(listener);

        // This should not trigger the removed listener
        latch = new CountDownLatch(1);
        repairEngine.handleFailure(failureInfo);

        assertFalse(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    void testRepairStatusEnum() {
        assertEquals(9, AutoRepairEngine.RepairStatus.values().length);

        assertTrue(AutoRepairEngine.RepairStatus.STARTED != AutoRepairEngine.RepairStatus.COMPLETED);
        assertTrue(AutoRepairEngine.RepairStatus.REPAIRING != AutoRepairEngine.RepairStatus.FAILED);
    }

    @Test
    void testRepairEventTypeEnum() {
        assertEquals(4, AutoRepairEngine.RepairEventType.values().length);

        assertTrue(AutoRepairEngine.RepairEventType.STARTED != AutoRepairEngine.RepairEventType.SUCCESS);
        assertTrue(AutoRepairEngine.RepairEventType.FAILED != AutoRepairEngine.RepairEventType.COMPLETED);
    }

    @Test
    void testRepairEvent() {
        AutoRepairEngine.FailureInfo failureInfo = new AutoRepairEngine.FailureInfo(
            "test",
            "Test failure",
            "Test error",
            "workflow-123",
            "job-456",
            Map.of()
        );

        AutoRepairEngine.RepairSession session = new AutoRepairEngine.RepairSession(
            "session-123",
            failureInfo,
            LocalDateTime.now()
        );

        AutoRepairEngine.RepairEvent event = new AutoRepairEngine.RepairEvent(
            session,
            AutoRepairEngine.RepairEventType.STARTED,
            "Repair started"
        );

        assertEquals(AutoRepairEngine.RepairEventType.STARTED, event.eventType());
        assertEquals("Repair started", event.message());
        assertEquals(session, event.session());
        assertNotNull(event.timestamp());
    }

    @Test
    void testCustomRepairStrategy() {
        AutoRepairEngine.RepairStrategy customStrategy = failureInfo -> {
            return new AutoRepairEngine.RepairResult(
                true,
                "Custom repair applied",
                java.util.List.of("Custom action 1", "Custom action 2")
            );
        };

        repairEngine.registerStrategy("custom-failure", customStrategy);

        AutoRepairEngine.FailureInfo failureInfo = new AutoRepairEngine.FailureInfo(
            "custom-failure",
            "Custom failure type",
            "Custom error",
            "workflow-123",
            "job-456",
            Map.of()
        );

        String sessionId = repairEngine.handleFailure(failureInfo);
        assertNotNull(sessionId);
    }
}