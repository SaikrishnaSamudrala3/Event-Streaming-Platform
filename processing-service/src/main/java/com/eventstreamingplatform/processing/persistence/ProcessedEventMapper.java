package com.eventstreamingplatform.processing.persistence;

import java.time.Clock;

import com.eventstreamingplatform.events.OrderEvent;

import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProcessedEventMapper {

    private final JsonMapper jsonMapper;
    private final Clock clock;

    public ProcessedEventMapper(JsonMapper jsonMapper, Clock clock) {
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    public ProcessedEventEntity toProcessedEntity(
            OrderEvent<?> event,
            KafkaRecordMetadata metadata) {

        String payloadJson = jsonMapper.writeValueAsString(event.payload());

        return new ProcessedEventEntity(
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                event.orderId(),
                event.source(),
                event.correlationId(),
                event.occurredAt(),
                metadata.receivedAt(),
                clock.instant(),
                ProcessingStatus.PROCESSED,
                payloadJson,
                metadata.topic(),
                metadata.partition(),
                metadata.offset());
    }
}
