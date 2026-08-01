package com.chachamaru.harness.ci.monitor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CI Status Monitor
 */
class CIStatusMonitorTest {

    private CIStatusMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new CIStatusMonitor();
    }

    @AfterEach
    void tearDown() {
        if (monitor.isRunning()) {
            monitor.stop();
        }
    }

    @Test
    void testMonitorCreation() {
        assertNotNull(monitor);
        assertFalse(monitor.isRunning());
        assertTrue(monitor.getMonitoredWorkflows().isEmpty());
    }

    @Test
    void testAddWorkflow() {
        monitor.addWorkflow("test-workflow", "github", "main", "CI Pipeline");

        Map<String, CIStatusMonitor.MonitoredWorkflow> workflows = monitor.getMonitoredWorkflows();
        assertEquals(1, workflows.size());
        assertTrue(workflows.containsKey("test-workflow"));

        CIStatusMonitor.MonitoredWorkflow workflow = workflows.get("test-workflow");
        assertEquals("test-workflow", workflow.workflowId());
        assertEquals("github", workflow.providerName());
        assertEquals("main", workflow.branch());
        assertEquals("CI Pipeline", workflow.workflowName());
    }

    @Test
    void testRemoveWorkflow() {
        monitor.addWorkflow("test-workflow", "github", "main", "CI Pipeline");
        assertEquals(1, monitor.getMonitoredWorkflows().size());

        monitor.removeWorkflow("test-workflow");
        assertTrue(monitor.getMonitoredWorkflows().isEmpty());
    }

    @Test
    void testWorkflowStatusCheck() {
        monitor.addWorkflow("test-workflow", "github", "main", "CI Pipeline");

        CIStatusMonitor.MonitoredWorkflow workflow = monitor.getWorkflow("test-workflow").orElseThrow();
        assertEquals(CIStatusMonitor.WorkflowStatus.UNKNOWN, workflow.status());
        assertTrue(workflow.addedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testSetPollInterval() {
        Duration interval = Duration.ofSeconds(45);
        monitor.setPollInterval(interval);
        // Interval is set, but not directly exposed for testing
        // The effect would be seen during actual monitoring
    }

    @Test
    void testStatusChangeListener() throws InterruptedException {
        monitor.addWorkflow("test-workflow", "github", "main", "CI Pipeline");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger callCount = new AtomicInteger(0);

        monitor.addStatusChangeListener(event -> {
            callCount.incrementAndGet();
            latch.countDown();
        });

        // Manually trigger a status update
        monitor.updateWorkflowStatus("test-workflow", CIStatusMonitor.WorkflowStatus.SUCCESS, Map.of());

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, callCount.get());

        CIStatusMonitor.MonitoredWorkflow workflow = monitor.getWorkflow("test-workflow").orElseThrow();
        assertEquals(CIStatusMonitor.WorkflowStatus.SUCCESS, workflow.status());
    }

    @Test
    void testMultipleStatusChangeListeners() throws InterruptedException {
        monitor.addWorkflow("test-workflow", "github", "main", "CI Pipeline");

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        monitor.addStatusChangeListener(event -> latch1.countDown());
        monitor.addStatusChangeListener(event -> latch2.countDown());

        // Manually trigger a status update
        monitor.updateWorkflowStatus("test-workflow", CIStatusMonitor.WorkflowStatus.FAILURE, Map.of());

        assertTrue(latch1.await(5, TimeUnit.SECONDS));
        assertTrue(latch2.await(5, TimeUnit.SECONDS));

        CIStatusMonitor.MonitoredWorkflow workflow = monitor.getWorkflow("test-workflow").orElseThrow();
        assertEquals(CIStatusMonitor.WorkflowStatus.FAILURE, workflow.status());
    }

    @Test
    void testStatusChangeEvent() {
        CIStatusMonitor.MonitoredWorkflow workflow = new CIStatusMonitor.MonitoredWorkflow(
            "test-workflow", "github", "main", "CI Pipeline",
            LocalDateTime.now(), LocalDateTime.now(),
            CIStatusMonitor.WorkflowStatus.SUCCESS, Map.of()
        );

        CIStatusMonitor.StatusChangeEvent event = new CIStatusMonitor.StatusChangeEvent(
            workflow,
            CIStatusMonitor.WorkflowStatus.RUNNING,
            CIStatusMonitor.WorkflowStatus.SUCCESS,
            LocalDateTime.now()
        );

        assertFalse(event.isFailure());
        assertTrue(event.isSuccess());
        assertFalse(event.isPending());
    }

    @Test
    void testFailureStatusChangeEvent() {
        CIStatusMonitor.MonitoredWorkflow workflow = new CIStatusMonitor.MonitoredWorkflow(
            "test-workflow", "github", "main", "CI Pipeline",
            LocalDateTime.now(), LocalDateTime.now(),
            CIStatusMonitor.WorkflowStatus.FAILURE, Map.of()
        );

        CIStatusMonitor.StatusChangeEvent event = new CIStatusMonitor.StatusChangeEvent(
            workflow,
            CIStatusMonitor.WorkflowStatus.RUNNING,
            CIStatusMonitor.WorkflowStatus.FAILURE,
            LocalDateTime.now()
        );

        assertTrue(event.isFailure());
        assertFalse(event.isSuccess());
        assertFalse(event.isPending());
    }

    @Test
    void testMonitoredWorkflowMethods() {
        CIStatusMonitor.MonitoredWorkflow runningWorkflow = new CIStatusMonitor.MonitoredWorkflow(
            "test-workflow", "github", "main", "CI Pipeline",
            LocalDateTime.now(), LocalDateTime.now(),
            CIStatusMonitor.WorkflowStatus.RUNNING, Map.of()
        );

        assertTrue(runningWorkflow.isRunning());
        assertFalse(runningWorkflow.isCompleted());
        assertFalse(runningWorkflow.isSuccess());

        CIStatusMonitor.MonitoredWorkflow successWorkflow = new CIStatusMonitor.MonitoredWorkflow(
            "success-workflow", "github", "main", "CI Pipeline",
            LocalDateTime.now(), LocalDateTime.now(),
            CIStatusMonitor.WorkflowStatus.SUCCESS, Map.of()
        );

        assertFalse(successWorkflow.isRunning());
        assertTrue(successWorkflow.isCompleted());
        assertTrue(successWorkflow.isSuccess());
    }

    @Test
    void testWorkflowStatusEnum() {
        assertEquals(9, CIStatusMonitor.WorkflowStatus.values().length);

        assertTrue(CIStatusMonitor.WorkflowStatus.SUCCESS != CIStatusMonitor.WorkflowStatus.FAILURE);
        assertTrue(CIStatusMonitor.WorkflowStatus.PENDING != CIStatusMonitor.WorkflowStatus.RUNNING);
    }

    @Test
    void testStartStopMonitoring() throws InterruptedException {
        // This test just verifies start/stop don't throw exceptions
        // Real monitoring would require a status provider to be registered

        monitor.setPollInterval(Duration.ofMillis(100));
        monitor.start();

        assertTrue(monitor.isRunning());

        Thread.sleep(200); // Let it run briefly

        monitor.stop();
        assertFalse(monitor.isRunning());
    }

    @Test
    void testRemoveStatusChangeListener() throws InterruptedException {
        monitor.addWorkflow("test-workflow", "github", "main", "CI Pipeline");

        CountDownLatch latch = new CountDownLatch(1);
        CIStatusMonitor.StatusChangeListener listener = event -> latch.countDown();

        monitor.addStatusChangeListener(listener);
        monitor.updateWorkflowStatus("test-workflow", CIStatusMonitor.WorkflowStatus.SUCCESS, Map.of());

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Remove the listener
        monitor.removeStatusChangeListener(listener);

        // This should not trigger the removed listener
        latch = new CountDownLatch(1);
        monitor.updateWorkflowStatus("test-workflow", CIStatusMonitor.WorkflowStatus.FAILURE, Map.of());

        assertFalse(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    void testMonitoredWorkflowWithStatus() {
        LocalDateTime now = LocalDateTime.now();

        CIStatusMonitor.MonitoredWorkflow baseWorkflow = new CIStatusMonitor.MonitoredWorkflow(
            "test-workflow", "github", "main", "CI Pipeline",
            now.minusHours(1), now.minusMinutes(30),
            CIStatusMonitor.WorkflowStatus.RUNNING, Map.of()
        );

        // Create updated workflow with new status
        CIStatusMonitor.MonitoredWorkflow updatedWorkflow = baseWorkflow.withStatus(
            CIStatusMonitor.WorkflowStatus.SUCCESS,
            now,
            Map.of("result", "passed")
        );

        assertEquals("test-workflow", updatedWorkflow.workflowId());
        assertEquals(CIStatusMonitor.WorkflowStatus.SUCCESS, updatedWorkflow.status());
        assertEquals(now, updatedWorkflow.lastChecked());
        assertTrue(updatedWorkflow.details().containsKey("result"));
    }
}