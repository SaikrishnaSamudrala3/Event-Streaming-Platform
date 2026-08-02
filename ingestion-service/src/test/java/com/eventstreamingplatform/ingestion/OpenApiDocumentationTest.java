package com.eventstreamingplatform.ingestion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesGeneratedOpenApiForTheIngestionEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title")
                        .value("Real-Time Event Streaming Platform API"))
                .andExpect(jsonPath("$.info.version").value("0.1.0"))
                .andExpect(jsonPath("$.paths['/api/v1/events'].post.operationId")
                        .value("submitOrderEvent"))
                .andExpect(jsonPath("$.paths['/api/v1/events'].post.responses['202']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/events'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/events'].post.responses['503']").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/events'].post.parameters[?(@.name == 'X-Correlation-ID')]")
                        .exists())
                .andExpect(jsonPath(
                        "$.components.schemas.CreateOrderEventRequest.properties.payload.oneOf")
                        .isArray())
                .andExpect(jsonPath("$.components.schemas.OrderCreatedPayload").exists())
                .andExpect(jsonPath("$.components.schemas.PaymentCompletedPayload").exists())
                .andExpect(jsonPath("$.components.schemas.PaymentFailedPayload").exists())
                .andExpect(jsonPath("$.components.schemas.OrderCancelledPayload").exists())
                .andExpect(jsonPath("$.components.schemas.OrderShippedPayload").exists());
    }

    @Test
    void exposesSwaggerUiWhenDocumentationIsEnabled() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
