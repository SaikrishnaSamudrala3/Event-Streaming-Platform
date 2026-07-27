package com.eventstreamingplatform.events;

/**
 * Marker contract for payloads that can be carried by an {@link OrderEvent}.
 */
public sealed interface OrderEventPayload
        permits OrderCreatedPayload, PaymentCompletedPayload, PaymentFailedPayload,
                OrderCancelledPayload, OrderShippedPayload {

    /**
     * Returns the event type that this payload represents.
     *
     * @return the matching event type
     */
    OrderEventType eventType();
}
