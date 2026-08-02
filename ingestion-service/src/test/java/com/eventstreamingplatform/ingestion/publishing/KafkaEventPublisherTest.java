package com.eventstreamingplatform.ingestion.publishing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import com.eventstreamingplatform.events.OrderCreatedPayload;
import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.ingestion.config.IngestionProperties;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@SuppressWarnings("unchecked")
class KafkaEventPublisherTest {

    private static final String TOPIC = "test.order.events.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final KafkaEventPublisher publisher = new KafkaEventPublisher(
            kafkaTemplate,
            new IngestionProperties(
                    new IngestionProperties.Kafka(TOPIC, Duration.ofSeconds(5)),
                    new IngestionProperties.Cors(List.of("http://test.example"))));

    @Test
    void publishesToConfiguredTopicUsingOrderIdAsKey() {
        OrderEvent<OrderCreatedPayload> event = event();
        SendResult<String, Object> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(TOPIC, event.orderId(), event))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        CompletableFuture<Void> publication = publisher.publish(event);

        assertThat(publication).isCompletedWithValue(null);
        verify(kafkaTemplate).send(TOPIC, "order-10001", event);
    }

    @Test
    void propagatesBrokerFailureToTheCaller() {
        OrderEvent<OrderCreatedPayload> event = event();
        RuntimeException brokerFailure = new RuntimeException("broker unavailable");
        CompletableFuture<SendResult<String, Object>> failedSend = new CompletableFuture<>();
        failedSend.completeExceptionally(brokerFailure);
        when(kafkaTemplate.send(TOPIC, event.orderId(), event)).thenReturn(failedSend);

        CompletableFuture<Void> publication = publisher.publish(event);

        assertThatThrownBy(publication::join)
                .isInstanceOf(CompletionException.class)
                .hasCause(brokerFailure);
    }

    private static OrderEvent<OrderCreatedPayload> event() {
        return new OrderEvent<>(
                UUID.fromString("5a020b5d-bf5c-4d48-8ab3-bbab06be6948"),
                com.eventstreamingplatform.events.OrderEventType.ORDER_CREATED,
                1,
                "order-10001",
                Instant.parse("2026-07-25T18:30:00Z"),
                "demo-order-service",
                "correlation-100",
                new OrderCreatedPayload(
                        "customer-200",
                        "USD",
                        new BigDecimal("149.99"),
                        2));
    }
}
