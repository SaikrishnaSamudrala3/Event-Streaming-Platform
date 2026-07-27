package com.eventstreamingplatform.events;

/**
 * Safe, non-sensitive categories for a failed payment.
 */
public enum PaymentFailureReason {
    DECLINED,
    INSUFFICIENT_FUNDS,
    EXPIRED_PAYMENT_METHOD,
    PROCESSOR_UNAVAILABLE,
    VALIDATION_ERROR,
    UNKNOWN
}
