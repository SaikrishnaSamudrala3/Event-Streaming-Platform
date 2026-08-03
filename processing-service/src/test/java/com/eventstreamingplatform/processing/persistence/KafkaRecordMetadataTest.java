package com.eventstreamingplatform.processing.persistence;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class KafkaRecordMetadataTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-08-01T12:00:01Z");

    @Test
    void rejectsMissingOrOversizedTopicNames() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new KafkaRecordMetadata(" ", 0, 0, RECEIVED_AT));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new KafkaRecordMetadata("a".repeat(250), 0, 0, RECEIVED_AT));
    }

    @Test
    void rejectsNegativeKafkaCoordinates() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new KafkaRecordMetadata("order.events.v1", -1, 0, RECEIVED_AT));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new KafkaRecordMetadata("order.events.v1", 0, -1, RECEIVED_AT));
    }

    @Test
    void rejectsMissingReceiptTime() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new KafkaRecordMetadata("order.events.v1", 0, 0, null));
    }
}
