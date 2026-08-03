package com.eventstreamingplatform.processing.persistence;

import java.time.Instant;
import java.util.UUID;

import com.eventstreamingplatform.events.OrderEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "event_id", nullable = false, length = 36, unique = true)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private OrderEventType eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant receivedAt;

    @Column(name = "processed_at", columnDefinition = "datetime(6)")
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus;

    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;

    @Column(name = "failure_category", length = 100)
    private String failureCategory;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "failed_at", columnDefinition = "datetime(6)")
    private Instant failedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "kafka_topic", nullable = false, length = 249)
    private String kafkaTopic;

    @Column(name = "kafka_partition", nullable = false)
    private int kafkaPartition;

    @Column(name = "kafka_offset", nullable = false)
    private long kafkaOffset;

    protected ProcessedEventEntity() {
    }

    public ProcessedEventEntity(
            UUID eventId,
            OrderEventType eventType,
            int eventVersion,
            String orderId,
            String source,
            String correlationId,
            Instant occurredAt,
            Instant receivedAt,
            Instant processedAt,
            ProcessingStatus processingStatus,
            String payload,
            String kafkaTopic,
            int kafkaPartition,
            long kafkaOffset) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.orderId = orderId;
        this.source = source;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
        this.processingStatus = processingStatus;
        this.payload = payload;
        this.retryCount = 0;
        this.kafkaTopic = kafkaTopic;
        this.kafkaPartition = kafkaPartition;
        this.kafkaOffset = kafkaOffset;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public OrderEventType getEventType() {
        return eventType;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSource() {
        return source;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public String getPayload() {
        return payload;
    }

    public String getFailureCategory() {
        return failureCategory;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public int getKafkaPartition() {
        return kafkaPartition;
    }

    public long getKafkaOffset() {
        return kafkaOffset;
    }
}
