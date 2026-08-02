package com.eventstreamingplatform.ingestion.mapping;

import java.util.UUID;

import com.eventstreamingplatform.events.OrderCancelledPayload;
import com.eventstreamingplatform.events.OrderCreatedPayload;
import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.events.OrderEventPayload;
import com.eventstreamingplatform.events.OrderShippedPayload;
import com.eventstreamingplatform.events.PaymentCompletedPayload;
import com.eventstreamingplatform.events.PaymentFailedPayload;
import com.eventstreamingplatform.ingestion.api.CreateOrderEventRequest;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Component;

@Component
public class EventRequestMapper {

    private final JsonMapper jsonMapper;

    public EventRequestMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public OrderEvent<? extends OrderEventPayload> toOrderEvent(
            CreateOrderEventRequest request,
            UUID eventId,
            String correlationId) {

        OrderEventPayload payload = toTypedPayload(request);

        return new OrderEvent<>(
                eventId,
                request.eventType(),
                request.eventVersion(),
                request.orderId(),
                request.occurredAt(),
                request.source(),
                correlationId,
                payload);
    }

    private OrderEventPayload toTypedPayload(CreateOrderEventRequest request) {
        Class<? extends OrderEventPayload> payloadType = switch (request.eventType()) {
            case ORDER_CREATED -> OrderCreatedPayload.class;
            case PAYMENT_COMPLETED -> PaymentCompletedPayload.class;
            case PAYMENT_FAILED -> PaymentFailedPayload.class;
            case ORDER_CANCELLED -> OrderCancelledPayload.class;
            case ORDER_SHIPPED -> OrderShippedPayload.class;
        };

        try {
            return jsonMapper.treeToValue(request.payload(), payloadType);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new InvalidEventPayloadException(
                    "payload is invalid for eventType " + request.eventType(),
                    exception);
        }
    }
}
