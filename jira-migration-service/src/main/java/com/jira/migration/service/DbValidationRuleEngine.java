package com.jira.migration.service;

import com.jira.migration.dto.ValidationResult;
import com.jira.migration.entity.ValidationRule;
import com.jira.migration.repository.ValidationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Applies active rows from validation_rules table (P4-01).
 */
@Service
@RequiredArgsConstructor
public class DbValidationRuleEngine {

    private final ValidationRuleRepository validationRuleRepository;

    public void applyRules(
            String entityType,
            Map<String, String> row,
            int rowNum,
            List<ValidationResult.ValidationError> errors,
            List<ValidationResult.ValidationWarning> warnings) {

        List<ValidationRule> rules = validationRuleRepository
                .findByEntityTypeAndIsActiveTrueOrderByDisplayOrderAsc(entityType.toUpperCase(Locale.ROOT));

        for (ValidationRule rule : rules) {
            String field = rule.getFieldName();
            String value = field != null ? row.get(field) : null;
            if (value == null && field != null) {
                value = row.get(field.toLowerCase(Locale.ROOT));
            }

            Map<String, Object> config = rule.getRuleConfig() != null ? rule.getRuleConfig() : Map.of();
            String template = rule.getErrorMessageTemplate() != null
                    ? rule.getErrorMessageTemplate()
                    : "Validation failed for " + field;

            switch (rule.getRuleType().toUpperCase(Locale.ROOT)) {
                case "REQUIRED" -> {
                    if (value == null || value.isBlank()) {
                        add(rule, rowNum, field, template, errors, warnings);
                    }
                }
                case "FORMAT" -> {
                    if (value != null && !value.isBlank() && config.get("pattern") != null) {
                        String pattern = config.get("pattern").toString();
                        if (!Pattern.matches(pattern, value)) {
                            add(rule, rowNum, field, template, errors, warnings);
                        }
                    }
                }
                case "RANGE" -> {
                    if (value != null && config.get("max") != null) {
                        try {
                            int max = Integer.parseInt(config.get("max").toString());
                            if (value.length() > max) {
                                add(rule, rowNum, field, template, errors, warnings);
                            }
                        } catch (NumberFormatException ignored) {
                            // skip
                        }
                    }
                }
                default -> {
                    // CUSTOM / FK handled by hardcoded engine for now
                }
            }
        }
    }

    private void add(
            ValidationRule rule,
            int rowNum,
            String field,
            String message,
            List<ValidationResult.ValidationError> errors,
            List<ValidationResult.ValidationWarning> warnings) {

        if ("WARNING".equalsIgnoreCase(rule.getSeverity())) {
            warnings.add(ValidationResult.ValidationWarning.builder()
                    .row(rowNum)
                    .field(field)
                    .message(message)
                    .warningCode(rule.getRuleName())
                    .build());
        } else {
            errors.add(ValidationResult.ValidationError.builder()
                    .row(rowNum)
                    .field(field)
                    .message(message)
                    .errorCode(rule.getRuleName())
                    .build());
        }
    }
}
