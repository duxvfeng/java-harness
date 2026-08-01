package com.chachamaru.harness.service.dto;

/**
 * State query response DTO
 */
public record StateQueryResponse(
    String requestId,
    String status,
    Object data,
    String error
) {
    public static StateQueryResponse success(String requestId, Object data) {
        return new StateQueryResponse(requestId, "success", data, null);
    }

    public static StateQueryResponse error(String requestId, String error) {
        return new StateQueryResponse(requestId, "error", null, error);
    }
}
