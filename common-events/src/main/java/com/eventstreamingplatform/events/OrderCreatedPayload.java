package com.eventstreamingplatform.events;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Data carried when a new order is created.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedPayload(
        String customerReference,
        String currency,
        BigDecimal totalAmount,
        int itemCount) implements OrderEventPayload {

    public OrderCreatedPayload {
        ContractValidation.requireIdentifier(customerReference, "customerReference");
        ContractValidation.requireCurrency(currency);
        ContractValidation.requirePositiveAmount(totalAmount, "totalAmount");
        if (itemCount <= 0) {
            throw new IllegalArgumentException("itemCount must be greater than zero");
        }
    }

    @Override
    public OrderEventType eventType() {
        return OrderEventType.ORDER_CREATED;
    }
}
