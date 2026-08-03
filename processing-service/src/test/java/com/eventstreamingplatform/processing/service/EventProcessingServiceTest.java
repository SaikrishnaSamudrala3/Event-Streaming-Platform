package com.eventstreamingplatform.processing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.eventstreamingplatform.events.OrderCreatedPayload;
import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.events.OrderEventType;
import com.eventstreamingplatform.processing.persistence.KafkaRecordMetadata;
import com.eventstreamingplatform.processing.persistence.ProcessedEventEntity;
import com.eventstreamingplatform.processing.persistence.ProcessedEventMapper;
import com.eventstreamingplatform.processing.persistence.ProcessedEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class EventProcessingServiceTest {

    private static final UUID EVENT_ID =
            UUID.fromString("83cc98d6-3360-425f-8ac5-642287b43f48");

    private final ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
    private final ProcessedEventMapper mapper = mock(ProcessedEventMapper.class);
    private final ProcessedEventEntity entity = mock(ProcessedEventEntity.class);

    private EventProcessingService service;
    private OrderEvent<OrderCreatedPayload> event;
    private KafkaRecordMetadata metadata;

    @BeforeEach
    void setUp() {
        service = new EventProcessingService(repository, mapper);
        event = new OrderEvent<>(
                EVENT_ID,
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
        metadata = new KafkaRecordMetadata(
                "order.events.v1",
                2,
                41L,
                Instant.parse("2026-08-01T12:00:01Z"));
    }

    @Test
    void persistsANewEvent() {
        when(mapper.toProcessedEntity(event, metadata)).thenReturn(entity);

        ProcessingOutcome outcome = service.process(event, metadata);

        assertThat(outcome).isEqualTo(ProcessingOutcome.PROCESSED);
        verify(mapper).toProcessedEntity(event, metadata);
        verify(repository).save(entity);
    }

    @Test
    void skipsAnEventWithAnExistingEventId() {
        when(repository.existsByEventId(EVENT_ID)).thenReturn(true);

        ProcessingOutcome outcome = service.process(event, metadata);

        assertThat(outcome).isEqualTo(ProcessingOutcome.DUPLICATE);
        verify(repository, never()).existsByKafkaTopicAndKafkaPartitionAndKafkaOffset(
                metadata.topic(), metadata.partition(), metadata.offset());
        verify(mapper, never()).toProcessedEntity(event, metadata);
        verify(repository, never()).save(entity);
    }

    @Test
    void skipsAnEventWithAnExistingKafkaPosition() {
        when(repository.existsByKafkaTopicAndKafkaPartitionAndKafkaOffset(
                metadata.topic(), metadata.partition(), metadata.offset()))
                .thenReturn(true);

        ProcessingOutcome outcome = service.process(event, metadata);

        assertThat(outcome).isEqualTo(ProcessingOutcome.DUPLICATE);
        verify(mapper, never()).toProcessedEntity(event, metadata);
        verify(repository, never()).save(entity);
    }

    @Test
    void propagatesPersistenceFailuresSoTheTransactionCanRollBack() {
        when(mapper.toProcessedEntity(event, metadata)).thenReturn(entity);
        var failure = new DataAccessResourceFailureException("MySQL unavailable");
        doThrow(failure).when(repository).save(entity);

        assertThatThrownBy(() -> service.process(event, metadata))
                .isSameAs(failure);
    }
}
