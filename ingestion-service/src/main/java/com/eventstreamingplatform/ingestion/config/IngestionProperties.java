package com.eventstreamingplatform.ingestion.config;

import java.time.Duration;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(
        @NotNull @Valid Kafka kafka,
        @NotNull @Valid Cors cors
) {

    public record Kafka(
            @NotBlank String topic,
            @NotNull Duration publishTimeout) {

        public Kafka {
            if (publishTimeout != null
                    && (publishTimeout.isZero() || publishTimeout.isNegative())) {
                throw new IllegalArgumentException("publishTimeout must be greater than zero");
            }
        }
    }

    public record Cors(@NotEmpty List<@NotBlank String> allowedOrigins) {

        public Cors {
            allowedOrigins = List.copyOf(allowedOrigins);
        }
    }
}
