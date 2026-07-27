package com.eventstreamingplatform.events;

import java.time.Instant;
import java.util.UUID;

final class EventTestFixtures {

    static final UUID EVENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    static final Instant OCCURRED_AT = Instant.parse("2026-07-25T18:30:00Z");

    private EventTestFixtures() {
    }

    static <P extends OrderEventPayload> OrderEvent<P> event(P payload) {
        return new OrderEvent<>(
                EVENT_ID,
                payload.eventType(),
                EventContractVersions.CURRENT,
                "order-10001",
                OCCURRED_AT,
                "demo-order-service",
                "correlation-10001",
                payload);
    }
}
