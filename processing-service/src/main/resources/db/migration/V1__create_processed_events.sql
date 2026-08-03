CREATE TABLE processed_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    event_version INT NOT NULL,
    order_id VARCHAR(100) NOT NULL,
    source VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    processing_status VARCHAR(20) NOT NULL,
    payload JSON NOT NULL,
    failure_category VARCHAR(100) NULL,
    failure_message VARCHAR(500) NULL,
    failed_at DATETIME(6) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    kafka_topic VARCHAR(249) NOT NULL,
    kafka_partition INT NOT NULL,
    kafka_offset BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT pk_processed_events PRIMARY KEY (id),
    CONSTRAINT uq_processed_events_event_id UNIQUE (event_id),
    CONSTRAINT uq_processed_events_kafka_position
        UNIQUE (kafka_topic, kafka_partition, kafka_offset),
    CONSTRAINT chk_processed_events_version CHECK (event_version > 0),
    CONSTRAINT chk_processed_events_status CHECK (
        processing_status IN ('RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED', 'DEAD_LETTERED')
    ),
    CONSTRAINT chk_processed_events_retry_count CHECK (retry_count >= 0),
    CONSTRAINT chk_processed_events_kafka_partition CHECK (kafka_partition >= 0),
    CONSTRAINT chk_processed_events_kafka_offset CHECK (kafka_offset >= 0),

    INDEX idx_processed_events_order_received (order_id, received_at),
    INDEX idx_processed_events_type_received (event_type, received_at),
    INDEX idx_processed_events_status_received (processing_status, received_at),
    INDEX idx_processed_events_received_at (received_at)
) ENGINE = InnoDB;
