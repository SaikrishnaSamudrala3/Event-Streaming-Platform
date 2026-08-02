package com.eventstreamingplatform.ingestion.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class UuidGenerator {

    public UUID generate() {
        return UUID.randomUUID();
    }
}
