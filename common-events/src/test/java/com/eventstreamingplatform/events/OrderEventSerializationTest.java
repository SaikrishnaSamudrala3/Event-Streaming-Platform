package com.eventstreamingplatform.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class OrderEventSerializationTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @ParameterizedTest(name = "{0}")
    @MethodSource("payloads")
    <P extends OrderEventPayload> void shouldRoundTripEveryPayload(
            String description,
            P payload,
            Class<P> payloadType) throws Exception {

        OrderEvent<P> original = EventTestFixtures.event(payload);

        String json = JSON_MAPPER.writeValueAsString(original);
        JavaType eventType = JSON_MAPPER.getTypeFactory()
                .constructParametricType(OrderEvent.class, payloadType);
        OrderEvent<P> restored = JSON_MAPPER.readValue(json, eventType);

        assertEquals(original, restored);
        assertTrue(json.contains("\"eventVersion\":1"));
        assertTrue(json.contains("\"occurredAt\":\"2026-07-25T18:30:00Z\""));
    }

    @Test
    void shouldIgnoreAdditiveUnknownFields() throws Exception {
        OrderEvent<OrderCreatedPayload> original = EventTestFixtures.event(
                new OrderCreatedPayload(
                        "customer-200",
                        "USD",
                        new BigDecimal("149.99"),
                        2));

        String json = JSON_MAPPER.writeValueAsString(original)
                .replace(
                        "\"eventId\"",
                        "\"futureEnvelopeField\":\"ignored\",\"eventId\"")
                .replace(
                        "\"customerReference\"",
                        "\"futurePayloadField\":true,\"customerReference\"");
        JavaType eventType = JSON_MAPPER.getTypeFactory()
                .constructParametricType(OrderEvent.class, OrderCreatedPayload.class);

        OrderEvent<OrderCreatedPayload> restored = JSON_MAPPER.readValue(json, eventType);

        assertEquals(original, restored);
    }

    @Test
    void shouldUseTheDocumentedJsonShape() throws Exception {
        OrderEvent<OrderCreatedPayload> event = EventTestFixtures.event(
                new OrderCreatedPayload(
                        "customer-200",
                        "USD",
                        new BigDecimal("149.99"),
                        2));

        JsonNode json = JSON_MAPPER.readTree(JSON_MAPPER.writeValueAsString(event));

        assertEquals("ORDER_CREATED", json.get("eventType").stringValue());
        assertEquals("order-10001", json.get("orderId").stringValue());
        assertEquals(
                "customer-200",
                json.get("payload").get("customerReference").stringValue());
        assertFalse(json.get("payload").has("eventType"));
    }

    private static Stream<Arguments> payloads() {
        return Stream.of(
                Arguments.of(
                        "order created",
                        new OrderCreatedPayload(
                                "customer-200",
                                "USD",
                                new BigDecimal("149.99"),
                                2),
                        OrderCreatedPayload.class),
                Arguments.of(
                        "payment completed",
                        new PaymentCompletedPayload(
                                "payment-300",
                                new BigDecimal("149.99"),
                                "USD",
                                Instant.parse("2026-07-25T18:31:00Z")),
                        PaymentCompletedPayload.class),
                Arguments.of(
                        "payment failed",
                        new PaymentFailedPayload(
                                "payment-301",
                                PaymentFailureReason.DECLINED,
                                Instant.parse("2026-07-25T18:31:30Z")),
                        PaymentFailedPayload.class),
                Arguments.of(
                        "order cancelled",
                        new OrderCancelledPayload(
                                "Customer requested cancellation",
                                Instant.parse("2026-07-25T18:32:00Z")),
                        OrderCancelledPayload.class),
                Arguments.of(
                        "order shipped",
                        new OrderShippedPayload(
                                "shipment-400",
                                "Demo Carrier",
                                Instant.parse("2026-07-25T18:33:00Z")),
                        OrderShippedPayload.class));
    }
}
