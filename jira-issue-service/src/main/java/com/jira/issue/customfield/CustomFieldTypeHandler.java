package com.jira.issue.customfield;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for custom field type handlers.
 * F7-US001: Custom Field Types
 *
 * Implement this interface to add support for new custom field types.
 * Each handler is responsible for:
 * - Validating field values
 * - Rendering field values for display
 * - Parsing input values
 * - Converting between storage and display formats
 */
public interface CustomFieldTypeHandler {

    /**
     * Get the unique type identifier for this handler
     * @return type identifier (e.g., "text", "number", "date", "cascadingselect")
     */
    String getType();

    /**
     * Get the display name for this field type
     * @return human-readable type name
     */
    String getDisplayName();

    /**
     * Validate a value for this custom field type
     * @param value the value to validate
     * @param config field configuration options
     * @return ValidationResult indicating success or failure with message
     */
    ValidationResult validate(Object value, Map<String, Object> config);

    /**
     * Render a value for display in issue view
     * @param value the stored value
     * @param config field configuration options
     * @return rendered HTML/string representation
     */
    String renderForDisplay(Object value, Map<String, Object> config);

    /**
     * Render a value for editing in issue edit form
     * @param value the current value
     * @param config field configuration options
     * @return rendered HTML/input representation
     */
    String renderForEdit(Object value, Map<String, Object> config);

    /**
     * Parse an input value from form submission
     * @param input the raw input string or object
     * @param config field configuration options
     * @return parsed value in storage format
     */
    Object parseInput(Object input, Map<String, Object> config);

    /**
     * Convert a stored value to JSON-serializable format for API responses
     * @param value the stored value
     * @param config field configuration options
     * @return JSON-compatible representation
     */
    Object toJsonValue(Object value, Map<String, Object> config);

    /**
     * Convert from JSON value (API input) to storage format
     * @param jsonValue the value from API
     * @param config field configuration options
     * @return value in storage format
     */
    Object fromJsonValue(Object jsonValue, Map<String, Object> config);

    /**
     * Check if this handler supports the given type
     * @param type the field type identifier
     * @return true if this handler can process the type
     */
    default boolean supports(String type) {
        return getType().equalsIgnoreCase(type);
    }

    /**
     * Get searchable representation of the value
     * @param value the stored value
     * @param config field configuration options
     * @return searchable text representation
     */
    default String toSearchableText(Object value, Map<String, Object> config) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    /**
     * Result of field validation
     */
    record ValidationResult(boolean valid, String message, Object sanitizedValue) {
        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult success(Object sanitizedValue) {
            return new ValidationResult(true, null, sanitizedValue);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message, null);
        }
    }
}