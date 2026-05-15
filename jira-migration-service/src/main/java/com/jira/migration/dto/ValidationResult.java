package com.jira.migration.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private boolean valid;
    private List<ValidationError> errors;
    private List<ValidationWarning> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String errorCode;
        private String message;
        private Integer row;
        private Object invalidValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationWarning {
        private String field;
        private String warningCode;
        private String message;
        private Integer row;
    }

    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .errors(List.of())
                .warnings(List.of())
                .build();
    }

    public static ValidationResult withErrors(List<ValidationError> errors) {
        return ValidationResult.builder()
                .valid(false)
                .errors(errors)
                .warnings(List.of())
                .build();
    }
}