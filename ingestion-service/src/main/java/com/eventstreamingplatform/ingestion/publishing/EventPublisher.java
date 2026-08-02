package com.eventstreamingplatform.ingestion.publishing;

import java.util.concurrent.CompletableFuture;

import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.events.OrderEventPayload;

public interface EventPublisher {

    CompletableFuture<Void> publish(OrderEvent<? extends OrderEventPayload> event);
}
