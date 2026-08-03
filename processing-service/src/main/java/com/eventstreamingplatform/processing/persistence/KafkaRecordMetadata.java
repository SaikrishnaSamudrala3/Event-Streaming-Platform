package com.eventstreamingplatform.processing.persistence;

import java.time.Instant;

public record KafkaRecordMetadata(
        String topic,
        int partition,
        long offset,
        Instant receivedAt) {

    public KafkaRecordMetadata {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic must contain text");
        }
        if (topic.length() > 249) {
            throw new IllegalArgumentException("topic must not exceed 249 characters");
        }
        if (partition < 0) {
            throw new IllegalArgumentException("partition must not be negative");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (receivedAt == null) {
            throw new IllegalArgumentException("receivedAt must not be null");
        }
    }
}
