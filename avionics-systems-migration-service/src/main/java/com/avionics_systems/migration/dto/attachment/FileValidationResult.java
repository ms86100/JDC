package com.avionics_systems.migration.dto.attachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of file validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileValidationResult {

    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private String detectedMimeType;
    private boolean suspicious;
    private String suggestedMimeType;

    public static FileValidationResult success() {
        return FileValidationResult.builder()
                .valid(true)
                .errors(List.of())
                .warnings(List.of())
                .suspicious(false)
                .build();
    }

    public static FileValidationResult error(String errorMessage) {
        return FileValidationResult.builder()
                .valid(false)
                .errors(List.of(errorMessage))
                .warnings(List.of())
                .suspicious(false)
                .build();
    }

    public static FileValidationResult withErrors(List<String> errors) {
        return FileValidationResult.builder()
                .valid(false)
                .errors(errors)
                .warnings(List.of())
                .suspicious(false)
                .build();
    }

    public static FileValidationResult withWarnings(List<String> warnings) {
        return FileValidationResult.builder()
                .valid(true)
                .errors(List.of())
                .warnings(warnings)
                .suspicious(false)
                .build();
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}