package com.jira.workflow.validation;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Result of workflow transition validation.
 * Supports two validation modes: failFast (stop on first error) and collectAll (collect all errors).
 */
@Data
@Builder
public class ValidationResult {

    @Builder.Default
    private boolean valid = true;

    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();

    @Builder.Default
    private List<ValidationWarning> warnings = new ArrayList<>();

    /**
     * Creates an empty valid result.
     */
    public static ValidationResult success() {
        return ValidationResult.builder()
                .valid(true)
                .errors(Collections.emptyList())
                .warnings(Collections.emptyList())
                .build();
    }

    /**
     * Creates a result with a single error.
     */
    public static ValidationResult error(ValidationError validationError) {
        return ValidationResult.builder()
                .valid(false)
                .errors(validationError != null ? List.of(validationError) : Collections.emptyList())
                .warnings(Collections.emptyList())
                .build();
    }

    /**
     * Creates a result with errors and optional warnings.
     */
    public static ValidationResult of(List<ValidationError> errors, List<ValidationWarning> warnings) {
        return ValidationResult.builder()
                .valid(errors == null || errors.isEmpty())
                .errors(errors != null ? errors : Collections.emptyList())
                .warnings(warnings != null ? warnings : Collections.emptyList())
                .build();
    }

    /**
     * Adds an error to this result (mutates in place, returns this for chaining).
     */
    public ValidationResult withError(ValidationError error) {
        if (error != null) {
            if (this.errors == null) {
                this.errors = new ArrayList<>();
            }
            this.errors.add(error);
            this.valid = false;
        }
        return this;
    }

    /**
     * Adds a warning to this result.
     */
    public ValidationResult withWarning(ValidationWarning warning) {
        if (warning != null) {
            if (this.warnings == null) {
                this.warnings = new ArrayList<>();
            }
            this.warnings.add(warning);
        }
        return this;
    }

    /**
     * Merges another ValidationResult into this one.
     */
    public ValidationResult merge(ValidationResult other) {
        if (other == null) {
            return this;
        }
        if (other.errors != null) {
            if (this.errors == null) {
                this.errors = new ArrayList<>();
            }
            this.errors.addAll(other.errors);
        }
        if (other.warnings != null) {
            if (this.warnings == null) {
                this.warnings = new ArrayList<>();
            }
            this.warnings.addAll(other.warnings);
        }
        this.valid = this.valid && (this.errors == null || this.errors.isEmpty());
        return this;
    }

    /**
     * Fail-fast mode: returns an Optional containing the first error, empty if no errors.
     * This is a terminal operation that should be used when you want to stop on first error.
     */
    public Optional<ValidationError> failFast() {
        if (errors != null && !errors.isEmpty()) {
            return Optional.of(errors.get(0));
        }
        return Optional.empty();
    }

    /**
     * Collect-all mode: returns all errors collected.
     * This should be used when you want to gather all validation errors before reporting.
     */
    public List<ValidationError> allCollect() {
        return errors != null ? new ArrayList<>(errors) : Collections.emptyList();
    }

    /**
     * Returns true if this result has no errors.
     */
    public boolean isValid() {
        return valid && (errors == null || errors.isEmpty());
    }

    /**
     * Returns true if there are any errors.
     */
    public boolean hasErrors() {
        return !isValid();
    }

    /**
     * Returns true if there are any warnings.
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    /**
     * Gets the first error message, if any.
     */
    public Optional<String> getFirstErrorMessage() {
        return errors != null && !errors.isEmpty()
                ? Optional.of(errors.get(0).getErrorMessage())
                : Optional.empty();
    }

    /**
     * Gets all error messages as a single concatenated string.
     */
    public String getAllErrorMessages() {
        if (errors == null || errors.isEmpty()) {
            return "";
        }
        return errors.stream()
                .map(ValidationError::getErrorMessage)
                .collect(Collectors.joining("; "));
    }

    /**
     * Builder for building validation results in stages.
     */
    public static class ValidationResultBuilder {
        private boolean valid = true;
        private List<ValidationError> errors = new ArrayList<>();
        private List<ValidationWarning> warnings = new ArrayList<>();
    }
}