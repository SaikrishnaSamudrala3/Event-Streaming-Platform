package com.eventstreamingplatform.events;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Data carried when an order is cancelled.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCancelledPayload(
        String reason,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant cancelledAt) implements OrderEventPayload {

    public OrderCancelledPayload {
        ContractValidation.requireText(reason, "reason", 250);
        ContractValidation.requireNonNull(cancelledAt, "cancelledAt");
    }

    @Override
    public OrderEventType eventType() {
        return OrderEventType.ORDER_CANCELLED;
    }
}
