package com.chachamaru.harness.ci.monitor;

import com.chachamaru.harness.ci.gitlab.GitLabCIIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GitLab status provider for CI Status Monitor
 * <p>
 * Adapts GitLab CI API to the CI Status Monitor interface
 * </p>
 */
public class GitLabStatusProvider implements CIStatusMonitor.CIStatusProvider {

    private static final Logger log = LoggerFactory.getLogger(GitLabStatusProvider.class);

    private final GitLabCIIntegration integration;

    public GitLabStatusProvider(GitLabCIIntegration integration) {
        this.integration = integration;
    }

    @Override
    public CIStatusMonitor.WorkflowStatus getStatus(CIStatusMonitor.MonitoredWorkflow workflow) throws Exception {
        try {
            // Get recent pipelines for the branch
            String branch = workflow.branch();

            log.debug("Checking GitLab status for workflow on branch: {}", branch);

            // Get recent pipelines (limit to 5 for efficiency)
            var pipelines = integration.getPipelines(branch, 5);

            if (pipelines.isEmpty()) {
                log.warn("No pipelines found for branch: {}", branch);
                return CIStatusMonitor.WorkflowStatus.UNKNOWN;
            }

            // Get the most recent pipeline
            GitLabCIIntegration.Pipeline latestPipeline = pipelines.get(0);

            // Convert GitLab status to monitor status
            return convertStatus(latestPipeline);

        } catch (IOException e) {
            log.error("Failed to get GitLab status: {}", e.getMessage());
            return CIStatusMonitor.WorkflowStatus.ERROR;
        }
    }

    /**
     * Convert GitLab CI status to monitor status
     */
    private CIStatusMonitor.WorkflowStatus convertStatus(GitLabCIIntegration.Pipeline pipeline) {
        String status = pipeline.status();

        return switch (status.toLowerCase()) {
            case "success" -> CIStatusMonitor.WorkflowStatus.SUCCESS;
            case "failed" -> CIStatusMonitor.WorkflowStatus.FAILURE;
            case "running" -> CIStatusMonitor.WorkflowStatus.RUNNING;
            case "pending" -> CIStatusMonitor.WorkflowStatus.PENDING;
            case "created" -> CIStatusMonitor.WorkflowStatus.PENDING;
            case "skipped" -> CIStatusMonitor.WorkflowStatus.CANCELLED;
            case "canceled" -> CIStatusMonitor.WorkflowStatus.CANCELLED;
            case "manual" -> CIStatusMonitor.WorkflowStatus.PENDING;
            default -> {
                log.warn("Unknown GitLab status: {}", status);
                yield CIStatusMonitor.WorkflowStatus.UNKNOWN;
            }
        };
    }

    /**
     * Create workflow details from GitLab pipeline
     */
    public Map<String, Object> createWorkflowDetails(GitLabCIIntegration.Pipeline pipeline) {
        Map<String, Object> details = new HashMap<>();
        details.put("pipelineId", pipeline.id());
        details.put("status", pipeline.status());
        details.put("ref", pipeline.ref());
        details.put("sha", pipeline.sha());
        details.put("webUrl", pipeline.webUrl());
        details.put("userName", pipeline.userName());
        details.put("createdAt", pipeline.createdAt().toString());
        details.put("updatedAt", pipeline.updatedAt().toString());
        return details;
    }
}