package com.chachamaru.harness.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Hook event DTO for IPC communication
 */
public record HookEvent(
    @JsonProperty("request_id")
    String requestId,

    @JsonProperty("event_type")
    String eventType,

    @JsonProperty("payload")
    Map<String, Object> payload,

    @JsonProperty("timestamp")
    long timestamp
) {
    public static HookEvent create(String eventType, Map<String, Object> payload) {
        return new HookEvent(
            java.util.UUID.randomUUID().toString(),
            eventType,
            payload,
            System.currentTimeMillis()
        );
    }
}
