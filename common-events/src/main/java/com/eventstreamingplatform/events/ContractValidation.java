package com.eventstreamingplatform.events;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;
import java.util.regex.Pattern;

final class ContractValidation {

    private static final Pattern IDENTIFIER_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]*$");

    private ContractValidation() {
    }

    static <T> T requireNonNull(T value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " is required");
    }

    static String requireText(String value, String fieldName, int maximumLength) {
        requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maximumLength + " characters");
        }
        return value;
    }

    static String requireIdentifier(String value, String fieldName) {
        requireText(value, fieldName, 100);
        if (!IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " may contain only letters, numbers, dots, underscores, and hyphens");
        }
        return value;
    }

    static String requireCurrency(String value) {
        requireText(value, "currency", 3);
        if (!value.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "currency must be a three-letter uppercase ISO 4217 code");
        }
        try {
            Currency.getInstance(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "currency must be a recognized ISO 4217 code", exception);
        }
        return value;
    }

    static BigDecimal requirePositiveAmount(BigDecimal value, String fieldName) {
        requireNonNull(value, fieldName);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        if (value.scale() > 2) {
            throw new IllegalArgumentException(
                    fieldName + " must not have more than two decimal places");
        }
        return value;
    }
}
