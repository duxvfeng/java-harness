package com.chachamaru.harness.ci.gitlab;

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
 * GitLab CI Integration Service
 * <p>
 * Provides integration with GitLab CI/CD including:
 * <ul>
 *   <li>Pipeline status monitoring</li>
 *   <li>Webhook event processing</li>
 *   <li>Job management and control</li>
 *   <li>Authentication and project handling</li>
 * </ul>
 * </p>
 */
public class GitLabCIIntegration {
    private static final Logger log = LoggerFactory.getLogger(GitLabCIIntegration.class);
    private static final String GITLAB_API_BASE = "https://gitlab.com/api/v4";

    private final String gitlabToken;
    private final String projectId;
    private final Gson gson;
    private final CloseableHttpClient httpClient;

    /**
     * Constructor with GitLab credentials
     *
     * @param gitlabToken GitLab personal access token for authentication
     * @param projectId   GitLab project ID (numeric or namespace/project)
     */
    public GitLabCIIntegration(String gitlabToken, String projectId) {
        this.gitlabToken = gitlabToken;
        this.projectId = projectId;
        this.gson = new Gson();
        this.httpClient = HttpClients.createDefault();
        log.info("GitLab CI integration initialized for project: {}", projectId);
    }

    /**
     * Constructor with auto-detection from git repository
     *
     * @param projectRoot  Project root directory
     * @param gitlabToken  Optional GitLab token (can be null)
     * @throws IOException if git config cannot be read
     */
    public GitLabCIIntegration(Path projectRoot, String gitlabToken) throws IOException {
        this(gitlabToken, detectProjectId(projectRoot));
    }

    /**
     * Detect project ID from git configuration
     */
    private static String detectProjectId(Path projectRoot) throws IOException {
        Path gitConfig = projectRoot.resolve(".git/config");
        if (!Files.exists(gitConfig)) {
            throw new IOException("Not a git repository: " + projectRoot);
        }

        List<String> lines = Files.readAllLines(gitConfig);
        for (String line : lines) {
            if (line.trim().startsWith("url =")) {
                String url = line.substring(line.indexOf("url =") + 6).trim();
                if (url.contains("gitlab.com")) {
                    // Extract project path from URL like: https://gitlab.com/namespace/project.git or git@gitlab.com:namespace/project.git
                    String[] parts = url.split("gitlab.com[:/]");
                    if (parts.length > 1) {
                        String projectPath = parts[parts.length - 1].replace(".git", "");
                        // URL encode the project path for API usage
                        return projectPath;
                    }
                }
            }
        }
        throw new IOException("Could not detect GitLab project ID from git config");
    }

    /**
     * Get pipelines for a branch
     *
     * @param branch        Branch name (default: "main")
     * @param limit         Maximum number of pipelines to return
     * @return List of pipelines
     */
    public List<Pipeline> getPipelines(String branch, int limit) throws IOException {
        String projectIdEncoded = java.net.URLEncoder.encode(projectId, java.nio.charset.StandardCharsets.UTF_8);
        String endpoint = "/projects/" + projectIdEncoded + "/pipelines";
        String url = GITLAB_API_BASE + endpoint;

        if (branch != null && !branch.isEmpty()) {
            url += "?ref=" + java.net.URLEncoder.encode(branch, java.nio.charset.StandardCharsets.UTF_8);
        }
        if (limit > 0) {
            url += (url.contains("?") ? "&" : "?") + "per_page=" + limit;
        }

        log.debug("Fetching pipelines from: {}", url);
        HttpGet request = new HttpGet(url);
        request.addHeader("PRIVATE-TOKEN", gitlabToken);
        request.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() >= 400) {
                throw new IOException("GitLab API request failed with status: " + response.getCode());
            }

            JsonObject[] jsonResponse = gson.fromJson(new InputStreamReader(response.getEntity().getContent()), JsonObject[].class);
            return parsePipelines(Arrays.asList(jsonResponse));
        }
    }

    /**
     * Get specific pipeline by ID
     *
     * @param pipelineId Pipeline ID
     * @return Pipeline details
     */
    public Pipeline getPipeline(long pipelineId) throws IOException {
        String projectIdEncoded = java.net.URLEncoder.encode(projectId, java.nio.charset.StandardCharsets.UTF_8);
        String endpoint = "/projects/" + projectIdEncoded + "/pipelines/" + pipelineId;
        String url = GITLAB_API_BASE + endpoint;

        log.debug("Fetching pipeline {} from: {}", pipelineId, url);
        HttpGet request = new HttpGet(url);
        request.addHeader("PRIVATE-TOKEN", gitlabToken);
        request.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() >= 400) {
                throw new IOException("GitLab API request failed with status: " + response.getCode());
            }

            JsonObject jsonResponse = gson.fromJson(new InputStreamReader(response.getEntity().getContent()), JsonObject.class);
            return parsePipeline(jsonResponse);
        }
    }

    /**
     * Get pipeline jobs
     *
     * @param pipelineId Pipeline ID
     * @return List of jobs in the pipeline
     */
    public List<PipelineJob> getPipelineJobs(long pipelineId) throws IOException {
        String projectIdEncoded = java.net.URLEncoder.encode(projectId, java.nio.charset.StandardCharsets.UTF_8);
        String endpoint = "/projects/" + projectIdEncoded + "/pipelines/" + pipelineId + "/jobs";
        String url = GITLAB_API_BASE + endpoint;

        log.debug("Fetching pipeline jobs for pipeline {} from: {}", pipelineId, url);
        HttpGet request = new HttpGet(url);
        request.addHeader("PRIVATE-TOKEN", gitlabToken);
        request.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() >= 400) {
                throw new IOException("GitLab API request failed with status: " + response.getCode());
            }

            JsonObject[] jsonResponse = gson.fromJson(new InputStreamReader(response.getEntity().getContent()), JsonObject[].class);
            return parsePipelineJobs(Arrays.asList(jsonResponse));
        }
    }

    /**
     * Retry a failed pipeline
     *
     * @param pipelineId Pipeline ID to retry
     * @return true if retry was triggered successfully
     */
    public boolean retryPipeline(long pipelineId) throws IOException {
        String projectIdEncoded = java.net.URLEncoder.encode(projectId, java.nio.charset.StandardCharsets.UTF_8);
        String endpoint = "/projects/" + projectIdEncoded + "/pipelines/" + pipelineId + "/retry";
        String url = GITLAB_API_BASE + endpoint;

        log.info("Retrying pipeline {} at: {}", pipelineId, url);
        HttpPost request = new HttpPost(url);
        request.addHeader("PRIVATE-TOKEN", gitlabToken);
        request.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            boolean success = response.getCode() >= 200 && response.getCode() < 300;
            if (success) {
                log.info("Successfully triggered retry for pipeline {}", pipelineId);
            } else {
                log.warn("Failed to retry pipeline {}, status: {}", pipelineId, response.getCode());
            }
            return success;
        }
    }

    /**
     * Process GitLab webhook event
     *
     * @param eventType   Webhook event type (e.g., "Pipeline Hook", "Job Hook")
     * @param payloadJson JSON payload from webhook
     * @return Parsed webhook event
     */
    public WebhookEvent processWebhook(String eventType, String payloadJson) {
        log.debug("Processing webhook event: {}", eventType);
        try {
            JsonObject payload = gson.fromJson(payloadJson, JsonObject.class);
            return switch (eventType) {
                case "Pipeline Hook" -> parsePipelineEvent(payload);
                case "Job Hook" -> parseJobEvent(payload);
                case "Push Hook" -> parsePushEvent(payload);
                case "Merge Request Hook" -> parseMergeRequestEvent(payload);
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
        String projectIdEncoded = java.net.URLEncoder.encode(projectId, java.nio.charset.StandardCharsets.UTF_8);
        String branchEncoded = java.net.URLEncoder.encode(branch, java.nio.charset.StandardCharsets.UTF_8);
        String endpoint = "/projects/" + projectIdEncoded + "/repository/branches/" + branchEncoded;
        String url = GITLAB_API_BASE + endpoint;

        log.debug("Getting commit SHA for branch {} from: {}", branch, url);
        HttpGet request = new HttpGet(url);
        request.addHeader("PRIVATE-TOKEN", gitlabToken);
        request.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() >= 400) {
                throw new IOException("GitLab API request failed with status: " + response.getCode());
            }

            JsonObject jsonResponse = gson.fromJson(new InputStreamReader(response.getEntity().getContent()), JsonObject.class);
            if (jsonResponse.has("commit") && jsonResponse.getAsJsonObject("commit").has("id")) {
                return jsonResponse.getAsJsonObject("commit").get("id").getAsString();
            }
            throw new IOException("No commit SHA found in response");
        }
    }

    /**
     * Check if GitLab CI is enabled for this project
     *
     * @return true if GitLab CI is enabled
     */
    public boolean isCIEnabled() throws IOException {
        String projectIdEncoded = java.net.URLEncoder.encode(projectId, java.nio.charset.StandardCharsets.UTF_8);
        String endpoint = "/projects/" + projectIdEncoded;
        String url = GITLAB_API_BASE + endpoint;

        HttpGet request = new HttpGet(url);
        request.addHeader("PRIVATE-TOKEN", gitlabToken);
        request.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getCode() >= 400) {
                return false;
            }

            JsonObject jsonResponse = gson.fromJson(new InputStreamReader(response.getEntity().getContent()), JsonObject.class);
            return jsonResponse.has("jobs_enabled") && jsonResponse.get("jobs_enabled").getAsBoolean();
        }
    }

    /**
     * Close HTTP client
     */
    public void close() throws IOException {
        httpClient.close();
    }

    // Private parsing methods

    private static List<Pipeline> parsePipelinesFromResponse(JsonObject response) {
        List<Pipeline> pipelines = new ArrayList<>();
        if (response != null && response.isJsonArray()) {
            var pipelineArray = response.getAsJsonArray();
            for (int i = 0; i < pipelineArray.size(); i++) {
                JsonObject pipelineObj = pipelineArray.get(i).getAsJsonObject();
                pipelines.add(parsePipeline(pipelineObj));
            }
        }
        return pipelines;
    }

    private static List<Pipeline> parsePipelines(List<JsonObject> pipelineArray) {
        List<Pipeline> pipelines = new ArrayList<>();
        for (JsonObject pipelineObj : pipelineArray) {
            pipelines.add(parsePipeline(pipelineObj));
        }
        return pipelines;
    }

    private static Pipeline parsePipeline(JsonObject pipelineObj) {
        long id = pipelineObj.get("id").getAsLong();
        String status = pipelineObj.get("status").getAsString();
        String ref = pipelineObj.get("ref").getAsString();
        String sha = pipelineObj.get("sha").getAsString();
        LocalDateTime createdAt = parseDateTime(pipelineObj.get("created_at").getAsString());
        LocalDateTime updatedAt = parseDateTime(pipelineObj.get("updated_at").getAsString());
        String webUrl = pipelineObj.get("web_url").getAsString();

        String userName = pipelineObj.has("user") && pipelineObj.getAsJsonObject("user").has("name")
            ? pipelineObj.getAsJsonObject("user").get("name").getAsString() : null;

        return new Pipeline(id, status, ref, sha, createdAt, updatedAt, webUrl, userName);
    }

    private static List<PipelineJob> parsePipelineJobs(List<JsonObject> jobArray) {
        List<PipelineJob> jobs = new ArrayList<>();
        for (JsonObject jobObj : jobArray) {
            jobs.add(parsePipelineJob(jobObj));
        }
        return jobs;
    }

    private static PipelineJob parsePipelineJob(JsonObject jobObj) {
        long id = jobObj.get("id").getAsLong();
        String name = jobObj.get("name").getAsString();
        String status = jobObj.get("status").getAsString();
        String stage = jobObj.get("stage").getAsString();
        LocalDateTime startedAt = jobObj.has("started_at") && !jobObj.get("started_at").isJsonNull()
            ? parseDateTime(jobObj.get("started_at").getAsString()) : null;
        LocalDateTime finishedAt = jobObj.has("finished_at") && !jobObj.get("finished_at").isJsonNull()
            ? parseDateTime(jobObj.get("finished_at").getAsString()) : null;

        boolean allowFailure = jobObj.has("allow_failure") && jobObj.get("allow_failure").getAsBoolean();

        return new PipelineJob(id, name, status, stage, startedAt, finishedAt, allowFailure);
    }

    private WebhookEvent parsePipelineEvent(JsonObject payload) {
        JsonObject pipelineObj = payload.getAsJsonObject("object_attributes");
        long pipelineId = pipelineObj.get("id").getAsLong();
        String status = pipelineObj.get("status").getAsString();

        boolean isSuccess = "success".equals(status);
        String message = String.format("Pipeline %d: %s", pipelineId, status);

        return new WebhookEvent("Pipeline Hook", payload, isSuccess, message);
    }

    private WebhookEvent parseJobEvent(JsonObject payload) {
        JsonObject jobObj = payload.getAsJsonObject("object_attributes");
        long jobId = jobObj.get("id").getAsLong();
        String status = jobObj.get("status").getAsString();
        String name = jobObj.get("name").getAsString();

        boolean isSuccess = "success".equals(status);
        String message = String.format("Job %s (%d): %s", name, jobId, status);

        return new WebhookEvent("Job Hook", payload, isSuccess, message);
    }

    private WebhookEvent parsePushEvent(JsonObject payload) {
        String ref = payload.get("ref").getAsString();
        String commitSha = payload.getAsJsonObject("after").getAsString();
        String message = String.format("Push to %s (commit: %s)", ref, commitSha.substring(0, 8));

        return new WebhookEvent("Push Hook", payload, true, message);
    }

    private WebhookEvent parseMergeRequestEvent(JsonObject payload) {
        JsonObject mrObj = payload.getAsJsonObject("object_attributes");
        int mrId = mrObj.get("iid").getAsInt();
        String action = mrObj.get("action").getAsString();
        String title = mrObj.get("title").getAsString();
        String message = String.format("MR #%d: %s (%s)", mrId, title, action);

        return new WebhookEvent("Merge Request Hook", payload, true, message);
    }

    private static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr.replace("Z", ""));
    }

    // Record classes for data

    public record Pipeline(
        long id,
        String status,
        String ref,
        String sha,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String webUrl,
        String userName
    ) {
        public boolean isCompleted() {
            return "success".equals(status) || "failed".equals(status) ||
                   "canceled".equals(status) || "skipped".equals(status);
        }

        public boolean isSuccess() {
            return "success".equals(status);
        }

        public boolean isFailure() {
            return "failed".equals(status);
        }

        public boolean isRunning() {
            return "running".equals(status) || "pending".equals(status);
        }
    }

    public record PipelineJob(
        long id,
        String name,
        String status,
        String stage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        boolean allowFailure
    ) {
        public boolean isCompleted() {
            return "success".equals(status) || "failed".equals(status) ||
                   "canceled".equals(status) || "skipped".equals(status);
        }

        public boolean isSuccess() {
            return "success".equals(status);
        }

        public boolean isFailure() {
            return "failed".equals(status);
        }

        public long getDurationSeconds() {
            if (startedAt != null && finishedAt != null) {
                return java.time.Duration.between(startedAt, finishedAt).getSeconds();
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
        public boolean isPipelineEvent() {
            return eventType.equals("Pipeline Hook");
        }

        public boolean isJobEvent() {
            return eventType.equals("Job Hook");
        }

        public Optional<Pipeline> asPipeline() {
            if (eventType.equals("Pipeline Hook") && payload != null && payload.has("object_attributes")) {
                JsonObject pipelineObj = payload.getAsJsonObject("object_attributes");
                return Optional.of(parsePipeline(pipelineObj));
            }
            return Optional.empty();
        }
    }
}