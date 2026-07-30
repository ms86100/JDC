package com.avionics_systems.issue.customfield;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Text area (multi-line) field type handler.
 * F7-US001: Custom Field Types
 */
@Component
public class TextAreaFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    @Override
    public String getType() {
        return "textarea";
    }

    @Override
    public String getDisplayName() {
        return "Text Area (Multi-Line)";
    }

    @Override
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null) {
            if (Boolean.TRUE.equals(config.get("required"))) {
                return ValidationResult.error("This field is required");
            }
            return ValidationResult.success();
        }

        String textValue = value.toString();

        // Check max length
        int maxLength = getMaxLength(config);
        if (maxLength > 0 && textValue.length() > maxLength) {
            return ValidationResult.error("Text exceeds maximum length of " + maxLength + " characters");
        }

        return ValidationResult.success(textValue);
    }

    @Override
    public String renderForDisplay(Object value, Map<String, Object> config) {
        if (value == null) {
            return "<span class=\"cf-empty\">-</span>";
        }
        // Convert newlines to <br> for display
        String formatted = escapeHtml(value.toString())
                .replace("\n", "<br>")
                .replace("\r", "");
        return "<div class=\"cf-textarea\">" + formatted + "</div>";
    }

    @Override
    public String renderForEdit(Object value, Map<String, Object> config) {
        String inputId = getInputId(config);
        String inputName = getInputName(config);
        String currentValue = value != null ? escapeHtml(value.toString()) : "";
        String cssClass = getCssClass(config) + " cf-textarea-editor";
        boolean required = isRequired(config);
        int maxLength = getMaxLength(config);
        int rows = getRows(config);

        StringBuilder html = new StringBuilder();
        html.append(String.format(
                "<textarea id=\"%s\" name=\"%s\" class=\"%s\" rows=\"%d\"",
                inputId, inputName, cssClass, rows
        ));

        if (required) {
            html.append(" required");
        }
        if (maxLength > 0) {
            html.append(" maxlength=\"").append(maxLength).append("\"");
        }
        html.append(">").append(currentValue).append("</textarea>");

        return html.toString();
    }

    private int getRows(Map<String, Object> config) {
        if (config != null && config.containsKey("rows")) {
            Object rows = config.get("rows");
            if (rows instanceof Number) {
                return ((Number) rows).intValue();
            }
        }
        return 5; // Default rows
    }

    @Override
    public String toSearchableText(Object value, Map<String, Object> config) {
        if (value == null) {
            return "";
        }
        // Remove extra whitespace for better search
        return value.toString()
                .replace("\n", " ")
                .replace("\r", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }
}