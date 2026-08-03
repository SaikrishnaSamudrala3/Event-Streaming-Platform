package com.eventstreamingplatform.processing.service;

import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.processing.persistence.KafkaRecordMetadata;
import com.eventstreamingplatform.processing.persistence.ProcessedEventEntity;
import com.eventstreamingplatform.processing.persistence.ProcessedEventMapper;
import com.eventstreamingplatform.processing.persistence.ProcessedEventRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventProcessingService {

    private final ProcessedEventRepository repository;
    private final ProcessedEventMapper mapper;

    public EventProcessingService(
            ProcessedEventRepository repository,
            ProcessedEventMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public ProcessingOutcome process(
            OrderEvent<?> event,
            KafkaRecordMetadata metadata) {

        if (repository.existsByEventId(event.eventId())
                || repository.existsByKafkaTopicAndKafkaPartitionAndKafkaOffset(
                        metadata.topic(), metadata.partition(), metadata.offset())) {
            return ProcessingOutcome.DUPLICATE;
        }

        ProcessedEventEntity entity = mapper.toProcessedEntity(event, metadata);
        repository.save(entity);

        return ProcessingOutcome.PROCESSED;
    }
}
