package com.chachamaru.harness.ci.github;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * GitHub Actions Webhook Handler
 * <p>
 * Handles incoming webhook events from GitHub Actions:
 * <ul>
 *   <li>Workflow run events (completed, started, requested)</li>
 *   <li>Workflow job events (completed, started, queued)</li>
 *   <li>Push events triggering workflows</li>
 *   <li>Pull request events with workflow runs</li>
 * </ul>
 * </p>
 */
public class GitHubActionsWebhookHandler {
    private static final Logger log = LoggerFactory.getLogger(GitHubActionsWebhookHandler.class);
    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";
    private static final String EVENT_HEADER = "X-GitHub-Event";
    private static final String DELIVERY_HEADER = "X-GitHub-Delivery";

    private final GitHubActionsIntegration integration;
    private final WebhookSecretManager secretManager;
    private final Map<String, WebhookEventHandler> eventHandlers;

    /**
     * Constructor
     *
     * @param integration GitHub Actions integration instance
     * @param webhookSecret Optional webhook secret for signature verification
     */
    public GitHubActionsWebhookHandler(GitHubActionsIntegration integration, String webhookSecret) {
        this.integration = integration;
        this.secretManager = new WebhookSecretManager(webhookSecret);
        this.eventHandlers = new HashMap<>();
        registerDefaultHandlers();
        log.info("GitHub Actions webhook handler initialized");
    }

    /**
     * Register default webhook event handlers
     */
    private void registerDefaultHandlers() {
        // Workflow run events
        eventHandlers.put("workflow_run", this::handleWorkflowRunEvent);
        eventHandlers.put("workflow_job", this::handleWorkflowJobEvent);
        eventHandlers.put("push", this::handlePushEvent);
        eventHandlers.put("pull_request", this::handlePullRequestEvent);
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
     * @param headers  HTTP headers
     * @param payload  Request body payload
     * @return Webhook response
     */
    public WebhookResponse handleWebhook(Map<String, String> headers, String payload) {
        try {
            // Extract delivery ID
            String deliveryId = headers.get(DELIVERY_HEADER);
            String eventType = headers.get(EVENT_HEADER);
            String signature = headers.get(SIGNATURE_HEADER);

            log.info("Received webhook: delivery={}, event={}", deliveryId, eventType);

            // Verify signature if secret is configured
            if (secretManager.hasSecret()) {
                if (!secretManager.verifySignature(payload, signature)) {
                    log.warn("Invalid webhook signature for delivery: {}", deliveryId);
                    return new WebhookResponse(401, "Unauthorized", "Invalid signature");
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
            log.info("Webhook processed: delivery={}, status={}", deliveryId, response.statusCode());

            return response;

        } catch (Exception e) {
            log.error("Failed to process webhook: {}", e.getMessage(), e);
            return new WebhookResponse(500, "Internal Server Error", "Processing failed: " + e.getMessage());
        }
    }

    /**
     * Handle workflow run events
     */
    private WebhookResponse handleWorkflowRunEvent(String eventType, String payload) {
        try {
            GitHubActionsIntegration.WebhookEvent event = integration.processWebhook(eventType, payload);

            if (event.isSuccess()) {
                log.info("Workflow run completed successfully: {}", event.message());
                // Could trigger additional actions here
            } else {
                log.warn("Workflow run failed or had issues: {}", event.message());
                // Could trigger automatic repair here
            }

            return new WebhookResponse(200, "OK", "Workflow run event processed");

        } catch (Exception e) {
            log.error("Failed to handle workflow run event: {}", e.getMessage());
            return new WebhookResponse(500, "Internal Server Error", "Failed to process event");
        }
    }

    /**
     * Handle workflow job events
     */
    private WebhookResponse handleWorkflowJobEvent(String eventType, String payload) {
        try {
            GitHubActionsIntegration.WebhookEvent event = integration.processWebhook(eventType, payload);

            if (event.isSuccess()) {
                log.info("Workflow job completed successfully: {}", event.message());
            } else {
                log.warn("Workflow job failed: {}", event.message());
                // Could trigger job-specific repair here
            }

            return new WebhookResponse(200, "OK", "Workflow job event processed");

        } catch (Exception e) {
            log.error("Failed to handle workflow job event: {}", e.getMessage());
            return new WebhookResponse(500, "Internal Server Error", "Failed to process event");
        }
    }

    /**
     * Handle push events
     */
    private WebhookResponse handlePushEvent(String eventType, String payload) {
        try {
            GitHubActionsIntegration.WebhookEvent event = integration.processWebhook(eventType, payload);
            log.info("Push event processed: {}", event.message());

            return new WebhookResponse(200, "OK", "Push event processed");

        } catch (Exception e) {
            log.error("Failed to handle push event: {}", e.getMessage());
            return new WebhookResponse(500, "Internal Server Error", "Failed to process event");
        }
    }

    /**
     * Handle pull request events
     */
    private WebhookResponse handlePullRequestEvent(String eventType, String payload) {
        try {
            GitHubActionsIntegration.WebhookEvent event = integration.processWebhook(eventType, payload);
            log.info("Pull request event processed: {}", event.message());

            return new WebhookResponse(200, "OK", "Pull request event processed");

        } catch (Exception e) {
            log.error("Failed to handle pull request event: {}", e.getMessage());
            return new WebhookResponse(500, "Internal Server Error", "Failed to process event");
        }
    }

    /**
     * Webhook secret manager for signature verification
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
         * Verify GitHub webhook signature
         *
         * @param payload   Request payload
         * @param signature X-Hub-Signature-256 header value (sha256=...)
         * @return true if signature is valid
         */
        public boolean verifySignature(String payload, String signature) {
            if (!hasSecret() || signature == null) {
                return false;
            }

            try {
                String expectedPrefix = "sha256=";
                if (!signature.startsWith(expectedPrefix)) {
                    return false;
                }

                String signatureHash = signature.substring(expectedPrefix.length());
                String calculatedHash = calculateHmacSha256(payload, webhookSecret);

                return signatureHash.equals(calculatedHash);

            } catch (Exception e) {
                log.error("Failed to verify webhook signature: {}", e.getMessage());
                return false;
            }
        }

        /**
         * Calculate HMAC-SHA256 hash
         */
        private String calculateHmacSha256(String data, String key) throws Exception {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacData = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacData) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
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