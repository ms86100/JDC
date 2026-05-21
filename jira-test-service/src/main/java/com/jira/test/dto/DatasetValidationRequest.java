package com.jira.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetValidationRequest {

    @NotNull(message = "Dataset ID is required")
    private UUID datasetId;

    private UUID projectId;

    // Validation rules to apply
    private List<ValidationRule> rules;

    // Whether to apply all default rules
    private Boolean applyDefaults = true;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationRule {
        private String fieldName;
        private String ruleType; // REQUIRED, TYPE, RANGE, PATTERN, UNIQUE, CUSTOM
        private String dataType; // STRING, NUMBER, BOOLEAN, DATE, EMAIL, URL
        private Object minValue;
        private Object maxValue;
        private String pattern; // Regex pattern
        private Boolean allowNull;
        private String customRule; // Custom validation expression
    }
}