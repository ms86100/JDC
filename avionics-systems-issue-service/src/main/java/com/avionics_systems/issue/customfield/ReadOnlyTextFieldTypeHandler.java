package com.avionics_systems.issue.customfield;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReadOnlyTextFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    @Override
    public String getType() { return "readonlytext"; }

    @Override
    public String getDisplayName() { return "Read Only Text"; }

    @Override
    public ValidationResult validate(Object value, Map<String, Object> config) {
        return ValidationResult.success(value);
    }

    @Override
    public String renderForEdit(Object value, Map<String, Object> config) {
        String display = value != null ? escapeHtml(value.toString()) : "";
        return String.format("<span class=\"cf-readonly\">%s</span>", display);
    }

    @Override
    public Object parseInput(Object input, Map<String, Object> config) {
        return null;
    }

    @Override
    public String toSearchableText(Object value, Map<String, Object> config) {
        return value != null ? value.toString().toLowerCase() : "";
    }
}
