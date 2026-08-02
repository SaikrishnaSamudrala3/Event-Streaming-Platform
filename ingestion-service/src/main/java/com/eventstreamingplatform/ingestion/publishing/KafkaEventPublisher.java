package com.eventstreamingplatform.ingestion.publishing;

import java.util.concurrent.CompletableFuture;

import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.events.OrderEventPayload;
import com.eventstreamingplatform.ingestion.config.IngestionProperties;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            IngestionProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.kafka().topic();
    }

    @Override
    public CompletableFuture<Void> publish(OrderEvent<? extends OrderEventPayload> event) {
        return kafkaTemplate.send(topic, event.orderId(), event)
                .thenApply(sendResult -> null);
    }
}
