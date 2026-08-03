package com.eventstreamingplatform.events;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Immutable envelope for an order lifecycle event.
 *
 * @param eventId globally unique idempotency identifier
 * @param eventType business event type
 * @param eventVersion event contract version
 * @param orderId order aggregate identifier and Kafka record key
 * @param occurredAt time at which the business event occurred
 * @param source system that created the event
 * @param correlationId identifier connecting related work across services
 * @param payload event-specific data
 * @param <P> concrete payload type
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderEvent<P extends OrderEventPayload>(
        UUID eventId,
        OrderEventType eventType,
        int eventVersion,
        String orderId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant occurredAt,
        String source,
        String correlationId,
        @JsonTypeInfo(
                use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "eventType")
        @JsonSubTypes({
                @JsonSubTypes.Type(
                        value = OrderCreatedPayload.class,
                        name = "ORDER_CREATED"),
                @JsonSubTypes.Type(
                        value = PaymentCompletedPayload.class,
                        name = "PAYMENT_COMPLETED"),
                @JsonSubTypes.Type(
                        value = PaymentFailedPayload.class,
                        name = "PAYMENT_FAILED"),
                @JsonSubTypes.Type(
                        value = OrderCancelledPayload.class,
                        name = "ORDER_CANCELLED"),
                @JsonSubTypes.Type(
                        value = OrderShippedPayload.class,
                        name = "ORDER_SHIPPED")
        })
        P payload) {

    public OrderEvent {
        ContractValidation.requireNonNull(eventId, "eventId");
        ContractValidation.requireNonNull(eventType, "eventType");
        ContractValidation.requireIdentifier(orderId, "orderId");
        ContractValidation.requireNonNull(occurredAt, "occurredAt");
        ContractValidation.requireText(source, "source", 100);
        ContractValidation.requireText(correlationId, "correlationId", 100);
        ContractValidation.requireNonNull(payload, "payload");

        if (!EventContractVersions.isSupported(eventVersion)) {
            throw new IllegalArgumentException(
                    "Unsupported eventVersion " + eventVersion
                            + "; supported version is " + EventContractVersions.CURRENT);
        }
        if (eventType != payload.eventType()) {
            throw new IllegalArgumentException(
                    "eventType " + eventType
                            + " does not match payload type " + payload.eventType());
        }
    }
}
