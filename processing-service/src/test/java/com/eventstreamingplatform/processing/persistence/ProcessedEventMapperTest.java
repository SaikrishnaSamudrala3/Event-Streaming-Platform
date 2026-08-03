package com.eventstreamingplatform.processing.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.eventstreamingplatform.events.OrderCreatedPayload;
import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.events.OrderEventType;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ProcessedEventMapperTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-08-01T12:00:00Z");
    private static final Instant RECEIVED_AT = Instant.parse("2026-08-01T12:00:01Z");
    private static final Instant PROCESSED_AT = Instant.parse("2026-08-01T12:00:02Z");

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final ProcessedEventMapper mapper = new ProcessedEventMapper(
            jsonMapper,
            Clock.fixed(PROCESSED_AT, ZoneOffset.UTC));

    @Test
    void mapsAContractEventAndKafkaPositionToAProcessedEntity() {
        UUID eventId = UUID.fromString("83cc98d6-3360-425f-8ac5-642287b43f48");
        var event = new OrderEvent<>(
                eventId,
                OrderEventType.ORDER_CREATED,
                1,
                "order-1001",
                OCCURRED_AT,
                "checkout-service",
                "correlation-1001",
                new OrderCreatedPayload(
                        "customer-1001",
                        "USD",
                        new BigDecimal("149.99"),
                        2));
        var metadata = new KafkaRecordMetadata("order.events.v1", 2, 41L, RECEIVED_AT);

        ProcessedEventEntity entity = mapper.toProcessedEntity(event, metadata);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getEventId()).isEqualTo(eventId);
        assertThat(entity.getEventType()).isEqualTo(OrderEventType.ORDER_CREATED);
        assertThat(entity.getEventVersion()).isEqualTo(1);
        assertThat(entity.getOrderId()).isEqualTo("order-1001");
        assertThat(entity.getSource()).isEqualTo("checkout-service");
        assertThat(entity.getCorrelationId()).isEqualTo("correlation-1001");
        assertThat(entity.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(entity.getReceivedAt()).isEqualTo(RECEIVED_AT);
        assertThat(entity.getProcessedAt()).isEqualTo(PROCESSED_AT);
        assertThat(entity.getProcessingStatus()).isEqualTo(ProcessingStatus.PROCESSED);
        assertThat(entity.getRetryCount()).isZero();
        assertThat(entity.getFailureCategory()).isNull();
        assertThat(entity.getFailureMessage()).isNull();
        assertThat(entity.getFailedAt()).isNull();
        assertThat(entity.getKafkaTopic()).isEqualTo("order.events.v1");
        assertThat(entity.getKafkaPartition()).isEqualTo(2);
        assertThat(entity.getKafkaOffset()).isEqualTo(41L);

        var payload = jsonMapper.readTree(entity.getPayload());
        assertThat(payload.get("customerReference").asString()).isEqualTo("customer-1001");
        assertThat(payload.get("currency").asString()).isEqualTo("USD");
        assertThat(payload.get("totalAmount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("149.99"));
        assertThat(payload.get("itemCount").asInt()).isEqualTo(2);
        assertThat(payload.has("eventId")).isFalse();
    }
}
