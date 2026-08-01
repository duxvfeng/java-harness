package com.chachamaru.harness.ci.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GitHub Actions Integration
 */
class GitHubActionsIntegrationTest {

    @TempDir
    Path tempDir;

    private GitHubActionsIntegration integration;

    @BeforeEach
    void setUp() {
        integration = new GitHubActionsIntegration("test-token", "test-owner", "test-repo");
    }

    @Test
    void testGitHubActionsIntegrationCreation() {
        assertNotNull(integration);
        // Test that integration was created successfully
        // Cannot directly access private fields
    }

    @Test
    void testWorkflowRunRecord() {
        GitHubActionsIntegration.WorkflowRun run = new GitHubActionsIntegration.WorkflowRun(
            12345L,
            "CI Pipeline",
            "completed",
            "success",
            "main",
            "abc123def456",
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now(),
            "push",
            "https://github.com/test-owner/test-repo/actions/runs/12345"
        );

        assertTrue(run.isCompleted());
        assertTrue(run.isSuccess());
        assertFalse(run.isFailure());
        assertEquals(12345L, run.id());
        assertEquals("CI Pipeline", run.name());
    }

    @Test
    void testWorkflowJobRecord() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        GitHubActionsIntegration.WorkflowJob job = new GitHubActionsIntegration.WorkflowJob(
            67890L,
            "build",
            "completed",
            "success",
            now.minusMinutes(5),
            now
        );

        assertTrue(job.isCompleted());
        assertTrue(job.isSuccess());
        assertFalse(job.isFailure());
        assertEquals(67890L, job.id());
        assertEquals("build", job.name());
        assertTrue(job.getDurationSeconds() > 0);
    }

    @Test
    void testWebhookEventRecord() {
        String jsonPayload = "{\"test\": \"data\"}";
        com.google.gson.JsonObject payload = new com.google.gson.JsonParser().parse(jsonPayload).getAsJsonObject();

        GitHubActionsIntegration.WebhookEvent event = new GitHubActionsIntegration.WebhookEvent(
            "workflow_run",
            payload,
            true,
            "Workflow completed successfully"
        );

        assertTrue(event.isSuccess());
        assertEquals("workflow_run", event.eventType());
        assertTrue(event.isWorkflowEvent());
        assertEquals("Workflow completed successfully", event.message());
    }

    @Test
    void testRepositoryDetectionFromGitConfig() throws IOException {
        // Create a mock .git/config file
        Path gitConfig = tempDir.resolve(".git/config");
        java.nio.file.Files.createDirectories(gitConfig.getParent());

        String configContent = """
            [remote "origin"]
                url = https://github.com/test-owner/test-repo.git
            """;
        java.nio.file.Files.write(gitConfig, configContent.getBytes());

        // Test detection
        GitHubActionsIntegration detectedIntegration = new GitHubActionsIntegration(tempDir, "token");
        // Successfully created integration from detected repository
        assertNotNull(detectedIntegration);
    }

    @Test
    void testRepositoryDetectionFromSshUrl() throws IOException {
        // Create a mock .git/config file with SSH URL
        Path gitConfig = tempDir.resolve(".git/config");
        java.nio.file.Files.createDirectories(gitConfig.getParent());

        String configContent = """
            [remote "origin"]
                url = git@github.com:ssh-owner/ssh-repo.git
            """;
        java.nio.file.Files.write(gitConfig, configContent.getBytes());

        // Test detection
        GitHubActionsIntegration detectedIntegration = new GitHubActionsIntegration(tempDir, "token");
        // Successfully created integration from SSH URL
        assertNotNull(detectedIntegration);
    }

    @Test
    void testWorkflowRunStatusCheck() {
        // Test various status combinations
        GitHubActionsIntegration.WorkflowRun successRun = new GitHubActionsIntegration.WorkflowRun(
            1L, "test", "completed", "success", "main", "sha",
            java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), "push", "url"
        );
        assertTrue(successRun.isSuccess());
        assertFalse(successRun.isFailure());

        GitHubActionsIntegration.WorkflowRun failureRun = new GitHubActionsIntegration.WorkflowRun(
            2L, "test", "completed", "failure", "main", "sha",
            java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), "push", "url"
        );
        assertFalse(failureRun.isSuccess());
        assertTrue(failureRun.isFailure());

        GitHubActionsIntegration.WorkflowRun runningRun = new GitHubActionsIntegration.WorkflowRun(
            3L, "test", "in_progress", null, "main", "sha",
            java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), "push", "url"
        );
        assertFalse(runningRun.isCompleted());
        assertFalse(runningRun.isSuccess());
        assertFalse(runningRun.isFailure());
    }

    @Test
    void testWorkflowJobStatusCheck() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        GitHubActionsIntegration.WorkflowJob successJob = new GitHubActionsIntegration.WorkflowJob(
            1L, "test", "completed", "success",
            now.minusMinutes(2), now.minusMinutes(1)
        );
        assertTrue(successJob.isSuccess());
        assertFalse(successJob.isFailure());

        GitHubActionsIntegration.WorkflowJob failedJob = new GitHubActionsIntegration.WorkflowJob(
            2L, "test", "completed", "failure",
            now.minusMinutes(4), now.minusMinutes(3)
        );
        assertFalse(failedJob.isSuccess());
        assertTrue(failedJob.isFailure());
    }
}