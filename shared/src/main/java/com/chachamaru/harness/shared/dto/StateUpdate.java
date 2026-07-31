package com.chachamaru.harness.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * State update event for IPC
 */
public record StateUpdate(
    @JsonProperty("event_type")
    String eventType,

    @JsonProperty("session_id")
    String sessionId,

    @JsonProperty("data")
    Map<String, Object> data,

    @JsonProperty("timestamp")
    long timestamp
) {
    public static StateUpdate create(String eventType, String sessionId, Map<String, Object> data) {
        return new StateUpdate(eventType, sessionId, data, System.currentTimeMillis());
    }
}
