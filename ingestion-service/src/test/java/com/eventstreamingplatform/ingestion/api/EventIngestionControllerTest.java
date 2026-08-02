package com.eventstreamingplatform.ingestion.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eventstreamingplatform.ingestion.config.WebConfiguration;
import com.eventstreamingplatform.ingestion.config.IngestionProperties;
import com.eventstreamingplatform.ingestion.mapping.InvalidEventPayloadException;
import com.eventstreamingplatform.ingestion.service.EventSubmissionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = EventIngestionController.class,
        properties = {
                "ingestion.kafka.topic=test.order.events.v1",
                "ingestion.cors.allowed-origins=http://frontend.example"
        })
@Import(WebConfiguration.class)
@EnableConfigurationProperties(IngestionProperties.class)
class EventIngestionControllerTest {

    private static final UUID EVENT_ID =
            UUID.fromString("5a020b5d-bf5c-4d48-8ab3-bbab06be6948");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventSubmissionService submissionService;

    @Test
    void acceptsAValidEvent() throws Exception {
        EventAcceptedResponse response = new EventAcceptedResponse(
                EVENT_ID,
                "correlation-100",
                EventAcceptanceStatus.ACCEPTED,
                Instant.parse("2026-08-01T20:00:00Z"),
                URI.create("/api/v1/events/" + EVENT_ID));
        when(submissionService.submit(
                any(CreateOrderEventRequest.class),
                eq("correlation-100")))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-ID", "correlation-100")
                        .content(validRequest()))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.correlationId").value("correlation-100"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedAt").value("2026-08-01T20:00:00Z"));

        verify(submissionService).submit(
                any(CreateOrderEventRequest.class),
                eq("correlation-100"));
    }

    @Test
    void rejectsAnInvalidRequestBeforeCallingTheService() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventVersion": 2,
                                  "orderId": " invalid order ",
                                  "source": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:validation-error"))
                .andExpect(jsonPath("$.title").value("Request validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/v1/events"))
                .andExpect(jsonPath("$.validationErrors").isArray());

        verify(submissionService, never()).submit(any(), any());
    }

    @Test
    void returnsSafeErrorForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:malformed-json"))
                .andExpect(jsonPath("$.detail")
                        .value("The request body is missing or contains invalid JSON"));
    }

    @Test
    void returnsSafeErrorForPayloadMismatch() throws Exception {
        when(submissionService.submit(any(CreateOrderEventRequest.class), any()))
                .thenThrow(new InvalidEventPayloadException(
                        "payload is invalid for eventType ORDER_CREATED",
                        new IllegalArgumentException("internal detail")));

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-event-payload"))
                .andExpect(jsonPath("$.detail")
                        .value("payload is invalid for eventType ORDER_CREATED"));
    }

    @Test
    void rejectsInvalidRequestHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-ID", "   ")
                        .content(validRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:validation-error"));

        verify(submissionService, never()).submit(any(), any());
    }

    @Test
    void permitsConfiguredCorsOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/events")
                        .header("Origin", "http://frontend.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://frontend.example"));
    }

    private static String validRequest() {
        return """
                {
                  "eventType": "ORDER_CREATED",
                  "eventVersion": 1,
                  "orderId": "order-10001",
                  "occurredAt": "2026-07-25T18:30:00Z",
                  "source": "demo-order-service",
                  "payload": {
                    "customerReference": "customer-200",
                    "currency": "USD",
                    "totalAmount": 149.99,
                    "itemCount": 2
                  }
                }
                """;
    }
}
