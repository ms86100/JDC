package com.jira.issue.customfield;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LabelsFieldTypeHandler extends AbstractCustomFieldTypeHandler {

    @Override
    public String getType() { return "labels"; }

    @Override
    public String getDisplayName() { return "Labels"; }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(Object value, Map<String, Object> config) {
        if (value == null) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        if (value instanceof List) {
            List<Object> labels = (List<Object>) value;
            for (Object label : labels) {
                if (label == null || label.toString().isBlank()) {
                    return ValidationResult.error("Label values cannot be empty");
                }
            }
            return ValidationResult.success(labels);
        }
        if (value.toString().isBlank()) {
            return isRequired(config) ? ValidationResult.error("Value is required") : ValidationResult.success();
        }
        return ValidationResult.success(List.of(value.toString()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String toSearchableText(Object value, Map<String, Object> config) {
        if (value == null) return "";
        if (value instanceof List) {
            return ((List<Object>) value).stream()
                    .map(v -> v.toString().toLowerCase())
                    .collect(Collectors.joining(" "));
        }
        return value.toString().toLowerCase();
    }
}
