package com.eventstreamingplatform.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OrderEventValidationTest {

    private final OrderCreatedPayload payload = new OrderCreatedPayload(
            "customer-200",
            "USD",
            new BigDecimal("149.99"),
            2);

    @Test
    void shouldAcceptCurrentContractVersion() {
        assertDoesNotThrow(() -> EventTestFixtures.event(payload));
        assertTrue(EventContractVersions.isSupported(EventContractVersions.CURRENT));
    }

    @Test
    void shouldRejectUnsupportedContractVersion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new OrderEvent<>(
                        EventTestFixtures.EVENT_ID,
                        OrderEventType.ORDER_CREATED,
                        2,
                        "order-10001",
                        EventTestFixtures.OCCURRED_AT,
                        "demo-order-service",
                        "correlation-10001",
                        payload));

        assertEquals(
                "Unsupported eventVersion 2; supported version is 1",
                exception.getMessage());
    }

    @Test
    void shouldRejectEventTypeThatDoesNotMatchPayload() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new OrderEvent<>(
                        EventTestFixtures.EVENT_ID,
                        OrderEventType.ORDER_SHIPPED,
                        EventContractVersions.CURRENT,
                        "order-10001",
                        EventTestFixtures.OCCURRED_AT,
                        "demo-order-service",
                        "correlation-10001",
                        payload));

        assertTrue(exception.getMessage().contains("does not match payload type"));
    }

    @Test
    void shouldRejectMissingRequiredEnvelopeField() {
        assertThrows(
                NullPointerException.class,
                () -> new OrderEvent<>(
                        null,
                        OrderEventType.ORDER_CREATED,
                        EventContractVersions.CURRENT,
                        "order-10001",
                        EventTestFixtures.OCCURRED_AT,
                        "demo-order-service",
                        "correlation-10001",
                        payload));
    }

    @Test
    void shouldRejectInvalidOrderIdentifier() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderEvent<>(
                        EventTestFixtures.EVENT_ID,
                        OrderEventType.ORDER_CREATED,
                        EventContractVersions.CURRENT,
                        "order id with spaces",
                        EventTestFixtures.OCCURRED_AT,
                        "demo-order-service",
                        "correlation-10001",
                        payload));
    }
}
