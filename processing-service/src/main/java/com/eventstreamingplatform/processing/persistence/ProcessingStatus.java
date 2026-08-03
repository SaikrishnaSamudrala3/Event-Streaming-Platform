package com.eventstreamingplatform.processing.persistence;

public enum ProcessingStatus {
    RECEIVED,
    PROCESSING,
    PROCESSED,
    FAILED,
    DEAD_LETTERED
}
