package com.eventstreamingplatform.ingestion.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.eventstreamingplatform.events.OrderCancelledPayload;
import com.eventstreamingplatform.events.OrderCreatedPayload;
import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.events.OrderEventPayload;
import com.eventstreamingplatform.events.OrderEventType;
import com.eventstreamingplatform.events.OrderShippedPayload;
import com.eventstreamingplatform.events.PaymentCompletedPayload;
import com.eventstreamingplatform.events.PaymentFailedPayload;
import com.eventstreamingplatform.ingestion.api.CreateOrderEventRequest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class EventRequestMapperTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final UUID EVENT_ID =
            UUID.fromString("5a020b5d-bf5c-4d48-8ab3-bbab06be6948");
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-25T18:30:00Z");

    private final EventRequestMapper mapper = new EventRequestMapper(JSON_MAPPER);

    @ParameterizedTest(name = "maps {0}")
    @MethodSource("validPayloads")
    void mapsEverySupportedPayload(
            OrderEventType eventType,
            String payloadJson,
            Class<? extends OrderEventPayload> expectedPayloadType) throws Exception {

        CreateOrderEventRequest request = request(eventType, JSON_MAPPER.readTree(payloadJson));

        OrderEvent<? extends OrderEventPayload> event =
                mapper.toOrderEvent(request, EVENT_ID, "correlation-100");

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.eventType()).isEqualTo(eventType);
        assertThat(event.eventVersion()).isEqualTo(1);
        assertThat(event.orderId()).isEqualTo("order-10001");
        assertThat(event.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(event.source()).isEqualTo("demo-order-service");
        assertThat(event.correlationId()).isEqualTo("correlation-100");
        assertThat(event.payload()).isInstanceOf(expectedPayloadType);
    }

    @Test
    void rejectsPayloadThatDoesNotMatchEventType() throws Exception {
        JsonNode paymentPayload = JSON_MAPPER.readTree("""
                {
                  "paymentReference": "payment-300",
                  "amount": 149.99,
                  "currency": "USD",
                  "completedAt": "2026-07-25T18:31:00Z"
                }
                """);
        CreateOrderEventRequest request = request(OrderEventType.ORDER_CREATED, paymentPayload);

        assertThatThrownBy(() -> mapper.toOrderEvent(request, EVENT_ID, "correlation-100"))
                .isInstanceOf(InvalidEventPayloadException.class)
                .hasMessage("payload is invalid for eventType ORDER_CREATED");
    }

    private static CreateOrderEventRequest request(OrderEventType eventType, JsonNode payload) {
        return new CreateOrderEventRequest(
                eventType,
                1,
                "order-10001",
                OCCURRED_AT,
                "demo-order-service",
                payload);
    }

    private static Stream<Arguments> validPayloads() {
        return Stream.of(
                Arguments.of(
                        OrderEventType.ORDER_CREATED,
                        """
                        {
                          "customerReference": "customer-200",
                          "currency": "USD",
                          "totalAmount": 149.99,
                          "itemCount": 2
                        }
                        """,
                        OrderCreatedPayload.class),
                Arguments.of(
                        OrderEventType.PAYMENT_COMPLETED,
                        """
                        {
                          "paymentReference": "payment-300",
                          "amount": 149.99,
                          "currency": "USD",
                          "completedAt": "2026-07-25T18:31:00Z"
                        }
                        """,
                        PaymentCompletedPayload.class),
                Arguments.of(
                        OrderEventType.PAYMENT_FAILED,
                        """
                        {
                          "paymentReference": "payment-301",
                          "failureReason": "DECLINED",
                          "failedAt": "2026-07-25T18:31:30Z"
                        }
                        """,
                        PaymentFailedPayload.class),
                Arguments.of(
                        OrderEventType.ORDER_CANCELLED,
                        """
                        {
                          "reason": "Customer requested cancellation",
                          "cancelledAt": "2026-07-25T18:32:00Z"
                        }
                        """,
                        OrderCancelledPayload.class),
                Arguments.of(
                        OrderEventType.ORDER_SHIPPED,
                        """
                        {
                          "shipmentReference": "shipment-400",
                          "carrier": "Demo Carrier",
                          "shippedAt": "2026-07-25T18:33:00Z"
                        }
                        """,
                        OrderShippedPayload.class));
    }
}
