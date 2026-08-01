package com.chachamaru.harness.cli.ipc;

import com.chachamaru.harness.shared.dto.GuardrailDecision;
import com.chachamaru.harness.shared.dto.HookEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP-based IPC client implementation for communicating with Spring Boot Service
 * Uses Java 11+ HttpClient with async support
 */
public class HttpIpcClient implements IpcClient {
    private static final Logger logger = LoggerFactory.getLogger(HttpIpcClient.class);
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final String DEFAULT_SERVICE_URL = "http://localhost:8080/api/hook";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String serviceUrl;
    private boolean serviceAvailable = true;

    public HttpIpcClient() {
        this(DEFAULT_SERVICE_URL);
    }

    public HttpIpcClient(String serviceUrl) {
        this.serviceUrl = serviceUrl != null ? serviceUrl : DEFAULT_SERVICE_URL;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(DEFAULT_TIMEOUT_MS))
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public CompletableFuture<GuardrailDecision> sendHookEvent(HookEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonPayload = objectMapper.writeValueAsString(event);
                logger.debug("Sending hook event to service: {}", jsonPayload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(serviceUrl))
                        .timeout(Duration.ofMillis(DEFAULT_TIMEOUT_MS))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    logger.debug("Received response from service: {}", responseBody);
                    return objectMapper.readValue(responseBody, GuardrailDecision.class);
                } else if (response.statusCode() == 503) {
                    // Service unavailable - return default ALLOW decision
                    logger.warn("Service unavailable (503), returning default ALLOW decision");
                    return GuardrailDecision.allow("Service unavailable - default allow");
                } else {
                    logger.error("Unexpected response status: {}", response.statusCode());
                    return GuardrailDecision.allow("Service error - default allow");
                }
            } catch (IOException e) {
                logger.error("IO error communicating with service", e);
                markServiceUnavailable();
                return GuardrailDecision.allow("Communication error - default allow");
            } catch (InterruptedException e) {
                logger.error("Request interrupted", e);
                Thread.currentThread().interrupt();
                return GuardrailDecision.allow("Request interrupted - default allow");
            }
        });
    }

    @Override
    public boolean isServiceAvailable() {
        return serviceAvailable;
    }

    @Override
    public void close() {
        // HttpClient doesn't need explicit closing
        logger.info("IPC client closed");
    }

    private void markServiceUnavailable() {
        serviceAvailable = false;
        logger.warn("Service marked as unavailable");
    }
}
