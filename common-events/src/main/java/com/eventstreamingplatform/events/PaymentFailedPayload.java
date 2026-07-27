package com.eventstreamingplatform.events;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Data carried when payment for an order fails.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentFailedPayload(
        String paymentReference,
        PaymentFailureReason failureReason,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant failedAt) implements OrderEventPayload {

    public PaymentFailedPayload {
        ContractValidation.requireIdentifier(paymentReference, "paymentReference");
        ContractValidation.requireNonNull(failureReason, "failureReason");
        ContractValidation.requireNonNull(failedAt, "failedAt");
    }

    @Override
    public OrderEventType eventType() {
        return OrderEventType.PAYMENT_FAILED;
    }
}
