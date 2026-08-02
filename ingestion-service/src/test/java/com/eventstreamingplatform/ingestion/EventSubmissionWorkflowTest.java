package com.eventstreamingplatform.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.events.OrderEventPayload;
import com.eventstreamingplatform.events.OrderEventType;
import com.eventstreamingplatform.ingestion.publishing.EventPublisher;
import com.eventstreamingplatform.ingestion.service.UuidGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventSubmissionWorkflowTest {

    private static final UUID EVENT_ID =
            UUID.fromString("5a020b5d-bf5c-4d48-8ab3-bbab06be6948");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventPublisher publisher;

    @MockitoBean
    private UuidGenerator uuidGenerator;

    @Test
    void submitsValidHttpRequestThroughMapperAndPublisher() throws Exception {
        when(uuidGenerator.generate()).thenReturn(EVENT_ID);
        when(publisher.publish(any())).thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-ID", "correlation-100")
                        .content(validRequest()))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.correlationId").value("correlation-100"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedAt").isNotEmpty())
                .andExpect(jsonPath("$.statusUrl").doesNotExist());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<OrderEvent<? extends OrderEventPayload>> eventCaptor =
                ArgumentCaptor.forClass(OrderEvent.class);
        verify(publisher).publish(eventCaptor.capture());
        OrderEvent<? extends OrderEventPayload> publishedEvent = eventCaptor.getValue();

        assertThat(publishedEvent.eventId()).isEqualTo(EVENT_ID);
        assertThat(publishedEvent.eventType()).isEqualTo(OrderEventType.ORDER_CREATED);
        assertThat(publishedEvent.orderId()).isEqualTo("order-10001");
        assertThat(publishedEvent.correlationId()).isEqualTo("correlation-100");
    }

    @Test
    void returnsServiceUnavailableWhenPublisherFails() throws Exception {
        when(uuidGenerator.generate()).thenReturn(EVENT_ID);
        CompletableFuture<Void> failure = new CompletableFuture<>();
        failure.completeExceptionally(new RuntimeException("broker unavailable"));
        when(publisher.publish(any())).thenReturn(failure);

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-ID", "correlation-100")
                        .content(validRequest()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem:service-unavailable"))
                .andExpect(jsonPath("$.detail")
                        .value("The event could not be acknowledged by Kafka"));
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
