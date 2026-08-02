package com.eventstreamingplatform.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.eventstreamingplatform.ingestion.config.IngestionProperties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class LocalProfileConfigurationTest {

    private final IngestionProperties properties;
    private final KafkaProperties kafkaProperties;

    @Autowired
    LocalProfileConfigurationTest(
            IngestionProperties properties,
            KafkaProperties kafkaProperties) {
        this.properties = properties;
        this.kafkaProperties = kafkaProperties;
    }

    @Test
    void usesSafeLocalDefaults() {
        assertThat(properties.kafka().topic()).isEqualTo("order.events.v1");
        assertThat(properties.cors().allowedOrigins())
                .containsExactly("http://localhost:3000", "http://localhost:5173");
    }

    @Test
    void configuresAKeyedIdempotentLocalProducer() {
        var producerProperties = kafkaProperties.buildProducerProperties();

        assertThat(producerProperties)
                .containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("localhost:9092"))
                .containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
                .containsEntry(ProducerConfig.ACKS_CONFIG, "all")
                .containsEntry(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
                .containsEntry(
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                        org.apache.kafka.common.serialization.StringSerializer.class)
                .containsEntry(
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                        JacksonJsonSerializer.class)
                .containsEntry("spring.json.add.type.headers", "false");
    }
}
