package com.eventstreamingplatform.ingestion.api;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        URI type,
        String title,
        int status,
        String detail,
        Instant timestamp,
        String path,
        String correlationId,
        List<FieldValidationError> validationErrors
) {
}
