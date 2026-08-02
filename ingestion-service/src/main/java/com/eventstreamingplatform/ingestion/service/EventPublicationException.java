package com.eventstreamingplatform.ingestion.service;

public class EventPublicationException extends RuntimeException {

    public EventPublicationException(String message) {
        super(message);
    }

    public EventPublicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
