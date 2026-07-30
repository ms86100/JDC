package com.avionics_systems.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldValidationResult {

    private UUID fieldId;
    private String fieldName;
    private boolean valid;
    private String errorMessage;

    public static FieldValidationResult success(UUID fieldId, String fieldName) {
        return FieldValidationResult.builder()
                .fieldId(fieldId)
                .fieldName(fieldName)
                .valid(true)
                .build();
    }

    public static FieldValidationResult failure(UUID fieldId, String fieldName, String errorMessage) {
        return FieldValidationResult.builder()
                .fieldId(fieldId)
                .fieldName(fieldName)
                .valid(false)
                .errorMessage(errorMessage)
                .build();
    }
}