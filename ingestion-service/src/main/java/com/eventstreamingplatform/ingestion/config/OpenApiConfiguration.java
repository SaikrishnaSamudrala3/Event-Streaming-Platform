package com.eventstreamingplatform.ingestion.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.tags.Tag;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI ingestionOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Real-Time Event Streaming Platform API")
                        .version("0.1.0")
                        .description(
                                "Accepts order events for acknowledged publication to Kafka. "
                                        + "Acceptance does not mean downstream processing completed.")
                        .contact(new Contact().name("Project Maintainer")))
                .addTagsItem(new Tag()
                        .name("Ingestion")
                        .description("Submit events for asynchronous downstream processing."));
    }

    @Bean
    OpenApiCustomizer orderPayloadSchemaCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }

            openApi.getComponents()
                .addSchemas("OrderCreatedPayload", orderCreatedPayload())
                .addSchemas("PaymentCompletedPayload", paymentCompletedPayload())
                .addSchemas("PaymentFailedPayload", paymentFailedPayload())
                .addSchemas("OrderCancelledPayload", orderCancelledPayload())
                .addSchemas("OrderShippedPayload", orderShippedPayload());
        };
    }

    private Schema<?> orderCreatedPayload() {
        return new ObjectSchema()
                .addProperty("customerReference", safeIdentifier())
                .addProperty("currency", currencyCode())
                .addProperty("totalAmount", positiveMoney())
                .addProperty("itemCount", new IntegerSchema().minimum(java.math.BigDecimal.ONE))
                .required(java.util.List.of(
                        "customerReference", "currency", "totalAmount", "itemCount"));
    }

    private Schema<?> paymentCompletedPayload() {
        return new ObjectSchema()
                .addProperty("paymentReference", safeIdentifier())
                .addProperty("amount", positiveMoney())
                .addProperty("currency", currencyCode())
                .addProperty("completedAt", new DateTimeSchema())
                .required(java.util.List.of(
                        "paymentReference", "amount", "currency", "completedAt"));
    }

    private Schema<?> paymentFailedPayload() {
        return new ObjectSchema()
                .addProperty("paymentReference", safeIdentifier())
                .addProperty("failureReason", new StringSchema()._enum(java.util.List.of(
                        "DECLINED",
                        "INSUFFICIENT_FUNDS",
                        "EXPIRED_PAYMENT_METHOD",
                        "PROCESSOR_UNAVAILABLE",
                        "VALIDATION_ERROR",
                        "UNKNOWN")))
                .addProperty("failedAt", new DateTimeSchema())
                .required(java.util.List.of("paymentReference", "failureReason", "failedAt"));
    }

    private Schema<?> orderCancelledPayload() {
        return new ObjectSchema()
                .addProperty("reason", new StringSchema().minLength(1).maxLength(250))
                .addProperty("cancelledAt", new DateTimeSchema())
                .required(java.util.List.of("reason", "cancelledAt"));
    }

    private Schema<?> orderShippedPayload() {
        return new ObjectSchema()
                .addProperty("shipmentReference", safeIdentifier())
                .addProperty("carrier", new StringSchema().minLength(1).maxLength(100))
                .addProperty("shippedAt", new DateTimeSchema())
                .required(java.util.List.of("shipmentReference", "carrier", "shippedAt"));
    }

    private Schema<?> safeIdentifier() {
        return new StringSchema()
                .minLength(1)
                .maxLength(100)
                .pattern("^[A-Za-z0-9][A-Za-z0-9._-]*$");
    }

    private Schema<?> currencyCode() {
        return new StringSchema().pattern("^[A-Z]{3}$").example("USD");
    }

    private Schema<?> positiveMoney() {
        return new NumberSchema()
                .minimum(java.math.BigDecimal.ZERO)
                .exclusiveMinimum(true);
    }
}
