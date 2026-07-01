package com.jira.workflow.validation;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Represents a validation error that occurred during workflow transition validation.
 * Matches Jira DC's validation error format.
 */
@Data
@Builder
public class ValidationError {

    private String fieldName;

    private String errorMessage;

    private String validatorType;

    private UUID validatorId;

    /**
     * Creates a ValidationError for a field validation failure.
     */
    public static ValidationError fieldError(UUID validatorId, String fieldName, String validatorType, String errorMessage) {
        return ValidationError.builder()
                .validatorId(validatorId)
                .fieldName(fieldName)
                .validatorType(validatorType)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Creates a ValidationError for a general validation failure (no specific field).
     */
    public static ValidationError generalError(UUID validatorId, String validatorType, String errorMessage) {
        return ValidationError.builder()
                .validatorId(validatorId)
                .validatorType(validatorType)
                .errorMessage(errorMessage)
                .build();
    }
}