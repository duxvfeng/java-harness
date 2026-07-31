package com.chachamaru.harness.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * State query request for IPC
 */
public record StateQuery(
    @JsonProperty("session_id")
    String sessionId,

    @JsonProperty("query_type")
    QueryType queryType
) {
    public enum QueryType {
        SESSION,
        WORK_STATES,
        SIGNALS
    }
}
