package com.eventstreamingplatform.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.eventstreamingplatform.events.OrderCreatedPayload;
import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.events.OrderEventType;
import com.eventstreamingplatform.ingestion.api.CreateOrderEventRequest;
import com.eventstreamingplatform.ingestion.api.EventAcceptanceStatus;
import com.eventstreamingplatform.ingestion.api.EventAcceptedResponse;
import com.eventstreamingplatform.ingestion.config.IngestionProperties;
import com.eventstreamingplatform.ingestion.mapping.EventRequestMapper;
import com.eventstreamingplatform.ingestion.publishing.EventPublisher;

import tools.jackson.databind.json.JsonMapper;

class KafkaEventSubmissionServiceTest {

    private static final UUID EVENT_ID =
            UUID.fromString("5a020b5d-bf5c-4d48-8ab3-bbab06be6948");
    private static final UUID GENERATED_CORRELATION_ID =
            UUID.fromString("e1576d4e-8c34-4b67-a037-fc2bd89b79b1");
    private static final Instant ACCEPTED_AT = Instant.parse("2026-08-01T20:00:00Z");

    private final EventRequestMapper mapper = mock(EventRequestMapper.class);
    private final EventPublisher publisher = mock(EventPublisher.class);
    private final UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    private final Clock clock = Clock.fixed(ACCEPTED_AT, ZoneOffset.UTC);

    @Test
    void mapsPublishesAndAcceptsAfterBrokerAcknowledgment() throws Exception {
        CreateOrderEventRequest request = request();
        OrderEvent<OrderCreatedPayload> event = event("correlation-100");
        when(uuidGenerator.generate()).thenReturn(EVENT_ID);
        doReturn(event).when(mapper).toOrderEvent(request, EVENT_ID, "correlation-100");
        when(publisher.publish(event)).thenReturn(CompletableFuture.completedFuture(null));
        KafkaEventSubmissionService service = service(Duration.ofSeconds(5));

        EventAcceptedResponse response = service.submit(request, "correlation-100");

        assertThat(response.eventId()).isEqualTo(EVENT_ID);
        assertThat(response.correlationId()).isEqualTo("correlation-100");
        assertThat(response.status()).isEqualTo(EventAcceptanceStatus.ACCEPTED);
        assertThat(response.acceptedAt()).isEqualTo(ACCEPTED_AT);
        assertThat(response.statusUrl()).isNull();
        verify(publisher).publish(event);
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsAbsent() throws Exception {
        CreateOrderEventRequest request = request();
        String correlationId = GENERATED_CORRELATION_ID.toString();
        OrderEvent<OrderCreatedPayload> event = event(correlationId);
        when(uuidGenerator.generate()).thenReturn(EVENT_ID, GENERATED_CORRELATION_ID);
        doReturn(event).when(mapper).toOrderEvent(request, EVENT_ID, correlationId);
        when(publisher.publish(event)).thenReturn(CompletableFuture.completedFuture(null));
        KafkaEventSubmissionService service = service(Duration.ofSeconds(5));

        EventAcceptedResponse response = service.submit(request, null);

        assertThat(response.correlationId()).isEqualTo(correlationId);
    }

    @Test
    void rejectsWhenBrokerReportsFailure() throws Exception {
        CreateOrderEventRequest request = request();
        OrderEvent<OrderCreatedPayload> event = event("correlation-100");
        RuntimeException brokerFailure = new RuntimeException("broker unavailable");
        CompletableFuture<Void> failedPublication = new CompletableFuture<>();
        failedPublication.completeExceptionally(brokerFailure);
        when(uuidGenerator.generate()).thenReturn(EVENT_ID);
        doReturn(event).when(mapper).toOrderEvent(request, EVENT_ID, "correlation-100");
        when(publisher.publish(event)).thenReturn(failedPublication);
        KafkaEventSubmissionService service = service(Duration.ofSeconds(5));

        assertThatThrownBy(() -> service.submit(request, "correlation-100"))
                .isInstanceOf(EventPublicationException.class)
                .hasMessage("Kafka did not acknowledge the event")
                .hasCause(brokerFailure);
    }

    @Test
    void rejectsWhenBrokerAcknowledgmentTimesOut() throws Exception {
        CreateOrderEventRequest request = request();
        OrderEvent<OrderCreatedPayload> event = event("correlation-100");
        when(uuidGenerator.generate()).thenReturn(EVENT_ID);
        doReturn(event).when(mapper).toOrderEvent(request, EVENT_ID, "correlation-100");
        when(publisher.publish(event)).thenReturn(new CompletableFuture<>());
        KafkaEventSubmissionService service = service(Duration.ofMillis(1));

        assertThatThrownBy(() -> service.submit(request, "correlation-100"))
                .isInstanceOf(EventPublicationException.class)
                .hasMessage("Kafka did not acknowledge the event before the publication timeout");
    }

    private KafkaEventSubmissionService service(Duration timeout) {
        return new KafkaEventSubmissionService(
                mapper,
                publisher,
                uuidGenerator,
                clock,
                new IngestionProperties(
                        new IngestionProperties.Kafka("test.order.events.v1", timeout),
                        new IngestionProperties.Cors(List.of("http://test.example"))));
    }

    private static CreateOrderEventRequest request() throws Exception {
        return new CreateOrderEventRequest(
                OrderEventType.ORDER_CREATED,
                1,
                "order-10001",
                Instant.parse("2026-07-25T18:30:00Z"),
                "demo-order-service",
                JsonMapper.builder().build().readTree("""
                        {
                          "customerReference": "customer-200",
                          "currency": "USD",
                          "totalAmount": 149.99,
                          "itemCount": 2
                        }
                        """));
    }

    private static OrderEvent<OrderCreatedPayload> event(String correlationId) {
        return new OrderEvent<>(
                EVENT_ID,
                OrderEventType.ORDER_CREATED,
                1,
                "order-10001",
                Instant.parse("2026-07-25T18:30:00Z"),
                "demo-order-service",
                correlationId,
                new OrderCreatedPayload(
                        "customer-200",
                        "USD",
                        new BigDecimal("149.99"),
                        2));
    }
}
