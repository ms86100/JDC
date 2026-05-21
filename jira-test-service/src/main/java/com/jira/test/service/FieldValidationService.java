package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.entity.CustomField;
import com.jira.test.exception.ValidationException;
import com.jira.test.repository.CustomFieldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for validating field values against type-specific and custom rules.
 * Supports batch validation, custom messages, and validation severity levels.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FieldValidationService {

    private final CustomFieldRepository customFieldRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-()]{10,20}$");

    @Transactional(readOnly = true)
    public FieldValidationResponse validateField(UUID fieldId, String value) {
        CustomField field = customFieldRepository.findById(fieldId)
                .orElseThrow(() -> new ValidationException("Field not found: " + fieldId));

        return validateFieldValue(field, value);
    }

    @Transactional(readOnly = true)
    public FieldValidationResponse validateFieldValue(CustomField field, String value) {
        List<ValidationMessage> messages = new ArrayList<>();
        boolean isValid = true;

        // Check required
        if (field.getIsRequired() != null && field.getIsRequired() && (value == null || value.trim().isEmpty())) {
            messages.add(new ValidationMessage(
                    ValidationSeverity.ERROR,
                    "required",
                    "This field is required"
            ));
            isValid = false;
        }

        // Skip further validation if empty and not required
        if (value == null || value.trim().isEmpty()) {
            return new FieldValidationResponse(field.getId(), field.getName(), isValid, messages);
        }

        // Type-specific validation
        ValidationMessage typeValidation = validateByType(field, value);
        if (typeValidation != null) {
            if (!typeValidation.isPassed()) {
                isValid = false;
            }
            messages.add(typeValidation);
        }

        // Custom validation rules
        if (field.getValidationRules() != null && !field.getValidationRules().isEmpty()) {
            List<ValidationMessage> ruleMessages = validateWithRules(field, value);
            for (ValidationMessage msg : ruleMessages) {
                if (!msg.isPassed()) {
                    isValid = false;
                }
                messages.add(msg);
            }
        }

        return new FieldValidationResponse(field.getId(), field.getName(), isValid, messages);
    }

    @Transactional(readOnly = true)
    public BatchValidationResponse validateBatch(Map<UUID, String> fieldValues) {
        Map<UUID, FieldValidationResponse> results = new LinkedHashMap<>();
        int errorCount = 0;
        int warningCount = 0;

        for (Map.Entry<UUID, String> entry : fieldValues.entrySet()) {
            FieldValidationResponse response = validateField(entry.getKey(), entry.getValue());
            results.put(entry.getKey(), response);

            for (ValidationMessage msg : response.getMessages()) {
                if (msg.getSeverity() == ValidationSeverity.ERROR) {
                    errorCount++;
                } else if (msg.getSeverity() == ValidationSeverity.WARNING) {
                    warningCount++;
                }
            }
        }

        boolean allValid = errorCount == 0;

        return new BatchValidationResponse(allValid, results, errorCount, warningCount);
    }

    @Transactional(readOnly = true)
    public List<ValidationMessage> validateWithRules(CustomField field, String value) {
        List<ValidationMessage> messages = new ArrayList<>();

        try {
            Map<String, Object> rules = objectMapper.readValue(
                    field.getValidationRules(), new TypeReference<Map<String, Object>>() {});

            CustomField.FieldType fieldType = field.getFieldType();

            // Min length validation
            if (fieldType == CustomField.FieldType.TEXT ||
                fieldType == CustomField.FieldType.TEXTAREA ||
                fieldType == CustomField.FieldType.LABEL) {
                Integer minLength = getIntRule(rules, "minLength");
                if (minLength != null && value.length() < minLength) {
                    messages.add(new ValidationMessage(
                            ValidationSeverity.ERROR,
                            "minLength",
                            String.format("Value must be at least %d characters", minLength)
                    ));
                }
            }

            // Max length validation
            if (fieldType == CustomField.FieldType.TEXT ||
                fieldType == CustomField.FieldType.TEXTAREA ||
                fieldType == CustomField.FieldType.LABEL ||
                fieldType == CustomField.FieldType.URL ||
                fieldType == CustomField.FieldType.EMAIL) {
                Integer maxLength = getIntRule(rules, "maxLength");
                if (maxLength != null && value.length() > maxLength) {
                    messages.add(new ValidationMessage(
                            ValidationSeverity.ERROR,
                            "maxLength",
                            String.format("Value must not exceed %d characters", maxLength)
                    ));
                }
            }

            // Regex pattern validation
            if (fieldType == CustomField.FieldType.TEXT ||
                fieldType == CustomField.FieldType.TEXTAREA) {
                String pattern = getStringRule(rules, "pattern");
                if (pattern != null) {
                    try {
                        if (!Pattern.matches(pattern, value)) {
                            String message = getStringRule(rules, "patternMessage");
                            messages.add(new ValidationMessage(
                                    ValidationSeverity.ERROR,
                                    "pattern",
                                    message != null ? message : "Value does not match the required pattern"
                            ));
                        }
                    } catch (Exception e) {
                        log.warn("Invalid regex pattern in field {}: {}", field.getId(), pattern);
                    }
                }
            }

            // Number range validation
            if (fieldType == CustomField.FieldType.NUMBER) {
                Double minValue = getDoubleRule(rules, "min");
                if (minValue != null) {
                    try {
                        double num = Double.parseDouble(value);
                        if (num < minValue) {
                            messages.add(new ValidationMessage(
                                    ValidationSeverity.ERROR,
                                    "min",
                                    String.format("Value must be at least %s", minValue)
                            ));
                        }
                    } catch (NumberFormatException e) {
                        // Will be caught by type validation
                    }
                }

                Double maxValue = getDoubleRule(rules, "max");
                if (maxValue != null) {
                    try {
                        double num = Double.parseDouble(value);
                        if (num > maxValue) {
                            messages.add(new ValidationMessage(
                                    ValidationSeverity.ERROR,
                                    "max",
                                    String.format("Value must not exceed %s", maxValue)
                            ));
                        }
                    } catch (NumberFormatException e) {
                        // Will be caught by type validation
                    }
                }

                Integer decimals = getIntRule(rules, "decimalPlaces");
                if (decimals != null) {
                    try {
                        double num = Double.parseDouble(value);
                        double factor = Math.pow(10, decimals);
                        if (Math.round(num * factor) / factor != num) {
                            messages.add(new ValidationMessage(
                                    ValidationSeverity.ERROR,
                                    "decimalPlaces",
                                    String.format("Value must have at most %d decimal places", decimals)
                            ));
                        }
                    } catch (NumberFormatException e) {
                        // Will be caught by type validation
                    }
                }
            }

            // Date range validation
            if (fieldType == CustomField.FieldType.DATE || fieldType == CustomField.FieldType.DATETIME) {
                String minDate = getStringRule(rules, "minDate");
                if (minDate != null) {
                    try {
                        LocalDate dateValue = LocalDate.parse(value.substring(0, 10));
                        LocalDate minLocalDate = LocalDate.parse(minDate);
                        if (dateValue.isBefore(minLocalDate)) {
                            messages.add(new ValidationMessage(
                                    ValidationSeverity.ERROR,
                                    "minDate",
                                    String.format("Date must be on or after %s", minDate)
                            ));
                        }
                    } catch (Exception e) {
                        log.warn("Error validating min date for field {}: {}", field.getId(), e.getMessage());
                    }
                }

                String maxDate = getStringRule(rules, "maxDate");
                if (maxDate != null) {
                    try {
                        LocalDate dateValue = LocalDate.parse(value.substring(0, 10));
                        LocalDate maxLocalDate = LocalDate.parse(maxDate);
                        if (dateValue.isAfter(maxLocalDate)) {
                            messages.add(new ValidationMessage(
                                    ValidationSeverity.ERROR,
                                    "maxDate",
                                    String.format("Date must be on or before %s", maxDate)
                            ));
                        }
                    } catch (Exception e) {
                        log.warn("Error validating max date for field {}: {}", field.getId(), e.getMessage());
                    }
                }
            }

            // Custom warning rules
            String warningRegex = getStringRule(rules, "warningPattern");
            if (warningRegex != null) {
                try {
                    if (Pattern.matches(warningRegex, value)) {
                        String warningMsg = getStringRule(rules, "warningMessage");
                        messages.add(new ValidationMessage(
                                ValidationSeverity.WARNING,
                                "warningPattern",
                                warningMsg != null ? warningMsg : "Value matched warning pattern"
                        ));
                    }
                } catch (Exception e) {
                    log.warn("Invalid warning pattern in field {}: {}", field.getId(), warningRegex);
                }
            }

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse validation rules for field {}: {}", field.getId(), e.getMessage());
            messages.add(new ValidationMessage(
                    ValidationSeverity.WARNING,
                    "parseError",
                    "Unable to parse validation rules"
            ));
        }

        return messages;
    }

    private ValidationMessage validateByType(CustomField field, String value) {
        CustomField.FieldType fieldType = field.getFieldType();

        switch (fieldType) {
            case TEXT:
            case TEXTAREA:
            case LABEL:
                if (value.length() > 10000) {
                    return new ValidationMessage(
                            ValidationSeverity.ERROR,
                            "type",
                            "Text value exceeds maximum allowed length"
                    );
                }
                return new ValidationMessage(ValidationSeverity.ERROR, "type", null, true);

            case NUMBER:
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return new ValidationMessage(
                            ValidationSeverity.ERROR,
                            "type",
                            "Value must be a valid number"
                    );
                }
                return new ValidationMessage(ValidationSeverity.ERROR, "type", null, true);

            case DATE:
                try {
                    LocalDate.parse(value);
                } catch (DateTimeParseException e) {
                    return new ValidationMessage(
                            ValidationSeverity.ERROR,
                            "type",
                            "Value must be a valid date (YYYY-MM-DD)"
                    );
                }
                return new ValidationMessage(ValidationSeverity.ERROR, "type", null, true);

            case DATETIME:
                try {
                    LocalDateTime.parse(value.replace(" ", "T"));
                } catch (DateTimeParseException e) {
                    return new ValidationMessage(
                            ValidationSeverity.ERROR,
                            "type",
                            "Value must be a valid datetime (YYYY-MM-DDTHH:mm:ss)"
                    );
                }
                return new ValidationMessage(ValidationSeverity.ERROR, "type", null, true);

            case EMAIL:
                if (!EMAIL_PATTERN.matcher(value).matches()) {
                    return new ValidationMessage(
                            ValidationSeverity.ERROR,
                            "type",
                            "Value must be a valid email address"
                    );
                }
                return new ValidationMessage(ValidationSeverity.ERROR, "type", null, true);

            case URL:
                if (!URL_PATTERN.matcher(value).matches()) {
                    return new ValidationMessage(
                            ValidationSeverity.ERROR,
                            "type",
                            "Value must be a valid URL"
                    );
                }
                return new ValidationMessage(ValidationSeverity.ERROR, "type", null, true);

            case CHECKBOX:
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    return new ValidationMessage(
                            ValidationSeverity.ERROR,
                            "type",
                            "Value must be true or false"
                    );
                }
                return new ValidationMessage(ValidationSeverity.ERROR, "type", null, true);

            case SELECT:
            case MULTI_SELECT:
            case RADIO:
                // Options validation is handled separately
                return new ValidationMessage(ValidationSeverity.ERROR, "type", null, true);

            default:
                return new ValidationMessage(ValidationSeverity.ERROR, "type", null, true);
        }
    }

    private Integer getIntRule(Map<String, Object> rules, String key) {
        Object value = rules.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Double getDoubleRule(Map<String, Object> rules, String key) {
        Object value = rules.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private String getStringRule(Map<String, Object> rules, String key) {
        Object value = rules.get(key);
        return value != null ? value.toString() : null;
    }

    // Response classes
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldValidationResponse {
        private UUID fieldId;
        private String fieldName;
        private boolean valid;
        private List<ValidationMessage> messages;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BatchValidationResponse {
        private boolean allValid;
        private Map<UUID, FieldValidationResponse> results;
        private int errorCount;
        private int warningCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ValidationMessage {
        private ValidationSeverity severity;
        private String rule;
        private String message;
        private boolean passed;

        public ValidationMessage(ValidationSeverity severity, String rule, String message) {
            this.severity = severity;
            this.rule = rule;
            this.message = message;
            this.passed = false;
        }

        public ValidationMessage(ValidationSeverity severity, String rule, String message, boolean passed) {
            this.severity = severity;
            this.rule = rule;
            this.message = message;
            this.passed = passed;
        }

        public boolean isPassed() {
            return passed;
        }

        public ValidationSeverity getSeverity() {
            return severity;
        }
    }

    public enum ValidationSeverity {
        ERROR,
        WARNING,
        INFO
    }
}