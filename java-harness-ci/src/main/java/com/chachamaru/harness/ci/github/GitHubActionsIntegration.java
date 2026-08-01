package com.chachamaru.harness.ci.github;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/**
 * GitHub Actions CI Integration Service
 * <p>
 * Provides integration with GitHub Actions including:
 * <ul>
 *   <li>Workflow status monitoring</li>
 *   <li>Webhook event processing</li>
 *   <li>Run management and control</li>
 *   <li>Authentication and rate limit handling</li>
 * </ul>
 * </p>
 */
public class GitHubActionsIntegration {
    private static final Logger log = LoggerFactory.getLogger(GitHubActionsIntegration.class);
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String GITHUB_WEBHOOK_USER_AGENT = "GitHub-Hookshot";

    private final String githubToken;
    private final String repositoryOwner;
    private final String repositoryName;
    private final Gson gson;
    private final CloseableHttpClient httpClient;

    /**
     * Constructor with GitHub credentials
     *
     * @param githubToken     GitHub personal access token for authentication
     * @param repositoryOwner Repository owner (organization or user)
     * @param repositoryName  Repository name
     */
    public GitHubActionsIntegration(String githubToken, String repositoryOwner, String repositoryName) {
        this.githubToken = githubToken;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.gson = new Gson();
        this.httpClient = HttpClients.createDefault();
        log.info("GitHub Actions integration initialized for {}/{}", repositoryOwner, repositoryName);
    }

    /**
     * Constructor with auto-detection from git repository
     *
     * @param projectRoot Project root directory
     * @param githubToken Optional GitHub token (can be null)
     * @throws IOException if git config cannot be read
     */
    public GitHubActionsIntegration(Path projectRoot, String githubToken) throws IOException {
        this(githubToken, detectRepositoryOwner(projectRoot), detectRepositoryName(projectRoot));
    }

    /**
     * Detect repository owner from git configuration
     */
    private static String detectRepositoryOwner(Path projectRoot) throws IOException {
        Path gitConfig = projectRoot.resolve(".git/config");
        if (!Files.exists(gitConfig)) {
            throw new IOException("Not a git repository: " + projectRoot);
        }

        List<String> lines = Files.readAllLines(gitConfig);
        for (String line : lines) {
            if (line.trim().startsWith("url =")) {
                String url = line.substring(line.indexOf("url =") + 6).trim();
                if (url.contains("github.com")) {
                    // Extract owner from URL like: https://github.com/owner/repo.git or git@github.com:owner/repo.git
                    String[] parts = url.split("github.com[:/]");
                    if (parts.length > 1) {
                        String repoPath = parts[parts.length - 1].replace(".git", "");
                        String[] pathParts = repoPath.split("/");
                        if (pathParts.length > 1) {
                            return pathParts[pathParts.length - 2];
                        }
                    }
                }
            }
        }
        throw new IOException("Could not detect repository owner from git config");
    }

    /**
     * Detect repository name from git configuration
     */
    private static String detectRepositoryName(Path projectRoot) throws IOException {
        Path gitConfig = projectRoot.resolve(".git/config");
        if (!Files.exists(gitConfig)) {
            throw new IOException("Not a git repository: " + projectRoot);
        }

        List<String> lines = Files.readAllLines(gitConfig);
        for (String line : lines) {
            if (line.trim().startsWith("url =")) {
                String url = line.substring(line.indexOf("url =") + 6).trim();
                if (url.contains("github.com")) {
                    String[] parts = url.split("github.com[:/]");
                    if (parts.length > 1) {
                        String repoPath = parts[parts.length - 1].replace(".git", "");
                        String[] pathParts = repoPath.split("/");
                        if (pathParts.length > 0) {
                            return pathParts[pathParts.length - 1];
                        }
                    }
                }
            }
        }
        throw new IOException("Could not detect repository name from git config");
    }

    /**
     * Get workflow runs for a branch
     *
     * @param branch Branch name (default: "main")
     * @param limit  Maximum number of runs to return
     * @return List of workflow runs
     */
    public List<WorkflowRun> getWorkflowRuns(String branch, int limit) throws IOException {
        String endpoint = String.format("/repos/%s/%s/actions/runs", repositoryOwner, repositoryName);
        String url = GITHUB_API_BASE + endpoint;
        if (branch != null && !branch.isEmpty()) {
            url += "?branch=" + branch;
        }
        if (limit > 0) {
            url += (url.contains("?") ? "&" : "?") + "per_page=" + limit;
        }

        log.debug("Fetching workflow runs from: {}", url);
        HttpGet request = new HttpGet(url);
        request.addHeader("Authorization", "Bearer " + githubToken);
        request.addHeader("Accept", "application/vnd.github+json");
        request.addHeader("X-GitHub-Api-Version", "2022-11-28");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() >= 400) {
                throw new IOException("GitHub API request failed with status: " + response.getCode());
            }

            JsonObject jsonResponse = gson.fromJson(new InputStreamReader(response.getEntity().getContent()), JsonObject.class);
            return parseWorkflowRuns(jsonResponse);
        }
    }

    /**
     * Get specific workflow run by ID
     *
     * @param runId Workflow run ID
     * @return Workflow run details
     */
    public WorkflowRun getWorkflowRun(long runId) throws IOException {
        String endpoint = String.format("/repos/%s/%s/actions/runs/%d", repositoryOwner, repositoryName, runId);
        String url = GITHUB_API_BASE + endpoint;

        log.debug("Fetching workflow run {} from: {}", runId, url);
        HttpGet request = new HttpGet(url);
        request.addHeader("Authorization", "Bearer " + githubToken);
        request.addHeader("Accept", "application/vnd.github+json");
        request.addHeader("X-GitHub-Api-Version", "2022-11-28");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() >= 400) {
                throw new IOException("GitHub API request failed with status: " + response.getCode());
            }

            JsonObject jsonResponse = gson.fromJson(new InputStreamReader(response.getEntity().getContent()), JsonObject.class);
            return parseWorkflowRun(jsonResponse);
        }
    }

    /**
     * Get workflow run jobs
     *
     * @param runId Workflow run ID
     * @return List of jobs in the workflow run
     */
    public List<WorkflowJob> getWorkflowJobs(long runId) throws IOException {
        String endpoint = String.format("/repos/%s/%s/actions/runs/%d/jobs", repositoryOwner, repositoryName, runId);
        String url = GITHUB_API_BASE + endpoint;

        log.debug("Fetching workflow jobs for run {} from: {}", runId, url);
        HttpGet request = new HttpGet(url);
        request.addHeader("Authorization", "Bearer " + githubToken);
        request.addHeader("Accept", "application/vnd.github+json");
        request.addHeader("X-GitHub-Api-Version", "2022-11-28");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() >= 400) {
                throw new IOException("GitHub API request failed with status: " + response.getCode());
            }

            JsonObject jsonResponse = gson.fromJson(new InputStreamReader(response.getEntity().getContent()), JsonObject.class);
            return parseWorkflowJobs(jsonResponse);
        }
    }

    /**
     * Re-run a failed workflow
     *
     * @param runId Workflow run ID to re-run
     * @return true if re-run was triggered successfully
     */
    public boolean rerunWorkflow(long runId) throws IOException {
        String endpoint = String.format("/repos/%s/%s/actions/runs/%d/rerun", repositoryOwner, repositoryName, runId);
        String url = GITHUB_API_BASE + endpoint;

        log.info("Re-running workflow {} at: {}", runId, url);
        HttpPost request = new HttpPost(url);
        request.addHeader("Authorization", "Bearer " + githubToken);
        request.addHeader("Accept", "application/vnd.github+json");
        request.addHeader("X-GitHub-Api-Version", "2022-11-28");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            boolean success = response.getCode() >= 200 && response.getCode() < 300;
            if (success) {
                log.info("Successfully triggered re-run for workflow {}", runId);
            } else {
                log.warn("Failed to re-run workflow {}, status: {}", runId, response.getCode());
            }
            return success;
        }
    }

    /**
     * Process GitHub webhook event
     *
     * @param eventType   Webhook event type (e.g., "workflow_run", "workflow_job")
     * @param payloadJson JSON payload from webhook
     * @return Parsed webhook event
     */
    public WebhookEvent processWebhook(String eventType, String payloadJson) {
        log.debug("Processing webhook event: {}", eventType);
        try {
            JsonObject payload = gson.fromJson(payloadJson, JsonObject.class);
            return switch (eventType) {
                case "workflow_run" -> parseWorkflowRunEvent(payload);
                case "workflow_job" -> parseWorkflowJobEvent(payload);
                case "push" -> parsePushEvent(payload);
                case "pull_request" -> parsePullRequestEvent(payload);
                default -> {
                    log.warn("Unsupported webhook event type: {}", eventType);
                    yield new WebhookEvent(eventType, payload, false, "Unsupported event type");
                }
            };
        } catch (Exception e) {
            log.error("Failed to process webhook event: {}", e.getMessage(), e);
            return new WebhookEvent(eventType, null, false, "Failed to parse payload: " + e.getMessage());
        }
    }

    /**
     * Get commit SHA from branch
     *
     * @param branch Branch name
     * @return Commit SHA
     */
    public String getBranchCommit(String branch) throws IOException {
        String endpoint = String.format("/repos/%s/%s/git/ref/heads/%s", repositoryOwner, repositoryName, branch);
        String url = GITHUB_API_BASE + endpoint;

        log.debug("Getting commit SHA for branch {} from: {}", branch, url);
        HttpGet request = new HttpGet(url);
        request.addHeader("Authorization", "Bearer " + githubToken);
        request.addHeader("Accept", "application/vnd.github+json");
        request.addHeader("X-GitHub-Api-Version", "2022-11-28");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() >= 400) {
                throw new IOException("GitHub API request failed with status: " + response.getCode());
            }

            JsonObject jsonResponse = gson.fromJson(new InputStreamReader(response.getEntity().getContent()), JsonObject.class);
            if (jsonResponse.has("object") && jsonResponse.getAsJsonObject("object").has("sha")) {
                return jsonResponse.getAsJsonObject("object").get("sha").getAsString();
            }
            throw new IOException("No commit SHA found in response");
        }
    }

    /**
     * Check if GitHub Actions is enabled for this repository
     *
     * @return true if GitHub Actions is enabled
     */
    public boolean isActionsEnabled() throws IOException {
        String endpoint = String.format("/repos/%s/%s/actions/workflows", repositoryOwner, repositoryName);
        String url = GITHUB_API_BASE + endpoint;

        HttpGet request = new HttpGet(url);
        request.addHeader("Authorization", "Bearer " + githubToken);
        request.addHeader("Accept", "application/vnd.github+json");
        request.addHeader("X-GitHub-Api-Version", "2022-11-28");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return response.getCode() >= 200 && response.getCode() < 300;
        }
    }

    /**
     * Close HTTP client
     */
    public void close() throws IOException {
        httpClient.close();
    }

    // Private parsing methods

    private static WorkflowRun parseWorkflowRun(JsonObject runObj) {
        long id = runObj.get("id").getAsLong();
        String name = runObj.get("name").getAsString();
        String status = runObj.get("status").getAsString();
        String conclusion = runObj.has("conclusion") && !runObj.get("conclusion").isJsonNull()
            ? runObj.get("conclusion").getAsString() : null;
        String branch = runObj.getAsJsonObject("head_branch").getAsString();
        String commitSha = runObj.getAsJsonObject("head_sha").getAsString();
        LocalDateTime createdAt = parseDateTime(runObj.get("created_at").getAsString());
        LocalDateTime updatedAt = parseDateTime(runObj.get("updated_at").getAsString());
        String event = runObj.get("event").getAsString();
        String url = runObj.get("html_url").getAsString();

        return new WorkflowRun(id, name, status, conclusion, branch, commitSha,
                                createdAt, updatedAt, event, url);
    }

    private static List<WorkflowRun> parseWorkflowRuns(JsonObject response) {
        List<WorkflowRun> runs = new ArrayList<>();
        if (response.has("workflow_runs")) {
            var runArray = response.getAsJsonArray("workflow_runs");
            for (int i = 0; i < runArray.size(); i++) {
                JsonObject runObj = runArray.get(i).getAsJsonObject();
                runs.add(parseWorkflowRun(runObj));
            }
        }
        return runs;
    }

    private static List<WorkflowJob> parseWorkflowJobs(JsonObject response) {
        List<WorkflowJob> jobs = new ArrayList<>();
        if (response.has("jobs")) {
            var jobArray = response.getAsJsonArray("jobs");
            for (int i = 0; i < jobArray.size(); i++) {
                JsonObject jobObj = jobArray.get(i).getAsJsonObject();
                jobs.add(parseWorkflowJob(jobObj));
            }
        }
        return jobs;
    }

    private static WorkflowJob parseWorkflowJob(JsonObject jobObj) {
        long id = jobObj.get("id").getAsLong();
        String name = jobObj.get("name").getAsString();
        String status = jobObj.get("status").getAsString();
        String conclusion = jobObj.has("conclusion") && !jobObj.get("conclusion").isJsonNull()
            ? jobObj.get("conclusion").getAsString() : null;
        LocalDateTime startedAt = jobObj.has("started_at") && !jobObj.get("started_at").isJsonNull()
            ? parseDateTime(jobObj.get("started_at").getAsString()) : null;
        LocalDateTime completedAt = jobObj.has("completed_at") && !jobObj.get("completed_at").isJsonNull()
            ? parseDateTime(jobObj.get("completed_at").getAsString()) : null;

        return new WorkflowJob(id, name, status, conclusion, startedAt, completedAt);
    }

    private WebhookEvent parseWorkflowRunEvent(JsonObject payload) {
        JsonObject actionObj = payload.getAsJsonObject("workflow_run");
        long runId = actionObj.get("id").getAsLong();
        String status = actionObj.get("status").getAsString();
        String conclusion = actionObj.has("conclusion") && !actionObj.get("conclusion").isJsonNull()
            ? actionObj.get("conclusion").getAsString() : null;

        boolean isSuccess = "completed".equals(status) && "success".equals(conclusion);
        String message = String.format("Workflow run %d: %s (%s)", runId, status,
                                       conclusion != null ? conclusion : "no conclusion");

        return new WebhookEvent("workflow_run", payload, isSuccess, message);
    }

    private WebhookEvent parseWorkflowJobEvent(JsonObject payload) {
        JsonObject actionObj = payload.getAsJsonObject("workflow_job");
        long jobId = actionObj.get("id").getAsLong();
        String status = actionObj.get("status").getAsString();
        String conclusion = actionObj.has("conclusion") && !actionObj.get("conclusion").isJsonNull()
            ? actionObj.get("conclusion").getAsString() : null;

        boolean isSuccess = "completed".equals(status) && "success".equals(conclusion);
        String message = String.format("Workflow job %d: %s (%s)", jobId, status,
                                       conclusion != null ? conclusion : "no conclusion");

        return new WebhookEvent("workflow_job", payload, isSuccess, message);
    }

    private WebhookEvent parsePushEvent(JsonObject payload) {
        String ref = payload.get("ref").getAsString();
        String commitSha = payload.getAsJsonObject("after").getAsString();
        String message = String.format("Push to %s (commit: %s)", ref, commitSha.substring(0, 8));

        return new WebhookEvent("push", payload, true, message);
    }

    private WebhookEvent parsePullRequestEvent(JsonObject payload) {
        JsonObject prObj = payload.getAsJsonObject("pull_request");
        int prNumber = prObj.get("number").getAsInt();
        String action = payload.get("action").getAsString();
        String title = prObj.get("title").getAsString();
        String message = String.format("PR #%d: %s (%s)", prNumber, title, action);

        return new WebhookEvent("pull_request", payload, true, message);
    }

    private static LocalDateTime parseDateTime(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr.replace("Z", ""));
    }

    // Record classes for data

    public record WorkflowRun(
        long id,
        String name,
        String status,
        String conclusion,
        String branch,
        String commitSha,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String event,
        String url
    ) {
        public boolean isCompleted() {
            return "completed".equals(status);
        }

        public boolean isSuccess() {
            return isCompleted() && "success".equals(conclusion);
        }

        public boolean isFailure() {
            return isCompleted() && ("failure".equals(conclusion) || "timed_out".equals(conclusion));
        }
    }

    public record WorkflowJob(
        long id,
        String name,
        String status,
        String conclusion,
        LocalDateTime startedAt,
        LocalDateTime completedAt
    ) {
        public boolean isCompleted() {
            return "completed".equals(status);
        }

        public boolean isSuccess() {
            return isCompleted() && "success".equals(conclusion);
        }

        public boolean isFailure() {
            return isCompleted() && ("failure".equals(conclusion) || "timed_out".equals(conclusion));
        }

        public long getDurationSeconds() {
            if (startedAt != null && completedAt != null) {
                return java.time.Duration.between(startedAt, completedAt).getSeconds();
            }
            return -1;
        }
    }

    public record WebhookEvent(
        String eventType,
        JsonObject payload,
        boolean isSuccess,
        String message
    ) {
        public boolean isWorkflowEvent() {
            return eventType.equals("workflow_run") || eventType.equals("workflow_job");
        }

        public Optional<WorkflowRun> asWorkflowRun() {
            if (eventType.equals("workflow_run") && payload != null && payload.has("workflow_run")) {
                JsonObject workflowRunObj = payload.getAsJsonObject("workflow_run");
                return Optional.of(parseWorkflowRun(workflowRunObj));
            }
            return Optional.empty();
        }
    }
}