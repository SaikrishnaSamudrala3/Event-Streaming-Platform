package com.eventstreamingplatform.ingestion.api;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventAcceptedResponse(
        UUID eventId,
        String correlationId,
        EventAcceptanceStatus status,
        Instant acceptedAt,
        URI statusUrl
) {
}
