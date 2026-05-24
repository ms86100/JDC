package com.jira.workflow.validation;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Represents a validation warning that does not block the transition but provides information.
 */
@Data
@Builder
public class ValidationWarning {

    private String fieldName;

    private String warningMessage;

    private String warningType;

    private UUID validatorId;

    /**
     * Creates a warning for a field.
     */
    public static ValidationWarning fieldWarning(UUID validatorId, String fieldName, String warningType, String message) {
        return ValidationWarning.builder()
                .validatorId(validatorId)
                .fieldName(fieldName)
                .warningType(warningType)
                .warningMessage(message)
                .build();
    }

    /**
     * Creates a general warning not tied to a specific field.
     */
    public static ValidationWarning generalWarning(UUID validatorId, String warningType, String message) {
        return ValidationWarning.builder()
                .validatorId(validatorId)
                .warningType(warningType)
                .warningMessage(message)
                .build();
    }
}