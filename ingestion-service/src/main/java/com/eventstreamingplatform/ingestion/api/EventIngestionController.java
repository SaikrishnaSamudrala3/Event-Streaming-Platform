package com.eventstreamingplatform.ingestion.api;

import com.eventstreamingplatform.ingestion.service.EventSubmissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Ingestion")
public class EventIngestionController {

    private final EventSubmissionService submissionService;

    public EventIngestionController(EventSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    @Operation(
            summary = "Submit an order event",
            description = "Validates an event and waits for Kafka acknowledgment. "
                    + "HTTP 202 does not mean downstream processing completed.",
            operationId = "submitOrderEvent")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Kafka acknowledged the event",
                    content = @Content(schema = @Schema(implementation = EventAcceptedResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed request or validation failure",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(
                    responseCode = "503",
                    description = "Kafka publication failed or timed out",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<EventAcceptedResponse> submitEvent(
            @Valid @RequestBody CreateOrderEventRequest request,
            @RequestHeader(name = "X-Correlation-ID", required = false)
            @Parameter(description = "Client correlation identifier; generated when absent")
            @Size(min = 1, max = 100)
            @Pattern(regexp = ".*\\S.*", message = "must contain non-whitespace text")
            String correlationId) {

        return ResponseEntity.accepted()
                .body(submissionService.submit(request, correlationId));
    }
}
