package com.eventstreamingplatform.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.sql.DataSource;

import com.eventstreamingplatform.processing.config.ProcessingProperties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ProcessingConfigurationTest {

    private final ProcessingProperties processingProperties;
    private final KafkaProperties kafkaProperties;
    private final DataSource dataSource;

    @Autowired
    ProcessingConfigurationTest(
            ProcessingProperties processingProperties,
            KafkaProperties kafkaProperties,
            DataSource dataSource) {
        this.processingProperties = processingProperties;
        this.kafkaProperties = kafkaProperties;
        this.dataSource = dataSource;
    }

    @Test
    void bindsDeterministicTestConfiguration() throws Exception {
        assertThat(processingProperties.kafka().topic()).isEqualTo("test.order.events.v1");
        assertThat(processingProperties.kafka().consumerGroup()).isEqualTo("test-order-processing");
        assertThat(processingProperties.kafka().concurrency()).isEqualTo(1);

        try (var connection = dataSource.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
        }
    }

    @Test
    void disablesAutoCommitAndConfiguresTypedOrderEventDeserialization() {
        var consumerProperties = kafkaProperties.buildConsumerProperties();

        assertThat(consumerProperties)
                .containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("localhost:19092"))
                .containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "test-order-processing")
                .containsEntry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
                .containsEntry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .containsEntry(
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        org.apache.kafka.common.serialization.StringDeserializer.class)
                .containsEntry(
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        JacksonJsonDeserializer.class)
                .containsEntry(
                        "spring.json.value.default.type",
                        "com.eventstreamingplatform.events.OrderEvent")
                .containsEntry("spring.json.use.type.headers", "false");

        assertThat(kafkaProperties.getListener().getAckMode())
                .isEqualTo(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        assertThat(kafkaProperties.getListener().getConcurrency()).isEqualTo(1);
    }
}
