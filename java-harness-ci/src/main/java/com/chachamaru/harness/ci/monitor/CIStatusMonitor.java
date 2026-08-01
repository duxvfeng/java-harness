package com.chachamaru.harness.ci.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * CI Status Monitor
 * <p>
 * Provides real-time CI status monitoring with polling and event-driven updates:
 * <ul>
 *   <li>Automatic polling at configurable intervals</li>
 *   <li>Event-driven status updates via webhooks</li>
 *   <li>Status change notifications</li>
 *   <li>Failure detection and alerting</li>
 * </ul>
 * </p>
 */
public class CIStatusMonitor {
    private static final Logger log = LoggerFactory.getLogger(CIStatusMonitor.class);
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(30);

    private final ScheduledExecutorService scheduler;
    private final Map<String, MonitoredWorkflow> monitoredWorkflows;
    private final List<StatusChangeListener> statusChangeListeners;
    private final Map<String, CIStatusProvider> statusProviders;
    private Duration pollInterval;
    private boolean isRunning;

    public CIStatusMonitor() {
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "ci-status-monitor");
            t.setDaemon(true);
            return t;
        });
        this.monitoredWorkflows = new ConcurrentHashMap<>();
        this.statusChangeListeners = new CopyOnWriteArrayList<>();
        this.statusProviders = new ConcurrentHashMap<>();
        this.pollInterval = DEFAULT_POLL_INTERVAL;
        this.isRunning = false;

        log.info("CI Status Monitor initialized");
    }

    /**
     * Add a workflow to monitor
     *
     * @param workflowId   Unique workflow identifier
     * @param providerName CI provider name (github, gitlab, etc.)
     * @param branch       Branch to monitor
     * @param workflowName Workflow name
     */
    public void addWorkflow(String workflowId, String providerName, String branch, String workflowName) {
        MonitoredWorkflow workflow = new MonitoredWorkflow(
            workflowId,
            providerName,
            branch,
            workflowName,
            LocalDateTime.now(),
            null
        );
        monitoredWorkflows.put(workflowId, workflow);
        log.info("Added workflow to monitor: {} ({}/{})", workflowId, providerName, workflowName);
    }

    /**
     * Remove a workflow from monitoring
     *
     * @param workflowId Workflow identifier
     */
    public void removeWorkflow(String workflowId) {
        MonitoredWorkflow removed = monitoredWorkflows.remove(workflowId);
        if (removed != null) {
            log.info("Removed workflow from monitoring: {}", workflowId);
        }
    }

    /**
     * Register a CI status provider
     *
     * @param providerName Provider name
     * @param provider     Status provider implementation
     */
    public void registerStatusProvider(String providerName, CIStatusProvider provider) {
        statusProviders.put(providerName.toLowerCase(), provider);
        log.info("Registered CI status provider: {}", providerName);
    }

    /**
     * Add status change listener
     *
     * @param listener Listener to notify of status changes
     */
    public void addStatusChangeListener(StatusChangeListener listener) {
        statusChangeListeners.add(listener);
        log.debug("Added status change listener: {}", listener.getClass().getSimpleName());
    }

    /**
     * Remove status change listener
     *
     * @param listener Listener to remove
     */
    public void removeStatusChangeListener(StatusChangeListener listener) {
        statusChangeListeners.remove(listener);
    }

    /**
     * Set polling interval
     *
     * @param interval Polling interval
     */
    public void setPollInterval(Duration interval) {
        this.pollInterval = interval;
        log.info("Polling interval set to: {}", interval);
    }

    /**
     * Start monitoring
     */
    public synchronized void start() {
        if (isRunning) {
            log.warn("CI Status Monitor is already running");
            return;
        }

        isRunning = true;
        log.info("Starting CI Status Monitor with interval: {}", pollInterval);

        // Schedule periodic status checks
        scheduler.scheduleAtFixedRate(
            this::performStatusCheck,
            pollInterval.toMillis(),
            pollInterval.toMillis(),
            TimeUnit.MILLISECONDS
        );

        log.info("CI Status Monitor started");
    }

    /**
     * Stop monitoring
     */
    public synchronized void stop() {
        if (!isRunning) {
            log.warn("CI Status Monitor is not running");
            return;
        }

        isRunning = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("CI Status Monitor stopped");
    }

    /**
     * Check if monitoring is running
     *
     * @return true if monitoring is active
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Get all monitored workflows
     *
     * @return Map of workflow ID to monitored workflow
     */
    public Map<String, MonitoredWorkflow> getMonitoredWorkflows() {
        return new HashMap<>(monitoredWorkflows);
    }

    /**
     * Get monitored workflow by ID
     *
     * @param workflowId Workflow identifier
     * @return Optional monitored workflow
     */
    public Optional<MonitoredWorkflow> getWorkflow(String workflowId) {
        return Optional.ofNullable(monitoredWorkflows.get(workflowId));
    }

    /**
     * Update workflow status externally (e.g., from webhook)
     *
     * @param workflowId Workflow identifier
     * @param newStatus  New status
     * @param details    Additional details
     */
    public void updateWorkflowStatus(String workflowId, WorkflowStatus newStatus, Map<String, Object> details) {
        MonitoredWorkflow workflow = monitoredWorkflows.get(workflowId);
        if (workflow == null) {
            log.warn("Attempted to update non-existent workflow: {}", workflowId);
            return;
        }

        WorkflowStatus oldStatus = workflow.status();
        workflow = workflow.withStatus(newStatus, LocalDateTime.now(), details);
        monitoredWorkflows.put(workflowId, workflow);

        log.info("Updated workflow status: {} {} -> {}", workflowId, oldStatus, newStatus);

        // Notify listeners if status changed
        if (!Objects.equals(oldStatus, newStatus)) {
            notifyStatusChange(workflow, oldStatus, newStatus);
        }
    }

    /**
     * Perform status check on all monitored workflows
     */
    private void performStatusCheck() {
        if (!isRunning) {
            return;
        }

        log.debug("Performing status check on {} workflows", monitoredWorkflows.size());

        for (MonitoredWorkflow workflow : monitoredWorkflows.values()) {
            try {
                checkWorkflowStatus(workflow);
            } catch (Exception e) {
                log.error("Failed to check status for workflow {}: {}", workflow.workflowId(), e.getMessage());
            }
        }
    }

    /**
     * Check status of a single workflow
     */
    private void checkWorkflowStatus(MonitoredWorkflow workflow) {
        CIStatusProvider provider = statusProviders.get(workflow.providerName().toLowerCase());
        if (provider == null) {
            log.warn("No status provider found for: {}", workflow.providerName());
            return;
        }

        try {
            WorkflowStatus currentStatus = provider.getStatus(workflow);
            WorkflowStatus oldStatus = workflow.status();

            if (!Objects.equals(oldStatus, currentStatus)) {
                log.info("Status change detected for {}: {} -> {}", workflow.workflowId(), oldStatus, currentStatus);

                // Update workflow
                MonitoredWorkflow updatedWorkflow = workflow.withStatus(currentStatus, LocalDateTime.now(), null);
                monitoredWorkflows.put(workflow.workflowId(), updatedWorkflow);

                // Notify listeners
                notifyStatusChange(updatedWorkflow, oldStatus, currentStatus);
            }

        } catch (Exception e) {
            log.error("Failed to get status for workflow {}: {}", workflow.workflowId(), e.getMessage());
        }
    }

    /**
     * Notify all listeners of status change
     */
    private void notifyStatusChange(MonitoredWorkflow workflow, WorkflowStatus oldStatus, WorkflowStatus newStatus) {
        StatusChangeEvent event = new StatusChangeEvent(workflow, oldStatus, newStatus, LocalDateTime.now());

        for (StatusChangeListener listener : statusChangeListeners) {
            try {
                listener.onStatusChange(event);
            } catch (Exception e) {
                log.error("Listener {} failed to handle status change: {}",
                         listener.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    /**
     * CI Status Provider interface
     */
    @FunctionalInterface
    public interface CIStatusProvider {
        /**
         * Get current status of a workflow
         *
         * @param workflow Workflow to check
         * @return Current status
         */
        WorkflowStatus getStatus(MonitoredWorkflow workflow) throws Exception;
    }

    /**
     * Status change listener interface
     */
    @FunctionalInterface
    public interface StatusChangeListener {
        /**
         * Called when workflow status changes
         *
         * @param event Status change event
         */
        void onStatusChange(StatusChangeEvent event);
    }

    /**
     * Status change event
     */
    public record StatusChangeEvent(
        MonitoredWorkflow workflow,
        WorkflowStatus oldStatus,
        WorkflowStatus newStatus,
        LocalDateTime timestamp
    ) {
        public boolean isFailure() {
            return newStatus == WorkflowStatus.FAILURE || newStatus == WorkflowStatus.ERROR;
        }

        public boolean isSuccess() {
            return newStatus == WorkflowStatus.SUCCESS;
        }

        public boolean isPending() {
            return newStatus == WorkflowStatus.PENDING || newStatus == WorkflowStatus.RUNNING;
        }
    }

    /**
     * Monitored workflow record
     */
    public record MonitoredWorkflow(
        String workflowId,
        String providerName,
        String branch,
        String workflowName,
        LocalDateTime addedAt,
        LocalDateTime lastChecked,
        WorkflowStatus status,
        Map<String, Object> details
    ) {
        public MonitoredWorkflow(String workflowId, String providerName, String branch, String workflowName,
                               LocalDateTime addedAt, LocalDateTime lastChecked) {
            this(workflowId, providerName, branch, workflowName, addedAt, lastChecked,
                 WorkflowStatus.UNKNOWN, Map.of());
        }

        public MonitoredWorkflow withStatus(WorkflowStatus status, LocalDateTime lastChecked, Map<String, Object> details) {
            return new MonitoredWorkflow(workflowId, providerName, branch, workflowName,
                                       addedAt, lastChecked, status,
                                       details != null ? details : this.details);
        }

        public boolean isRunning() {
            return status == WorkflowStatus.RUNNING || status == WorkflowStatus.PENDING;
        }

        public boolean isCompleted() {
            return status == WorkflowStatus.SUCCESS || status == WorkflowStatus.FAILURE ||
                   status == WorkflowStatus.ERROR || status == WorkflowStatus.CANCELLED;
        }

        public boolean isSuccess() {
            return status == WorkflowStatus.SUCCESS;
        }
    }

    /**
     * Workflow status enum
     */
    public enum WorkflowStatus {
        UNKNOWN,
        PENDING,
        QUEUED,
        RUNNING,
        SUCCESS,
        FAILURE,
        ERROR,
        CANCELLED,
        TIMED_OUT
    }
}