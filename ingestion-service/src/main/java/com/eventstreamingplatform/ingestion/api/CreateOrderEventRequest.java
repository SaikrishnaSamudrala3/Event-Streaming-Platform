package com.eventstreamingplatform.ingestion.api;

import java.time.Instant;

import com.eventstreamingplatform.events.EventContractVersions;
import com.eventstreamingplatform.events.OrderCancelledPayload;
import com.eventstreamingplatform.events.OrderCreatedPayload;
import com.eventstreamingplatform.events.OrderEventType;
import com.eventstreamingplatform.events.OrderShippedPayload;
import com.eventstreamingplatform.events.PaymentCompletedPayload;
import com.eventstreamingplatform.events.PaymentFailedPayload;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

@Schema(name = "CreateOrderEventRequest", description = "Order event submitted for publication")
public record CreateOrderEventRequest(
        @NotNull OrderEventType eventType,
        @NotNull @Min(1) @Max(EventContractVersions.CURRENT) Integer eventVersion,
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$")
        String orderId,
        @NotNull Instant occurredAt,
        @NotBlank @Size(max = 100) String source,
        @NotNull
        @Schema(
                description = "Event-specific data matching eventType",
                oneOf = {
                        OrderCreatedPayload.class,
                        PaymentCompletedPayload.class,
                        PaymentFailedPayload.class,
                        OrderCancelledPayload.class,
                        OrderShippedPayload.class
                })
        JsonNode payload
) {
}
