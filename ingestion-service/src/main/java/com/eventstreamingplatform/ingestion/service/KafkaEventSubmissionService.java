package com.eventstreamingplatform.ingestion.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.events.OrderEventPayload;
import com.eventstreamingplatform.ingestion.api.CreateOrderEventRequest;
import com.eventstreamingplatform.ingestion.api.EventAcceptanceStatus;
import com.eventstreamingplatform.ingestion.api.EventAcceptedResponse;
import com.eventstreamingplatform.ingestion.config.IngestionProperties;
import com.eventstreamingplatform.ingestion.mapping.EventRequestMapper;
import com.eventstreamingplatform.ingestion.publishing.EventPublisher;

import org.springframework.stereotype.Service;

@Service
public class KafkaEventSubmissionService implements EventSubmissionService {

    private final EventRequestMapper mapper;
    private final EventPublisher publisher;
    private final UuidGenerator uuidGenerator;
    private final Clock clock;
    private final long publishTimeoutMillis;

    public KafkaEventSubmissionService(
            EventRequestMapper mapper,
            EventPublisher publisher,
            UuidGenerator uuidGenerator,
            Clock clock,
            IngestionProperties properties) {
        this.mapper = mapper;
        this.publisher = publisher;
        this.uuidGenerator = uuidGenerator;
        this.clock = clock;
        this.publishTimeoutMillis = properties.kafka().publishTimeout().toMillis();
    }

    @Override
    public EventAcceptedResponse submit(
            CreateOrderEventRequest request,
            String suppliedCorrelationId) {

        UUID eventId = uuidGenerator.generate();
        String correlationId = suppliedCorrelationId != null
                ? suppliedCorrelationId
                : uuidGenerator.generate().toString();
        OrderEvent<? extends OrderEventPayload> event =
                mapper.toOrderEvent(request, eventId, correlationId);

        awaitBrokerAcknowledgment(event);

        return new EventAcceptedResponse(
                eventId,
                correlationId,
                EventAcceptanceStatus.ACCEPTED,
                Instant.now(clock),
                null);
    }

    private void awaitBrokerAcknowledgment(OrderEvent<? extends OrderEventPayload> event) {
        try {
            publisher.publish(event).get(publishTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            throw new EventPublicationException(
                    "Kafka did not acknowledge the event before the publication timeout",
                    exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new EventPublicationException("Event publication was interrupted", exception);
        } catch (ExecutionException exception) {
            throw new EventPublicationException(
                    "Kafka did not acknowledge the event",
                    exception.getCause());
        } catch (RuntimeException exception) {
            throw new EventPublicationException("Kafka did not accept the event", exception);
        }
    }
}
