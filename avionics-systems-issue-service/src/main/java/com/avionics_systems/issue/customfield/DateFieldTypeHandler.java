package com.avionics_systems.issue.customfield;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Date field type handler for date custom fields.
 * F7-US001: Custom Field Types
 */
@Component
public class DateFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    @Override
    public String getType() {
        return "date";
    }

    @Override
    public String getDisplayName() {
        return "Date Field";
    }

    @Override
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null) {
            if (Boolean.TRUE.equals(config.get("required"))) {
                return ValidationResult.error("This field is required");
            }
            return ValidationResult.success();
        }

        String dateStr = value.toString().trim();

        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);

            // Check date range if configured
            LocalDate minDate = getMinDate(config);
            if (minDate != null && date.isBefore(minDate)) {
                return ValidationResult.error("Date must be on or after " + minDate.format(DISPLAY_FORMATTER));
            }

            LocalDate maxDate = getMaxDate(config);
            if (maxDate != null && date.isAfter(maxDate)) {
                return ValidationResult.error("Date must be on or before " + maxDate.format(DISPLAY_FORMATTER));
            }

            return ValidationResult.success(date);

        } catch (DateTimeParseException e) {
            return ValidationResult.error("Invalid date format. Use YYYY-MM-DD");
        }
    }

    @Override
    public String renderForDisplay(Object value, Map<String, Object> config) {
        if (value == null) {
            return "<span class=\"cf-empty\">-</span>";
        }

        LocalDate date = parseDate(value);
        if (date == null) {
            return escapeHtml(value.toString());
        }

        return "<time class=\"cf-date\" datetime=\"" + date.format(DATE_FORMATTER) + "\">" +
                date.format(DISPLAY_FORMATTER) + "</time>";
    }

    @Override
    public String renderForEdit(Object value, Map<String, Object> config) {
        String inputId = getInputId(config);
        String inputName = getInputName(config);
        String currentValue = value != null ? formatDateValue(value) : "";
        String cssClass = getCssClass(config) + " cf-datepicker";
        boolean required = isRequired(config);

        StringBuilder html = new StringBuilder();
        html.append(String.format(
                "<input type=\"date\" id=\"%s\" name=\"%s\" value=\"%s\" class=\"%s\"",
                inputId, inputName, currentValue, cssClass
        ));

        if (required) {
            html.append(" required");
        }

        html.append(" />");

        // Add date format hint
        html.append("<span class=\"cf-date-format-hint\">Format: YYYY-MM-DD</span>");

        return html.toString();
    }

    @Override
    public Object parseInput(Object input, Map<String, Object> config) {
        if (input == null || input.toString().trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(input.toString().trim(), DATE_FORMATTER);
    }

    @Override
    public Object toJsonValue(Object value, Map<String, Object> config) {
        if (value == null) {
            return null;
        }
        LocalDate date = parseDate(value);
        return date != null ? date.format(DATE_FORMATTER) : value.toString();
    }

    private LocalDate parseDate(Object value) {
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        try {
            return LocalDate.parse(value.toString(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String formatDateValue(Object value) {
        LocalDate date = parseDate(value);
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private LocalDate getMinDate(Map<String, Object> config) {
        if (config != null && config.containsKey("minDate")) {
            try {
                return LocalDate.parse(config.get("minDate").toString(), DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private LocalDate getMaxDate(Map<String, Object> config) {
        if (config != null && config.containsKey("maxDate")) {
            try {
                return LocalDate.parse(config.get("maxDate").toString(), DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    @Override
    public String toSearchableText(Object value, Map<String, Object> config) {
        LocalDate date = parseDate(value);
        return date != null ? date.format(DATE_FORMATTER) : "";
    }
}