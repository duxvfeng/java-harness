package com.chachamaru.harness.ci.gitlab;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * GitLab CI Webhook Handler
 * <p>
 * Handles incoming webhook events from GitLab CI/CD:
 * <ul>
 *   <li>Pipeline events (success, failed, running)</li>
 *   <li>Job events (completed, started, queued)</li>
 *   <li>Push events triggering pipelines</li>
 *   <li>Merge request events with pipeline runs</li>
 * </ul>
 * </p>
 */
public class GitLabWebhookHandler {
    private static final Logger log = LoggerFactory.getLogger(GitLabWebhookHandler.class);
    private static final String SIGNATURE_HEADER = "X-Gitlab-Token";
    private static final String EVENT_HEADER = "X-Gitlab-Event";

    private final GitLabCIIntegration integration;
    private final WebhookSecretManager secretManager;
    private final Map<String, WebhookEventHandler> eventHandlers;

    /**
     * Constructor
     *
     * @param integration  GitLab CI integration instance
     * @param webhookSecret Optional webhook secret for verification
     */
    public GitLabWebhookHandler(GitLabCIIntegration integration, String webhookSecret) {
        this.integration = integration;
        this.secretManager = new WebhookSecretManager(webhookSecret);
        this.eventHandlers = new HashMap<>();
        registerDefaultHandlers();
        log.info("GitLab webhook handler initialized");
    }

    /**
     * Register default webhook event handlers
     */
    private void registerDefaultHandlers() {
        // Pipeline events
        eventHandlers.put("Pipeline Hook", this::handlePipelineEvent);
        eventHandlers.put("Job Hook", this::handleJobEvent);
        eventHandlers.put("Push Hook", this::handlePushEvent);
        eventHandlers.put("Merge Request Hook", this::handleMergeRequestEvent);
    }

    /**
     * Register custom webhook event handler
     *
     * @param eventType Event type to handle
     * @param handler   Handler function
     */
    public void registerHandler(String eventType, WebhookEventHandler handler) {
        eventHandlers.put(eventType, handler);
        log.info("Registered custom handler for event type: {}", eventType);
    }

    /**
     * Handle incoming webhook request
     *
     * @param headers HTTP headers
     * @param payload Request body payload
     * @return Webhook response
     */
    public WebhookResponse handleWebhook(Map<String, String> headers, String payload) {
        try {
            // Extract event information
            String eventType = headers.get(EVENT_HEADER);
            String token = headers.get(SIGNATURE_HEADER);

            log.info("Received GitLab webhook: event={}", eventType);

            // Verify token if secret is configured
            if (secretManager.hasSecret()) {
                if (!secretManager.verifyToken(token)) {
                    log.warn("Invalid webhook token");
                    return new WebhookResponse(401, "Unauthorized", "Invalid token");
                }
            }

            // Process event
            if (eventType == null) {
                log.warn("Missing event type header");
                return new WebhookResponse(400, "Bad Request", "Missing event type");
            }

            WebhookEventHandler handler = eventHandlers.get(eventType);
            if (handler == null) {
                log.debug("No handler registered for event type: {}", eventType);
                return new WebhookResponse(200, "OK", "Event received but not processed");
            }

            // Call handler
            WebhookResponse response = handler.handle(eventType, payload);
            log.info("Webhook processed: status={}", response.statusCode());

            return response;

        } catch (Exception e) {
            log.error("Failed to process webhook: {}", e.getMessage(), e);
            return new WebhookResponse(500, "Internal Server Error", "Processing failed: " + e.getMessage());
        }
    }

    /**
     * Handle pipeline events
     */
    private WebhookResponse handlePipelineEvent(String eventType, String payload) {
        try {
            GitLabCIIntegration.WebhookEvent event = integration.processWebhook(eventType, payload);

            if (event.isSuccess()) {
                log.info("Pipeline completed successfully: {}", event.message());
            } else {
                log.warn("Pipeline failed or had issues: {}", event.message());
            }

            return new WebhookResponse(200, "OK", "Pipeline event processed");

        } catch (Exception e) {
            log.error("Failed to handle pipeline event: {}", e.getMessage());
            return new WebhookResponse(500, "Internal Server Error", "Failed to process event");
        }
    }

    /**
     * Handle job events
     */
    private WebhookResponse handleJobEvent(String eventType, String payload) {
        try {
            GitLabCIIntegration.WebhookEvent event = integration.processWebhook(eventType, payload);

            if (event.isSuccess()) {
                log.info("Job completed successfully: {}", event.message());
            } else {
                log.warn("Job failed: {}", event.message());
            }

            return new WebhookResponse(200, "OK", "Job event processed");

        } catch (Exception e) {
            log.error("Failed to handle job event: {}", e.getMessage());
            return new WebhookResponse(500, "Internal Server Error", "Failed to process event");
        }
    }

    /**
     * Handle push events
     */
    private WebhookResponse handlePushEvent(String eventType, String payload) {
        try {
            GitLabCIIntegration.WebhookEvent event = integration.processWebhook(eventType, payload);
            log.info("Push event processed: {}", event.message());

            return new WebhookResponse(200, "OK", "Push event processed");

        } catch (Exception e) {
            log.error("Failed to handle push event: {}", e.getMessage());
            return new WebhookResponse(500, "Internal Server Error", "Failed to process event");
        }
    }

    /**
     * Handle merge request events
     */
    private WebhookResponse handleMergeRequestEvent(String eventType, String payload) {
        try {
            GitLabCIIntegration.WebhookEvent event = integration.processWebhook(eventType, payload);
            log.info("Merge request event processed: {}", event.message());

            return new WebhookResponse(200, "OK", "Merge request event processed");

        } catch (Exception e) {
            log.error("Failed to handle merge request event: {}", e.getMessage());
            return new WebhookResponse(500, "Internal Server Error", "Failed to process event");
        }
    }

    /**
     * Webhook secret manager for token verification
     */
    public static class WebhookSecretManager {
        private final String webhookSecret;

        public WebhookSecretManager(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public boolean hasSecret() {
            return webhookSecret != null && !webhookSecret.isEmpty();
        }

        /**
         * Verify GitLab webhook token
         *
         * @param token X-Gitlab-Token header value
         * @return true if token is valid
         */
        public boolean verifyToken(String token) {
            if (!hasSecret() || token == null) {
                return false;
            }

            return webhookSecret.equals(token);
        }
    }

    /**
     * Webhook event handler interface
     */
    @FunctionalInterface
    public interface WebhookEventHandler {
        WebhookResponse handle(String eventType, String payload) throws Exception;
    }

    /**
     * Webhook response record
     */
    public record WebhookResponse(
        int statusCode,
        String statusText,
        String message
    ) {
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}