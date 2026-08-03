package com.eventstreamingplatform.processing.messaging;

import java.time.Clock;

import com.eventstreamingplatform.events.OrderEvent;
import com.eventstreamingplatform.processing.persistence.KafkaRecordMetadata;
import com.eventstreamingplatform.processing.service.EventProcessingService;
import com.eventstreamingplatform.processing.service.ProcessingOutcome;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventListener.class);

    private final EventProcessingService processingService;
    private final Clock clock;

    public OrderEventListener(EventProcessingService processingService, Clock clock) {
        this.processingService = processingService;
        this.clock = clock;
    }

    @KafkaListener(
            topics = "${processing.kafka.topic}",
            groupId = "${processing.kafka.consumer-group}")
    public void consume(
            ConsumerRecord<String, OrderEvent<?>> record,
            Acknowledgment acknowledgment) {

        var metadata = new KafkaRecordMetadata(
                record.topic(),
                record.partition(),
                record.offset(),
                clock.instant());

        ProcessingOutcome outcome = processingService.process(record.value(), metadata);
        acknowledgment.acknowledge();

        LOGGER.info(
                "Event consumption completed: eventId={}, outcome={}, topic={}, partition={}, offset={}",
                record.value().eventId(),
                outcome,
                record.topic(),
                record.partition(),
                record.offset());
    }
}
