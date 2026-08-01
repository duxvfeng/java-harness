package com.chachamaru.harness.workflow.recovery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Peer recovery strategy (Phase 2).
 *
 * <p>Handles recovery by assigning the failed task to an alternative worker:
 * <ul>
 *   <li>Reassigning to alternative worker with different capabilities</li>
 *   <li>Load balancing across available workers</li>
 *   <li>Resource reallocation for better performance</li>
 * </ul>
 *
 * <p>This strategy is invoked when self-healing fails, indicating the issue
 * may be worker-specific rather than transient.
 *
 * @spec_reference spec.md#Workflow System - State Recovery
 */
public class PeerRecoveryStrategy implements RecoveryStrategy {

    /** Maximum number of peer recovery attempts */
    private static final int MAX_ATTEMPTS = 2;

    /** Error types this strategy can handle (worker-specific failures) */
    private static final Set<String> HANDLABLE_ERRORS = Set.of(
        "WorkerCapacityException",
        "ResourceExhaustedException",
        "WorkerFailedException",
        "AgentUnavailableException",
        "CapacityLimitException",
        "ResourceLimitException"
    );

    /** Registry of available workers for peer recovery */
    private final Map<String, WorkerInfo> availableWorkers;

    /** Assignment history to prevent re-assigning to failed workers */
    private final Map<String, Set<String>> failedWorkerAssignments;

    /**
     * Creates a peer recovery strategy.
     */
    public PeerRecoveryStrategy() {
        this.availableWorkers = new ConcurrentHashMap<>();
        this.failedWorkerAssignments = new ConcurrentHashMap<>();
        initializeDefaultWorkers();
    }

    /**
     * Creates a peer recovery strategy with custom workers.
     *
     * @param workers Custom worker registry
     */
    public PeerRecoveryStrategy(Map<String, WorkerInfo> workers) {
        this.availableWorkers = new ConcurrentHashMap<>(workers);
        this.failedWorkerAssignments = new ConcurrentHashMap<>();
    }

    @Override
    public RecoveryResult.RecoveryPhase getPhase() {
        return RecoveryResult.RecoveryPhase.PEER_RECOVERY;
    }

    @Override
    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    @Override
    public boolean canHandle(String errorType) {
        return HANDLABLE_ERRORS.contains(errorType) ||
               errorType.contains("Capacity") ||
               errorType.contains("Resource") ||
               errorType.contains("Worker");
    }

    @Override
    public RecoveryResult recover(String sessionId, RecoveryContext context)
            throws RecoveryException {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId cannot be null or blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }

        int attempt = context.previousAttempts() + 1;

        // Check if we've exceeded maximum attempts
        if (attempt > MAX_ATTEMPTS) {
            return RecoveryResult.failure(
                sessionId,
                getPhase(),
                String.format("Exceeded maximum peer recovery attempts (%d)", MAX_ATTEMPTS),
                attempt
            );
        }

        // Get failed worker ID from context or previous attempts
        String failedWorker = extractFailedWorkerId(context);
        if (failedWorker != null) {
            failedWorkerAssignments
                .computeIfAbsent(sessionId, k -> new HashSet<>())
                .add(failedWorker);
        }

        // Find alternative worker
        Optional<WorkerInfo> alternativeWorker = selectAlternativeWorker(sessionId);
        if (alternativeWorker.isEmpty()) {
            return RecoveryResult.failure(
                sessionId,
                getPhase(),
                "No available alternative workers",
                attempt
            );
        }

        WorkerInfo selectedWorker = alternativeWorker.get();

        System.out.printf(
            "[PeerRecovery] Attempt %d/%d for session %s - Reassigning to worker %s (type: %s)%n",
            attempt, MAX_ATTEMPTS, sessionId, selectedWorker.workerId(), selectedWorker.workerType()
        );

        // In a real implementation, this would:
        // 1. Reassign the task to the selected worker
        // 2. Monitor the new assignment
        // 3. Handle the result
        // For now, we simulate the reassignment

        // Check if this is the last attempt
        if (attempt >= MAX_ATTEMPTS) {
            // Last peer recovery attempt also failed, escalate to lead intervention
            return RecoveryResult.failure(
                sessionId,
                getPhase(),
                String.format("Peer recovery failed after %d attempts", attempt),
                attempt
            );
        }

        // Simulate peer recovery attempt (may need retry)
        return RecoveryResult.failure(
            sessionId,
            getPhase(),
            String.format("Reassigned to worker %s, monitoring progress", selectedWorker.workerId()),
            attempt
        );
    }

    /**
     * Extracts the failed worker ID from the recovery context.
     *
     * @param context Recovery context
     * @return Failed worker ID, or null if not available
     */
    private String extractFailedWorkerId(RecoveryContext context) {
        // In a real implementation, this would extract from error message or state
        if (context.stateSnapshot() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> state = (Map<String, Object>) context.stateSnapshot();
            Object workerId = state.get("workerId");
            if (workerId instanceof String) {
                return (String) workerId;
            }
        }
        return null;
    }

    /**
     * Selects an alternative worker for the given session.
     *
     * <p>Selection criteria:
     * <ul>
     *   <li>Not previously failed for this session</li>
     *   <li>Available capacity</li>
     *   <li>Least loaded (load balancing)</li>
     * </ul>
     *
     * @param sessionId Session ID
     * @return Optional containing selected worker info
     */
    private Optional<WorkerInfo> selectAlternativeWorker(String sessionId) {
        Set<String> failedWorkers = failedWorkerAssignments.getOrDefault(sessionId, Set.of());

        return availableWorkers.values().stream()
            .filter(worker -> !failedWorkers.contains(worker.workerId()))
            .filter(worker -> worker.hasCapacity())
            .min(Comparator.comparingInt(WorkerInfo::currentLoad))
            .or(() -> availableWorkers.values().stream()
                .filter(worker -> !failedWorkers.contains(worker.workerId()))
                .findFirst()
            );
    }

    /**
     * Initializes default workers for demonstration.
     */
    private void initializeDefaultWorkers() {
        availableWorkers.put("worker-1", new WorkerInfo(
            "worker-1",
            WorkerType.WORKER,
            5, // max capacity
            2  // current load
        ));
        availableWorkers.put("worker-2", new WorkerInfo(
            "worker-2",
            WorkerType.WORKER,
            5,
            3
        ));
        availableWorkers.put("worker-3", new WorkerInfo(
            "worker-3",
            WorkerType.WORKER,
            5,
            1
        ));
        availableWorkers.put("specialist-worker", new WorkerInfo(
            "specialist-worker",
            WorkerType.SPECIALIST,
            3,
            0
        ));
    }

    /**
     * Registers a new worker for peer recovery.
     *
     * @param workerId Worker ID
     * @param workerType Worker type
     * @param maxCapacity Maximum capacity
     * @param currentLoad Current load
     */
    public void registerWorker(String workerId, WorkerType workerType, int maxCapacity, int currentLoad) {
        availableWorkers.put(workerId, new WorkerInfo(workerId, workerType, maxCapacity, currentLoad));
    }

    /**
     * Removes a worker from the available pool.
     *
     * @param workerId Worker ID to remove
     */
    public void unregisterWorker(String workerId) {
        availableWorkers.remove(workerId);
    }

    /**
     * Clears failed worker history for a session.
     *
     * @param sessionId Session ID
     */
    public void clearFailedWorkerHistory(String sessionId) {
        failedWorkerAssignments.remove(sessionId);
    }

    /**
     * Gets the number of available workers.
     *
     * @return Number of available workers
     */
    public int getAvailableWorkerCount() {
        return availableWorkers.size();
    }

    /**
     * Checks if there are any workers with available capacity.
     *
     * @return true if at least one worker has capacity
     */
    public boolean hasAvailableWorkers() {
        return availableWorkers.values().stream().anyMatch(WorkerInfo::hasCapacity);
    }

    /**
     * Information about a worker.
     */
    public record WorkerInfo(
        String workerId,
        WorkerType workerType,
        int maxCapacity,
        int currentLoad
    ) {
        public WorkerInfo {
            if (workerId == null || workerId.isBlank()) {
                throw new IllegalArgumentException("workerId cannot be null or blank");
            }
            if (maxCapacity <= 0) {
                throw new IllegalArgumentException("maxCapacity must be positive");
            }
            if (currentLoad < 0) {
                throw new IllegalArgumentException("currentLoad cannot be negative");
            }
            if (currentLoad > maxCapacity) {
                throw new IllegalArgumentException("currentLoad cannot exceed maxCapacity");
            }
        }

        /**
         * Checks if this worker has available capacity.
         *
         * @return true if current load < max capacity
         */
        public boolean hasCapacity() {
            return currentLoad < maxCapacity;
        }

        /**
         * Gets the available capacity slots.
         *
         * @return Number of available slots
         */
        public int availableCapacity() {
            return maxCapacity - currentLoad;
        }
    }

    /**
     * Worker type classification.
     */
    public enum WorkerType {
        /** General purpose worker */
        WORKER,

        /** Specialist worker for specific tasks */
        SPECIALIST,

        /** Heavy-duty worker for resource-intensive tasks */
        HEAVY_WORKER
    }
}
