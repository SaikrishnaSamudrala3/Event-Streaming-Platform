package com.eventstreamingplatform.events;

/**
 * The supported business events in the order lifecycle.
 */
public enum OrderEventType {
    ORDER_CREATED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    ORDER_CANCELLED,
    ORDER_SHIPPED
}
