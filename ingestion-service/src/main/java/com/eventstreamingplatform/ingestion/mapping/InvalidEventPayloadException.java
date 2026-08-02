package com.eventstreamingplatform.ingestion.mapping;

public class InvalidEventPayloadException extends IllegalArgumentException {

    public InvalidEventPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
