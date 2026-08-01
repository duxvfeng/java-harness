package com.chachamaru.harness.ci.gitlab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GitLab CI Integration
 */
class GitLabCIIntegrationTest {

    @TempDir
    Path tempDir;

    private GitLabCIIntegration integration;

    @BeforeEach
    void setUp() {
        integration = new GitLabCIIntegration("test-token", "test-group/test-project");
    }

    @Test
    void testGitLabCIIntegrationCreation() {
        assertNotNull(integration);
        assertEquals("test-group/test-project", integration.projectId);
    }

    @Test
    void testPipelineRecord() {
        GitLabCIIntegration.Pipeline pipeline = new GitLabCIIntegration.Pipeline(
            12345L,
            "success",
            "main",
            "abc123def456",
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now(),
            "https://gitlab.com/test-group/test-project/-/pipelines/12345",
            "testuser"
        );

        assertTrue(pipeline.isCompleted());
        assertTrue(pipeline.isSuccess());
        assertFalse(pipeline.isFailure());
        assertEquals(12345L, pipeline.id());
        assertEquals("success", pipeline.status());
    }

    @Test
    void testPipelineJobRecord() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        GitLabCIIntegration.PipelineJob job = new GitLabCIIntegration.PipelineJob(
            67890L,
            "build",
            "success",
            "build",
            now.minusMinutes(5),
            now,
            false
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

        GitLabCIIntegration.WebhookEvent event = new GitLabCIIntegration.WebhookEvent(
            "Pipeline Hook",
            payload,
            true,
            "Pipeline completed successfully"
        );

        assertTrue(event.isSuccess());
        assertEquals("Pipeline Hook", event.eventType());
        assertTrue(event.isPipelineEvent());
        assertFalse(event.isJobEvent());
        assertEquals("Pipeline completed successfully", event.message());
    }

    @Test
    void testProjectDetectionFromGitConfig() throws IOException {
        // Create a mock .git/config file
        Path gitConfig = tempDir.resolve(".git/config");
        java.nio.file.Files.createDirectories(gitConfig.getParent());

        String configContent = """
            [remote "origin"]
                url = https://gitlab.com/test-group/test-project.git
            """;
        java.nio.file.Files.write(gitConfig, configContent.getBytes());

        // Test detection
        GitLabCIIntegration detectedIntegration = new GitLabCIIntegration(tempDir, "token");
        assertEquals("test-group/test-project", detectedIntegration.projectId);
    }

    @Test
    void testProjectDetectionFromSshUrl() throws IOException {
        // Create a mock .git/config file with SSH URL
        Path gitConfig = tempDir.resolve(".git/config");
        java.nio.file.Files.createDirectories(gitConfig.getParent());

        String configContent = """
            [remote "origin"]
                url = git@gitlab.com:ssh-group/ssh-project.git
            """;
        java.nio.file.Files.write(gitConfig, configContent.getBytes());

        // Test detection
        GitLabCIIntegration detectedIntegration = new GitLabCIIntegration(tempDir, "token");
        assertEquals("ssh-group/ssh-project", detectedIntegration.projectId);
    }

    @Test
    void testPipelineStatusCheck() {
        // Test various status combinations
        GitLabCIIntegration.Pipeline successPipeline = new GitLabCIIntegration.Pipeline(
            1L, "success", "main", "sha",
            java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
            "url", "user"
        );
        assertTrue(successPipeline.isSuccess());
        assertFalse(successPipeline.isFailure());
        assertTrue(successPipeline.isCompleted());

        GitLabCIIntegration.Pipeline failurePipeline = new GitLabCIIntegration.Pipeline(
            2L, "failed", "main", "sha",
            java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
            "url", "user"
        );
        assertFalse(failurePipeline.isSuccess());
        assertTrue(failurePipeline.isFailure());
        assertTrue(failurePipeline.isCompleted());

        GitLabCIIntegration.Pipeline runningPipeline = new GitLabCIIntegration.Pipeline(
            3L, "running", "main", "sha",
            java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
            "url", "user"
        );
        assertFalse(runningPipeline.isCompleted());
        assertTrue(runningPipeline.isRunning());
    }

    @Test
    void testPipelineJobStatusCheck() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        GitLabCIIntegration.PipelineJob successJob = new GitLabCIIntegration.PipelineJob(
            1L, "test", "success", "test",
            now.minusMinutes(2), now.minusMinutes(1),
            false
        );
        assertTrue(successJob.isSuccess());
        assertFalse(successJob.isFailure());

        GitLabCIIntegration.PipelineJob failedJob = new GitLabCIIntegration.PipelineJob(
            2L, "test", "failed", "test",
            now.minusMinutes(4), now.minusMinutes(3),
            false
        );
        assertFalse(failedJob.isSuccess());
        assertTrue(failedJob.isFailure());
    }
}