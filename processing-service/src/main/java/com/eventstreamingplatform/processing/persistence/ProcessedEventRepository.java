package com.eventstreamingplatform.processing.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long> {

    Optional<ProcessedEventEntity> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    boolean existsByKafkaTopicAndKafkaPartitionAndKafkaOffset(
            String kafkaTopic,
            int kafkaPartition,
            long kafkaOffset);
}
