package com.chachamaru.harness.workflow.orchestration;

import com.chachamaru.harness.workflow.model.PlansDocument;

/**
 * Task orchestrator interface.
 *
 * <p>Responsible for creating execution plans and managing
 * the execution of workflow tasks.</p>
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Create optimized execution plans from PlansDocument</li>
 *   <li>Execute plans with support for pause/resume/cancel</li>
 *   <li>Track execution state and metrics</li>
 * </ul>
 *
 * @spec_reference spec.md#API Contracts
 */
public interface TaskOrchestrator {

    /**
     * Creates an execution plan from a PlansDocument.
     *
     * <p>Analyzes task dependencies and determines optimal execution order.
     * Supports sequential, parallel, and hybrid execution strategies.</p>
     *
     * @param plans The plans document to create a plan for
     * @return An optimized orchestration plan
     * @throws OrchestrationException if plan creation fails
     */
    OrchestrationPlan createPlan(PlansDocument plans) throws OrchestrationException;

    /**
     * Executes an orchestration plan.
     *
     * <p>Executes tasks according to the plan's strategy.
     * Returns when execution completes (success or failure).</p>
     *
     * @param plan The plan to execute
     * @return Execution result with outcomes and metrics
     * @throws OrchestrationException if execution fails catastrophically
     */
    ExecutionResult execute(OrchestrationPlan plan) throws OrchestrationException;

    /**
     * Pauses a running execution.
     *
     * @param executionId The execution ID to pause
     * @throws OrchestrationException if pause fails
     */
    void pause(String executionId) throws OrchestrationException;

    /**
     * Resumes a paused execution.
     *
     * @param executionId The execution ID to resume
     * @return Updated execution result
     * @throws OrchestrationException if resume fails
     */
    ExecutionResult resume(String executionId) throws OrchestrationException;

    /**
     * Cancels a running or paused execution.
     *
     * @param executionId The execution ID to cancel
     * @throws OrchestrationException if cancellation fails
     */
    void cancel(String executionId) throws OrchestrationException;

    /**
     * Exception thrown when orchestration operations fail.
     */
    class OrchestrationException extends Exception {
        private final String executionId;
        private final String planId;

        public OrchestrationException(String message, String executionId, String planId) {
            super(message);
            this.executionId = executionId;
            this.planId = planId;
        }

        public OrchestrationException(String message, Throwable cause, String executionId, String planId) {
            super(message, cause);
            this.executionId = executionId;
            this.planId = planId;
        }

        public String getExecutionId() {
            return executionId;
        }

        public String getPlanId() {
            return planId;
        }
    }
}
