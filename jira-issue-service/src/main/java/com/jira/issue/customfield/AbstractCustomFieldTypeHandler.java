package com.jira.issue.customfield;

import java.util.Map;

/**
 * Abstract base class for custom field type handlers providing common functionality.
 * F7-US001: Custom Field Types
 */
public abstract class AbstractCustomFieldTypeHandler implements CustomFieldTypeHandler {

    @Override
    public String renderForDisplay(Object value, Map<String, Object> config) {
        if (value == null) {
            return "<span class=\"cf-empty\">-</span>";
        }
        return escapeHtml(value.toString());
    }

    @Override
    public String renderForEdit(Object value, Map<String, Object> config) {
        String inputId = getInputId(config);
        String inputName = getInputName(config);
        String currentValue = value != null ? escapeHtml(value.toString()) : "";
        String placeholder = getPlaceholder(config);
        String cssClass = getCssClass(config);
        boolean required = isRequired(config);

        return String.format(
                "<input type=\"text\" id=\"%s\" name=\"%s\" value=\"%s\" placeholder=\"%s\" class=\"%s\" %s />",
                inputId, inputName, currentValue, placeholder, cssClass, required ? "required" : ""
        );
    }

    @Override
    public Object parseInput(Object input, Map<String, Object> config) {
        if (input == null) {
            return null;
        }
        return input.toString().trim();
    }

    @Override
    public Object toJsonValue(Object value, Map<String, Object> config) {
        return value;
    }

    @Override
    public Object fromJsonValue(Object jsonValue, Map<String, Object> config) {
        return jsonValue;
    }

    /**
     * Escape HTML special characters for safe rendering
     */
    protected String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Get the input ID from configuration
     */
    protected String getInputId(Map<String, Object> config) {
        if (config != null && config.containsKey("fieldId")) {
            return "customfield_" + config.get("fieldId");
        }
        return "customfield_input";
    }

    /**
     * Get the input name from configuration
     */
    protected String getInputName(Map<String, Object> config) {
        if (config != null && config.containsKey("fieldKey")) {
            return "customfield_" + config.get("fieldKey");
        }
        return "customfield";
    }

    /**
     * Get placeholder text from configuration
     */
    protected String getPlaceholder(Map<String, Object> config) {
        if (config != null && config.containsKey("placeholder")) {
            return escapeHtml(config.get("placeholder").toString());
        }
        return "";
    }

    /**
     * Get CSS class from configuration
     */
    protected String getCssClass(Map<String, Object> config) {
        StringBuilder css = new StringBuilder("jira-form-field custom-field");
        if (config != null && config.containsKey("cssClass")) {
            css.append(" ").append(config.get("cssClass"));
        }
        return css.toString();
    }

    /**
     * Check if field is required from configuration
     */
    protected boolean isRequired(Map<String, Object> config) {
        if (config != null && config.containsKey("required")) {
            return Boolean.TRUE.equals(config.get("required"));
        }
        return false;
    }

    /**
     * Get maximum length from configuration
     */
    protected int getMaxLength(Map<String, Object> config) {
        if (config != null && config.containsKey("maxLength")) {
            Object maxLength = config.get("maxLength");
            if (maxLength instanceof Number) {
                return ((Number) maxLength).intValue();
            }
        }
        return -1; // No limit
    }

    /**
     * Get minimum value from configuration (for numeric fields)
     */
    protected Number getMinValue(Map<String, Object> config) {
        if (config != null && config.containsKey("minValue")) {
            return (Number) config.get("minValue");
        }
        return null;
    }

    /**
     * Get maximum value from configuration (for numeric fields)
     */
    protected Number getMaxValue(Map<String, Object> config) {
        if (config != null && config.containsKey("maxValue")) {
            return (Number) config.get("maxValue");
        }
        return null;
    }
}