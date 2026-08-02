package com.eventstreamingplatform.ingestion.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.eventstreamingplatform.events.OrderEventType;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import tools.jackson.databind.json.JsonMapper;

class CreateOrderEventRequestValidationTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Test
    void acceptsAValidEnvelope() {
        CreateOrderEventRequest request = validRequest();

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingAndMalformedEnvelopeFields() {
        CreateOrderEventRequest request = new CreateOrderEventRequest(
                null,
                2,
                " invalid order ",
                null,
                " ",
                null);

        Set<ConstraintViolation<CreateOrderEventRequest>> violations =
                VALIDATOR.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("eventType", "eventVersion", "orderId", "occurredAt", "source", "payload");
    }

    private static CreateOrderEventRequest validRequest() {
        return new CreateOrderEventRequest(
                OrderEventType.ORDER_CREATED,
                1,
                "order-10001",
                Instant.parse("2026-07-25T18:30:00Z"),
                "demo-order-service",
                JSON_MAPPER.createObjectNode());
    }
}
