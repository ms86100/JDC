package com.jira.issue.customfield;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Text field type handler for single-line text custom fields.
 * F7-US001: Custom Field Types
 */
@Component
public class TextFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    private static final Pattern TEXT_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}\\p{S}]*$");

    @Override
    public String getType() {
        return "textfield";
    }

    @Override
    public String getDisplayName() {
        return "Text Field (Single Line)";
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

        // Validate characters
        if (!TEXT_PATTERN.matcher(textValue).matches()) {
            return ValidationResult.error("Text contains invalid characters");
        }

        return ValidationResult.success(textValue);
    }

    @Override
    public String renderForEdit(Object value, Map<String, Object> config) {
        String inputId = getInputId(config);
        String inputName = getInputName(config);
        String currentValue = value != null ? escapeHtml(value.toString()) : "";
        String placeholder = getPlaceholder(config);
        String cssClass = getCssClass(config);
        boolean required = isRequired(config);
        int maxLength = getMaxLength(config);

        StringBuilder html = new StringBuilder();
        html.append(String.format(
                "<input type=\"text\" id=\"%s\" name=\"%s\" value=\"%s\" placeholder=\"%s\" class=\"%s\"",
                inputId, inputName, currentValue, placeholder, cssClass
        ));

        if (required) {
            html.append(" required");
        }
        if (maxLength > 0) {
            html.append(" maxlength=\"").append(maxLength).append("\"");
        }
        html.append(" />");

        return html.toString();
    }

    @Override
    public String toSearchableText(Object value, Map<String, Object> config) {
        if (value == null) {
            return "";
        }
        return value.toString().toLowerCase();
    }
}