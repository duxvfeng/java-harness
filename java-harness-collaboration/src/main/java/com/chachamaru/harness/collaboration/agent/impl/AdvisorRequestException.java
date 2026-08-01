package com.chachamaru.harness.collaboration.agent.impl;

import com.chachamaru.harness.collaboration.agent.message.AdvisorRequestV1;

/**
 * Exception thrown when Worker agent needs advisor consultation.
 *
 * <p>This exception carries the AdvisorRequestV1 that should be sent to the Advisor agent.
 * The BreezingMode workflow catches this exception and routes the request appropriately.</p>
 *
 * @spec_reference spec.md#Breezing Mode Advisor Protocol
 * @since 4.2.0
 */
public class AdvisorRequestException extends RuntimeException {

    private final AdvisorRequestV1 request;

    /**
     * Creates an advisor request exception.
     *
     * @param message the exception message
     * @param request the advisor request
     */
    public AdvisorRequestException(String message, AdvisorRequestV1 request) {
        super(message);
        this.request = request;
    }

    /**
     * Gets the advisor request.
     *
     * @return the advisor request
     */
    public AdvisorRequestV1 getRequest() {
        return request;
    }
}