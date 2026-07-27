package com.eventstreamingplatform.events;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Data carried when an order is shipped.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderShippedPayload(
        String shipmentReference,
        String carrier,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant shippedAt) implements OrderEventPayload {

    public OrderShippedPayload {
        ContractValidation.requireIdentifier(shipmentReference, "shipmentReference");
        ContractValidation.requireText(carrier, "carrier", 100);
        ContractValidation.requireNonNull(shippedAt, "shippedAt");
    }

    @Override
    public OrderEventType eventType() {
        return OrderEventType.ORDER_SHIPPED;
    }
}
