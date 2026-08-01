package com.chachamaru.harness.service.dto;

/**
 * State query request DTO
 */
public record StateQueryRequest(
    String sessionId,
    String queryType
) {
    public StateQueryRequest {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId cannot be null or blank");
        }
        if (queryType == null || queryType.isBlank()) {
            throw new IllegalArgumentException("queryType cannot be null or blank");
        }
    }
}
