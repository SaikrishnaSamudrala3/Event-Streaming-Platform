package com.eventstreamingplatform.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.eventstreamingplatform.ingestion.config.IngestionProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class IngestionServiceApplicationTest {

    private final IngestionProperties properties;

    @Autowired
    IngestionServiceApplicationTest(IngestionProperties properties) {
        this.properties = properties;
    }

    @Test
    void contextLoads() {
        assertThat(properties.kafka().topic()).isEqualTo("test.order.events.v1");
        assertThat(properties.cors().allowedOrigins())
                .containsExactly("http://test.example");
    }
}
