package com.eventstreamingplatform.events;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Data carried when payment for an order completes successfully.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCompletedPayload(
        String paymentReference,
        BigDecimal amount,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant completedAt) implements OrderEventPayload {

    public PaymentCompletedPayload {
        ContractValidation.requireIdentifier(paymentReference, "paymentReference");
        ContractValidation.requirePositiveAmount(amount, "amount");
        ContractValidation.requireCurrency(currency);
        ContractValidation.requireNonNull(completedAt, "completedAt");
    }

    @Override
    public OrderEventType eventType() {
        return OrderEventType.PAYMENT_COMPLETED;
    }
}
