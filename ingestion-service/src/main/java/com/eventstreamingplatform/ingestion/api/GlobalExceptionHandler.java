package com.eventstreamingplatform.ingestion.api;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.eventstreamingplatform.ingestion.mapping.InvalidEventPayloadException;
import com.eventstreamingplatform.ingestion.service.EventPublicationException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI VALIDATION_ERROR_TYPE = URI.create("urn:problem:validation-error");
    private static final URI MALFORMED_JSON_TYPE = URI.create("urn:problem:malformed-json");
    private static final URI INVALID_PAYLOAD_TYPE = URI.create("urn:problem:invalid-event-payload");
    private static final URI SERVICE_UNAVAILABLE_TYPE = URI.create("urn:problem:service-unavailable");
    private static final URI RESOURCE_NOT_FOUND_TYPE = URI.create("urn:problem:resource-not-found");
    private static final URI INTERNAL_ERROR_TYPE = URI.create("urn:problem:internal-error");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleRequestValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        List<FieldValidationError> validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldValidationError::field))
                .toList();

        return problem(
                HttpStatus.BAD_REQUEST,
                VALIDATION_ERROR_TYPE,
                "Request validation failed",
                "One or more request fields are invalid",
                request,
                validationErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintValidation(
            ConstraintViolationException exception,
            HttpServletRequest request) {

        List<FieldValidationError> validationErrors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new FieldValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .sorted(Comparator.comparing(FieldValidationError::field))
                .toList();

        return problem(
                HttpStatus.BAD_REQUEST,
                VALIDATION_ERROR_TYPE,
                "Request validation failed",
                "One or more request values are invalid",
                request,
                validationErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleMalformedJson(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.BAD_REQUEST,
                MALFORMED_JSON_TYPE,
                "Malformed JSON request",
                "The request body is missing or contains invalid JSON",
                request,
                null);
    }

    @ExceptionHandler(InvalidEventPayloadException.class)
    ResponseEntity<ApiError> handleInvalidPayload(
            InvalidEventPayloadException exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.BAD_REQUEST,
                INVALID_PAYLOAD_TYPE,
                "Invalid event payload",
                exception.getMessage(),
                request,
                null);
    }

    @ExceptionHandler(EventPublicationException.class)
    ResponseEntity<ApiError> handlePublicationFailure(
            EventPublicationException exception,
            HttpServletRequest request) {

        LOGGER.warn("Event publication failed for path {}", request.getRequestURI(), exception);

        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                SERVICE_UNAVAILABLE_TYPE,
                "Event ingestion unavailable",
                "The event could not be acknowledged by Kafka",
                request,
                null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleResourceNotFound(
            NoResourceFoundException exception,
            HttpServletRequest request) {

        return problem(
                HttpStatus.NOT_FOUND,
                RESOURCE_NOT_FOUND_TYPE,
                "Resource not found",
                "The requested resource does not exist",
                request,
                null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpectedError(
            Exception exception,
            HttpServletRequest request) {

        LOGGER.error("Unexpected request failure for path {}", request.getRequestURI(), exception);

        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_TYPE,
                "Internal server error",
                "An unexpected error occurred",
                request,
                null);
    }

    private ResponseEntity<ApiError> problem(
            HttpStatus status,
            URI type,
            String title,
            String detail,
            HttpServletRequest request,
            List<FieldValidationError> validationErrors) {

        String correlationId = safeCorrelationId(request.getHeader("X-Correlation-ID"));
        ApiError error = new ApiError(
                type,
                title,
                status.value(),
                detail,
                Instant.now(),
                request.getRequestURI(),
                correlationId,
                validationErrors);

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(error);
    }

    private String safeCorrelationId(String correlationId) {
        if (correlationId == null
                || correlationId.isBlank()
                || correlationId.length() > 100) {
            return null;
        }
        return correlationId;
    }
}
