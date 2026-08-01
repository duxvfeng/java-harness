package com.chachamaru.harness.ci.monitor;

import com.chachamaru.harness.ci.github.GitHubActionsIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GitHub status provider for CI Status Monitor
 * <p>
 * Adapts GitHub Actions API to the CI Status Monitor interface
 * </p>
 */
public class GitHubStatusProvider implements CIStatusMonitor.CIStatusProvider {

    private static final Logger log = LoggerFactory.getLogger(GitHubStatusProvider.class);

    private final GitHubActionsIntegration integration;

    public GitHubStatusProvider(GitHubActionsIntegration integration) {
        this.integration = integration;
    }

    @Override
    public CIStatusMonitor.WorkflowStatus getStatus(CIStatusMonitor.MonitoredWorkflow workflow) throws Exception {
        try {
            // Get recent workflow runs for the branch
            String workflowName = workflow.workflowName();
            String branch = workflow.branch();

            log.debug("Checking GitHub status for workflow: {} on branch: {}", workflowName, branch);

            // Get recent runs (limit to 5 for efficiency)
            var runs = integration.getWorkflowRuns(branch, 5);

            // Find the most recent run matching the workflow name
            GitHubActionsIntegration.WorkflowRun latestRun = null;
            for (var run : runs) {
                if (workflowName == null || workflowName.isEmpty() || run.name().equals(workflowName)) {
                    if (latestRun == null || run.updatedAt().isAfter(latestRun.updatedAt())) {
                        latestRun = run;
                    }
                }
            }

            if (latestRun == null) {
                log.warn("No workflow runs found for: {} on branch: {}", workflowName, branch);
                return CIStatusMonitor.WorkflowStatus.UNKNOWN;
            }

            // Convert GitHub status to monitor status
            return convertStatus(latestRun);

        } catch (IOException e) {
            log.error("Failed to get GitHub status: {}", e.getMessage());
            return CIStatusMonitor.WorkflowStatus.ERROR;
        }
    }

    /**
     * Convert GitHub Actions status to monitor status
     */
    private CIStatusMonitor.WorkflowStatus convertStatus(GitHubActionsIntegration.WorkflowRun run) {
        String status = run.status();
        String conclusion = run.conclusion();

        return switch (status.toLowerCase()) {
            case "completed" -> {
                if (conclusion == null) {
                    yield CIStatusMonitor.WorkflowStatus.UNKNOWN;
                }
                yield switch (conclusion.toLowerCase()) {
                    case "success" -> CIStatusMonitor.WorkflowStatus.SUCCESS;
                    case "failure" -> CIStatusMonitor.WorkflowStatus.FAILURE;
                    case "timed_out" -> CIStatusMonitor.WorkflowStatus.TIMED_OUT;
                    case "cancelled" -> CIStatusMonitor.WorkflowStatus.CANCELLED;
                    default -> CIStatusMonitor.WorkflowStatus.UNKNOWN;
                };
            }
            case "in_progress" -> CIStatusMonitor.WorkflowStatus.RUNNING;
            case "queued" -> CIStatusMonitor.WorkflowStatus.QUEUED;
            case "pending" -> CIStatusMonitor.WorkflowStatus.PENDING;
            case "waiting" -> CIStatusMonitor.WorkflowStatus.PENDING;
            case "requested" -> CIStatusMonitor.WorkflowStatus.PENDING;
            default -> {
                log.warn("Unknown GitHub status: {} / {}", status, conclusion);
                yield CIStatusMonitor.WorkflowStatus.UNKNOWN;
            }
        };
    }

    /**
     * Create workflow details from GitHub run
     */
    public Map<String, Object> createWorkflowDetails(GitHubActionsIntegration.WorkflowRun run) {
        Map<String, Object> details = new HashMap<>();
        details.put("runId", run.id());
        details.put("name", run.name());
        details.put("status", run.status());
        details.put("conclusion", run.conclusion());
        details.put("branch", run.branch());
        details.put("commitSha", run.commitSha());
        details.put("event", run.event());
        details.put("url", run.url());
        details.put("createdAt", run.createdAt().toString());
        details.put("updatedAt", run.updatedAt().toString());
        return details;
    }
}