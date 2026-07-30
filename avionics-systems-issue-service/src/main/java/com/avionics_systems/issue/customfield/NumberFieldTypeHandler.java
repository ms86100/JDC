package com.avionics_systems.issue.customfield;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Number field type handler for numeric custom fields.
 * F7-US001: Custom Field Types
 */
@Component
public class NumberFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    @Override
    public String getType() {
        return "number";
    }

    @Override
    public String getDisplayName() {
        return "Number Field";
    }

    @Override
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null) {
            if (Boolean.TRUE.equals(config.get("required"))) {
                return ValidationResult.error("This field is required");
            }
            return ValidationResult.success();
        }

        String strValue = value.toString().trim();

        // Check if it's a valid number format
        if (!NUMBER_PATTERN.matcher(strValue).matches()) {
            return ValidationResult.error("Invalid number format");
        }

        try {
            double numValue = Double.parseDouble(strValue);

            // Check min value
            Number minValue = getMinValue(config);
            if (minValue != null && numValue < minValue.doubleValue()) {
                return ValidationResult.error("Value must be at least " + minValue);
            }

            // Check max value
            Number maxValue = getMaxValue(config);
            if (maxValue != null && numValue > maxValue.doubleValue()) {
                return ValidationResult.error("Value must be at most " + maxValue);
            }

            // Handle decimal precision
            int precision = getDecimalPrecision(config);
            if (precision == 0) {
                long longValue = (long) numValue;
                return ValidationResult.success(longValue);
            }

            // Round to precision
            double rounded = Math.round(numValue * Math.pow(10, precision)) / Math.pow(10, precision);
            return ValidationResult.success(rounded);

        } catch (NumberFormatException e) {
            return ValidationResult.error("Invalid number format");
        }
    }

    @Override
    public String renderForEdit(Object value, Map<String, Object> config) {
        String inputId = getInputId(config);
        String inputName = getInputName(config);
        String currentValue = value != null ? value.toString() : "";
        String placeholder = getPlaceholder(config);
        String cssClass = getCssClass(config);
        boolean required = isRequired(config);

        StringBuilder html = new StringBuilder();
        html.append(String.format(
                "<input type=\"number\" id=\"%s\" name=\"%s\" value=\"%s\" placeholder=\"%s\" class=\"%s\"",
                inputId, inputName, currentValue, placeholder, cssClass
        ));

        if (required) {
            html.append(" required");
        }

        Number minValue = getMinValue(config);
        if (minValue != null) {
            html.append(" min=\"").append(minValue).append("\"");
        }

        Number maxValue = getMaxValue(config);
        if (maxValue != null) {
            html.append(" max=\"").append(maxValue).append("\"");
        }

        int precision = getDecimalPrecision(config);
        if (precision > 0) {
            html.append(" step=\"0.").append("0".repeat(precision - 1)).append("1\"");
        }

        html.append(" />");
        return html.toString();
    }

    @Override
    public Object parseInput(Object input, Map<String, Object> config) {
        if (input == null || input.toString().trim().isEmpty()) {
            return null;
        }
        return Double.parseDouble(input.toString().trim());
    }

    @Override
    public Object toJsonValue(Object value, Map<String, Object> config) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return value;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int getDecimalPrecision(Map<String, Object> config) {
        if (config != null && config.containsKey("decimalPrecision")) {
            Object precision = config.get("decimalPrecision");
            if (precision instanceof Number) {
                return ((Number) precision).intValue();
            }
        }
        return 2; // Default precision
    }

    @Override
    public String toSearchableText(Object value, Map<String, Object> config) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}