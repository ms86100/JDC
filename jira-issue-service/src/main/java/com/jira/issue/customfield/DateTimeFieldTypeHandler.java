package com.jira.issue.customfield;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Component
public class DateTimeFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public String getType() { return "datetime"; }

    @Override
    public String getDisplayName() { return "Date Time Picker"; }

    @Override
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null || value.toString().isBlank()) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        try {
            LocalDateTime.parse(value.toString(), FORMATTER);
            return ValidationResult.success(value);
        } catch (DateTimeParseException e) {
            try {
                LocalDateTime.parse(value.toString());
                return ValidationResult.success(value);
            } catch (DateTimeParseException e2) {
                return ValidationResult.error("Invalid datetime format. Use ISO format: yyyy-MM-ddTHH:mm:ss");
            }
        }
    }

    @Override
    public String toSearchableText(Object value, Map<String, Object> config) {
        return value != null ? value.toString() : "";
    }
}
