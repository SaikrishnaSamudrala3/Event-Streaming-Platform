package com.eventstreamingplatform.processing.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "processing")
public record ProcessingProperties(@NotNull @Valid Kafka kafka) {

    public record Kafka(
            @NotBlank String topic,
            @NotBlank String consumerGroup,
            @Min(1) @Max(32) int concurrency) {
    }
}
