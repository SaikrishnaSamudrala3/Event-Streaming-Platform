package com.eventstreamingplatform.processing.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.eventstreamingplatform.events.OrderCreatedPayload;
import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.events.OrderEventType;
import com.eventstreamingplatform.processing.persistence.KafkaRecordMetadata;
import com.eventstreamingplatform.processing.service.EventProcessingService;
import com.eventstreamingplatform.processing.service.ProcessingOutcome;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class OrderEventListenerTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-08-01T12:00:01Z");

    private final EventProcessingService processingService = mock(EventProcessingService.class);
    private final Acknowledgment acknowledgment = mock(Acknowledgment.class);

    private OrderEventListener listener;
    private ConsumerRecord<String, OrderEvent<?>> record;
    private KafkaRecordMetadata expectedMetadata;

    @BeforeEach
    void setUp() {
        listener = new OrderEventListener(
                processingService,
                Clock.fixed(RECEIVED_AT, ZoneOffset.UTC));

        var event = new OrderEvent<>(
                UUID.fromString("83cc98d6-3360-425f-8ac5-642287b43f48"),
                OrderEventType.ORDER_CREATED,
                1,
                "order-1001",
                Instant.parse("2026-08-01T12:00:00Z"),
                "checkout-service",
                "correlation-1001",
                new OrderCreatedPayload(
                        "customer-1001",
                        "USD",
                        new BigDecimal("149.99"),
                        2));

        record = new ConsumerRecord<>("order.events.v1", 2, 41L, "order-1001", event);
        expectedMetadata = new KafkaRecordMetadata("order.events.v1", 2, 41L, RECEIVED_AT);
    }

    @Test
    void acknowledgesAfterANewEventIsProcessed() {
        when(processingService.process(record.value(), expectedMetadata))
                .thenReturn(ProcessingOutcome.PROCESSED);

        listener.consume(record, acknowledgment);

        verify(processingService).process(record.value(), expectedMetadata);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void acknowledgesAnIdempotentDuplicate() {
        when(processingService.process(record.value(), expectedMetadata))
                .thenReturn(ProcessingOutcome.DUPLICATE);

        listener.consume(record, acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void doesNotAcknowledgeWhenProcessingFails() {
        var failure = new IllegalStateException("database unavailable");
        when(processingService.process(record.value(), expectedMetadata)).thenThrow(failure);

        assertThatThrownBy(() -> listener.consume(record, acknowledgment))
                .isSameAs(failure);

        verify(acknowledgment, never()).acknowledge();
    }
}
