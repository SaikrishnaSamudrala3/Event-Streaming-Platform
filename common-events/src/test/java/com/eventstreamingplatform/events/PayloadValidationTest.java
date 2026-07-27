package com.eventstreamingplatform.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class PayloadValidationTest {

    @Test
    void shouldExposeMatchingEventTypeForEveryPayload() {
        assertEquals(
                OrderEventType.ORDER_CREATED,
                new OrderCreatedPayload(
                        "customer-200",
                        "USD",
                        new BigDecimal("149.99"),
                        2).eventType());
        assertEquals(
                OrderEventType.PAYMENT_COMPLETED,
                new PaymentCompletedPayload(
                        "payment-300",
                        new BigDecimal("149.99"),
                        "USD",
                        Instant.parse("2026-07-25T18:31:00Z")).eventType());
        assertEquals(
                OrderEventType.PAYMENT_FAILED,
                new PaymentFailedPayload(
                        "payment-301",
                        PaymentFailureReason.DECLINED,
                        Instant.parse("2026-07-25T18:31:30Z")).eventType());
        assertEquals(
                OrderEventType.ORDER_CANCELLED,
                new OrderCancelledPayload(
                        "Customer requested cancellation",
                        Instant.parse("2026-07-25T18:32:00Z")).eventType());
        assertEquals(
                OrderEventType.ORDER_SHIPPED,
                new OrderShippedPayload(
                        "shipment-400",
                        "Demo Carrier",
                        Instant.parse("2026-07-25T18:33:00Z")).eventType());
    }

    @Test
    void shouldRejectInvalidCurrency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderCreatedPayload(
                        "customer-200",
                        "US",
                        new BigDecimal("149.99"),
                        2));
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentCompletedPayload(
                        "payment-300",
                        BigDecimal.ZERO,
                        "USD",
                        Instant.parse("2026-07-25T18:31:00Z")));
    }

    @Test
    void shouldRejectAmountWithExcessivePrecision() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderCreatedPayload(
                        "customer-200",
                        "USD",
                        new BigDecimal("149.999"),
                        2));
    }

    @Test
    void shouldRejectEmptyOrder() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderCreatedPayload(
                        "customer-200",
                        "USD",
                        new BigDecimal("149.99"),
                        0));
    }

    @Test
    void shouldRejectMissingFailureReason() {
        assertThrows(
                NullPointerException.class,
                () -> new PaymentFailedPayload(
                        "payment-301",
                        null,
                        Instant.parse("2026-07-25T18:31:30Z")));
    }
}
